package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.MathParser;

/**
 * MESH GENERATOR — Samples f(x,y,t) on a grid and builds a triangle mesh with normals
 *
 * Steps:
 * 1. Samples the function on a regular N×N grid
 * 2. Computes the normals from partial derivatives
 * 3. Maps colors by the Z value (gradient)
 * 4. Generates the triangle index buffer
 */
public class FunctionMesh {
    private float[] vertices;   // x, y, z per vertex
    private float[] normals;    // nx, ny, nz per vertex
    private float[] colors;     // r, g, b per vertex (Z-mapped gradient)
    private int[]   indices;    // triangle indices
    private int     gridN;      // number of vertices per axis
    private float   zMin, zMax; // Z range (for color mapping)

    /**
     * Gradient color palette (low Z → high Z):
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
     * Creates and fills the mesh
     * @param parser   compiled expression f(x,y,t)
     * @param xMin     X range — minimum
     * @param xMax     X range — maximum
     * @param yMin     Y range — minimum
     * @param yMax     Y range — maximum
     * @param steps    number of grid divisions (the resulting grid is (steps+1)×(steps+1))
     * @param t        time for the animation
     */
    public void build(MathParser parser, float xMin, float xMax,
                      float yMin, float yMax, int steps, double t) {

        gridN = steps + 1;
        int N = gridN;
        // Helper array of Z values (before the normals are computed)
        float[] z = new float[N * N];

        // STEP 1: Sample the function and find the Z range
        float zMinLocal = Float.MAX_VALUE;
        float zMaxLocal = -Float.MAX_VALUE;

        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);

                // Evaluate f(x, y, t)
                double val = parser.evaluateSafe(x, y, t);
                float fval = Double.isNaN(val) ? Float.NaN : (float) val;
                z[iy * N + ix] = fval;

                // Find min/max (ignoring NaN)
                if (!Float.isNaN(fval)) {
                    if (fval < zMinLocal) zMinLocal = fval;
                    if (fval > zMaxLocal) zMaxLocal = fval;
                }
            }
        }

        // Fallback if all values are NaN or identical
        if (zMinLocal == Float.MAX_VALUE) { zMinLocal = -1f; zMaxLocal = 1f; }
        if (Math.abs(zMaxLocal - zMinLocal) < 1e-10f) { zMinLocal -= 0.5f; zMaxLocal += 0.5f; }
        zMin = zMinLocal;
        zMax = zMaxLocal;

        // STEP 2: Allocate the arrays for the grid data
        vertices = new float[N * N * 3];
        normals  = new float[N * N * 3];
        colors   = new float[N * N * 3];

        // Fill in the vertices and colors
        for (int iy = 0; iy < N; iy++) {
            double y = yMin + (yMax - yMin) * iy / (N - 1);
            for (int ix = 0; ix < N; ix++) {
                double x = xMin + (xMax - xMin) * ix / (N - 1);
                int idx = iy * N + ix;
                float fz = z[idx];

                // Store the position
                vertices[idx * 3]     = (float) x;
                vertices[idx * 3 + 1] = (float) y;
                vertices[idx * 3 + 2] = fz;

                // Map Z to a color using the gradient
                float t01 = (fz - zMin) / (zMax - zMin);
                float[] col = colorFromGradient(t01);
                colors[idx * 3]     = col[0];
                colors[idx * 3 + 1] = col[1];
                colors[idx * 3 + 2] = col[2];
            }
        }

        // STEP 3: Compute the normals from partial derivatives: N = (-dz/dx, -dz/dy, 1)
        for (int iy = 0; iy < N; iy++) {
            for (int ix = 0; ix < N; ix++) {
                int idx = iy * N + ix;

                // Neighboring points for the gradient
                int ixL = Math.max(ix - 1, 0);
                int ixR = Math.min(ix + 1, N - 1);
                int iyD = Math.max(iy - 1, 0);
                int iyU = Math.min(iy + 1, N - 1);

                // Z value to the right/left
                float zR = safeZ(z, iy * N + ixR, z[idx]);
                float zL = safeZ(z, iy * N + ixL, z[idx]);

                // Partial derivative in X: ∂z/∂x ≈ (zR - zL) / Δx
                float dx = ((xMax - xMin) / (N - 1)) * (ixR - ixL);
                float dzdx = (dx == 0) ? 0f : (zR - zL) / dx;

                // Z value up/down
                float zU2 = safeZ(z, iyU * N + ix, z[idx]);
                float zD2 = safeZ(z, iyD * N + ix, z[idx]);

                // Partial derivative in Y: ∂z/∂y ≈ (zU - zD) / Δy
                float dy = ((yMax - yMin) / (N - 1)) * (iyU - iyD);
                float dzdy = (dy == 0) ? 0f : (zU2 - zD2) / dy;

                // Normal = (-∂z/∂x, -∂z/∂y, 1), normalized
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

        // STEP 4: Generate the triangle index buffer
        // A steps×steps grid has steps×steps×2 triangles
        int numTriangles = steps * steps * 2;
        indices = new int[numTriangles * 3];
        int idx = 0;
        for (int iy = 0; iy < steps; iy++) {
            for (int ix = 0; ix < steps; ix++) {
                // The four corners of the quad
                int tl = iy * N + ix;        // top-left
                int tr = tl + 1;             // top-right
                int bl = (iy + 1) * N + ix;  // bottom-left
                int br = bl + 1;             // bottom-right
                
                // Triangle 1: top-left → bottom-left → top-right
                indices[idx++] = tl;
                indices[idx++] = bl;
                indices[idx++] = tr;
                
                // Triangle 2: top-right → bottom-left → bottom-right
                indices[idx++] = tr;
                indices[idx++] = bl;
                indices[idx++] = br;
            }
        }
    }

    /**
     * Safely reads a Z value, returns the fallback for NaN
     */
    private float safeZ(float[] z, int i, float fallback) {
        if (i < 0 || i >= z.length) return fallback;
        float v = z[i];
        return Float.isNaN(v) ? fallback : v;
    }

    /**
     * Interpolates a palette color by its position in [0,1]
     * @param t normalized Z position (0=lowest, 1=highest)
     */
    private float[] colorFromGradient(float t) {
        // Make sure t is in [0,1]
        t = Math.max(0f, Math.min(1f, t));

        // Map t to an index in the color array
        float scaled = t * (COLOR_STOPS.length - 1);
        int lo = (int) scaled;
        int hi = Math.min(lo + 1, COLOR_STOPS.length - 1);
        float frac = scaled - lo; // Linear interpolation between neighboring colors
        
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