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
 * Renderer vizualizéru matematických funkcí f(x, y, t)
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
    private volatile boolean meshDirty = true;

    // Animace
    private volatile boolean animating = false;
    private volatile float   animSpeed = 1.0f;
    private          double  animTime  = 0.0;

    // Přepínače zobrazení
    private boolean showWireframe = false;
    private boolean showNormals   = false;
    private boolean showAxes      = true;
    private boolean showGrid      = true;

    // Kamera
    private GLCamera camera;
    // defaultní pozice pro reset kamery
    private double defaultRadius;
    private double defaultZenith;
    private double defaultAzimuth;
    private Vec3D defaultPosition;

    // Stav myši pro výpočet delta pohybu
    private boolean mouseLeft  = false;
    private boolean mouseRight = false;
    private double  lastMouseX = 0, lastMouseY = 0;

    //  Konstruktor — přepíše protected callback pole z AbstractRenderer
    public FunctionRenderer() {
        super();

        // Klávesnice
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);

                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    switch (key) {
                        case GLFW_KEY_W -> camera.forward(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_S -> camera.backward(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_A -> camera.left(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_D -> camera.right(0.2 * camera.getRadius() * 0.1);
                        case GLFW_KEY_Q -> camera.addAzimuth(-camera.getRadius() * 0.01);
                        case GLFW_KEY_E -> camera.addAzimuth( camera.getRadius() * 0.01);
                        case GLFW_KEY_R -> resetCamera();
                    }
                }
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_M -> showWireframe = !showWireframe;
                        case GLFW_KEY_N -> showNormals   = !showNormals;
                        case GLFW_KEY_O -> showAxes      = !showAxes;
                        case GLFW_KEY_K -> showGrid      = !showGrid;
                    }
                }
            }
        };

        // Tlačítka myši
        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xb, yb);
                lastMouseX = xb.get(0);
                lastMouseY = yb.get(0);

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
                double dx = x - lastMouseX;
                double dy = y - lastMouseY;
                lastMouseX = x;
                lastMouseY = y;

                if (mouseLeft) {
                    // Orbit: azimut + zenit
                    camera.addAzimuth(Math.toRadians(-dx * 0.4));
                    camera.addZenith (Math.toRadians( dy * 0.4));
                }
                if (mouseRight) {
                    // Pan: strafe + up/down
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

    @Override
    public void init() {
        glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_NORMALIZE);
        glShadeModel(GL_SMOOTH);

        // Osvětlení
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glEnable(GL_LIGHT1);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

        // Hlavní světlo (směrové, shora-zprava)
        glLightfv(GL_LIGHT0, GL_POSITION, new float[]{ 5f,  8f,  6f, 0f});
        glLightfv(GL_LIGHT0, GL_DIFFUSE,  new float[]{ 1f,  1f, 0.95f, 1f});
        glLightfv(GL_LIGHT0, GL_AMBIENT,  new float[]{ 0.12f, 0.12f, 0.18f, 1f});
        glLightfv(GL_LIGHT0, GL_SPECULAR, new float[]{ 0.8f, 0.8f, 0.8f, 1f});

        // Doplňkové světlo (zezdola, studené)
        glLightfv(GL_LIGHT1, GL_POSITION, new float[]{-4f, -6f, -3f, 0f});
        glLightfv(GL_LIGHT1, GL_DIFFUSE,  new float[]{ 0.25f, 0.25f, 0.45f, 1f});
        glLightfv(GL_LIGHT1, GL_AMBIENT,  new float[]{ 0f, 0f, 0f, 1f});

        glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR,  new float[]{0.35f, 0.35f, 0.35f, 1f});
        glMaterialf (GL_FRONT_AND_BACK, GL_SHININESS, 40f);

        // Kamera - orbitální, ale s nastavením na third-person (neotáčí se kolem sebe, ale hledí stále dopředu)
        camera = new GLCamera();
        camera.setFirstPerson(false);
        camera.setRadius(10.0);
        camera.setZenith  (Math.toRadians(25));
        camera.setAzimuth (Math.toRadians(30));
        // uložení default hodnot pro reset kamery
        defaultRadius  = camera.getRadius();
        defaultZenith  = camera.getZenith();
        defaultAzimuth = camera.getAzimuth();
        // Počáteční pozice — střed bude přesunut po buildu meshe

        // Mesh
        mesh = new FunctionMesh();
        rebuildMesh(0.0);
    }

    @Override
    public void display() {
        // Animační tik
        if (animating) {
            animTime += 0.016 * animSpeed;
            rebuildMesh(animTime);
        } else if (meshDirty) {
            rebuildMesh(animTime);
        }

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Projekce
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GluUtils.gluPerspective(45.0, (double) width / Math.max(1, height), 0.05, 500.0);

        // View (kamera)
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        camera.setMatrix(); // volá GluUtils.gluLookAt interně

        // Scéna
        if (showAxes) drawAxes();
        if (showGrid) drawXYGrid();

        if (mesh.getVertices() != null) {
            if (showWireframe) drawWireframe();
            else               drawSurface();
            if (showNormals)   drawNormals();
        }
    }

    // Sestavení meshe
    private void rebuildMesh(double t) {
        try {
            if (parser == null || meshDirty) {
                parser = new MathParser(expression);
            }
            mesh.build(parser, xMin, xMax, yMin, yMax, steps, t);

            // Vycentrování orbit kamery
            float cx = (xMin + xMax) / 2f;
            float cy = (yMin + yMax) / 2f;
            float cz = (mesh.getZMin() + mesh.getZMax()) / 2f;

            if (meshDirty) {
                camera.setPosition(new Vec3D(cx, cy, cz));
            }

            if (defaultPosition == null) {
                defaultPosition = new transforms.Vec3D(cx, cy, cz);
            }

            meshDirty = false;
        } catch (ParseException e) {
            System.err.println("[FuncViz] Parse error: " + e.getMessage());
        }
    }

    //  Kreslicí metody
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

    private void drawNormals() {
        glDisable(GL_LIGHTING);
        glColor3f(1f, 1f, 0.2f);
        glLineWidth(1f);
        float[] verts   = mesh.getVertices();
        float[] normals = mesh.getNormals();
        float   scale   = 0.12f;

        glBegin(GL_LINES);
        for (int i = 0; i < verts.length / 3; i += 5) { // každý 5. vrchol
            float vx = verts[i*3],   vy = verts[i*3+1],   vz = verts[i*3+2];
            float nx = normals[i*3], ny = normals[i*3+1], nz = normals[i*3+2];
            glVertex3f(vx,            vy,            vz);
            glVertex3f(vx + nx*scale, vy + ny*scale, vz + nz*scale);
        }
        glEnd();
        glEnable(GL_LIGHTING);
    }

    private void drawAxes() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f); // >1.0 deprecated v Core Profile

        float cx    = (xMin + xMax) / 2f;
        float cy    = (yMin + yMax) / 2f;
        float cz    = (mesh == null) ? 0f : (mesh.getZMin() + mesh.getZMax()) / 2f;
        float span  = Math.max(xMax - xMin, yMax - yMin) * 0.65f;
        float zSpan = (mesh == null) ? span : (mesh.getZMax() - mesh.getZMin()) * 0.75f + 0.5f;

        glBegin(GL_LINES);
        glColor3f(1f, 0.25f, 0.25f); // X – červená
        glVertex3f(cx - span, cy, cz);
        glVertex3f(cx + span, cy, cz);

        glColor3f(0.25f, 1f, 0.25f); // Y – zelená
        glVertex3f(cx, cy - span, cz);
        glVertex3f(cx, cy + span, cz);

        glColor3f(0.35f, 0.55f, 1f); // Z – modrá
        glVertex3f(cx, cy, cz - zSpan);
        glVertex3f(cx, cy, cz + zSpan);
        glEnd();

        glLineWidth(1f);
        glEnable(GL_LIGHTING);
    }

    private void drawXYGrid() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f);
        glColor3f(0.28f, 0.28f, 0.38f);

        float zGround = (mesh == null) ? 0f : mesh.getZMin() - 0.15f;
        float stepX   = (xMax - xMin) / 10f;
        float stepY   = (yMax - yMin) / 10f;

        glBegin(GL_LINES);
        for (float x = xMin; x <= xMax + stepX * 0.01f; x += stepX) {
            glVertex3f(x, yMin, zGround);
            glVertex3f(x, yMax, zGround);
        }
        for (float y = yMin; y <= yMax + stepY * 0.01f; y += stepY) {
            glVertex3f(xMin, y, zGround);
            glVertex3f(xMax, y, zGround);
        }
        glEnd();

        glLineWidth(1f);
        glEnable(GL_LIGHTING);
    }

    // Veřejné API pro Swing GUI (volané z EDT)
    public void applySettings(String expr, float x0, float x1,
                              float y0, float y1, int s) {
        this.expression = expr;
        this.xMin = x0;  this.xMax = x1;
        this.yMin = y0;  this.yMax = y1;
        this.steps = Math.max(10, Math.min(s, 300));
        this.meshDirty = true;
    }

    public void setAnimating(boolean on, float speed) {
        this.animating = on;
        this.animSpeed = speed;
        if (!on) this.meshDirty = true;
    }

    private void resetCamera() {
        camera.setRadius(defaultRadius);
        camera.setZenith(defaultZenith);
        camera.setAzimuth(defaultAzimuth);

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
    public boolean isAnimating()   { return animating; }
}