package lvl0fixpipeline.p06camera;

import lvl0fixpipeline.global.AbstractRenderer;
import lvl0fixpipeline.global.GLCamera;
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
 * Shows possibilities of camera definition and application
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;
    private float px, py, pz;
    private double ex, ey, ez;
    private float zenit, azimut;

    private float trans, deltaTrans = 0;

    private float uhel = 0;
    private float[] modelMatrix = new float[16];

    private boolean mouseButton1 = false;
    private boolean per = true, move = false;
    private int cameraMode, lastCameraMode = -1;

    private OGLTexture2D texture;
    private OGLTexture2D.Viewer textureViewer;
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
                        case GLFW_KEY_C:
                            cameraMode++;
                            break;
                        case GLFW_KEY_LEFT_SHIFT:
                        case GLFW_KEY_LEFT_CONTROL:
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

                        if (cameraMode == 0) {
                            pz -= trans;
                        } else {
                            px += ex * trans;
                            py += ey * trans;
                            pz += ez * trans;
                        }
                        camera.forward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_S:
                        if (cameraMode == 0) {
                            pz += trans;
                        } else {
                            px -= ex * trans;
                            py -= ey * trans;
                            pz -= ez * trans;
                        }
                        camera.backward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_A:
                        if (cameraMode == 0) {
                            px -= trans;
                        } else {
                            pz -= Math.cos(azimut * Math.PI / 180 - Math.PI / 2) * trans;
                            px += Math.sin(azimut * Math.PI / 180 - Math.PI / 2) * trans;
                        }
                        camera.left(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_D:
                        if (cameraMode == 0) {
                            px += trans;
                        } else {
                            pz += Math.cos(azimut * Math.PI / 180 - Math.PI / 2) * trans;
                            px -= Math.sin(azimut * Math.PI / 180 - Math.PI / 2) * trans;
                        }
                        camera.right(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;
                    case GLFW_KEY_LEFT_SHIFT:
                        if (cameraMode == 0) {
                            py -= trans;
                        } else {
                            py -= Math.sin(zenit * Math.PI / 180 - Math.PI / 2) * trans;
                        }
                        camera.up(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;
                    case GLFW_KEY_LEFT_CONTROL:
                        if (cameraMode == 0) {
                            py += trans;
                        } else {
                            py += Math.sin(zenit * Math.PI / 180 - Math.PI / 2) * trans;
                        }
                        camera.down(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
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
        textureViewer = new OGLTexture2D.Viewer();

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_FILL);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        System.out.println("Loading texture...");
        try {
            texture = new OGLTexture2D("textures/globe.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        camera = new GLCamera();
    }

    private void drawScene() {
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();

        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glActiveTexture(GL_TEXTURE0);
        texture.bind();

        glPushMatrix();
        glRotatef(90, -1, 0, 0);
        glRotatef(2 * uhel, 0, 0, 1);
        glColor3f(1.0f, 1.0f, 1.0f);
        glutSolidSphere(10f, 32, 32);
        glPopMatrix();

        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glRotatef(0.5f * uhel, 0, 0, 1);
        glTranslatef(11, 0, 0);
        glColor3f(0.6f, 0.1f, 0.3f);
        glutSolidSphere(0.3f, 16, 16);
        glPopMatrix();
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": [lmb] move";

        trans += deltaTrans;


        double a_rad = azimut * Math.PI / 180;
        double z_rad = zenit * Math.PI / 180;
        ex = Math.sin(a_rad) * Math.cos(z_rad);
        ey = Math.sin(z_rad);
        ez = -Math.cos(a_rad) * Math.cos(z_rad);
        double ux = Math.sin(a_rad) * Math.cos(z_rad + Math.PI / 2);
        double uy = Math.sin(z_rad + Math.PI / 2);
        double uz = -Math.cos(a_rad) * Math.cos(z_rad + Math.PI / 2);


        glMatrixMode(GL_MODELVIEW);
        switch (cameraMode % 5) {
            case 0:
                if (lastCameraMode != cameraMode) {
                    glLoadIdentity();
                }
                glGetFloatv(GL_MODELVIEW_MATRIX, modelMatrix);
                glLoadIdentity();

                glRotatef(-zenit, 1.0f, 0, 0);
                glRotatef(azimut, 0, 1.0f, 0);
                glTranslated(-px, -py, -pz);
                glMultMatrixf(modelMatrix);

                zenit = 0;
                azimut = 0;
                px = 0;
                py = 0;
                pz = 0;
                text += ", [C]amera: Free ";
                break;
            case 1:
                glLoadIdentity();
                gluLookAt(px, py, pz, ex + px, ey + py, ez + pz, ux, uy, uz);
                text += ", [C]amera: LookAt ";
                break;
            case 2:
                glLoadIdentity();
                glRotatef(-zenit, 1.0f, 0, 0);
                glRotatef(azimut, 0, 1.0f, 0);
                glTranslated(-px, -py, -pz);
                text += ", [C]amera: Rot+Trans ";
                break;
            case 3:
                glLoadIdentity();
                camera.setFirstPerson(true);
                camera.setMatrix();
                text += ", [C]amera: GLCamera 1st";
                break;
            case 4:
                glLoadIdentity();
                camera.setFirstPerson(false);
                camera.setRadius(30);
                camera.setMatrix();
                text += ", [C]amera: GLCamera 3rd";
                break;
        }
        lastCameraMode = cameraMode;
        text +="("+(cameraMode%5)+trans+")";

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            gluPerspective(45, width / (float) height, 0.1f, 200.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 200.0f);

        glMatrixMode(GL_MODELVIEW);

        glPushMatrix();
        if (move) {
            uhel++;
        }

        drawScene();
        glPopMatrix();

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (move)
            text += ", Ani[M] ";
        else
            text += ", Ani[m] ";


        String textInfo = String.format("position (%3.1f, %3.1f, %3.1f)", px, py, pz);
        textInfo += String.format(" view (%3.1f, %3.1f, %3.1f)", ex, ey, ez);
        textInfo += String.format(" up (%3.1f, %3.1f, %3.1f)", ux, uy, uz);
        textInfo += String.format(" azimuth %3.1f, zenith %3.1f)", azimut, zenit);
        textInfo += String.format(" trans %3.1f,  delta %3.1f)", trans, deltaTrans);
        textureViewer.view(texture, -1, -1, 0.5);
        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
