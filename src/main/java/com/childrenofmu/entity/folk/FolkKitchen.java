package com.childrenofmu.entity.folk;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.childrenofmu.registry.ModItems;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The Chitinier's restaurant. Right-click the Chitinier (the chef in the tall toque) for tonight's menu in chat; click
 * a dish, pay its (outrageous) price in emeralds, and the Cooks nearby rush between the stoves, pots and cutting
 * boards until it's ready. Nothing is cooked ahead: every dish is made fresh to order, then handed over (or left on
 * the pass if you've wandered off). Frog Folk eat here too: now and then a frog nearby places an order of its own.
 */
public final class FolkKitchen {
    /** One dish on the menu: what it is, its price in emeralds, and how long the kitchen takes over it (ticks). */
    public record Dish(String id, Supplier<? extends Item> item, int price, int ticks) {}

    public static final List<Dish> MENU = List.of(
            new Dish("honey_glazed_beetle", ModItems.HONEY_GLAZED_BEETLE, 12, 400),
            new Dish("cricket_crumble", ModItems.CRICKET_CRUMBLE, 10, 300),
            new Dish("mosquito_consomme", ModItems.MOSQUITO_CONSOMME, 14, 500),
            new Dish("dragonfly_tartare", ModItems.DRAGONFLY_TARTARE, 16, 400),
            new Dish("firefly_flambe", ModItems.FIREFLY_FLAMBE, 18, 600),
            new Dish("moth_souffle", ModItems.MOTH_SOUFFLE, 20, 800),
            new Dish("bog_snail_escargot", ModItems.BOG_SNAIL_ESCARGOT, 22, 600));

    /** An order in the kitchen: the dish, ticks left, and who it's for (a player, or a frog customer). */
    public static final class Order {
        final String dish;
        int ticksLeft;
        final UUID customer;
        final boolean player;

        Order(String dish, int ticksLeft, UUID customer, boolean player) {
            this.dish = dish;
            this.ticksLeft = ticksLeft;
            this.customer = customer;
            this.player = player;
        }
    }

    /** Cooks work for a chef this close; kitchen stations are looked for this close to the chef. */
    private static final double KITCHEN = 16.0;
    private static final int MAX_ORDERS = 4;
    /** Without any Cooks the chef cooks alone, at a third of the speed. */
    private static final int ALONE_SLOWDOWN = 3;

    private FolkKitchen() {}

    private static Dish dish(String id) {
        for (Dish d : MENU) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    public static boolean isChef(FolkEntity e) {
        return e.getRace() == FolkRace.FROG && "chitinier".equals(e.getVariantName());
    }

    public static boolean isCook(FolkEntity e) {
        return e.getRace() == FolkRace.FROG && "cook".equals(e.getVariantName());
    }

    // ---- The menu ---------------------------------------------------------------------------

    /** A player right-clicked the chef. */
    public static void offer(FolkEntity chef, Player player) {
        MutableComponent menu = Component.translatable("kitchen.childrenofmu.menu", chef.getDisplayName());
        for (Dish d : MENU) {
            Component name = new ItemStack(d.item().get()).getHoverName();
            Style style = Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/childrenofmu order " + chef.getId() + " " + MENU.indexOf(d)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("kitchen.childrenofmu.hover", d.price(), Math.max(1, d.ticks() / 20))));
            menu.append("\n ").append(Component.literal("[").append(name)
                    .append(" — " + d.price() + " emeralds]").withStyle(style));
        }
        player.sendSystemMessage(menu);
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("childrenofmu")
                .then(Commands.literal("order")
                        .then(Commands.argument("chef", IntegerArgumentType.integer())
                                .then(Commands.argument("dish", IntegerArgumentType.integer(0, MENU.size() - 1))
                                        .executes(FolkKitchen::order)))));
    }

