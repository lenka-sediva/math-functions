package lvl0fixpipeline.p04light;

import lvl0fixpipeline.global.AbstractRenderer;
import lwjglutils.OGLTextRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GluUtils.gluLookAt;
import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Shows light source application
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float mouseX, mouseY;
    private boolean mouseButton1 = false;
    private boolean per = true, flat = false, light = false;
    private int wire = 0;

    public Renderer() {
        super();

        /*used default glfwWindowSizeCallback see AbstractRenderer*/

        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    // We will detect this in our rendering loop
                    glfwSetWindowShouldClose(window, true);
                if (action == GLFW_RELEASE) {
                    //do nothing
                }
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_P:
                            per = !per;
                            break;
                        case GLFW_KEY_F:
                            flat = !flat;
                            break;
                        case GLFW_KEY_L:
                            light = !light;
                            break;
                        case GLFW_KEY_I:
                            wire = (wire + 1);
                            break;

                    }
                }
            }
        };

        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);

                mouseButton1 = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

                if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                    mouseX = (float) xBuffer.get(0);
                    mouseY = (float) yBuffer.get(0);
                }
            }

        };

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1) {
                    mouseX = (float) x;
                    mouseY = (float) y;
                }
            }
        };

        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
                //do nothing
            }
        };
    }

    @Override
    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        textRenderer = new OGLTextRenderer(width, height);

        glEnable(GL_DEPTH_TEST);

        // surface material setting - diffuse reflection
        float[] mat_dif = new float[]{0, 1, 1, 1};
        // surface material setting - specular reflection
        float[] mat_spec = new float[]{1, 0, 0, 1};
        // surface material setting - ambient reflection
        float[] mat_amb = new float[]{0.1f, 0.1f, 0, 1};

        glMaterialfv(GL_FRONT, GL_AMBIENT, mat_amb);
        glMaterialfv(GL_FRONT, GL_DIFFUSE, mat_dif);
        glMaterialfv(GL_FRONT, GL_SPECULAR, mat_spec);
        glMaterialf(GL_FRONT, GL_SHININESS, 10);
        // gl.glMaterialfv(GL_FRONT, GL_EMISSION, mat);

        // light source setting - diffuse component
        float[] light_dif = new float[]{1, 1, 1, 1};
        // light source setting - ambient component
        float[] light_amb = new float[]{1, 1, 1, 1};
        // light source setting - specular component
        float[] light_spec = new float[]{1, 1, 1, 1};

        glLightfv(GL_LIGHT0, GL_AMBIENT, light_amb);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, light_dif);
        glLightfv(GL_LIGHT0, GL_SPECULAR, light_spec);
    }

    private void drawScene() {
        glEnable(GL_NORMALIZE);
        glFrontFace(GL_CCW);
        glPushMatrix();
        glTranslatef(20, 0, 0);
        glutSolidSphere(5, 30, 30);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(0, 0, 0);
        glutSolidSphere(5, 30, 30);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(-20, 0, 0);
        glutSolidSphere(5, 30, 30);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(20, -15, 0);
        glutSolidCube(5);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(-20, -15, 0);
        glutSolidCube(5);
        glPopMatrix();
    }

    public void drawAxis(){
        glBegin(GL_LINES);
        glColor3f(1f, 0f, 0f);
        glVertex3f(0f, 0f, 0f);
        glVertex3f(100f, 0f, 0f);
        glColor3f(0f, 1f, 0f);
        glVertex3f(0f, 0f, 0f);
        glVertex3f(0f, 100f, 0f);
        glColor3f(0f, 0f, 1f);
        glVertex3f(0f, 0f, 0f);
        glVertex3f(0f, 0f, 100f);
        glEnd();
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            gluPerspective(45, width / (float) height, 0.1f, 200.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 200.0f);


        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        gluLookAt(0, 0, 50, 0, 0, 0, 0, 1, 0);
        drawAxis();
        // nastaveni pozice svetla
        float[] light_position;
        if (!light) {
            // bod v prostoru
            light_position = new float[]{ mouseX - width / 2f, height / 2f - mouseY, 25, 1.0f};
        } else {
            // smer - umisteni v nekonecnu
            light_position = new float[]{ mouseX - width / 2f, height / 2f - mouseY, 25, 0.0f};
        }
        glLightfv(GL_LIGHT0, GL_POSITION, light_position);

        glFrontFace(GL_CCW);
        glPushMatrix();
        // koule znazornujici bodovy zdroj svetla
        glTranslatef(mouseX - width / 2f, height / 2f - mouseY, 25);
        glColor3f(1.0f, 1.0f, 0.0f);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glutSolidSphere(1, 10, 10);
        glPopMatrix();


        //orezani odvracenych ploch
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // zapnuti svetla a nastaveni modelu stinovani
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        if (flat)
            glShadeModel(GL_FLAT);
        else
            glShadeModel(GL_SMOOTH);

        // gl.glEnable(GL_POLYGON_OFFSET_FILL);
        wire = wire % 2;
        switch (wire) {
            case 0:
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                break;
            case 1:
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                break;
        }
        drawScene();

        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);


        // zobrazeni telesa bez osvetleni
        glPolygonMode(GL_FRONT, GL_LINE);
        glPolygonMode(GL_BACK, GL_POINT);
        glPushMatrix();
        glColor3f(1.0f, 0f, 1.0f);
        glTranslatef( 10, 15, 0);
        glRotatef(90, 1, 0, 0);
        glutWireSphere(5, 13, 12);
        glPopMatrix();

        glPushMatrix();
        glColor3f(0.0f, 1.f, 0f);
        glTranslatef(-10, 15, 0);
        glRotatef(90, 1, 0, 0);
        glutWireCube(5);
        glPopMatrix();

        float[] color = {1.0f, 1.0f, 1.0f};
        glColor3fv(color);
        glDisable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": [lmb] move, ";

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (flat)
            text += ", [F]lat ";
        else
            text += ", [f]lat ";

        if (light)
            text += ", [L]ight infinity position";
        else
            text += ", [l]ight infinity position";

        switch (wire) {
            case 0:
                text += ", sol[i]d";
                break;
            case 1:
                text += ", w[i]re";
                break;
        }

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        //textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
