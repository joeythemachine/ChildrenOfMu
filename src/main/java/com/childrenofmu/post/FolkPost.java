package com.childrenofmu.post;

import java.util.List;

import com.childrenofmu.entity.folk.FolkEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The Jumper couriers' parcel service. Hand a courier something and it offers every town whose post box you've opened;
 * click one in chat and, for one emerald, the parcel is on its way. It lands in that town's post box after a while
 * (about a minute, plus a minute for every 360 blocks or so).
 */
public final class FolkPost {
    /** The courier flies about 6 blocks a second. */
    private static final double BLOCKS_PER_TICK = 0.3;
    private static final long MIN_TRIP = 1200, MAX_TRIP = 24000;
    /** Post boxes this close to the courier count as "here", not somewhere to send to. */
    private static final double LOCAL = 48.0;

    /** A player right-clicked a courier. */
    public static void offer(FolkEntity courier, Player player) {
        if (!(courier.level() instanceof ServerLevel level)) {
            return;
        }
        Component name = courier.getDisplayName();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("post.childrenofmu.explain", name));
            return;
        }
        PostOffice office = PostOffice.get(level.getServer());
        String dim = level.dimension().location().toString();
        List<PostOffice.Box> towns = office.visitedBy(player.getUUID(), dim, courier.blockPosition()).stream()
                .filter(b -> !b.dimension.equals(dim) || b.pos.distSqr(courier.blockPosition()) > LOCAL * LOCAL)
                .toList();
        if (towns.isEmpty()) {
            player.sendSystemMessage(Component.translatable("post.childrenofmu.nowhere", name));
            return;
        }
        MutableComponent line = Component.translatable("post.childrenofmu.where", name, held.getHoverName());
        for (PostOffice.Box b : towns) {
            long minutes = Math.max(1, trip(courier, b) / 1200);
            Style style = Style.EMPTY.withColor(ChatFormatting.GREEN).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/childrenofmu post " + courier.getId() + " " + b.id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("post.childrenofmu.hover", b.town, minutes)));
            line.append(" ").append(Component.literal("[" + b.town + "]").withStyle(style));
        }
        player.sendSystemMessage(line);
    }

    private static long trip(Entity courier, PostOffice.Box to) {
        double distance = Math.sqrt(to.pos.distSqr(courier.blockPosition()));
        return Math.min(MAX_TRIP, MIN_TRIP + (long) (distance / BLOCKS_PER_TICK));
    }

    // ---- The command the chat links run --------------------------------------------

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("childrenofmu")
                .then(Commands.literal("post")
                        .then(Commands.argument("courier", IntegerArgumentType.integer())
                                .then(Commands.argument("box", IntegerArgumentType.integer())
                                        .executes(FolkPost::send)))));
    }

    private static int send(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(IntegerArgumentType.getInteger(ctx, "courier"));
        PostOffice office = PostOffice.get(level.getServer());
        PostOffice.Box box = office.box(IntegerArgumentType.getInteger(ctx, "box"));
        if (!(e instanceof FolkEntity courier) || !"jumper".equals(courier.getVariantName()) || box == null
                || courier.distanceTo(player) > 8.0F) {
            player.sendSystemMessage(Component.translatable("post.childrenofmu.gone"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("post.childrenofmu.explain", courier.getDisplayName()));
            return 0;
        }
        // The fee: one emerald from anywhere in your inventory (not the stack being sent).
        ItemStack parcel = held.copy();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        int paid = player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.EMERALD), 1,
                player.inventoryMenu.getCraftSlots());
        if (paid < 1) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, parcel);
            player.sendSystemMessage(Component.translatable("post.childrenofmu.fee", courier.getDisplayName()));
            return 0;
        }
        long arrives = level.getGameTime() + trip(courier, box);
        office.send(box, parcel, arrives, player.getGameProfile().getName());
        long minutes = Math.max(1, trip(courier, box) / 1200);
        player.sendSystemMessage(Component.translatable("post.childrenofmu.sent", courier.getDisplayName(),
                parcel.getHoverName(), box.town, minutes));
        level.playSound(null, courier.getX(), courier.getY(), courier.getZ(), SoundEvents.BUNDLE_INSERT,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        courier.getJumpControl().jump();
        return 1;
    }

    // ---- Deliveries -------------------------------------------------------------------

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            PostOffice.get(event.getServer()).deliver(event.getServer().overworld().getGameTime());
        }
    }

    private FolkPost() {}
}
