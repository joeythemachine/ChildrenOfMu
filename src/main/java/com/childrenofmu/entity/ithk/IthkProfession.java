package com.childrenofmu.entity.ithk;

import net.minecraft.util.RandomSource;

/**
 * What an Ithk does. QUIESCENT Ithk are at rest: beyond any vocation, and no lesser for it.
 * NONE only exists briefly before a profession is assigned.
 */
public enum IthkProfession {
    NONE("none"),
    TRANSMUTER("transmuter"),
    ARCHIVIST("archivist"),
    GATEKEEPER("gatekeeper"),
    CURATOR("curator"),
    QUIESCENT("quiescent");

    private final String id;

    IthkProfession(String id) {
        this.id = id;
    }

    /** Lowercase name, used in save data, lang keys and texture names. */
    public String id() {
        return this.id;
    }

    public static IthkProfession byId(String id) {
        for (IthkProfession p : values()) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return NONE;
    }

    public static IthkProfession byOrdinal(int ordinal) {
        IthkProfession[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
    }

    public static IthkProfession random(RandomSource random) {
        return values()[1 + random.nextInt(values().length - 1)];
    }
}