    private static int order(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        Entity e = player.serverLevel().getEntity(IntegerArgumentType.getInteger(ctx, "chef"));
        Dish d = MENU.get(IntegerArgumentType.getInteger(ctx, "dish"));
        if (!(e instanceof FolkEntity chef) || !isChef(chef) || chef.distanceTo(player) > 10.0F) {
            player.sendSystemMessage(Component.translatable("kitchen.childrenofmu.gone"));
            return 0;
        }
        if (chef.orders.size() >= MAX_ORDERS) {
            player.sendSystemMessage(Component.translatable("kitchen.childrenofmu.busy", chef.getDisplayName()));
            return 0;
        }
        int have = player.getInventory().countItem(Items.EMERALD);
        if (have < d.price()) {
            player.sendSystemMessage(Component.translatable("kitchen.childrenofmu.poor", chef.getDisplayName(), d.price()));
            return 0;
        }
        player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), d.price(),
                player.inventoryMenu.getCraftSlots());
        chef.orders.add(new Order(d.id(), d.ticks(), player.getUUID(), true));
        player.sendSystemMessage(Component.translatable("kitchen.childrenofmu.ordered", chef.getDisplayName(),
                new ItemStack(d.item().get()).getHoverName()));
        chef.level().playSound(null, chef.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.8F, 1.6F);
        return 1;
    }

    // ---- The kitchen at work ------------------------------------------------------------------

    /** Every tick, on the chef. */
    public static void tickChef(FolkEntity chef) {
        if (!(chef.level() instanceof ServerLevel level)) {
            return;
        }
        int cooks = level.getEntitiesOfClass(FolkEntity.class, chef.getBoundingBox().inflate(KITCHEN), FolkKitchen::isCook).size();
        // A frog in town fancies dinner now and then.
        if (chef.orders.isEmpty() && chef.getRandom().nextInt(3600) == 0) {
            List<FolkEntity> diners = level.getEntitiesOfClass(FolkEntity.class, chef.getBoundingBox().inflate(24.0),
                    f -> f.getRace() == FolkRace.FROG && !isChef(f) && !isCook(f) && !"froglet".equals(f.getVariantName()));
            if (!diners.isEmpty()) {
                FolkEntity diner = diners.get(chef.getRandom().nextInt(diners.size()));
                Dish d = MENU.get(chef.getRandom().nextInt(MENU.size()));
                chef.orders.add(new Order(d.id(), d.ticks(), diner.getUUID(), false));
            }
        }
        if (chef.orders.isEmpty()) {
            return;
        }
        Order o = chef.orders.get(0);
        if (cooks > 0 || level.getGameTime() % ALONE_SLOWDOWN == 0) {
            o.ticksLeft--;
        }
        if (o.ticksLeft % 40 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, chef.getX(), chef.getY() + 1.6, chef.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
        }
        if (o.ticksLeft <= 0) {
            chef.orders.remove(0);
            serve(level, chef, o);
        }
    }

    private static void serve(ServerLevel level, FolkEntity chef, Order o) {
        Dish d = dish(o.dish);
        if (d == null) {
            return;
        }
        ItemStack plate = new ItemStack(d.item().get());
        level.playSound(null, chef.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1.0F, 2.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, chef.getX(), chef.getY() + 1.2, chef.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
        if (o.player) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(o.customer);
            if (p != null && p.level() == level && p.distanceTo(chef) < 24.0F) {
                p.sendSystemMessage(Component.translatable("kitchen.childrenofmu.served", chef.getDisplayName(), plate.getHoverName()));
                if (!p.getInventory().add(plate)) {
                    p.drop(plate, false);
                }
            } else {
                // Left on the pass for whenever they come back.
                level.addFreshEntity(new ItemEntity(level, chef.getX(), chef.getY() + 0.5, chef.getZ(), plate));
            }
        } else if (level.getEntity(o.customer) instanceof FolkEntity diner && diner.isAlive()) {
            // The diner eats on the spot, noisily.
            diner.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            level.playSound(null, diner.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, plate),
                    diner.getX(), diner.getY() + 1.0, diner.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, diner.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.8F, 1.4F);
        }
    }

    /** True if a chef close by has orders on: the Cooks get to work. */
    static FolkEntity busyChef(FolkEntity cook) {
        for (FolkEntity f : cook.level().getEntitiesOfClass(FolkEntity.class, cook.getBoundingBox().inflate(KITCHEN), FolkKitchen::isChef)) {
            if (!f.orders.isEmpty()) {
                return f;
            }
        }
        return null;
    }

    /** Blocks a Cook will work at. */
    static boolean isStation(net.minecraft.world.level.block.state.BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id.contains("stove") || id.contains("cooking_pot") || id.contains("cutting_board") || id.contains("skillet")
                || id.equals("smoker") || id.equals("furnace") || id.contains("cauldron") || id.equals("crafting_table")
                || id.contains("basin") || id.contains("blaze_burner");
    }

    /** The Cooks' job: while there are orders, rush from station to station, chopping and stirring. */
    static class CookGoal extends Goal {
        private final FolkEntity cook;
        private BlockPos station;
        private int working;
        private int giveUp;

        CookGoal(FolkEntity cook) {
            this.cook = cook;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isCook(this.cook) && busyChef(this.cook) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            this.station = null;
            this.cook.getNavigation().stop();
            this.cook.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }

        private void pickStation() {
            FolkEntity chef = busyChef(this.cook);
            if (chef == null) {
                return;
            }
            List<BlockPos> found = new ArrayList<>();
            BlockPos c = chef.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -3, -8), c.offset(8, 3, 8))) {
                if (isStation(this.cook.level().getBlockState(p)) && !p.equals(this.station)) {
                    found.add(p.immutable());
                }
            }
            this.station = found.isEmpty() ? null : found.get(this.cook.getRandom().nextInt(found.size()));
            this.working = 0;
            this.giveUp = 160;
            if (this.station != null) {
                this.cook.getNavigation().moveTo(this.station.getX() + 0.5, this.station.getY(), this.station.getZ() + 0.5, 1.15);
            }
        }

        @Override
        public void tick() {
            if (this.station == null || --this.giveUp <= 0) {
                pickStation();
                if (this.station == null) {
                    return;
                }
            }
            this.cook.getLookControl().setLookAt(this.station.getX() + 0.5, this.station.getY() + 0.5, this.station.getZ() + 0.5);
            double d = this.cook.distanceToSqr(this.station.getX() + 0.5, this.station.getY(), this.station.getZ() + 0.5);
            if (d > 4.0) {
                if (this.cook.getNavigation().isDone()) {
                    this.cook.getNavigation().moveTo(this.station.getX() + 0.5, this.station.getY(), this.station.getZ() + 0.5, 1.15);
                }
                return;
            }
            this.cook.getNavigation().stop();
            this.working++;
            if (this.working % 8 == 0) {
                this.cook.swing(InteractionHand.MAIN_HAND);
                if (this.cook.level() instanceof ServerLevel level) {
                    level.sendParticles(this.cook.getRandom().nextBoolean() ? ParticleTypes.SMOKE : ParticleTypes.CRIT,
                            this.station.getX() + 0.5, this.station.getY() + 1.1, this.station.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.01);
                    level.playSound(null, this.station, this.cook.getRandom().nextBoolean()
                                    ? SoundEvents.AXE_STRIP : SoundEvents.FIRE_AMBIENT,
                            SoundSource.NEUTRAL, 0.5F, 1.2F + this.cook.getRandom().nextFloat() * 0.4F);
                }
            }
            if (this.working > 40 + this.cook.getRandom().nextInt(40)) {
                pickStation();                       // on to the next station
            }
        }
    }

    // ---- Saving the chef's tickets -----------------------------------------------------------

    static void save(FolkEntity chef, CompoundTag tag) {
        if (chef.orders.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Order o : chef.orders) {
            CompoundTag t = new CompoundTag();
            t.putString("Dish", o.dish);
            t.putInt("Ticks", o.ticksLeft);
            t.putUUID("Customer", o.customer);
            t.putBoolean("Player", o.player);
            list.add(t);
        }
        tag.put("Orders", list);
    }

    static void load(FolkEntity chef, CompoundTag tag) {
        chef.orders.clear();
        for (Tag t : tag.getList("Orders", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (c.hasUUID("Customer")) {
                chef.orders.add(new Order(c.getString("Dish"), c.getInt("Ticks"), c.getUUID("Customer"), c.getBoolean("Player")));
            }
        }
    }
}
