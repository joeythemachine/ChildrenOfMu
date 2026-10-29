package com.childrenofmu.registry;

import com.childrenofmu.ChildrenOfMu;
import com.childrenofmu.worldgen.BoardwalkPiece;
import com.childrenofmu.worldgen.IthkBuildingPiece;
import com.childrenofmu.worldgen.IthkBuildingStructure;
import com.childrenofmu.worldgen.IthkVillageStructure;
import com.childrenofmu.worldgen.StreetPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ChildrenOfMu.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ChildrenOfMu.MODID);

    /** One Ithk building, floating over the land as it is, with mud and red plants worked into the ground beneath. */
    public static final DeferredHolder<StructureType<?>, StructureType<IthkBuildingStructure>> ITHK_BUILDING =
            STRUCTURE_TYPES.register("ithk_building", () -> () -> IthkBuildingStructure.CODEC);

    /** A whole Ithk village: the Amethyst Tower at the center and the other buildings scattered around it. */
    public static final DeferredHolder<StructureType<?>, StructureType<IthkVillageStructure>> ITHK_VILLAGE =
            STRUCTURE_TYPES.register("ithk_village", () -> () -> IthkVillageStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ITHK_BUILDING_PIECE =
            PIECE_TYPES.register("ithk_building", () -> (StructurePieceType.StructureTemplateType) IthkBuildingPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> BOARDWALK_PIECE =
            PIECE_TYPES.register("boardwalk", () -> (StructurePieceType.ContextlessType) BoardwalkPiece::new);

    /** A cobbled street between two doors of an RCB City. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> STREET_PIECE =
            PIECE_TYPES.register("street", () -> (StructurePieceType.ContextlessType) StreetPiece::new);

    /** The sewer under a street of an RCB City. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SEWER_PIECE =
            PIECE_TYPES.register("sewer", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.SewerPiece::new);

    /** The paved ground of an RCB City between its buildings and streets. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_GROUND_PIECE =
            PIECE_TYPES.register("city_ground", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.CityGroundPiece::new);

    /** A stretch of an RCB City's town wall between the buildings of its perimeter. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_WALL_PIECE =
            PIECE_TYPES.register("city_wall", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.CityWallPiece::new);

    /** The levelled ground under an RCB City (plots for its buildings and walls, banked into the land). */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_TERRAIN_PIECE =
            PIECE_TYPES.register("city_terrain", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.CityTerrainPiece::new);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, ChildrenOfMu.MODID);

    /** Random spread kept clear of several structure sets (pillager outposts, kept away from our towns). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<com.childrenofmu.worldgen.AvoidingSpreadPlacement>>
            AVOIDING_SPREAD = PLACEMENT_TYPES.register("avoiding_spread",
            () -> () -> com.childrenofmu.worldgen.AvoidingSpreadPlacement.CODEC);

    /** A covered timber skywalk between two buildings of an RCB City, across a street. */
    public static final DeferredHolder<StructureType<?>, StructureType<com.childrenofmu.worldgen.DogCityStructure>> DOG_CITY =
            STRUCTURE_TYPES.register("dog_city", () -> () -> com.childrenofmu.worldgen.DogCityStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> DOG_MOAT_PIECE =
            PIECE_TYPES.register("dog_moat", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.DogMoatPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> DOG_GROUND_PIECE =
            PIECE_TYPES.register("dog_ground", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.DogGroundPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SKYWALK_PIECE =
            PIECE_TYPES.register("skywalk", () -> (StructurePieceType.ContextlessType) com.childrenofmu.worldgen.SkywalkPiece::new);

    private ModStructures() {}
}
