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
 * RENDERER — OpenGL visualizer of math functions f(x, y, t)
 *
 * Architecture:
 *  - Extends AbstractRenderer, overrides the protected GLFW callback fields
 *  - Uses GLCamera (third-person orbit) from the global package
 *  - Uses GluUtils.gluPerspective + GLCamera.setMatrix() for the transformations
 *  - OpenGL fixed pipeline (GL_LIGHTING, glBegin/glEnd)
 */
public class FunctionRenderer extends AbstractRenderer {
    // Parser and mesh
    private MathParser   parser;
    private FunctionMesh mesh;

    // Visualization parameters (set by the Swing GUI)
    private volatile String  expression = "sin(sqrt(x*x + y*y))"; // default expression
    private volatile float   xMin = -6f, xMax = 6f;
    private volatile float   yMin = -6f, yMax = 6f;
    private volatile int     steps     = 80;
    private volatile boolean meshDirty = true; // flag: the mesh is out of date and needs a rebuild

    // Animation
    private volatile boolean animating = false;
    private volatile float   animSpeed = 1.0f;
    private          double  animTime  = 0.0;

    // Display toggles
    private boolean showWireframe = false;
    private boolean showNormals   = false;
    private boolean showAxes      = true;
    private boolean showGrid      = true;
    private boolean isOrthographic = false;

    // Camera
    private GLCamera camera;

    // Default values for reset
    private double defaultRadius;
    private double defaultZenith;
    private double defaultAzimuth;
    private Vec3D defaultPosition;

    // Mouse state for computing the movement delta
    private boolean mouseLeft  = false;
    private boolean mouseRight = false;
    private double  lastMouseX = 0, lastMouseY = 0;

    /**
     * Constructor — initializes the GLFW callbacks
     */
    public FunctionRenderer() {
        super();

        // Keyboard
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                // ESC → close the window
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);

                // Continuous actions (repeat while the key is held)
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

