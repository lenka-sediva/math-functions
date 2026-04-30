package lvl0fixpipeline.app;

import lvl0fixpipeline.global.*;
import lvl0fixpipeline.app.parser.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import transforms.Vec3D;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * RENDERER — OpenGL visualizátor matematických funkcí f(x, y, t)
 *
 * Architektura:
 *  - Dědí AbstractRenderer, přepisuje protected GLFW callback pole
 *  - Používá GLCamera (third-person orbit) z balíčku global
 *  - Používá GluUtils.gluPerspective + GLCamera.setMatrix() pro transformace
 *  - OpenGL fixed pipeline (GL_LIGHTING, glBegin/glEnd)
 */
public class FunctionRenderer extends AbstractRenderer {
    // Parser a mesh
    private MathParser   parser;
    private FunctionMesh mesh;

    // Parametry vizualizace (nastavuje Swing GUI)
    private volatile String  expression = "sin(sqrt(x*x + y*y))"; // výchozí výraz
    private volatile float   xMin = -6f, xMax = 6f;
    private volatile float   yMin = -6f, yMax = 6f;
    private volatile int     steps     = 80;
    private volatile boolean meshDirty = true; // flag, že mesh je zastaralý, potřebuje rebuild

    // Animace
    private volatile boolean animating = false;
    private volatile float   animSpeed = 1.0f;
    private          double  animTime  = 0.0;

    // Přepínače zobrazení
    private boolean showWireframe = false;
    private boolean showNormals   = false;
    private boolean showAxes      = true;
    private boolean showGrid      = true;
    private boolean isOrthographic = false;

    // Kamera
    private GLCamera camera;

    // Výchozí hodnoty pro reset
    private double defaultRadius;
    private double defaultZenith;
    private double defaultAzimuth;
    private Vec3D defaultPosition;

    // Stav myši pro výpočet delta pohybu
    private boolean mouseLeft  = false;
    private boolean mouseRight = false;
    private double  lastMouseX = 0, lastMouseY = 0;

    /**
     * Konstruktor — inicializuje GLFW callbacky
     */
    public FunctionRenderer() {
        super();

        // Klávesnice
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                // ESC → zavřít okno
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);

