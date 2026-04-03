package lvl0fixpipeline.p07viewport;

import lvl0fixpipeline.global.AbstractRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GluUtils.gluLookAt;
import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.glutSolidCube;
import static lvl0fixpipeline.global.GlutUtils.glutSolidSphere;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shows application of viewport transformation
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;

    private float uhel = 0;
    private float[] modelMatrix = new float[16];

    private boolean mouseButton1 = false;
    private boolean per = true, move = false, wire = true;

    public Renderer() {
        super();

        /*used default glfwWindowSizeCallback see AbstractRenderer*/

        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    // We will detect this in our rendering loop
                    glfwSetWindowShouldClose(window, true);
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_P:
                            per = !per;
                            break;
                        case GLFW_KEY_M:
                            move = !move;
                            break;
                        case GLFW_KEY_W:
                            wire = !wire;
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
        glPolygonMode(GL_BACK, GL_NONE);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
    }

    private void drawScene() {
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glScalef(3, 3, 3);

        glPushMatrix();
        glRotatef(2 * uhel, 0, 0, 1);
        glColor3f(0f, 1.0f, 0f);
        glutSolidCube(5f);
        glPopMatrix();

        glPushMatrix();
        glRotatef(0.5f * uhel, 0, 1, 0);
        glTranslatef(5, 0, 0);
        glColor3f(0.6f, 0.1f, 0.1f);
        glutSolidSphere(2f, 16, 16);
        glPopMatrix();

        glPopMatrix();
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
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

        if (move) uhel++;

        if (wire)
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        else
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        gluLookAt(50, 0, 0, 0, 0, 0, 0, 0, 1);
        glViewport(0, height / 2, width / 2, height / 2);
        drawScene();

        glLoadIdentity();
        gluLookAt(0, 50, 0, 0, 0, 0, 0, 0, 1);
        glViewport(width / 2, height / 2, width / 2, height / 2);
        drawScene();

        glLoadIdentity();
        gluLookAt(0, 0, 50, 0, 0, 0, -1, 0, 0);
        glViewport(0, 0, width / 2, height / 2);
        drawScene();

        glLoadIdentity();
        glRotated(dx, 0, 1, 0);
        glRotated(dy, 1, 0, 0);
        glMultMatrixf(modelMatrix);
        glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
        dx = 0;
        dy = 0;
        glLoadIdentity();
        glTranslatef(0, 0, -50);
        glMultMatrixf(modelMatrix);
        glViewport(width / 2, 0, width / 2, height / 2);
        drawScene();

        String text = this.getClass().getName() + ": [lmb] move, Ani[m], [W]ire";
        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        //create and draw text
        glViewport(0, 0, width, height);
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(0, height - 3, " Pudorys");
        textRenderer.addStr2D(0, height / 2 - 3, " Narys");
        textRenderer.addStr2D(width / 2, height / 2 - 3, " Bokorys");
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
