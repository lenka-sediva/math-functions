package lvl0fixpipeline.p10fog;

import lvl0fixpipeline.global.AbstractRenderer;
import lvl0fixpipeline.global.GLCamera;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.Vec3D;

import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.glutSolidSphere;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * It shows the possibilities of applying fog in the scene
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;
    private float zenit, azimut;

    private float trans, deltaTrans = 0;

    private float uhel = 0;

    private boolean mouseButton1 = false;
    private boolean per = true, move = false;
    private int fog;
    private float fogStart = 0f;
    private float fogEnd = 100f;
    private float fogDensity = 0.08f;

    private GLCamera camera;

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
                    trans = 0;
                    deltaTrans = 0;
                }

                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_P:
                            per = !per;
                            break;
                        case GLFW_KEY_M:
                            move = !move;
                            break;
                        case GLFW_KEY_F:
                            fog++;
                            break;
                        case GLFW_KEY_W:
                        case GLFW_KEY_S:
                        case GLFW_KEY_A:
                        case GLFW_KEY_D:
                            deltaTrans = 0.001f;
                            break;
                    }
                }

                switch (key) {
                    case GLFW_KEY_W:
                        camera.forward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_S:
                        camera.backward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_A:
                        camera.left(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_D:
                        camera.right(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_H:
                        fogStart += 0.1f;
                        break;
                    case GLFW_KEY_G:
                        fogStart -= 0.1f;
                        break;
                    case GLFW_KEY_N:
                        fogEnd *= 1.05f;
                        break;
                    case GLFW_KEY_B:
                        fogEnd *= 0.95f;
                        break;
                    case GLFW_KEY_Y:
                        fogDensity *= 1.05f;
                        break;
                    case GLFW_KEY_T:
                        fogDensity *= 0.95f;
                        break;
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
                    ox = (float) x;
                    oy = (float) y;
                }
            }

        };

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1) {
                    dx = (float) x - ox;
                    dy = (float) y - oy;
                    ox = (float) x;
                    oy = (float) y;
                    zenit -= dy / width * 180;
                    if (zenit > 90)
                        zenit = 90;
                    if (zenit <= -90)
                        zenit = -90;
                    azimut += dx / height * 180;
                    azimut = azimut % 360;
                    camera.setAzimuth(Math.toRadians(azimut));
                    camera.setZenith(Math.toRadians(zenit));
                    dx = 0;
                    dy = 0;
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
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_FILL);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        camera = new GLCamera();
        camera.setPosition(new Vec3D(10));
        camera.setFirstPerson(true);

        scene();
    }

    private void scene() {
        glNewList(1, GL_COMPILE);
        glPushMatrix();
        glTranslatef(100, 0, 0);

        glColor3f(1, 0, 0);
        for (int i = 0; i < 10; i++) {
            glTranslatef(-10, 0, 0);
            glutSolidSphere(5, 30, 30);
        }
        glColor3f(0.5f, 0, 0);
        for (int i = 0; i < 10; i++) {
            glTranslatef(-10, 0, 0);
            glutSolidSphere(5, 30, 30);
        }

        glPopMatrix();

        glPushMatrix();
        glTranslatef(0, 100, 0);

        glColor3f(0, 1, 0);
        for (int i = 0; i < 10; i++) {
            glTranslatef(0, -10, 0);
            glutSolidSphere(5, 30, 30);
        }
        glColor3f(0, 0.5f, 0);
        for (int i = 0; i < 10; i++) {
            glTranslatef(0, -10, 0);
            glutSolidSphere(5, 30, 30);
        }
        glPopMatrix();

        glPushMatrix();
        glTranslatef(0, 0, 100);
        glColor3f(0, 0, 1);
        for (int i = 0; i < 10; i++) {
            glTranslatef(0, 0, -10);
            glutSolidSphere(5, 30, 30);
        }
        glColor3f(0, 0, 0.5f);
        for (int i = 0; i < 10; i++) {
            glTranslatef(0, 0, -10);
            glutSolidSphere(5, 30, 30);
        }
        glPopMatrix();

        glTranslatef(0, 0, 0);
        glColor3f(1, 1, 1);
        glutSolidSphere(8, 30, 30);

        glEndList();
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": [lmb] move";

        trans += deltaTrans;

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            gluPerspective(45, width / (float) height, 0.1f, 500.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 500.0f);

        if (move) {
            uhel++;
        }

        String textInfo = "";
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogi(GL_FOG_START, (int) fogStart);
        glFogi(GL_FOG_END, (int) fogEnd);
        glFogf(GL_FOG_DENSITY, fogDensity);
        glFogfv(GL_FOG_COLOR, new float[]{0.1f, 0.1f, 0.1f, 1});
        switch (fog % 4) {
            case 0:
                glDisable(GL_FOG);
                text += ", [F]og func: Disable";
                break;
            case 1:
                //fog = (end-z)/(end-start)
                glFogi(GL_FOG_MODE, GL_LINEAR);
                textInfo += "fogStart[GH]: " + fogStart + ", fogEnd[BN]: " + fogEnd + ", fog = (end-z)/(end-start)";
                text += ", [F]og func: GL_LINEAR";
                break;
            case 2:
                //fog = e^(-(density*z))
                glFogi(GL_FOG_MODE, GL_EXP);
                textInfo += " fogDensity[TY]: " + fogDensity + ",  fog = e^(-(density*z))";
                text += ", [F]og func: GL_EXP";
                break;
            case 3:
                //fog = e^(-(density*z)^2)
                glFogi(GL_FOG_MODE, GL_EXP2);
                textInfo += " fogDensity[TY]: " + fogDensity + ", fog = e^(-(density*z)^2)";
                text += ", [F]og func: GL_EXP2";
                break;
        }


        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glPushMatrix();
        camera.setMatrix();
        glRotatef(uhel, 0, 1, 0);
        glCallList(1);
        glPopMatrix();

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (move)
            text += ", Ani[M] ";
        else
            text += ", Ani[m] ";


        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        if (fog > 0) textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
