package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.MathParser;

/**
 * Vzorkuje f(x,y,t) na mřížce a počítá trojúhelníkovou síť s normálami
 */
public class FunctionMesh {
    private float[] vertices;   // x, y, z per vertex
    private float[] normals;    // nx, ny, nz per vertex
    private float[] colors;     // r, g, b per vertex (Z-mapovaný gradient)
    private int[]   indices;    // trojúhelníkové indexy
    private int     gridN;      // počet vertexů per axis
    private float   zMin, zMax; // Z rozsah (pro barevné mapování)

    // Zastavení barevného přechodu: nízké Z → vysoké Z Z
    // [blue → cyan → green → yellow → orange → red]
    private static final float[][] COLOR_STOPS = {
            {0.1f, 0.2f, 0.9f},   // deep blue
            {0.0f, 0.8f, 0.9f},   // cyan
            {0.0f, 0.9f, 0.2f},   // green
            {0.9f, 0.9f, 0.0f},   // yellow
            {1.0f, 0.5f, 0.0f},   // orange
            {0.9f, 0.1f, 0.1f},   // red
    };

    /**
     * Vytvoří Mesh
     * @param parser  kompilovaný výraz f(x,y,t)
     * @param xMin    x range start
     * @param xMax    x range end
     * @param yMin    y range start
     * @param yMax    y range end
     * @param steps   počet rozdělení mřížky
     * @param t       animace v čase
     */
    public void build(MathParser parser, float xMin, float xMax,
                      float yMin, float yMax, int steps, double t) {

        gridN = steps + 1;
        int N = gridN;
        float[] z = new float[N * N];

        float zMinLocal = Float.MAX_VALUE;
        float zMaxLocal = -Float.MAX_VALUE;

        // Z hodnoty
        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);
                double val = parser.evaluateSafe(x, y, t);
                float fval = Double.isNaN(val) ? Float.NaN : (float) val;
                z[iy * N + ix] = fval;
                if (!Float.isNaN(fval)) {
                    if (fval < zMinLocal) zMinLocal = fval;
                    if (fval > zMaxLocal) zMaxLocal = fval;
                }
            }
        }

        // Fallback, pokud všechno NaN
        if (zMinLocal == Float.MAX_VALUE) { zMinLocal = -1f; zMaxLocal = 1f; }
        if (Math.abs(zMaxLocal - zMinLocal) < 1e-10f) { zMinLocal -= 0.5f; zMaxLocal += 0.5f; }
        zMin = zMinLocal;
        zMax = zMaxLocal;

        // Inicializace polí pro data mřížky
        vertices = new float[N * N * 3];
        normals  = new float[N * N * 3];
        colors   = new float[N * N * 3];

        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);
                int idx = iy * N + ix;
                float fz = z[idx];
                if (Float.isNaN(fz)) fz = 0f;

                vertices[idx * 3]     = (float) x;
                vertices[idx * 3 + 1] = (float) y;
                vertices[idx * 3 + 2] = fz;

                // barva podle Z
                float t01 = (fz - zMin) / (zMax - zMin);
                float[] col = colorFromGradient(t01);
                colors[idx * 3]     = col[0];
                colors[idx * 3 + 1] = col[1];
                colors[idx * 3 + 2] = col[2];
            }
        }

        // Pro každý vertex spočítá normálu z okolních Z hodnot
        for (int iy = 0; iy < N; iy++) {
            for (int ix = 0; ix < N; ix++) {
                int idx = iy * N + ix;

                // sousedi pro výpočet normály (ixL, iy), (ixR, iy), (ix, iyD), (ix, iyU)
                int ixL = Math.max(ix - 1, 0);
                int ixR = Math.min(ix + 1, N - 1);
                int iyD = Math.max(iy - 1, 0);
                int iyU = Math.min(iy + 1, N - 1);

                float zR = safeZ(z, iyU * N + ixR, iy, ix, z[idx]);
                float zL = safeZ(z, iy * N + ixL, iy, ix, z[idx]);

                // Parciální derviace v x: (zR - zL) / (xR - xL)
                float dx = ((xMax - xMin) / (N - 1)) * (ixR - ixL);
                float dzdx = (dx == 0) ? 0f : (zR - zL) / dx;

                float zU2 = safeZ(z, iyU * N + ix, iy, ix, z[idx]);
                float zD2 = safeZ(z, iyD * N + ix, iy, ix, z[idx]);

                float dy = ((yMax - yMin) / (N - 1)) * (iyU - iyD);
                float dzdy = (dy == 0) ? 0f : (zU2 - zD2) / dy;

                // Normal = (-dzdx, -dzdy, 1) normalized
                float nx = -dzdx;
                float ny = -dzdy;
                float nz = 1f;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1e-10f) { nx /= len; ny /= len; nz /= len; }

                normals[idx * 3]     = nx;
                normals[idx * 3 + 1] = ny;
                normals[idx * 3 + 2] = nz;
            }
        }

        // Vytvoří trojúhelníkovou síť
        int numTriangles = steps * steps * 2;
        indices = new int[numTriangles * 3];
        int idx = 0;
        for (int iy = 0; iy < steps; iy++) {
            for (int ix = 0; ix < steps; ix++) {
                int tl = iy * N + ix;
                int tr = tl + 1;
                int bl = (iy + 1) * N + ix;
                int br = bl + 1;
                // Trojúhelník 1
                indices[idx++] = tl;
                indices[idx++] = bl;
                indices[idx++] = tr;
                // Trojúhelník 2
                indices[idx++] = tr;
                indices[idx++] = bl;
                indices[idx++] = br;
            }
        }
    }

    private float safeZ(float[] z, int i, int iy, int ix, float fallback) {
        if (i < 0 || i >= z.length) return fallback;
        float v = z[i];
        return Float.isNaN(v) ? fallback : v;
    }

    private float[] colorFromGradient(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float scaled = t * (COLOR_STOPS.length - 1);
        int lo = (int) scaled;
        int hi = Math.min(lo + 1, COLOR_STOPS.length - 1);
        float frac = scaled - lo;
        float[] a = COLOR_STOPS[lo];
        float[] b = COLOR_STOPS[hi];
        return new float[] {
                a[0] + frac * (b[0] - a[0]),
                a[1] + frac * (b[1] - a[1]),
                a[2] + frac * (b[2] - a[2])
        };
    }

    public float[] getVertices() { return vertices; }
    public float[] getNormals()  { return normals; }
    public float[] getColors()   { return colors; }
    public int[]   getIndices()  { return indices; }
    public float   getZMin()     { return zMin; }
    public float   getZMax()     { return zMax; }
}