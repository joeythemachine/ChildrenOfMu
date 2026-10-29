package com.childrenofmu.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * Dog City: a strict, orderly fort, laid out to one plan every time (only turned to a different heading).
 *
 * <pre>
 *   - An eight-pointed star of crenellated stone brick wall (a square and a diamond laid over one another), with a
 *     catwalk all round and a water moat outside it. The gatehouse stands in the southern point, with a bridge.
 *   - The four military towers on the four axes between the quarters: bowmen, swordsmen, pikemen and the Biters'
 *     kennel (never the tower by the gate).
 *   - The four quarters, a block of four workshops each (beds upstairs for the dogs who work there):
 *       1 (north-west) clothing: weavery, dyeworks, tailor, wool barn
 *       2 (north-east) smithy, tannery, livery stable, sawmill
 *       3 (south-west) slaughterhouse, butchery, smokehouse, kitchen
 *       4 (south-east) mess hall, music hall, school, tea house
 *   - In the middle the Keep, the monks' stone hall on its raised plinth, with one wide stair up to it and through to
 *     its inner courtyard; and in the courtyard the forbidden dark tower, far higher than anything else.
 *   - Stone-paved roads in a square grid between it all; grass everywhere else.
 * </pre>
 * Plan coordinates are relative to the center, with the gate to the south (+z), before the city is turned.
 */
public class DogCityStructure extends Structure {
    public static final MapCodec<DogCityStructure> CODEC = simpleCodec(DogCityStructure::new);

    /** The star: the diamond's points are TIP out, the square's corners SQ out; they cross NOTCH along a side. */
    static final int TIP = 85, SQ = 60, NOTCH = TIP - SQ;
    /** Corners of the star, clockwise from the northern point (the wall's inside is to the right of each edge). */
    static final int[][] STAR = {{0, -TIP}, {NOTCH, -SQ}, {SQ, -SQ}, {SQ, -NOTCH}, {TIP, 0}, {SQ, NOTCH}, {SQ, SQ},
            {NOTCH, SQ}, {0, TIP}, {-NOTCH, SQ}, {-SQ, SQ}, {-SQ, NOTCH}, {-TIP, 0}, {-SQ, -NOTCH}, {-SQ, -SQ},
            {-NOTCH, -SQ}};
    /** The towers on the axes. */
    static final int TOWER_AT = 47;
    /** The two rows of workshop plots in each quarter (centers, from the axes). */
    static final int[] PLOTS = {35, 50};
    /** The Keep's half-width and the height of its plinth (the courtyard's floor, above the ground). */
    static final int KEEP_HALF = 23, PLINTH = 4;
    /** The gatehouse: its center from the middle, and its half-width. */
    static final int GATE_AT = 80, GATE_HALF = 5;
    /** How far the leveled ground reaches (past the points, to take in the moat). */
    static final int REACH = 97;

    private static final String[][] QUARTERS = {
            {"dc_weavery", "dc_dyeworks", "dc_tailor", "dc_woolbarn"},             // 1, north-west
            {"dc_smithy", "dc_tannery", "dc_livery", "dc_sawmill"},                // 2, north-east
            {"dc_slaughterhouse", "dc_butchery", "dc_smokehouse", "dc_kitchen"},   // 3, south-west
            {"dc_mess_hall", "dc_music_hall", "dc_school", "dc_teahouse"}};        // 4, south-east
    private static final int[][] QUARTER_SIGNS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    public DogCityStructure(StructureSettings settings) {
        super(settings);
    }