                // Once per key press
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_M -> showWireframe = !showWireframe;
                        case GLFW_KEY_N -> showNormals   = !showNormals;
                        case GLFW_KEY_O -> showAxes      = !showAxes;
                        case GLFW_KEY_K -> showGrid      = !showGrid;
                        case GLFW_KEY_P -> isOrthographic = !isOrthographic; // Toggle orthographic/perspective
                        case GLFW_KEY_X -> setViewFromAxis(0); // View from the X axis
                        case GLFW_KEY_Y -> setViewFromAxis(1); // View from the Y axis
                        case GLFW_KEY_Z -> setViewFromAxis(2); // View from the Z axis
                    }
                }
            }
        };

        // Mouse buttons
        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                // Read the current mouse position
                DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xb, yb);
                lastMouseX = xb.get(0);
                lastMouseY = yb.get(0);

                // Flags for the left/right button
                if (button == GLFW_MOUSE_BUTTON_LEFT)
                    mouseLeft  = (action == GLFW_PRESS);
                if (button == GLFW_MOUSE_BUTTON_RIGHT)
                    mouseRight = (action == GLFW_PRESS);
            }
        };

        // Mouse movement
        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                // Compute the movement delta
                double dx = x - lastMouseX;
                double dy = y - lastMouseY;
                lastMouseX = x;
                lastMouseY = y;

                // Left mouse — orbit around the target
                if (mouseLeft) {
                    camera.addAzimuth(Math.toRadians(-dx * 0.4));
                    camera.addZenith (Math.toRadians( dy * 0.4));
                }
                // Right mouse — pan (move the camera)
                if (mouseRight) {
                    double panScale = camera.getRadius() * 0.001;
                    camera.right(-dx * panScale);
                    camera.up   ( dy * panScale);
                }
            }
        };

        // Mouse wheel → zoom
        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
                camera.mulRadius(dy > 0 ? 0.92 : 1.08);
            }
        };
    }

    /**
     * INITIALIZATION — set up OpenGL, the camera and lighting
     */
    @Override
    public void init() {
        glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glEnable(GL_NORMALIZE); // Normalize normals automatically
        glShadeModel(GL_SMOOTH); // Smooth shading

        // Lighting
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);    // Main light
        glEnable(GL_LIGHT1);    // Fill light
        glEnable(GL_COLOR_MATERIAL); // Let lighting affect the color

        // Object material
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);

        // Main light — from the top right (warm)
        glLightfv(GL_LIGHT0, GL_POSITION, new float[]{ 5f,  8f,  6f, 0f});
        glLightfv(GL_LIGHT0, GL_DIFFUSE,  new float[]{ 1f,  1f, 0.95f, 1f});
        glLightfv(GL_LIGHT0, GL_AMBIENT,  new float[]{ 0.12f, 0.12f, 0.18f, 1f});
        glLightfv(GL_LIGHT0, GL_SPECULAR, new float[]{ 0.8f, 0.8f, 0.8f, 1f});

        // Fill light — from below (cool, only to fill in shadows)
        glLightfv(GL_LIGHT1, GL_POSITION, new float[]{-4f, -6f, -3f, 0f});
        glLightfv(GL_LIGHT1, GL_DIFFUSE,  new float[]{ 0.25f, 0.25f, 0.45f, 1f});
        glLightfv(GL_LIGHT1, GL_AMBIENT,  new float[]{ 0f, 0f, 0f, 1f});

        // Material properties
        glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR,  new float[]{0.35f, 0.35f, 0.35f, 1f});
        glMaterialf (GL_FRONT_AND_BACK, GL_SHININESS, 40f);

        // Camera — orbital, set to third-person (it does not spin in place, it keeps looking forward)
        camera = new GLCamera();
        camera.setFirstPerson(false); // Third-person mode
        camera.setRadius(10.0);
        camera.setZenith  (Math.toRadians(25)); // Angle from the top
        camera.setAzimuth (Math.toRadians(30)); // Azimuth angle

        // Store the default values for reset
        defaultRadius  = camera.getRadius();
        defaultZenith  = camera.getZenith();
        defaultAzimuth = camera.getAzimuth();

        // Mesh
        mesh = new FunctionMesh();
        rebuildMesh(0.0);
    }

    /**
     * RENDER LOOP — called every frame
     */
    @Override
    public void display() {
        // Animation tick
        if (animating) {
            animTime += 0.016 * animSpeed; // ~60fps
            rebuildMesh(animTime);
        } else if (meshDirty) {
            // The function changed → rebuild once
            rebuildMesh(animTime);
        }

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        double aspect = (double) width / Math.max(1, height);
        if (isOrthographic) {
            double orthoSize = Math.max(xMax - xMin, yMax - yMin) * 0.75; // ortho projection size based on the function range
            glOrtho(-orthoSize, orthoSize, -orthoSize * aspect, orthoSize * aspect, -500, 500);
        } else {
            GluUtils.gluPerspective(45.0, aspect, 0.05, 500.0);
        }

        // View (camera)
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        camera.setMatrix(); // Calls gluLookAt internally

        // Move to the center
        float cx = (xMin + xMax) / 2f;
        float cy = (yMin + yMax) / 2f;
        float cz = (mesh.getZMin() + mesh.getZMax()) / 2f;

        glTranslatef(-cx, -cy, -cz);

        // Scene
        if (showAxes) drawAxes();
        if (showGrid) drawXYGrid();

        if (mesh.getVertices() != null) {
            if (showWireframe) drawWireframe();
            else               drawSurface();
            if (showNormals)   drawNormals();
        }
    }

    /**
     * Rebuilds the mesh — parses the expression, samples the function, computes the normals
     */
    private void rebuildMesh(double t) {
        try {
            // The expression is new or out of date → parse it again
            if (parser == null || meshDirty) {
                parser = new MathParser(expression);
            }
            // Build the mesh
            mesh.build(parser, xMin, xMax, yMin, yMax, steps, t);

            meshDirty = false;

            // Center the camera on the mesh (only on the first build or after Apply changes)
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

    // DRAWING METHODS
    /**
     * Draws the solid surface with shading and lighting
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
     * Draws the wireframe (no fill)
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
     * Draws the normals as short yellow lines
     */
    private void drawNormals() {
        glDisable(GL_LIGHTING);
        glColor3f(1f, 1f, 0.2f); // Yellow
        glLineWidth(1f);
        float[] verts   = mesh.getVertices();
        float[] normals = mesh.getNormals();
        float   scale   = 0.12f; // Normal length

        glBegin(GL_LINES);
        // Draw every 5th vertex (to avoid clutter)
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
     * Draws the coordinate axes (X=red, Y=green, Z=blue)
     */
    private void drawAxes() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f);

        // draws the axes at the center of the mesh
        float cx = (xMin + xMax) / 2f;
        float cy = (yMin + yMax) / 2f;
        float cz = 0f;

        float span  = Math.max(xMax - xMin, yMax - yMin) * 0.65f;
        float zSpan = (mesh == null) ? span : (mesh.getZMax() - mesh.getZMin()) * 0.75f + 0.5f;

        glBegin(GL_LINES);
        // X — red
        glColor3f(1f, 0.25f, 0.25f);
        glVertex3f(cx - span, cy, cz);
        glVertex3f(cx + span, cy, cz);

        // Y — green
        glColor3f(0.25f, 1f, 0.25f);
        glVertex3f(cx, cy - span, cz);
        glVertex3f(cx, cy + span, cz);

        // Z — blue
        glColor3f(0.35f, 0.55f, 1f);
        glVertex3f(cx, cy, cz - zSpan);
        glVertex3f(cx, cy, cz + zSpan);
        glEnd();

        glLineWidth(1f);
        glEnable(GL_LIGHTING);
    }

    /**
     * Draws the ground grid (in a Z plane)
     */
    private void drawXYGrid() {
        glDisable(GL_LIGHTING);
        glLineWidth(1f);
        glColor3f(0.28f, 0.28f, 0.38f); // Dark blue

        // Z position of the grid (below the surface)
        float zGround = - (mesh.getZMax() - mesh.getZMin()) / 2f - 0.15f;

        float stepX   = (xMax - xMin) / 10f;
        float stepY   = (yMax - yMin) / 10f;

        glBegin(GL_LINES);
        // Vertical lines (constant X)
        for (float x = xMin; x <= xMax + stepX * 0.01f; x += stepX) {
            glVertex3f(x, yMin, zGround);
            glVertex3f(x, yMax, zGround);
        }
        // Horizontal lines (constant Y)
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
     * Applies new settings from the GUI
     */
    public void applySettings(String expr, float x0, float x1,
                              float y0, float y1, int s) {
        this.expression = expr;
        this.xMin = x0;  this.xMax = x1;
        this.yMin = y0;  this.yMax = y1;
        this.steps = Math.max(10, Math.min(s, 300)); // Clamp the steps
        this.meshDirty = true;
    }

    /**
     * Turns the animation on/off
     */
    public void setAnimating(boolean on, float speed) {
        this.animating = on;
        this.animSpeed = speed;
        if (!on) this.meshDirty = true;
    }

    /**
     * Sets the view from an axis (X=0, Y=1, Z=2)
     */
    private void setViewFromAxis(int axis) {
        resetCamera(false);

        camera.setRadius(20.0); // radius = distance of the viewer

        switch (axis) {
            case 0: // View from the X axis (positive X)
                camera.setAzimuth(0);
                camera.setZenith(0);
                break;
            case 1: // View from the Y axis (positive Y)
                camera.setAzimuth(Math.PI / 2);
                camera.setZenith(0);
                break;
            case 2: // View from the Z axis (positive Z)
                camera.setAzimuth(0);
                camera.setZenith(Math.PI / 2);
                break;
        }
    }

    /**
     * Resets the camera to its default position
     */
    public void resetCamera(boolean resetOrtho) {
        camera.setRadius(defaultRadius);
        camera.setZenith(defaultZenith);
        camera.setAzimuth(defaultAzimuth);

        if (resetOrtho) {
            isOrthographic = false; // back to the perspective view
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
