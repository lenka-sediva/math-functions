package lvl0fixpipeline.p05texture;

import lvl0fixpipeline.global.AbstractRenderer;
import lwjglutils.OGLTextRenderer;
import lwjglutils.OGLTexture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.io.IOException;
import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GluUtils.gluLookAt;
import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.glutSolidSphere;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shows texture mapping
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;
    private float uhel = 0;
    private int mode = 0;
    private float[] modelMatrix = new float[16];

    private boolean mouseButton1 = false;
    private boolean per = true, anim = false;
    private int tex = 1;
    private int texApp = 0;

    private OGLTexture2D texture1, texture2;
    private OGLTexture2D.Viewer textureViewer;

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
                        case GLFW_KEY_T:
                            tex++;
                            break;
                        case GLFW_KEY_A:
                            texApp++;
                            break;
                        case GLFW_KEY_N:
                            anim = !anim;
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
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        textRenderer = new OGLTextRenderer(width, height);
        textureViewer = new OGLTexture2D.Viewer();

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_FILL);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        System.out.println("Loading textures...");
        try {
            texture1 = new OGLTexture2D("textures/testTexture.jpg"); // vzhledem k adresari res v projektu
            texture2 = new OGLTexture2D("textures/globe.jpg"); // vzhledem k adresari res v projektu
        } catch (IOException e) {
            e.printStackTrace();
        }

        glMatrixMode(GL_MODELVIEW);

        glLoadIdentity();
        glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
    }

    private void drawSimpleScene() {
        texture1.bind();

        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Rendering triangle by fixed pipeline
        glBegin(GL_TRIANGLES);
        glTexCoord2f(0, 0);
        glColor3f(1f, 0f, 0f);
        glVertex3f(-1f, -1, 0.9f);

        glTexCoord2f(0, 1);
        glColor3f(0f, 1f, 0f);
        glVertex3f(1, 0, 0.9f);

        glTexCoord2f(1, 0);
        glColor3f(0f, 0f, 1f);
        glVertex3f(0, 1, 0.9f);
        glEnd();

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_TEXTURE);
        glPopMatrix();
    }

    @Override
    public void display() {
        String textInfo = "";

        glViewport(0, 0, width, height);
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glActiveTexture(GL_TEXTURE0);

        //set texture parameters
        int paramTex = GL_REPEAT;
        String textureMode = "";
        switch (tex % 5) {
            case 1:
                paramTex = GL_REPEAT;
                textureMode = "REPEAT";
                break;
            case 2:
                paramTex = GL_MIRRORED_REPEAT;
                textureMode = "MIRRORED_REPEAT";
                break;
            case 3:
                paramTex = GL_CLAMP_TO_EDGE;
                textureMode = "CLAMP_TO_EDGE";
                break;
            case 4:
                paramTex = GL_CLAMP_TO_BORDER;
                textureMode = "CLAMP_TO_BORDER";
                break;
            case 0:
                paramTex = GL_CLAMP;
                textureMode = "CLAMP";
                break;
        }

        int paramTexApp = GL_REPLACE;
        String textureApp = "";
        switch (texApp % 4) {
            case 0:
                glDisable(GL_TEXTURE_2D);
                textureApp = "Disable";
                break;
            case 1:
                paramTexApp = GL_REPLACE;
                textureApp = "REPLACE";
                break;
            case 2:
                paramTexApp = GL_MODULATE;
                textureApp = "MODULATE";
                break;
            case 3:
                paramTexApp = GL_ADD;
                textureApp = "ADD ";
                break;
        }

        texture1.bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, paramTex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, paramTex);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, paramTexApp);
        // glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        // glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);
        texture2.bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, paramTex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, paramTex);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, paramTexApp);
        // glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        // glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);

        if (mode % 2 == 0) {
            drawSimpleScene();
        } else {
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            if (per)
                gluPerspective(45, width / (float) height, 0.1f, 200.0f);
            else
                glOrtho(-20 * width / (float) height,
                        20 * width / (float) height,
                        -20, 20, 0.1f, 200.0f);

            gluLookAt(50, 0, 0, 0, 0, 0, 0, 0, 1);

            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glRotatef(dy, 0, 1, 0);
            glRotatef(dx, 0, 0, 1);
            glMultMatrixf(modelMatrix);
            glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
            dx = 0;
            dy = 0;

            if (anim) {
                uhel++;
            }

            //rotated scaled sphere with static texture
            texture2.bind();
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glTranslatef(0, -20, 10);
            glRotatef(2 * uhel, 0, 0, 1);
            glScalef(1.2f, 1.0f, 0.9f);
            glColor3f(0.9f, 0.1f, 0.1f);
            glutSolidSphere(10f, 18, 18);// Koule
            glPopMatrix();

            texture1.bind();
            //static object with static scaled texture
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glTranslatef(10f, 5f, 5f);
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glScalef(2f, 0.5f, 1f);

            glColor3f(0.1f, 0.1f, 0.9f);
            glBegin(GL_QUADS);
            glTexCoord2f(0.1f, 0.1f);
            glVertex3f(0.0f, 10.0f, 0.0f);
            glTexCoord2f(0.0f, 0.9f);
            glVertex3f(0.0f, 10.0f, 10.0f);
            glTexCoord2f(1.1f, 0.8f);
            glVertex3f(0.0f, 0.0f, 10.0f);
            glTexCoord2f(1.0f, 0.0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glEnd();

            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();

            //static object with rotated texture
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glRotatef(uhel / 2, 0, 0, 1);
            glTranslatef(-1f, -1f, 0);
            glScalef(2f, 2f, 0);

            glColor3f(0.1f, 0.9f, 0.1f);
            glBegin(GL_QUADS);
            glTexCoord2f(0f, 0f);
            glVertex3f(0.0f, 10.0f, 0.0f);
            glTexCoord2f(0.0f, 1f);
            glVertex3f(0.0f, 10.0f, 10.0f);
            glTexCoord2f(1f, 1f);
            glVertex3f(0.0f, 0.0f, 10.0f);
            glTexCoord2f(1f, 0f);
            glVertex3f(0.0f, 0.0f, 0.0f);
            glEnd();
        }

        float[] color = {1.0f, 1.0f, 1.0f};
        glColor3fv(color);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": [lmb] move, [M]ode";
        if (mode % 2 != 0) {
            text += ", a[N]im";
            text += per?", [P]ersp ":", [p]ersp";


            textureViewer.view(texture2, 0.5, -1, 0.5);
        }
        textInfo += "[T]exture map: " + textureMode;
        textInfo += ", [A]pplication: " + textureApp;

        textureViewer.view(texture1, -1, -1, 0.5);
        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
