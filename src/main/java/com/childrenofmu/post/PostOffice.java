package com.childrenofmu.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every Frog town post box in the world, what's inside each, which boxes each player has visited, and the parcels the
 * couriers are carrying. Kept with the world's save, so deliveries arrive even when nobody is near the box.
 */
public class PostOffice extends SavedData {
    private static final String NAME = "childrenofmu_post";
    public static final int BOX_SIZE = 27;

    private static final String[] TOWN_NAMES = {
            "Mossbottom", "Reedholm", "Croakwater", "Lilyford", "Bogsworth", "Mudpuddle", "Ribbet Rill", "Cattail Cross",
            "Duckweed Hollow", "Peatmarsh", "Fenwick", "Tadpole Bend", "Newtley", "Sedgefield", "Murkwell", "Bulrush Bay",
            "Willowmire", "Greenpool", "Sloughton", "Pondsby", "Hopsdown", "Lilypad Landing", "Marshmallow Fen", "Gloop"};

    /** One post box: where it is, its town's name, and what's waiting in it. */
    public final class Box {
        public final int id;
        public final String dimension;
        public final BlockPos pos;
        public final String town;
        public final SimpleContainer contents = new SimpleContainer(BOX_SIZE);

        Box(int id, String dimension, BlockPos pos, String town) {
            this.id = id;
            this.dimension = dimension;
            this.pos = pos;
            this.town = town;
            this.contents.addListener(c -> setDirty());
        }
    }

    private record Parcel(int box, ItemStack stack, long arrives, String from) {}

    private final Map<Integer, Box> boxes = new HashMap<>();
    private final Map<String, Integer> byPlace = new HashMap<>();
    private final Map<UUID, Set<Integer>> visited = new HashMap<>();
    private final List<Parcel> parcels = new ArrayList<>();
    private int nextId = 1;

    public static PostOffice get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PostOffice::new, PostOffice::load, null), NAME);
    }

    private static String place(String dimension, BlockPos pos) {
        return dimension + "|" + pos.asLong();
    }

    /** The box at this spot, registering it (and naming its town) the first time anyone opens it. */
    public Box boxAt(ResourceLocation dimension, BlockPos pos, RandomSource random) {
        String key = place(dimension.toString(), pos);
        Integer id = this.byPlace.get(key);
        if (id != null) {
            return this.boxes.get(id);
        }
        Set<String> used = new HashSet<>();
        this.boxes.values().forEach(b -> used.add(b.town));
        String town = TOWN_NAMES[random.nextInt(TOWN_NAMES.length)];
        for (int tries = 0; used.contains(town) && tries < 40; tries++) {
            town = TOWN_NAMES[random.nextInt(TOWN_NAMES.length)];
        }
        if (used.contains(town)) {
            town = town + " " + (this.nextId);
        }
        Box box = new Box(this.nextId++, dimension.toString(), pos.immutable(), town);
        this.boxes.put(box.id, box);
        this.byPlace.put(key, box.id);
        setDirty();
        return box;
    }

    @Nullable
    public Box box(int id) {
        return this.boxes.get(id);
    }

    public void visit(UUID player, Box box) {
        if (this.visited.computeIfAbsent(player, k -> new HashSet<>()).add(box.id)) {
            setDirty();
        }
    }

    /** The boxes this player has opened, nearest first. */
    public List<Box> visitedBy(UUID player, String dimension, BlockPos from) {
        List<Box> out = new ArrayList<>();
        for (int id : this.visited.getOrDefault(player, Set.of())) {
            Box b = this.boxes.get(id);
            if (b != null) {
                out.add(b);
            }
        }
        out.sort((a, b) -> Double.compare(a.pos.distSqr(from), b.pos.distSqr(from)));
        return out;
    }

    /** The names of every town with a post box anyone has opened (for gossip from afar). */
    public List<String> towns() {
        List<String> out = new ArrayList<>();
        this.boxes.values().forEach(b -> out.add(b.town));
        return out;
    }

    public void send(Box to, ItemStack stack, long arrives, String from) {
        this.parcels.add(new Parcel(to.id, stack.copy(), arrives, from));
        setDirty();
    }

    /** Drops every parcel that has arrived into its box (keeping what doesn't fit for later). */
    public void deliver(long now) {
        boolean changed = false;
        for (Iterator<Parcel> it = this.parcels.iterator(); it.hasNext(); ) {
            Parcel p = it.next();
            if (p.arrives() > now) {
                continue;
            }
            Box box = this.boxes.get(p.box());
            if (box == null) {
                it.remove();
                changed = true;
                continue;
            }
            ItemStack rest = box.contents.addItem(p.stack().copy());
            if (rest.isEmpty()) {
                it.remove();
            } else {
                p.stack().setCount(rest.getCount());
            }
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    // ---- Saving -------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextId", this.nextId);
        ListTag boxList = new ListTag();
        for (Box b : this.boxes.values()) {
            CompoundTag bt = new CompoundTag();
            bt.putInt("Id", b.id);
            bt.putString("Dim", b.dimension);
            bt.putLong("Pos", b.pos.asLong());
            bt.putString("Town", b.town);
            NonNullList<ItemStack> items = NonNullList.withSize(BOX_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < BOX_SIZE; i++) {
                items.set(i, b.contents.getItem(i));
            }
            ContainerHelper.saveAllItems(bt, items, registries);
            boxList.add(bt);
        }
        tag.put("Boxes", boxList);
        CompoundTag visits = new CompoundTag();
        this.visited.forEach((uuid, ids) -> visits.put(uuid.toString(),
                new IntArrayTag(ids.stream().mapToInt(Integer::intValue).toArray())));
        tag.put("Visited", visits);
        ListTag parcelList = new ListTag();
        for (Parcel p : this.parcels) {
            CompoundTag pt = new CompoundTag();
            pt.putInt("Box", p.box());
            pt.put("Item", p.stack().save(registries));
            pt.putLong("Arrives", p.arrives());
            pt.putString("From", p.from());
            parcelList.add(pt);
        }
        tag.put("Parcels", parcelList);
        return tag;
    }

    private static PostOffice load(CompoundTag tag, HolderLookup.Provider registries) {
        PostOffice office = new PostOffice();
        office.nextId = Math.max(1, tag.getInt("NextId"));
        for (Tag t : tag.getList("Boxes", Tag.TAG_COMPOUND)) {
            CompoundTag bt = (CompoundTag) t;
            Box b = office.new Box(bt.getInt("Id"), bt.getString("Dim"), BlockPos.of(bt.getLong("Pos")), bt.getString("Town"));
            NonNullList<ItemStack> items = NonNullList.withSize(BOX_SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(bt, items, registries);
            for (int i = 0; i < BOX_SIZE; i++) {
                b.contents.setItem(i, items.get(i));
            }
            office.boxes.put(b.id, b);
            office.byPlace.put(place(b.dimension, b.pos), b.id);
        }
        CompoundTag visits = tag.getCompound("Visited");
        for (String key : visits.getAllKeys()) {
            Set<Integer> ids = new HashSet<>();
            for (int id : visits.getIntArray(key)) {
                ids.add(id);
            }
            office.visited.put(UUID.fromString(key), ids);
        }
        for (Tag t : tag.getList("Parcels", Tag.TAG_COMPOUND)) {
            CompoundTag pt = (CompoundTag) t;
            ItemStack stack = ItemStack.parseOptional(registries, pt.getCompound("Item"));
            if (!stack.isEmpty()) {
                office.parcels.add(new Parcel(pt.getInt("Box"), stack, pt.getLong("Arrives"), pt.getString("From")));
            }
        }
        return office;
    }
}