                // Kontinuální akce (opakují se při držení tlačítka)
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    switch (key) {
                        case GLFW_KEY_W -> camera.forward(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_S -> camera.backward(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_A -> camera.left(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_D -> camera.right(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_Q -> camera.addAzimuth(-camera.getRadius() * 0.01);
                        case GLFW_KEY_E -> camera.addAzimuth( camera.getRadius() * 0.01);
                        case GLFW_KEY_R -> resetCamera(true);
                    }
                }

                // Jedenkrát při stisknutí
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_M -> showWireframe = !showWireframe;
                        case GLFW_KEY_N -> showNormals   = !showNormals;
                        case GLFW_KEY_O -> showAxes      = !showAxes;
                        case GLFW_KEY_K -> showGrid      = !showGrid;
                        case GLFW_KEY_P -> isOrthographic = !isOrthographic; // Přepínání ortografický/perspektivní
                        case GLFW_KEY_X -> setViewFromAxis(0); // Pohled z osy X
                        case GLFW_KEY_Y -> setViewFromAxis(1); // Pohled z osy Y
                        case GLFW_KEY_Z -> setViewFromAxis(2); // Pohled z osy Z
                    }
                }
            }
        };

        // Tlačítka myši
        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                // Načte aktuální pozici myši
                DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xb, yb);
                lastMouseX = xb.get(0);
                lastMouseY = yb.get(0);

                // Flagy pro levé/pravé tlačítko
                if (button == GLFW_MOUSE_BUTTON_LEFT)
                    mouseLeft  = (action == GLFW_PRESS);
                if (button == GLFW_MOUSE_BUTTON_RIGHT)
                    mouseRight = (action == GLFW_PRESS);
            }
        };

        // Pohyb myší
        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                // Počítá delta pohybu
                double dx = x - lastMouseX;
                double dy = y - lastMouseY;
                lastMouseX = x;
                lastMouseY = y;

                // Levá myš — orbituj kolem cíle
                if (mouseLeft) {
                    camera.addAzimuth(Math.toRadians(-dx * 0.4));
                    camera.addZenith (Math.toRadians( dy * 0.4));
                }
                // Pravá myš — pan (posuň kameru)
                if (mouseRight) {
                    double panScale = camera.getRadius() * 0.001;
                    camera.right(-dx * panScale);
                    camera.up   ( dy * panScale);
                }
            }
        };

        // Kolečko myši → zoom
        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
                camera.mulRadius(dy > 0 ? 0.92 : 1.08);
            }
        };
    }

    /**
     * INICIALIZACE — nastav OpenGL, kameru, osvětlení
     */
    @Override
    public void init() {
        glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_NORMALIZE); // Automaticky normalizuj normály
        glShadeModel(GL_SMOOTH); // Smooth shading

        // Osvětlení
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);    // Hlavní světlo
        glEnable(GL_LIGHT1);    // Doplňkové světło
        glEnable(GL_COLOR_MATERIAL); // Ať se barva ovlivňuje osvětlením

        // Materiál objektu
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

        // Hlavní světlo — shora zprava (teplé)
        glLightfv(GL_LIGHT0, GL_POSITION, new float[]{ 5f,  8f,  6f, 0f});
        glLightfv(GL_LIGHT0, GL_DIFFUSE,  new float[]{ 1f,  1f, 0.95f, 1f});
        glLightfv(GL_LIGHT0, GL_AMBIENT,  new float[]{ 0.12f, 0.12f, 0.18f, 1f});
        glLightfv(GL_LIGHT0, GL_SPECULAR, new float[]{ 0.8f, 0.8f, 0.8f, 1f});

        // Doplňkové světlo — zezdola (chladné, jen pro vyplnění stínů)
        glLightfv(GL_LIGHT1, GL_POSITION, new float[]{-4f, -6f, -3f, 0f});
        glLightfv(GL_LIGHT1, GL_DIFFUSE,  new float[]{ 0.25f, 0.25f, 0.45f, 1f});
        glLightfv(GL_LIGHT1, GL_AMBIENT,  new float[]{ 0f, 0f, 0f, 1f});

        // Vlastnosti materiálu
        glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR,  new float[]{0.35f, 0.35f, 0.35f, 1f});
        glMaterialf (GL_FRONT_AND_BACK, GL_SHININESS, 40f);

        // Kamera - orbitální, ale s nastavením na third-person (neotáčí se kolem sebe, ale hledí stále dopředu)
        camera = new GLCamera();
        camera.setFirstPerson(false); // Third-person mode
        camera.setRadius(10.0);
        camera.setZenith  (Math.toRadians(25)); // Úhel od nahoře
        camera.setAzimuth (Math.toRadians(30)); // Azimutální úhel

        // Uloží default hodnoty pro reset
        defaultRadius  = camera.getRadius();
        defaultZenith  = camera.getZenith();
        defaultAzimuth = camera.getAzimuth();

        // Mesh
        mesh = new FunctionMesh();
        rebuildMesh(0.0);
    }

    /**
     * RENDER LOOP — voláno každý frame
     */
    @Override
    public void display() {
        // Animační tik
        if (animating) {
            animTime += 0.016 * animSpeed; // ~60fps
            rebuildMesh(animTime);
        } else if (meshDirty) {
            // Pokud se změnila funkce → přebuduj jednou
            rebuildMesh(animTime);
        }

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Projekce
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        double aspect = (double) width / Math.max(1, height);
        if (isOrthographic) {
            double orthoSize = Math.max(xMax - xMin, yMax - yMin) * 0.75; // velikost ortho projekce založená na rozsahu funkce
            glOrtho(-orthoSize, orthoSize, -orthoSize * aspect, orthoSize * aspect, -500, 500);
        } else {
            GluUtils.gluPerspective(45.0, aspect, 0.05, 500.0);
        }

        // View (kamera)
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        camera.setMatrix(); // Nastaví gluLookAt interně

        // Posun do středu
        float cx = (xMin + xMax) / 2f;
        float cy = (yMin + yMax) / 2f;
        float cz = (mesh.getZMin() + mesh.getZMax()) / 2f;

        glTranslatef(-cx, -cy, -cz);

        // Scéna
        if (showAxes) drawAxes();
        if (showGrid) drawXYGrid();

        if (mesh.getVertices() != null) {
            if (showWireframe) drawWireframe();
            else               drawSurface();
            if (showNormals)   drawNormals();
        }
    }

    /**
     * Přebuduje mesh — parseuje výraz, vzorkuje funkci, počítá normály
     */
    private void rebuildMesh(double t) {
        try {
            // Pokud je výraz nový nebo zastaralý → reparse
            if (parser == null || meshDirty) {
                parser = new MathParser(expression);
            }
            // Builduj mesh
            mesh.build(parser, xMin, xMax, yMin, yMax, steps, t);

            meshDirty = false;

            // Vycentruje kameru na střed meshe (jen při prvním buildu nebo po Aplikovat změny)
            float cx = (xMin + xMax) / 2f;
            float cy = (yMin + yMax) / 2f;
            float cz = (mesh.getZMin() + mesh.getZMax()) / 2f;

            if (defaultPosition == null) {
                defaultPosition = new Vec3D(cx, cy, cz);
                camera.setPosition(defaultPosition);
            }
        } catch (ParseException e) {
            System.err.println("[FuncViz] Parse error: " + e.getMessage());
        }
    }

    // KRESLÍCÍ METODY
    /**
     * Vykresli plný povrch se stínováním a osvětlením
     */
    private void drawSurface() {
        glEnable(GL_LIGHTING);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        float[] vertices = mesh.getVertices();
        float[] normals  = mesh.getNormals();
        float[] colors   = mesh.getColors();
        int[]   indices  = mesh.getIndices();

        glBegin(GL_TRIANGLES);
        for (int idx : indices) {
            int i = idx * 3;
            glNormal3f(normals[i],  normals[i+1],  normals[i+2]);
            glColor3f (colors[i],   colors[i+1],   colors[i+2]);
            glVertex3f(vertices[i], vertices[i+1], vertices[i+2]);
        }
        glEnd();
    }

    /**
     * Vykresli drátový model (bez vyplnění)
     */
    private void drawWireframe() {
        glDisable(GL_LIGHTING);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(1f);

        float[] vertices = mesh.getVertices();
        float[] colors   = mesh.getColors();
        int[]   indices  = mesh.getIndices();

        glBegin(GL_TRIANGLES);
        for (int idx : indices) {
            int i = idx * 3;
            glColor3f (colors[i],   colors[i+1],   colors[i+2]);
            glVertex3f(vertices[i], vertices[i+1], vertices[i+2]);
        }
        glEnd();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_LIGHTING);
    }

    /**
     * Vykresli normály jako malé žluté čáry
     */
    private void drawNormals() {
        glDisable(GL_LIGHTING);
        glColor3f(1f, 1f, 0.2f); // Žlutá
        glLineWidth(1f);
        float[] verts   = mesh.getVertices();
        float[] normals = mesh.getNormals();
        float   scale   = 0.12f; // Délka normály

        glBegin(GL_LINES);
        // Vykresli každý 5. vertex (aby nebyl chaos)
        for (int i = 0; i < verts.length / 3; i += 5) {
            float vx = verts[i*3],   vy = verts[i*3+1],   vz = verts[i*3+2];
            float nx = normals[i*3], ny = normals[i*3+1], nz = normals[i*3+2];
            glVertex3f(vx,            vy,            vz);
            glVertex3f(vx + nx*scale, vy + ny*scale, vz + nz*scale);
        }
        glEnd();
        glEnable(GL_LIGHTING);
    }

    /**
     * Vykresli souřadnicové osy (X=red, Y=green, Z=blue)
     */
    private void drawAxes() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f);

        // kreslí osy na střed meshe
        float cx = (xMin + xMax) / 2f;
        float cy = (yMin + yMax) / 2f;
        float cz = 0f;

        float span  = Math.max(xMax - xMin, yMax - yMin) * 0.65f;
        float zSpan = (mesh == null) ? span : (mesh.getZMax() - mesh.getZMin()) * 0.75f + 0.5f;

        glBegin(GL_LINES);
        // X — červená
        glColor3f(1f, 0.25f, 0.25f);
        glVertex3f(cx - span, cy, cz);
        glVertex3f(cx + span, cy, cz);

        // Y — zelená
        glColor3f(0.25f, 1f, 0.25f);
        glVertex3f(cx, cy - span, cz);
        glVertex3f(cx, cy + span, cz);

        // Z — modrá
        glColor3f(0.35f, 0.55f, 1f);
        glVertex3f(cx, cy, cz - zSpan);
        glVertex3f(cx, cy, cz + zSpan);
        glEnd();

        glLineWidth(1f);
        glEnable(GL_LIGHTING);
    }

    /**
     * Vykresli podkladovou mřížku (v rovině Z)
     */
    private void drawXYGrid() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f);
        glColor3f(0.28f, 0.28f, 0.38f); // Tmavě modrá

        // Z-pozice mřížky (pod povrchem)
        float zGround = - (mesh.getZMax() - mesh.getZMin()) / 2f - 0.15f;

        float stepX   = (xMax - xMin) / 10f;
        float stepY   = (yMax - yMin) / 10f;

        glBegin(GL_LINES);
        // Svislé čáry (konstantní X)
        for (float x = xMin; x <= xMax + stepX * 0.01f; x += stepX) {
            glVertex3f(x, yMin, zGround);
            glVertex3f(x, yMax, zGround);
        }
        // Vodorovné čáry (konstantní Y)
        for (float y = yMin; y <= yMax + stepY * 0.01f; y += stepY) {
            glVertex3f(xMin, y, zGround);
            glVertex3f(xMax, y, zGround);
        }
        glEnd();

        glLineWidth(1f);
        glEnable(GL_LIGHTING);
    }

    // PUBLIC API — Swing GUI
    /**
     * Aplikuje nová nastavení z GUI
     */
    public void applySettings(String expr, float x0, float x1,
                              float y0, float y1, int s) {
        this.expression = expr;
        this.xMin = x0;  this.xMax = x1;
        this.yMin = y0;  this.yMax = y1;
        this.steps = Math.max(10, Math.min(s, 300)); // Limituj kroky
        this.meshDirty = true;
    }

    /**
     * Zapne/vypne animaci
     */
    public void setAnimating(boolean on, float speed) {
        this.animating = on;
        this.animSpeed = speed;
        if (!on) this.meshDirty = true;
    }

    /**
     * Nastaví pohled z osy (X=0, Y=1, Z=2)
     */
    private void setViewFromAxis(int axis) {
        resetCamera(false);

        camera.setRadius(20.0); // radius pro vzdálenost pozorovatele

        switch (axis) {
            case 0: // Pohled z osy X (kladná X)
                camera.setAzimuth(0);
                camera.setZenith(0);
                break;
            case 1: // Pohled z osy Y (kladná Y)
                camera.setAzimuth(Math.PI / 2);
                camera.setZenith(0);
                break;
            case 2: // Pohled z osy Z (kladná Z)
                camera.setAzimuth(0);
                camera.setZenith(Math.PI / 2);
                break;
        }
    }

    /**
     * Resetuje kameru na výchozí pozici
     */
    public void resetCamera(boolean resetOrtho) {
        camera.setRadius(defaultRadius);
        camera.setZenith(defaultZenith);
        camera.setAzimuth(defaultAzimuth);

        if (resetOrtho) {
            isOrthographic = false; // reset na perspektivní pohled
        }

        if (defaultPosition != null) {
            camera.setPosition(defaultPosition);
        }
    }

    public String  getExpression() { return expression; }
    public float   getXMin()       { return xMin; }
    public float   getXMax()       { return xMax; }
    public float   getYMin()       { return yMin; }
    public float   getYMax()       { return yMax; }
    public int     getSteps()      { return steps; }
}