    /** Turns a plan offset by k quarter turns clockwise. */
    static int[] turn(int dx, int dz, int k) {
        for (int i = 0; i < Math.floorMod(k, 4); i++) {
            int t = dx;
            dx = -dz;
            dz = t;
        }
        return new int[]{dx, dz};
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(ChildrenOfMu.MODID, name);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int cx = context.chunkPos().getMiddleBlockX();
        int cz = context.chunkPos().getMiddleBlockZ();
        // A broad, dry, fairly level stretch of land: most of a grid of samples over the whole fort near one height.
        List<Integer> ys = new ArrayList<>();
        int wet = 0;
        for (int dx = -72; dx <= 72; dx += 24) {
            for (int dz = -72; dz <= 72; dz += 24) {
                ys.add(IthkBuildingStructure.surface(context, cx + dx, cz + dz));
                if (IthkBuildingStructure.isWet(context, cx + dx, cz + dz)) {
                    wet++;
                }
            }
        }
        if (wet > 4 || IthkBuildingStructure.isWet(context, cx, cz)) {
            return Optional.empty();
        }
        List<Integer> sorted = new ArrayList<>(ys);
        java.util.Collections.sort(sorted);
        int cy = sorted.get(sorted.size() / 2);
        int near = 0;
        for (int y : ys) {
            if (Math.abs(y - cy) <= 7) {
                near++;
            }
        }
        if (near < ys.size() * 4 / 5 || cy < context.chunkGenerator().getSeaLevel() + 1) {
            return Optional.empty();
        }
        // Inside its biomes (the heart and the four quarters, at least).
        for (int[] d : new int[][]{{0, 0}, {40, 40}, {-40, 40}, {40, -40}, {-40, -40}}) {
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(
                    net.minecraft.core.QuartPos.fromBlock(cx + d[0]), net.minecraft.core.QuartPos.fromBlock(cy),
                    net.minecraft.core.QuartPos.fromBlock(cz + d[1]), context.randomState().sampler()))) {
                return Optional.empty();
            }
        }

        RandomSource random = context.random();
        int k = random.nextInt(4);
        Rotation turned = Rotation.values()[k];
        List<StructurePiece> pieces = new ArrayList<>();
        int ground = cy - 1;                      // the top block of the leveled ground
        var manager = context.structureTemplateManager();

        // ---- The buildings. (Template y = 0 is laid in the ground; sink 1.)
        java.util.function.BiFunction<int[], Rotation, BlockPos> at = (d, r) -> {
            int[] t = turn(d[0], d[1], k);
            return new BlockPos(cx + t[0], ground, cz + t[1]);
        };
        // The Keep, its stair to the south; then the dark tower on the courtyard in its middle.
        pieces.add(new IthkBuildingPiece(manager, id("dc_keep"), at.apply(new int[]{0, 0}, null),
                Rotation.CLOCKWISE_180.getRotated(turned), 0, true, -1, 1));
        pieces.add(new IthkBuildingPiece(manager, id("dc_dark_tower"),
                at.apply(new int[]{0, 0}, null).above(PLINTH), Rotation.CLOCKWISE_180.getRotated(turned), 0, false, -1, 1));
        // The towers: the kennel anywhere but by the gate.
        List<String> towers = new ArrayList<>(List.of("dc_tower_bow", "dc_tower_sword", "dc_tower_pike"));
        for (int i = towers.size() - 1; i > 0; i--) {
            java.util.Collections.swap(towers, i, random.nextInt(i + 1));
        }
        towers.add(random.nextInt(3), "dc_kennel");       // north, east or west; the gate's tower is the last
        int[][] towerAt = {{0, -TOWER_AT}, {TOWER_AT, 0}, {-TOWER_AT, 0}, {0, TOWER_AT}};
        Rotation[] towerFace = {Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90,
                Rotation.NONE};                            // each with its door toward the Keep
        for (int i = 0; i < 4; i++) {
            pieces.add(new IthkBuildingPiece(manager, id(towers.get(i)), at.apply(towerAt[i], null),
                    towerFace[i].getRotated(turned), 0, true, -1, 1));
        }
        // The gatehouse in the southern point.
        pieces.add(new IthkBuildingPiece(manager, id("dc_gatehouse"), at.apply(new int[]{0, GATE_AT}, null),
                turned, 0, true, -1, 1));
        // The quarters: each quarter's four workshops in its own order, doors onto the road between the two rows.
        for (int q = 0; q < 4; q++) {
            List<String> shops = new ArrayList<>(List.of(QUARTERS[q]));
            for (int i = shops.size() - 1; i > 0; i--) {
                java.util.Collections.swap(shops, i, random.nextInt(i + 1));
            }
            int sx = QUARTER_SIGNS[q][0], sz = QUARTER_SIGNS[q][1];
            int n = 0;
            for (int a : PLOTS) {
                for (int b : PLOTS) {
                    boolean nearRow = b == PLOTS[0];
                    // Facing +z (CLOCKWISE_180) or -z (NONE), toward the middle road.
                    boolean facePlus = (sz > 0) == nearRow;
                    pieces.add(new IthkBuildingPiece(manager, id(shops.get(n++)), at.apply(new int[]{sx * a, sz * b}, null),
                            (facePlus ? Rotation.CLOCKWISE_180 : Rotation.NONE).getRotated(turned), 0, true, -1, 1));
                }
            }
        }

        // ---- The ground, leveled all over (and out past the moat), first of all.
        int[] ring = new int[STAR.length * 2];
        for (int i = 0; i < STAR.length; i++) {
            int[] t = turn(STAR[i][0], STAR[i][1], k);
            ring[i * 2] = cx + t[0];
            ring[i * 2 + 1] = cz + t[1];
        }
        List<int[]> plots = new ArrayList<>();
        plots.add(new int[]{cx - REACH, cz - REACH, cx + REACH, cz + REACH, ground});
        pieces.add(0, new CityTerrainPiece(cy, ring, plots));

        // ---- The wall round the star (it leaves the gatehouse be, and opens into it).
        int[] g0 = turn(-GATE_HALF, GATE_AT - GATE_HALF, k), g1 = turn(GATE_HALF, GATE_AT + GATE_HALF, k);
        int[] keepOut = {cx + Math.min(g0[0], g1[0]), cz + Math.min(g0[1], g1[1]), cx + Math.max(g0[0], g1[0]),
                cz + Math.max(g0[1], g1[1]), ground + CityWallPiece.HEIGHT};
        for (int i = 0; i < STAR.length; i++) {
            int j = (i + 1) % STAR.length;
            pieces.add(new CityWallPiece(ring[i * 2], ring[i * 2 + 1], cy, ring[j * 2], ring[j * 2 + 1], cy, keepOut, 1));
        }
        // ---- The moat outside it, bridged at the gate; and last, the roads (only over open ground).
        pieces.add(new DogMoatPiece(cx, cz, cy, k, ring));
        pieces.add(new DogGroundPiece(cx, cz, cy, k));
        return Optional.of(new GenerationStub(new BlockPos(cx, cy, cz), (StructurePiecesBuilder b) -> pieces.forEach(b::addPiece)));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.DOG_CITY.get();
    }
}
