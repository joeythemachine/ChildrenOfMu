package com.childrenofmu.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;

/**
 * The outline of a walled RCB City: an irregular ring, different in every city. Four long sides, each set its own
 * distance from the heart and often broken by a jog (part of the side stepped in or out), with the corners cut off on
 * the diagonal. Every bend gets a tower. The ring runs clockwise (x east, z south), so the inside of every edge is
 * on its right.
 */
public final class CityRing {
    /** Kinds of edge. */
    public static final int SIDE = 0, JOG = 1, CHAMFER = 2;

    /** One straight run of wall. side: which of the four sides (0 N, 1 E, 2 S, 3 W) it belongs to. */
    public record Edge(int x0, int z0, int x1, int z1, int kind, int side) {
        public int length() {
            return Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        }

        /** Unit step along the edge (each component -1, 0 or 1). */
        public int dx() {
            return Integer.signum(x1 - x0);
        }

        public int dz() {
            return Integer.signum(z1 - z0);
        }

        /** The inward side (to the right walking clockwise). */
        public int ix() {
            return -dz();
        }

        public int iz() {
            return dx();
        }
    }

    /** A tower at a bend: where, and which edge it sits on (and how far along it, 0..1). */
    public record Tower(int x, int z, int edge, float along) {}

    public final int cx, cz;
    public final List<int[]> verts = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
    public final List<Tower> towers = new ArrayList<>();

    private CityRing(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
    }

    public static CityRing make(RandomSource r, int cx, int cz, int radius) {
        CityRing ring = new CityRing(cx, cz);
        int[] d1 = new int[4], d2 = new int[4], c = new int[4];
        for (int s = 0; s < 4; s++) {
            d1[s] = radius + r.nextInt(15) - 7;
            d2[s] = r.nextFloat() < 0.75F ? d1[s] + (r.nextBoolean() ? 1 : -1) * (4 + r.nextInt(6)) : d1[s];
            c[s] = 12 + r.nextInt(16);     // the corner after side s, cut off on the diagonal
        }
        int dN1 = d1[0], dN2 = d2[0], dE1 = d1[1], dE2 = d2[1], dS1 = d1[2], dS2 = d2[2], dW1 = d1[3], dW2 = d2[3];
        // Each side: start, (jog), end. A jog is only made where it leaves both parts a decent length.
        ring.side(0, -dW2 + c[3], -dN1, dE1 - c[0], -dN2, true, r);
        ring.side(1, dE1, -dN2 + c[0], dE2, dS1 - c[1], false, r);
        ring.side(2, dE2 - c[1], dS1, -dW1 + c[2], dS2, true, r);
        ring.side(3, -dW1, dS2 - c[2], -dW2, -dN1 + c[3], false, r);
        // Edges between consecutive corners, and towers.
        int n = ring.verts.size();
        int[] kinds = ring.kinds.stream().mapToInt(Integer::intValue).toArray();
        for (int i = 0; i < n; i++) {
            int[] a = ring.verts.get(i), b = ring.verts.get((i + 1) % n);
            ring.edges.add(new Edge(a[0] + cx, a[1] + cz, b[0] + cx, b[1] + cz, kinds[i], ring.sides.get(i)));
        }
        for (int i = 0; i < ring.edges.size(); i++) {
            Edge e = ring.edges.get(i);
            if (e.kind() == CHAMFER || e.kind() == JOG) {
                ring.towers.add(new Tower((e.x0() + e.x1()) / 2, (e.z0() + e.z1()) / 2, i, 0.5F));
            }
        }
        return ring;
    }

    private final List<Integer> kinds = new ArrayList<>();
    private final List<Integer> sides = new ArrayList<>();

    /**
     * Adds side s from (ax, az) to (bx, bz): part 1 on a's line, part 2 on b's line, joined by a jog if they differ;
     * then the chamfer to the next side follows from b. alongX: the side runs east-west.
     */
    private void side(int s, int ax, int az, int bx, int bz, boolean alongX, RandomSource r) {
        add(ax, az, SIDE, s);
        boolean differ = alongX ? az != bz : ax != bx;
        if (differ) {
            int lo = alongX ? Math.min(ax, bx) : Math.min(az, bz), hi = alongX ? Math.max(ax, bx) : Math.max(az, bz);
            if (hi - lo >= 40) {
                int j = lo + 18 + r.nextInt(hi - lo - 35);
                if (alongX) {
                    add(j, az, JOG, s);
                    add(j, bz, SIDE, s);
                } else {
                    add(ax, j, JOG, s);
                    add(bx, j, SIDE, s);
                }
            } else {
                // Too short for a jog: keep the side straight on a's line.
                if (alongX) {
                    bz = az;
                } else {
                    bx = ax;
                }
            }
        }
        add(bx, bz, CHAMFER, s);
    }

    /** A corner, and the kind of the edge that leaves it. */
    private void add(int x, int z, int kindOfNextEdge, int side) {
        this.verts.add(new int[]{x, z});
        this.kinds.add(kindOfNextEdge);
        this.sides.add(side);
    }

    /** Inside the ring? (Ray casting.) */
    public boolean inside(int x, int z) {
        return inside(this.edges, x, z);
    }

    public static boolean inside(List<Edge> edges, double x, double z) {
        boolean in = false;
        for (Edge e : edges) {
            double x0 = e.x0(), z0 = e.z0(), x1 = e.x1(), z1 = e.z1();
            if ((z0 > z) != (z1 > z) && x < (x1 - x0) * (z - z0) / (z1 - z0) + x0) {
                in = !in;
            }
        }
        return in;
    }

    /** Distance from (x, z) to the nearest stretch of wall. */
    public double distance(double x, double z) {
        double best = Double.MAX_VALUE;
        for (Edge e : this.edges) {
            double vx = e.x1() - e.x0(), vz = e.z1() - e.z0();
            double len = vx * vx + vz * vz;
            double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((x - e.x0()) * vx + (z - e.z0()) * vz) / len));
            double px = e.x0() + t * vx - x, pz = e.z0() + t * vz - z;
            best = Math.min(best, Math.sqrt(px * px + pz * pz));
        }
        return best;
    }

    /** The corners as a flat int array (x, z, x, z...), for saving with a piece. */
    public int[] flat() {
        int[] out = new int[this.edges.size() * 2];
        for (int i = 0; i < this.edges.size(); i++) {
            out[i * 2] = this.edges.get(i).x0();
            out[i * 2 + 1] = this.edges.get(i).z0();
        }
        return out;
    }

    /** Edges of a ring saved with flat(). */
    public static List<Edge> edges(int[] flat) {
        List<Edge> out = new ArrayList<>();
        int n = flat.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            out.add(new Edge(flat[i * 2], flat[i * 2 + 1], flat[j * 2], flat[j * 2 + 1], SIDE, 0));
        }
        return out;
    }
}
