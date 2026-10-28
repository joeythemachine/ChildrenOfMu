package com.childrenofmu.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.childrenofmu.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * An Ithk village: one building at the center (the Amethyst Tower) with the others scattered around it at random,
 * never overlapping, each at its own height over the land, each turned to a random side. Which buildings, how many
 * and how far out all come from the structure's json (data/childrenofmu/worldgen/structure/ithk_village.json),
 * so the mix can be tuned without touching code.
 */
public class IthkVillageStructure extends Structure {

    /** One kind of building in the village. */
    public record Building(ResourceLocation template, int groundRadius, int footprint, int minCount, int maxCount,
                           int minDistance, int maxDistance, int sink, boolean followSlopes, boolean radial,
                           boolean supports, int door, String district, List<Integer> span, boolean levelGround,
                           int back) {
        /** Where the building's walls start and end along its width (template x, from its center). */
        int spanLo() {
            return span.isEmpty() ? -footprint : span.get(0);
        }

        int spanHi() {
            return span.isEmpty() ? footprint : span.get(1);
        }

        public static final Codec<Building> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("template").forGetter(Building::template),
                Codec.intRange(0, 32).optionalFieldOf("ground_radius", 0).forGetter(Building::groundRadius),
                Codec.intRange(1, 48).fieldOf("footprint").forGetter(Building::footprint),
                Codec.intRange(0, 32).optionalFieldOf("min_count", 1).forGetter(Building::minCount),
                Codec.intRange(0, 32).optionalFieldOf("max_count", 1).forGetter(Building::maxCount),
                Codec.intRange(0, 112).optionalFieldOf("min_distance", 0).forGetter(Building::minDistance),
                Codec.intRange(0, 112).optionalFieldOf("max_distance", 0).forGetter(Building::maxDistance),
                Codec.intRange(0, 16).optionalFieldOf("sink", 0).forGetter(Building::sink),
                Codec.BOOL.optionalFieldOf("follow_slopes", true).forGetter(Building::followSlopes),
                Codec.BOOL.optionalFieldOf("radial", false).forGetter(Building::radial),
                Codec.BOOL.optionalFieldOf("extend_supports", false).forGetter(Building::supports),
                Codec.intRange(0, 48).optionalFieldOf("door", 0).forGetter(Building::door),
                Codec.STRING.optionalFieldOf("district", "any").forGetter(Building::district),
                Codec.INT.listOf().optionalFieldOf("span", List.of()).forGetter(Building::span),
                Codec.BOOL.optionalFieldOf("level_ground", false).forGetter(Building::levelGround),
                Codec.intRange(0, 32).optionalFieldOf("back", 0).forGetter(Building::back)
        ).apply(i, Building::new));
    }

    public static final MapCodec<IthkVillageStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Building.CODEC.fieldOf("center").forGetter(s -> s.center),
            Building.CODEC.listOf().fieldOf("buildings").forGetter(s -> s.buildings),
            Codec.intRange(0, 16).optionalFieldOf("spacing", 3).forGetter(s -> s.spacing),
            Codec.BOOL.optionalFieldOf("allow_water", false).forGetter(s -> s.allowWater),
            Codec.BOOL.optionalFieldOf("boardwalks", false).forGetter(s -> s.boardwalks),
            Codec.BOOL.optionalFieldOf("stay_in_biome", false).forGetter(s -> s.stayInBiome),
            Codec.intRange(-1, 64).optionalFieldOf("max_rise", -1).forGetter(s -> s.maxRise),
            Codec.BOOL.optionalFieldOf("clear_ground", false).forGetter(s -> s.clearGround),
            Codec.BOOL.optionalFieldOf("streets", false).forGetter(s -> s.streets),
            Codec.intRange(-1, 64).optionalFieldOf("flat", -1).forGetter(s -> s.flat),
            Codec.BOOL.optionalFieldOf("sewers", false).forGetter(s -> s.sewers),
            Codec.intRange(0, 112).optionalFieldOf("pave_radius", 0).forGetter(s -> s.paveRadius),
            Codec.intRange(0, 112).optionalFieldOf("wall_radius", 0).forGetter(s -> s.wallRadius),
            Codec.intRange(0, 4).optionalFieldOf("gates", 2).forGetter(s -> s.gates)
    ).apply(instance, IthkVillageStructure::new));

    /** Tries per building to find a free, dry spot before giving up on it. */
    private static final int ATTEMPTS = 40;

    private final Building center;
    private final List<Building> buildings;
    /** Minimum gap between neighboring footprints. */
    private final int spacing;
    /** Towns that may stand over water (the Frog folk's swamp towns). */
    private final boolean allowWater;
    /** Join every building's door to the center building's door with plank boardwalks. */
    private final boolean boardwalks;
    /** Every building must stand in one of the town's biomes (so a swamp town never climbs out of its swamp). */
    private final boolean stayInBiome;
    /** How far above or below the center building another may stand (-1: any height). */
    private final int maxRise;
    /**
     * Stilt towns over water (the Frog folk's): the center must stand in water, no ground inside a building's footprint
     * or along a boardwalk may rise above the deck, and the land around the town must be mostly low, so a town never
     * gets wedged against a hill or a cliff.
     */
    private final boolean clearGround;
    /**
     * Cities (RCB City): every building's door joins the nearest door already placed by a crooked cobbled street,
     * so the streets branch and wind like a labyrinth instead of all running to the middle.
     */
    private final boolean streets;
    /** How far (up or down) most of the land around the center may stray from it (-1: don't check). */
    private final int flat;
    /** A sewer tunnel under every street, all at one depth so they meet. */
    private final boolean sewers;
    /** Pave all natural ground within this distance of the center (0: don't). */
    private final int paveRadius;
    /**
     * Walled cities: the half-width of the square the town's perimeter follows (0: no wall). Buildings marked for the
     * wall stand shoulder to shoulder along it with their backs to the outside, stretches of stone wall fill the gaps,
     * towers stand at the corners and gatehouses let the streets out. Inside, the buildings keep to their districts.
     */
    private final int wallRadius;
    private final int gates;

    public IthkVillageStructure(StructureSettings settings, Building center, List<Building> buildings, int spacing,
                                boolean allowWater, boolean boardwalks, boolean stayInBiome, int maxRise,
                                boolean clearGround, boolean streets, int flat, boolean sewers, int paveRadius,
                                int wallRadius, int gates) {
        super(settings);
        this.center = center;
        this.buildings = buildings;
        this.spacing = spacing;
        this.allowWater = allowWater;
        this.boardwalks = boardwalks;
        this.stayInBiome = stayInBiome;
        this.maxRise = maxRise;
        this.clearGround = clearGround;
        this.streets = streets;
        this.flat = flat;
        this.sewers = sewers;
        this.paveRadius = paveRadius;
        this.wallRadius = wallRadius;
        this.gates = gates;
    }

    /** Most of the land in rings around the center lies within flat blocks of its height. */
    private static boolean flatEnough(GenerationContext context, int cx, int cy, int cz, int flat) {
        for (int radius : new int[]{26, 44}) {
            int ok = 0;
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                int y = IthkBuildingStructure.surface(context, cx + (int) (Math.cos(a) * radius), cz + (int) (Math.sin(a) * radius));
                if (Math.abs(y - cy) <= flat && !IthkBuildingStructure.isWet(context, cx + (int) (Math.cos(a) * radius),
                        cz + (int) (Math.sin(a) * radius))) {
                    ok++;
                }
            }
            if (ok < 13) {
                return false;
            }
        }
        return true;
    }

    /** No ground higher than maxY at the middle, edges and corners of a square of this half-size. */
    private static boolean clear(GenerationContext context, int x, int z, int half, int maxY) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (IthkBuildingStructure.surface(context, x + dx * half, z + dz * half) > maxY) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The boardwalk's route (along x, then along z, as BoardwalkPiece lays it) stays at or below maxY. */
    private static boolean clearPath(GenerationContext context, int x0, int z0, int x1, int z1, int maxY) {
        int sx = Integer.signum(x1 - x0), sz = Integer.signum(z1 - z0);
        for (int x = x0; x != x1; x += sx) {
            if ((x - x0) % 2 == 0 && IthkBuildingStructure.surface(context, x, z0) > maxY) {
                return false;
            }
        }
        for (int z = z0; z != z1; z += sz) {
            if ((z - z0) % 2 == 0 && IthkBuildingStructure.surface(context, x1, z) > maxY) {
                return false;
            }
        }
        return true;
    }

    private static boolean inBiome(GenerationContext context, int x, int y, int z) {
        return context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), context.randomState().sampler()));
    }

    private record Spot(int x, int z, int footprint) {}

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        if (!allowWater && IthkBuildingStructure.isWet(context, cx, cz)) {
            return Optional.empty();
        }
        RandomSource random = context.random();
        int cy = height(context, center, cx, cz);
        if (clearGround) {
            if (!IthkBuildingStructure.isWet(context, cx, cz) || !clear(context, cx, cz, center.footprint() / 2, cy + 1)) {
                return Optional.empty();
            }
            int low = 0;
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                if (IthkBuildingStructure.surface(context, cx + (int) (Math.cos(a) * 32), cz + (int) (Math.sin(a) * 32)) <= cy + 3) {
                    low++;
                }
            }
            if (low < 13) {
                return Optional.empty();   // hills or cliffs too close
            }
        }
        if (flat >= 0 && !flatEnough(context, cx, IthkBuildingStructure.surface(context, cx, cz), cz, flat)) {
            return Optional.empty();
        }
        if (stayInBiome) {
            // The heart of town and the land around it must all be the right biome, not just one corner of it.
            for (int[] d : new int[][]{{0, 0}, {24, 0}, {-24, 0}, {0, 24}, {0, -24}}) {
                if (!inBiome(context, cx + d[0], cy, cz + d[1])) {
                    return Optional.empty();
                }
            }
        }

        // Lay out the whole village now, while the random source is this structure's own.
        List<StructurePiece> pieces = new ArrayList<>();
        List<StructurePiece> walks = new ArrayList<>();
        List<BlockPos> doors = new ArrayList<>();
        List<Spot> taken = new ArrayList<>();
        Rotation centerRotation = Rotation.getRandom(random);
        pieces.add(piece(context, center, cx, cy, cz, centerRotation));
        taken.add(new Spot(cx, cz, center.footprint()));
        BlockPos hub = new BlockPos(cx, cy, cz).offset(new BlockPos(0, 0, -center.door()).rotate(centerRotation));
        doors.add(hub);

        CityRing ring = wallRadius > 0 ? CityRing.make(random, cx, cz, wallRadius) : null;
        // Districts: each kind of quarter gets a quadrant of its own, in a different order in every city.
        List<String> quarters = new ArrayList<>(List.of("bazz", "market", "homes", "works"));
        for (int i = quarters.size() - 1; i > 0; i--) {
            java.util.Collections.swap(quarters, i, random.nextInt(i + 1));
        }
        for (Building b : buildings) {
            if (isPerimeter(b)) {
                continue;
            }
            int quarter = quarters.indexOf(b.district());
            int count = b.minCount() + (b.maxCount() > b.minCount() ? random.nextInt(b.maxCount() - b.minCount() + 1) : 0);
            for (int n = 0; n < count; n++) {
                for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                    double angle = quarter >= 0 && attempt < ATTEMPTS - 8
                            ? (quarter + 0.08 + random.nextDouble() * 0.84) * Math.PI / 2.0
                            : random.nextDouble() * Math.PI * 2.0;
                    double dist = b.minDistance() + random.nextDouble() * Math.max(0, b.maxDistance() - b.minDistance());
                    int x = cx + (int) Math.round(Math.cos(angle) * dist);
                    int z = cz + (int) Math.round(Math.sin(angle) * dist);
                    if (ring != null && (!ring.inside(x, z) || ring.distance(x, z) < b.footprint() + 11)) {
                        continue;    // too close to the wall (and the buildings along it)
                    }
                    if (!isFree(taken, x, z, b.footprint()) || (!allowWater && IthkBuildingStructure.isWet(context, x, z))) {
                        continue;
                    }
                    Rotation rotation = b.radial()
                            // Turn it so you pass through it on the way into the village (an arch at the edge).
                            ? (Math.abs(x - cx) > Math.abs(z - cz) ? Rotation.CLOCKWISE_90 : Rotation.NONE)
                            : ring != null && ring.distance(x, z) < b.footprint() + 24
                            ? facing(cx - x, cz - z)          // out toward the wall: its door faces into town
                            : Rotation.getRandom(random);
                    int y = height(context, b, x, z);
                    if ((maxRise >= 0 && Math.abs(y - cy) > maxRise) || (stayInBiome && !inBiome(context, x, y, z))) {
                        continue;
                    }
                    BlockPos doorAt = new BlockPos(x, y, z).offset(new BlockPos(0, 0, -b.door()).rotate(rotation));
                    if (clearGround && (!clear(context, x, z, b.footprint() / 2 + 1, y + 1)
                            || (boardwalks && b.door() > 0
                                && !clearPath(context, hub.getX(), hub.getZ(), doorAt.getX(), doorAt.getZ(), cy + 1)))) {
                        continue;
                    }
                    pieces.add(piece(context, b, x, y, z, rotation));
                    taken.add(new Spot(x, z, b.footprint()));
                    if (boardwalks && b.door() > 0) {
                        BlockPos door = new BlockPos(x, y, z).offset(new BlockPos(0, 0, -b.door()).rotate(rotation));
                        walks.add(new BoardwalkPiece(hub.getX(), hub.getZ(), door.getX(), door.getZ(), cy));
                    }
                    if (streets && b.door() > 0) {
                        connect(random, cy, walks, doors, doorAt);
                    }
                    break;
                }
            }
        }
        int interior = pieces.size();
        List<StructurePiece> walls = new ArrayList<>();
        List<int[]> plots = new ArrayList<>();
        if (ring != null) {
            perimeter(context, random, cx, cy, cz, ring, pieces, walls, walks, doors, plots);
            for (int i = 0; i < interior; i++) {
                StructurePiece p = pieces.get(i);
                if (p instanceof IthkBuildingPiece bp) {
                    plot(plots, bp, bp.groundY());
                }
            }
            pieces.add(0, new CityTerrainPiece(cy, ring.flat(), plots));   // first: the land is levelled before anything stands on it
        }
        // The walls after the buildings (they fill the gaps between them); streets and boardwalks after that, and the
        // skywalks across the streets last of all.
        pieces.addAll(walls);
        pieces.addAll(walks);
        if (streets) {
            pieces.addAll(skywalks(pieces, random));
        }
        if (paveRadius > 0) {
            pieces.add(ring != null ? new CityGroundPiece(cx, cz, cy, ring.flat())
                    : new CityGroundPiece(cx, cz, cy, paveRadius));   // last: it only fills what's left
        }
        return Optional.of(new GenerationStub(new BlockPos(cx, cy, cz), (StructurePiecesBuilder builder) -> pieces.forEach(builder::addPiece)));
    }

    /** Where a building's template origin goes: on a levelled plot at the lie of the land, or as heightAt says. */
    private static int height(GenerationContext context, Building b, int x, int z) {
        return b.levelGround() ? IthkBuildingStructure.averageHeight(context, x, z) - b.sink()
                : IthkBuildingStructure.heightAt(context, x, z, b.followSlopes(), b.sink());
    }

    private static boolean isPerimeter(Building b) {
        return "wall".equals(b.district()) || "gate".equals(b.district()) || "corner".equals(b.district());
    }

    /** Joins a door to the nearest door already on the street map (and lays a sewer under the street). */
    private void connect(RandomSource random, int cy, List<StructurePiece> walks, List<BlockPos> doors, BlockPos doorAt) {
        BlockPos from = doors.get(0);
        for (BlockPos d : doors) {
            if (d.distSqr(doorAt) < from.distSqr(doorAt)) {
                from = d;
            }
        }
        boolean xFirst = random.nextBoolean();
        walks.add(new StreetPiece(from.getX(), from.getZ(), doorAt.getX(), doorAt.getZ(), (from.getY() + doorAt.getY()) / 2,
                xFirst, sewers ? cy - SewerPiece.DEPTH : Integer.MIN_VALUE));
        if (sewers) {
            walks.add(new SewerPiece(from.getX(), from.getZ(), doorAt.getX(), doorAt.getZ(), cy, xFirst));
        }
        doors.add(doorAt);
    }

    /**
     * The town's perimeter, an irregular ring about wallRadius out from the heart (see CityRing): a tower at every
     * bend, gatehouses on some sides (the streets run out through them), the wall buildings shoulder to shoulder along
     * the rest, backs to the outside, and stone wall laid along every edge after them, filling every gap. Heights run
     * evenly round the ring, so towers, buildings and wall all meet.
     */
    private void perimeter(GenerationContext context, RandomSource random, int cx, int cy, int cz, CityRing ring,
                           List<StructurePiece> pieces, List<StructurePiece> walls, List<StructurePiece> walks,
                           List<BlockPos> doors, List<int[]> plots) {
        List<Building> wall = new ArrayList<>(), gate = new ArrayList<>(), corner = new ArrayList<>();
        for (Building b : buildings) {
            ("wall".equals(b.district()) ? wall : "gate".equals(b.district()) ? gate
                    : "corner".equals(b.district()) ? corner : new ArrayList<Building>()).add(b);
        }
        List<CityRing.Edge> edges = ring.edges;
        int n = edges.size();
        // Heights at the corners (surface: first air above the ground), from the land, kept near the heart's, smoothed.
        int[] vh = new int[n];
        for (int i = 0; i < n; i++) {
            CityRing.Edge e = edges.get(i);
            vh[i] = Mth.clamp(IthkBuildingStructure.averageHeight(context, e.x0(), e.z0()), cy - 5, cy + 5);
        }
        int[] sm = new int[n];
        for (int i = 0; i < n; i++) {
            sm[i] = Math.round((vh[(i + n - 1) % n] + 2 * vh[i] + vh[(i + 1) % n]) / 4.0F);
        }
        // Towers at the bends. Keep-out boxes (the wall leaves them be and opens into them at walk level).
        List<Integer> keep = new ArrayList<>();
        for (CityRing.Tower t : ring.towers) {
            if (corner.isEmpty()) {
                break;
            }
            Building b = corner.get(random.nextInt(corner.size()));
            int y = surfaceAt(edges, sm, t.edge(), t.along());
            Rotation rot = facing(cx - t.x(), cz - t.z());
            IthkBuildingPiece piece = piece(context, b, t.x(), y - b.sink(), t.z(), rot);
            pieces.add(piece);
            plot(plots, piece, y - 1);
            int half = b.footprint() - 1;
            keep.addAll(List.of(t.x() - half, t.z() - half, t.x() + half, t.z() + half, y - b.sink() + CityWallPiece.HEIGHT));
            if (b.door() > 0) {
                connect(random, cy, walks, doors, new BlockPos(t.x(), y, t.z()).offset(new BlockPos(0, 0, -b.door()).rotate(rot)));
            }
        }
        // Gatehouses: on some sides, in the middle of that side's longest run.
        List<Integer> sides = new ArrayList<>(List.of(0, 1, 2, 3));
        for (int i = 3; i > 0; i--) {
            java.util.Collections.swap(sides, i, random.nextInt(i + 1));
        }
        java.util.Map<Integer, int[]> gateAt = new java.util.HashMap<>();     // edge -> {from, to} along it
        for (int s : sides.subList(0, gate.isEmpty() ? 0 : Math.min(gates, 4))) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (edges.get(i).kind() == CityRing.SIDE && edges.get(i).side() == s
                        && (best < 0 || edges.get(i).length() > edges.get(best).length())) {
                    best = i;
                }
            }
            if (best < 0) {
                continue;
            }
            CityRing.Edge e = edges.get(best);
            Building b = gate.get(random.nextInt(gate.size()));
            Rotation rot = facing(e.ix(), e.iz());
            int[] span = span(b, rot, e);
            int len = e.length();
            if (len < span[1] - span[0] + 12) {
                continue;
            }
            int tc = len / 2 + random.nextInt(7) - 3;
            int gx = e.x0() + e.dx() * tc + e.ix() * b.back(), gz = e.z0() + e.dz() * tc + e.iz() * b.back();
            int y = surfaceAt(edges, sm, best, (float) tc / len);
            IthkBuildingPiece piece = piece(context, b, gx, y - b.sink(), gz, rot);
            pieces.add(piece);
            plot(plots, piece, y - 1);
            gateAt.put(best, new int[]{tc + span[0] - 1, tc + span[1] + 1});
            int ax = e.x0() + e.dx() * (tc + span[0]), az = e.z0() + e.dz() * (tc + span[0]);
            int bx2 = e.x0() + e.dx() * (tc + span[1]), bz2 = e.z0() + e.dz() * (tc + span[1]);
            keep.addAll(List.of(Math.min(ax, bx2) - Math.abs(e.ix()) * 4, Math.min(az, bz2) - Math.abs(e.iz()) * 4,
                    Math.max(ax, bx2) + Math.abs(e.ix()) * 4, Math.max(az, bz2) + Math.abs(e.iz()) * 4,
                    y - b.sink() + CityWallPiece.HEIGHT));
            // A street in through the gate, and a road out of it.
            BlockPos inside = new BlockPos(gx + e.ix() * 9, y, gz + e.iz() * 9);
            BlockPos outside = new BlockPos(gx - e.ix() * 16, y, gz - e.iz() * 16);
            walks.add(new StreetPiece(inside.getX(), inside.getZ(), outside.getX(), outside.getZ(), y, e.ix() != 0));
            connect(random, cy, walks, doors, inside);
        }
        // The wall buildings, along every long run.
        for (int i = 0; i < n; i++) {
            CityRing.Edge e = edges.get(i);
            if (e.kind() != CityRing.SIDE || wall.isEmpty()) {
                continue;
            }
            int len = e.length();
            Rotation rot = facing(e.ix(), e.iz());
            int prevKind = edges.get((i + n - 1) % n).kind(), nextKind = edges.get((i + 1) % n).kind();
            int start = prevKind == CityRing.JOG ? 6 : 4, end = len - (nextKind == CityRing.JOG ? 6 : 4);
            int[] g = gateAt.get(i);
            int t = start;
            int tries = 0;
            while (t < end && tries++ < 40) {
                if (g != null && t >= g[0] && t <= g[1]) {
                    t = g[1] + 1;
                    continue;
                }
                int limit = g != null && t < g[0] ? g[0] : end;
                // Whatever fits in the room left (rows if there's space, single houses to fill in), at random.
                int gap = 1 + random.nextInt(3);
                List<Building> fits = new ArrayList<>();
                for (Building c : wall) {
                    int[] sp = span(c, rot, e);
                    if (t + gap + sp[1] - sp[0] + 1 <= limit) {
                        fits.add(c);
                    }
                }
                if (fits.isEmpty()) {
                    t += 2;              // nothing fits here: just wall
                    continue;
                }
                // Prefer the long rows: pick among the widest few.
                fits.sort((p1, p2) -> Integer.compare(p2.spanHi() - p2.spanLo(), p1.spanHi() - p1.spanLo()));
                Building b = fits.get(random.nextInt(Math.min(fits.size(), random.nextFloat() < 0.7F ? 3 : fits.size())));
                int[] span = span(b, rot, e);
                int tc = t + gap - span[0];
                int bx = e.x0() + e.dx() * tc + e.ix() * b.back(), bz = e.z0() + e.dz() * tc + e.iz() * b.back();
                int y = surfaceAt(edges, sm, i, (float) tc / len);
                IthkBuildingPiece piece = piece(context, b, bx, y - b.sink(), bz, rot);
                pieces.add(piece);
                plot(plots, piece, y - 1);
                if (b.door() > 0) {
                    connect(random, cy, walks, doors, new BlockPos(bx, y, bz).offset(new BlockPos(0, 0, -b.door()).rotate(rot)));
                }
                t = tc + span[1] + 1;
            }
        }
        // The wall itself, every edge, and the plots it stands on.
        int[] keepOut = keep.stream().mapToInt(Integer::intValue).toArray();
        for (int i = 0; i < n; i++) {
            CityRing.Edge e = edges.get(i);
            int ya = sm[i], yb = sm[(i + 1) % n];
            walls.add(new CityWallPiece(e.x0(), e.z0(), ya, e.x1(), e.z1(), yb, keepOut));
            int len = Math.max(1, e.length());
            for (int t = 0; t <= len; t += 2) {
                int x = e.x0() + Math.round((float) (e.x1() - e.x0()) * t / len);
                int z = e.z0() + Math.round((float) (e.z1() - e.z0()) * t / len);
                int y = Math.round(Mth.lerp((float) t / len, ya, yb)) - 1;
                plots.add(new int[]{Math.min(x, x + e.ix()) - 1, Math.min(z, z + e.iz()) - 1,
                        Math.max(x, x + e.ix()) + 1, Math.max(z, z + e.iz()) + 1, y});
            }
        }
    }

    /** The surface height a fraction of the way along an edge. */
    private static int surfaceAt(List<CityRing.Edge> edges, int[] sm, int edge, float along) {
        return Math.round(Mth.lerp(along, sm[edge], sm[(edge + 1) % edges.size()]));
    }

    /** The rotation that turns a building's front (template -z) toward this direction. */
    private static Rotation facing(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        }
        return dz > 0 ? Rotation.CLOCKWISE_180 : Rotation.NONE;
    }

    /** Where a building's width starts and ends along an edge, from its center, once turned. */
    private static int[] span(Building b, Rotation rot, CityRing.Edge e) {
        BlockPos lo = new BlockPos(b.spanLo(), 0, 0).rotate(rot), hi = new BlockPos(b.spanHi(), 0, 0).rotate(rot);
        int a = lo.getX() * e.dx() + lo.getZ() * e.dz(), c = hi.getX() * e.dx() + hi.getZ() * e.dz();
        return new int[]{Math.min(a, c), Math.max(a, c)};
    }

    /** The ground a building stands on, levelled by the city terrain: the building's own walls and a little round them. */
    private static void plot(List<int[]> plots, StructurePiece piece, int ground) {
        if (piece instanceof IthkBuildingPiece bp) {
            int[] f = bp.footprint();
            plots.add(new int[]{f[0] - 2, f[1] - 2, f[2] + 2, f[3] + 2, ground});
            return;
        }
        net.minecraft.world.level.levelgen.structure.BoundingBox bb = piece.getBoundingBox();
        plots.add(new int[]{bb.minX(), bb.minZ(), bb.maxX(), bb.maxZ(), ground});
    }

    /** Buildings too grand, too low or too busy for a skywalk to run into. */
    private static boolean bridgeable(IthkBuildingPiece p) {
        String t = p.templateId();
        for (String no : new String[]{"justice_hall", "guard_tower", "bridge_houses", "fish_stall", "sewer", "fight_pit",
                "watch_house", "rat_works", "bazz_club"}) {
            if (t.contains(no)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Skywalks across the streets: wherever an upper floor of one building looks straight across at an upper floor of
     * another, four to sixteen blocks off and at (nearly) the same height, with no other building in the way, a covered
     * timber walk joins them. The best-placed pairs first; no building gets more than two, and no walk crosses another.
     */
    private static List<StructurePiece> skywalks(List<StructurePiece> pieces, RandomSource random) {
        List<IthkBuildingPiece> blds = new ArrayList<>();
        for (StructurePiece p : pieces) {
            if (p instanceof IthkBuildingPiece bp) {
                blds.add(bp);
            }
        }
        List<int[]> feet = new ArrayList<>();
        List<List<com.childrenofmu.worldgen.TemplateInfo.Anchor>> anchors = new ArrayList<>();
        for (IthkBuildingPiece bp : blds) {
            feet.add(bp.footprint());
            anchors.add(bridgeable(bp) ? bp.worldAnchors() : List.of());
        }
        record Pair(int i, int j, BlockPos a, BlockPos b, int score) {}
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < blds.size(); i++) {
            for (int j = i + 1; j < blds.size(); j++) {
                for (com.childrenofmu.worldgen.TemplateInfo.Anchor a : anchors.get(i)) {
                    for (com.childrenofmu.worldgen.TemplateInfo.Anchor b : anchors.get(j)) {
                        if (b.out() != a.out().getOpposite()) {
                            continue;
                        }
                        BlockPos pa = a.wall(), pb = b.wall();
                        int dy = Math.abs(pa.getY() - pb.getY());
                        boolean alongX = a.out().getAxis() == net.minecraft.core.Direction.Axis.X;
                        if (dy > 2 || (alongX ? pa.getZ() != pb.getZ() : pa.getX() != pb.getX())) {
                            continue;
                        }
                        int gap = (alongX ? pb.getX() - pa.getX() : pb.getZ() - pa.getZ()) * (alongX ? a.out().getStepX() : a.out().getStepZ()) - 1;
                        if (gap < 4 || gap > 16 || gap < dy + 3) {
                            continue;
                        }
                        int ground = Math.max(blds.get(i).groundY(), blds.get(j).groundY());
                        if (Math.min(pa.getY(), pb.getY()) - ground < 4) {
                            continue;    // too low to walk under
                        }
                        pairs.add(new Pair(i, j, pa, pb, dy * 6 + gap + random.nextInt(3)));
                    }
                }
            }
        }
        pairs.sort(java.util.Comparator.comparingInt(Pair::score));
        List<StructurePiece> out = new ArrayList<>();
        int[] count = new int[blds.size()];
        java.util.Set<Long> used = new java.util.HashSet<>();
        for (Pair p : pairs) {
            if (out.size() >= 10 || count[p.i()] >= 2 || count[p.j()] >= 2) {
                continue;
            }
            boolean alongX = p.a().getZ() == p.b().getZ();
            int n = Math.abs(alongX ? p.b().getX() - p.a().getX() : p.b().getZ() - p.a().getZ());
            int sx = Integer.signum(p.b().getX() - p.a().getX()), sz = Integer.signum(p.b().getZ() - p.a().getZ());
            List<Long> cells = new ArrayList<>();
            boolean ok = true;
            for (int t = 1; t < n && ok; t++) {
                for (int l = -1; l <= 1; l++) {
                    int x = p.a().getX() + sx * t + (alongX ? 0 : l), z = p.a().getZ() + sz * t + (alongX ? l : 0);
                    long key = BlockPos.asLong(x, 0, z);
                    if (used.contains(key)) {
                        ok = false;
                        break;
                    }
                    for (int k = 0; k < feet.size(); k++) {
                        int[] f = feet.get(k);
                        if (k != p.i() && k != p.j() && x >= f[0] && x <= f[2] && z >= f[1] && z <= f[3]) {
                            ok = false;
                            break;
                        }
                    }
                    cells.add(key);
                }
            }
            if (!ok) {
                continue;
            }
            used.addAll(cells);
            count[p.i()]++;
            count[p.j()]++;
            out.add(new SkywalkPiece(p.a(), p.b()));
        }
        return out;
    }

    private boolean isFree(List<Spot> taken, int x, int z, int footprint) {
        for (Spot s : taken) {
            int min = s.footprint() + footprint + spacing;
            long dx = x - s.x(), dz = z - s.z();
            if (dx * dx + dz * dz < (long) min * min) {
                return false;
            }
        }
        return true;
    }

    private IthkBuildingPiece piece(GenerationContext context, Building b, int x, int y, int z, Rotation rotation) {
        // In a walled city the terrain piece levels every plot; elsewhere a building levels its own.
        return new IthkBuildingPiece(context.structureTemplateManager(), b.template(), new BlockPos(x, y, z), rotation,
                b.groundRadius(), b.supports(), b.levelGround() && wallRadius == 0 ? b.sink() : -1, b.sink());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ITHK_VILLAGE.get();
    }
}
