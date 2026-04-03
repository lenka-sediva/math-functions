package lvl0fixpipeline.p03transforms;

import lvl0fixpipeline.global.AbstractRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.DoubleBuffer;
import java.util.Locale;

import static lvl0fixpipeline.global.GluUtils.gluLookAt;
import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Shows using 3D transformation and interaction in a scene
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private double dx, dy;
    private double ox, oy;
    private long oldmils;
    private long oldFPSmils;
    private double fps;

    private float uhel = 0;
    private int mode = 0;
    private float[] modelMatrix = new float[16];

    private boolean per = false, depth = true;
    private boolean mouseButton1 = false;

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
                        case GLFW_KEY_D:
                            depth = !depth;
                            break;
                        case GLFW_KEY_M:
                            mode++;
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
                double x = xBuffer.get(0);
                double y = yBuffer.get(0);

                mouseButton1 = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

                if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                    ox = x;
                    oy = y;
                }
            }

        };

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1) {
                    dx = x - ox;
                    dy = y - oy;
                    ox = x;
                    oy = y;
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
        super.init();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);

        glFrontFace(GL_CCW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_LINE);
        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glMatrixMode(GL_MODELVIEW);

        glLoadIdentity();
        glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        // zapnuti nebo vypnuti viditelnosti
        if (depth)
            glEnable(GL_DEPTH_TEST);
        else
            glDisable(GL_DEPTH_TEST);

        // mazeme image buffer i z-buffer
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

        // vypocet fps, nastaveni rychlosti otaceni podle rychlosti prekresleni
        long mils = System.currentTimeMillis();
        if ((mils - oldFPSmils) > 300) {
            fps = 1000 / (double) (mils - oldmils + 1);
            oldFPSmils = mils;
        }
        String textInfo = String.format(Locale.US, "FPS %3.1f", fps);

        //System.out.println(fps);
        float speed = 10; // pocet stupnu rotace za vterinu
        float step = speed * (mils - oldmils) / 1000.0f; // krok za jedno prekresleni
        oldmils = mils;

        glMatrixMode(GL_MODELVIEW);

        mode = mode % 7;

        switch (mode) {
            case 0:
                // rotace postupnou upravou matice
                glRotatef(1, 0, 0, 1);
                break;

            case 1:
                // rotace mazanim matice a zvetsovanim uhlu
                glLoadIdentity();
                uhel++;
                textInfo += ", angle = " + uhel;
                glRotatef(uhel, 0, 0, 1);
                break;

            case 2:
                // rotace podle zmeny pozice mysi
                glRotated(dx, 1, 0, 0);
                glRotated(dy, 0, 0, 1);
                textInfo += ", dx = " + dx + ", dy = " + dy;
                break;

            case 3:
                // rotace podle fps
                glRotatef(step, 0, 0, 1);
                textInfo += ", step = " + step;
                break;

            case 4:
                // rotace mazanim matice a vypocet uhlu na zaklade fps
                glLoadIdentity();
                uhel = (uhel + step) % 360;
                textInfo += ", angle = " + uhel;
                glRotatef(uhel, 0, 0, 1);
                break;

            case 5:
                // rotace podle zmeny pozice mysi, osy rotace rotuji s telesem s telesem
                glLoadIdentity();
                glMultMatrixf(modelMatrix);
                textInfo += ", dx = " + dx + ", dy = " + dy;

                if (Math.abs(dx) > Math.abs(dy)) {
                    glRotated(dx, 0, 1, 0);
                    dx = 0;
                } else {
                    glRotated(dy, 1, 0, 0);
                    dy = 0;
                }
                glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
                break;

            case 6:
                textInfo += ", dx = " + dx + ", dy = " + dy;
                // rotace podle zmeny pozice mysi, osy rotace zustavaji svisle a vodorovne
                glLoadIdentity();
                glRotated(dx, 0, 0, 1);
                glRotated(dy, 0, 1, 0);
                glMultMatrixf(modelMatrix);
                glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
                dx = 0;
                dy = 0;
                break;
        }

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        // nastaveni transformace zobrazovaciho objemu
        if (per)
            gluPerspective(45, width / (float) height, 0.1f, 100.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 100.0f);

        // pohledova transformace
        // divame se do sceny z kladne osy x, osa z je svisla
        gluLookAt(50, 0, 0, 0, 0, 0, 0, 0, 1);

        glBegin(GL_TRIANGLE_FAN);
        glColor3f(1.0f, 1.0f, 1.0f);
        glVertex3f(5.0f, 5.0f, 10.0f);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(10.0f, 0.0f, 0.0f);
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(10.0f, 10.0f, 0.0f);
        glColor3f(1.0f, 1.0f, 0.0f);
        glVertex3f(0.0f, 10.0f, 0.0f);
        glEnd();

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

        float[] color = {1.0f, 1.0f, 1.0f};
        glColor3fv(color);
        glDisable(GL_DEPTH_TEST);

        String text = this.getClass().getName() + ": [Mouse] [M]ode: " + mode + " ";
        if (per)
            text += "[P]ersp, ";
        else
            text += "[p]ersp, ";

        if (depth)
            text += "[D]epth ";
        else
            text += "[d]epth ";

        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
