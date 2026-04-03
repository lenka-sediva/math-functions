package lvl0fixpipeline.p14pushpopclip;

import lvl0fixpipeline.global.AbstractRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GlutUtils.*;
import static lvl0fixpipeline.global.GluUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shows application of clipping, push and pop matrix
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2023-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;

    private float uhel = 0;
    private float[] modelMatrix = new float[16];

    private boolean mouseButton1 = false;
    private boolean per = true, move = true, wire = true, clip = true;

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
                        case GLFW_KEY_C:
                            clip = !clip;
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

        //modelovani kola
        glNewList(1, GL_COMPILE);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glRotatef(90, 0, 1, 0);
        glColor3f(0.2f, 0.2f, 0f);
        glutSolidTorus(4, 11, 10, 30);// pneu
        glutSolidCylinder(1, 2, 10, 20);// osa
        glColor3f(0.8f, 0.6f, 0.4f);
        glutSolidCylinder(8, 0.8, 1, 20);// disk
        glColor3f(0.3f, 0.2f, 0.4f);
        for (int i = 0; i < 5; i++) // srouby
        {
            glRotatef(75, 0, 0, 1);
            glPushMatrix();
            glTranslatef(3f, 3f, 0f);
            glutSolidCylinder(1, 1, 4, 6);
            glPopMatrix();
        }
        glPopMatrix();
        glEndList();

        //modelovani podvozku
        glNewList(2, GL_COMPILE);
        glPushMatrix();
        glTranslatef(9f, 20f, 0f);
        glCallList(1);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(9f, -20f, 0f);
        glCallList(1);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(-9f, 20f, 0f);
        glCallList(1);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(-9f, -20f, 0f);
        glCallList(1);
        glPopMatrix();

        glPushMatrix();
        glScalef(9f, 60f, 3f);
        glutSolidCube(1);
        glPopMatrix();
        glEndList();
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

        double[] eqn = { 0, 1, 1, 0 };

        glMatrixMode(GL_MODELVIEW);

        if (clip)
            glEnable(GL_CLIP_PLANE0);
        else
            glDisable(GL_CLIP_PLANE0);

        glPushMatrix();
        glRotated(uhel, 1, 1, 0);
        glClipPlane(GL_CLIP_PLANE0, eqn);
        glPopMatrix();

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
        glViewport(0, 0, width, height);
        glCallList(2);

        String text = this.getClass().getName() + ": [lmb] move, Ani[m]";
        if (wire)
            text += ", [W]ire ";
        else
            text += ", [w]ire ";

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (clip)
            text += "[C]lip ";
        else
            text += "[c]lip ";

        //create and draw text
        glViewport(0, 0, width, height);
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
