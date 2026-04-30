package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.MathParser;

/**
 * GENERÁTOR MESHE — Vzorkuje f(x,y,t) na mřížce a počítá trojúhelníkovou síť s normálami
 * 
 * Postup:
 * 1. Vzorkuje funkci na pravidelné N×N mřížce
 * 2. Počítá normály pomocí parciálních derivací
 * 3. Mapuje barvy podle Z-hodnoty (gradient)
 * 4. Generuje trojúhelníkový index buffer
 */
public class FunctionMesh {
    private float[] vertices;   // x, y, z per vertex
    private float[] normals;    // nx, ny, nz per vertex
    private float[] colors;     // r, g, b per vertex (Z-mapovaný gradient)
    private int[]   indices;    // trojúhelníkové indexy
    private int     gridN;      // počet vertexů per axis
    private float   zMin, zMax; // Z rozsah (pro barevné mapování)

    /**
     * Paleta barev pro gradient (nízké Z → vysoké Z):
     * deep blue → cyan → green → yellow → orange → red
     */
    private static final float[][] COLOR_STOPS = {
            {0.1f, 0.2f, 0.9f},   // deep blue
            {0.0f, 0.8f, 0.9f},   // cyan
            {0.0f, 0.9f, 0.2f},   // green
            {0.9f, 0.9f, 0.0f},   // yellow
            {1.0f, 0.5f, 0.0f},   // orange
            {0.9f, 0.1f, 0.1f},   // red
    };

    /**
     * Vytvoří a vyplní mesh
     * @param parser   kompilovaný výraz f(x,y,t)
     * @param xMin     X rozsah — minimum
     * @param xMax     X rozsah — maximum
     * @param yMin     Y rozsah — minimum
     * @param yMax     Y rozsah — maximum
     * @param steps    počet rozdělení mřížky (výsledná mřížka bude (steps+1)×(steps+1))
     * @param t        čas pro animaci
     */
    public void build(MathParser parser, float xMin, float xMax,
                      float yMin, float yMax, int steps, double t) {

        gridN = steps + 1;
        int N = gridN;
        // Pomocné pole Z-hodnot (před procesováním normál)
        float[] z = new float[N * N];

        // KROK 1: Vzorkuje funkci a nachází Z-rozsah
        float zMinLocal = Float.MAX_VALUE;
        float zMaxLocal = -Float.MAX_VALUE;

        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);

                // Evaluuje f(x, y, t)
                double val = parser.evaluateSafe(x, y, t);
                float fval = Double.isNaN(val) ? Float.NaN : (float) val;
                z[iy * N + ix] = fval;

                // Najde min/max (ignoruje NaN)
                if (!Float.isNaN(fval)) {
                    if (fval < zMinLocal) zMinLocal = fval;
                    if (fval > zMaxLocal) zMaxLocal = fval;
                }
            }
        }

        // Fallback, pokud by všechny hodnoty byly NaN nebo identické
        if (zMinLocal == Float.MAX_VALUE) { zMinLocal = -1f; zMaxLocal = 1f; }
        if (Math.abs(zMaxLocal - zMinLocal) < 1e-10f) { zMinLocal -= 0.5f; zMaxLocal += 0.5f; }
        zMin = zMinLocal;
        zMax = zMaxLocal;

        // KROK 2: Inicializuje pole pro data mřížky
        vertices = new float[N * N * 3];
        normals  = new float[N * N * 3];
        colors   = new float[N * N * 3];

        // Naplní vrcholy a barvy
        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);
                int idx = iy * N + ix;
                float fz = z[idx];
                if (Float.isNaN(fz)) fz = Float.NaN; // NaN zůstane NaN (nevykreslí se) a zajistí, že se nebudou počítat normály

                // Uloží pozici
                vertices[idx * 3]     = (float) x;
                vertices[idx * 3 + 1] = (float) y;
                vertices[idx * 3 + 2] = fz;

                // Mapuje Z na barvu pomocí gradientu
                float t01 = (fz - zMin) / (zMax - zMin);
                float[] col = colorFromGradient(t01);
                colors[idx * 3]     = col[0];
                colors[idx * 3 + 1] = col[1];
                colors[idx * 3 + 2] = col[2];
            }
        }

        // KROK 3: Počítá normály z parciálních derivací: N = (-dz/dx, -dz/dy, 1)
        for (int iy = 0; iy < N; iy++) {
            for (int ix = 0; ix < N; ix++) {
                int idx = iy * N + ix;

                // Sousední body pro výpočet gradientu
                int ixL = Math.max(ix - 1, 0);
                int ixR = Math.min(ix + 1, N - 1);
                int iyD = Math.max(iy - 1, 0);
                int iyU = Math.min(iy + 1, N - 1);

                // Z-hodnota vpravo/vlevo
                float zR = safeZ(z, iyU * N + ixR, iy, ix, z[idx]);
                float zL = safeZ(z, iy * N + ixL, iy, ix, z[idx]);

                // Parciální derivace v X: ∂z/∂x ≈ (zR - zL) / Δx
                float dx = ((xMax - xMin) / (N - 1)) * (ixR - ixL);
                float dzdx = (dx == 0) ? 0f : (zR - zL) / dx;

                // Z-hodnota nahoru/dolu
                float zU2 = safeZ(z, iyU * N + ix, iy, ix, z[idx]);
                float zD2 = safeZ(z, iyD * N + ix, iy, ix, z[idx]);

                // Parciální derivace v Y: ∂z/∂y ≈ (zU - zD) / Δy
                float dy = ((yMax - yMin) / (N - 1)) * (iyU - iyD);
                float dzdy = (dy == 0) ? 0f : (zU2 - zD2) / dy;

                // Normála = (-∂z/∂x, -∂z/∂y, 1), normalizovaná
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

        // KROK 4: Generuje trojúhelníkový index buffer
        // Mřížka steps×steps má steps×steps×2 trojúhelníků
        int numTriangles = steps * steps * 2;
        indices = new int[numTriangles * 3];
        int idx = 0;
        for (int iy = 0; iy < steps; iy++) {
            for (int ix = 0; ix < steps; ix++) {
                // Čtyři rohy čtverce
                int tl = iy * N + ix;        // top-left
                int tr = tl + 1;             // top-right
                int bl = (iy + 1) * N + ix;  // bottom-left
                int br = bl + 1;             // bottom-right
                
                // Trojúhelník 1: top-left → bottom-left → top-right
                indices[idx++] = tl;
                indices[idx++] = bl;
                indices[idx++] = tr;
                
                // Trojúhelník 2: top-right → bottom-left → bottom-right
                indices[idx++] = tr;
                indices[idx++] = bl;
                indices[idx++] = br;
            }
        }
    }

    /**
     * Bezpečně načte Z-hodnotu, vrátí fallback pro NaN
     */
    private float safeZ(float[] z, int i, int iy, int ix, float fallback) {
        if (i < 0 || i >= z.length) return fallback;
        float v = z[i];
        return Float.isNaN(v) ? fallback : v;
    }

    /**
     * Interpoluje barvu z palety podle pozice v [0,1]
     * @param t normalizovaná Z-pozice (0=nejnižší, 1=nejvyšší)
     */
    private float[] colorFromGradient(float t) {
        // Zajistí, že t je v [0,1]
        t = Math.max(0f, Math.min(1f, t));

        // Mapuje t na index v poli barev
        float scaled = t * (COLOR_STOPS.length - 1);
        int lo = (int) scaled;
        int hi = Math.min(lo + 1, COLOR_STOPS.length - 1);
        float frac = scaled - lo; // Lineární interpolace mezi sousedními barvami
        
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