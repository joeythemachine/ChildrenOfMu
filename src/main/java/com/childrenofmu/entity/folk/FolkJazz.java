package com.childrenofmu.entity.folk;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Bazz: the bat folk's all-night jazz. Every playing bat keeps time by the world clock, so any bats near each other
 * play together over a twelve-bar blues. Plain bat folk walk the bass, flappers keep the drums, and jazz bats
 * improvise over the top. The barman just serves.
 *
 * The music drifts between styles every couple of minutes: mostly bebop (fast, busy, flute runs) and lounge (slow,
 * soft bells and brushes), and now and then it sinks into dub, all heavy bass and space, with the jazz bats' eeks
 * echoed back over and over as if off a tape loop.
 */
public final class FolkJazz {
    public enum Style {
        BEBOP(8, 5), LOUNGE(16, 11), DUB(14, 9);

        /** Ticks per beat, and where the swung off-beat lands. */
        public final int beat, swing;

        Style(int beat, int swing) {
            this.beat = beat;
            this.swing = swing;
        }
    }

    /** How long one style lasts before the band drifts into the next (ticks). */
    private static final int SET = 2400;
    /** Twelve-bar blues: I I I I  IV IV I I  V IV I V (semitones above the key). */
    private static final int[] BLUES = {0, 0, 0, 0, 5, 5, 0, 0, 7, 5, 0, 7};
    /** The key, as a note-block note (0 = F#, so 6 = C). */
    private static final int KEY = 6;
    private static final int[] WALK = {0, 4, 7, 9};
    private static final int[] SCALE = {0, 3, 5, 6, 7, 10, 12, 15};

    /** What the band is playing right now (the same everywhere, so every club in town swings together). */
    public static Style style(long gameTime) {
        long set = gameTime / SET;
        int roll = Math.floorMod((int) (set * 2654435761L >>> 7), 10);
        return roll < 4 ? Style.BEBOP : roll < 8 ? Style.LOUNGE : Style.DUB;
    }

    /** Plays this bat's part for the current tick, if it has a note now. */
    public static void tick(FolkEntity bat, ServerLevel level) {
        long t = level.getGameTime();
        // Tape echoes still ringing from earlier.
        while (!bat.echoes.isEmpty() && bat.echoes.peekFirst()[0] <= t) {
            double[] e = bat.echoes.pollFirst();
            level.playSound(null, bat.getX(), bat.getY(), bat.getZ(), SoundEvents.BAT_AMBIENT, SoundSource.RECORDS,
                    (float) e[1], (float) e[2]);
        }
        Style style = style(t);
        int step = (int) (t % style.beat);
        if (step != 0 && step != style.swing) {
            return;
        }
        long beat = t / style.beat;
        int beatInBar = (int) (beat % 4);
        int root = BLUES[(int) ((beat / 4) % BLUES.length)];
        RandomSource r = RandomSource.create(bat.getId() * 7919L + t);
        switch (bat.getVariantName()) {
            case "folk" -> {
                if (style == Style.DUB) {
                    if (step == 0 && (beatInBar == 0 || beatInBar == 2)) {
                        play(bat, level, SoundEvents.NOTE_BLOCK_BASS, KEY + root - 12 + (beatInBar == 2 ? 7 : 0), 1.6F);
                    }
                } else if (style == Style.LOUNGE) {
                    if (step == 0 && beatInBar % 2 == 0) {
                        play(bat, level, SoundEvents.NOTE_BLOCK_BASS, KEY + root + WALK[beatInBar], 1.1F);
                    }
                } else if (step == 0 || r.nextFloat() < 0.3F) {
                    play(bat, level, SoundEvents.NOTE_BLOCK_BASS, KEY + root + WALK[(beatInBar + (step == 0 ? 0 : 1)) % 4], 1.3F);
                }
            }
            case "flapper" -> {
                switch (style) {
                    case DUB -> {
                        if (step == 0 && beatInBar == 2) {
                            play(bat, level, SoundEvents.NOTE_BLOCK_SNARE, 10, 1.0F);   // the one-drop
                        } else if (step == 0 && beatInBar == 0) {
                            play(bat, level, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, 1.2F);
                        }
                    }
                    case LOUNGE -> play(bat, level, SoundEvents.NOTE_BLOCK_HAT, 14, step == 0 ? 0.35F : 0.2F);   // brushes
                    default -> {
                        if (step == 0 && beatInBar == 0) {
                            play(bat, level, SoundEvents.NOTE_BLOCK_BASEDRUM, 8, 1.0F);
                        } else if (step == 0 && beatInBar % 2 == 1) {
                            play(bat, level, SoundEvents.NOTE_BLOCK_SNARE, 12, 0.9F);
                        }
                        play(bat, level, SoundEvents.NOTE_BLOCK_HAT, 16, 0.6F);
                    }
                }
            }
            case "jazz" -> {
                switch (style) {
                    case DUB -> {
                        if (step == 0 && beatInBar == 1 && r.nextFloat() < 0.5F) {
                            // An eek, fed back through the tape loop: each repeat quieter and a shade lower.
                            float pitch = 0.8F + r.nextFloat() * 0.5F;
                            level.playSound(null, bat.getX(), bat.getY(), bat.getZ(), SoundEvents.BAT_AMBIENT,
                                    SoundSource.RECORDS, 1.0F, pitch);
                            for (int k = 1; k <= 4; k++) {
                                bat.echoes.addLast(new double[]{t + k * style.swing, Math.pow(0.55, k), pitch * (1 - 0.02 * k)});
                            }
                        } else if (step == 0 && beatInBar == 3 && r.nextFloat() < 0.4F) {
                            play(bat, level, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, KEY + root + 12, 0.7F);
                        }
                    }
                    case LOUNGE -> {
                        if (r.nextFloat() < 0.35F) {
                            int idx = Math.floorMod(bat.melodyIndex + r.nextInt(3) - 1, SCALE.length);
                            bat.melodyIndex = idx;
                            play(bat, level, SoundEvents.NOTE_BLOCK_BELL, KEY + SCALE[idx], 0.7F);
                        }
                    }
                    default -> {
                        if (r.nextFloat() < 0.8F) {
                            // Bebop: fast runs with big leaps.
                            int idx = Math.floorMod(bat.melodyIndex + r.nextInt(7) - 3, SCALE.length);
                            bat.melodyIndex = idx;
                            play(bat, level, SoundEvents.NOTE_BLOCK_FLUTE, KEY + SCALE[idx], 1.1F);
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void play(FolkEntity bat, ServerLevel level, Holder<SoundEvent> sound, int note, float volume) {
        note = Math.max(0, Math.min(24, note));
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        level.playSound(null, bat.getX(), bat.getY(), bat.getZ(), sound.value(), SoundSource.RECORDS, volume, pitch);
        level.sendParticles(ParticleTypes.NOTE, bat.getX(), bat.getY() + bat.getBbHeight() + 0.3, bat.getZ(),
                0, note / 24.0, 0.0, 0.0, 1.0);
    }

    private FolkJazz() {}
}
