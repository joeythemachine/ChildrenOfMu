package com.childrenofmu.entity.ithk;

/** Iris colors (ARGB). The iris texture is greyscale and gets tinted with one of these. */
public final class IthkEyeColors {
    public static final int[] COLORS = {
            0xFFE8A13A, // amber
            0xFF9B59D9, // violet
            0xFF2EC4B6, // teal
            0xFFD7263D, // crimson
            0xFFF4D35E, // gold
            0xFF7FD3F7, // ice blue
            0xFF3CB371, // emerald
            0xFFE056A0  // magenta
    };
    public static final int COUNT = COLORS.length;

    private IthkEyeColors() {}
}
