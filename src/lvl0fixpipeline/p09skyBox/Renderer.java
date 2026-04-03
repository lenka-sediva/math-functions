package lvl0fixpipeline.p09skyBox;

import lvl0fixpipeline.global.AbstractRenderer;
import lvl0fixpipeline.global.GLCamera;
import lwjglutils.OGLTexture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.Vec3D;

import java.io.IOException;
import java.nio.DoubleBuffer;

import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Shows rendering of a skybox in a scene
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
    private int sky = 0;

    private OGLTexture2D texture;
    private OGLTexture2D[] textureCube;
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
                        case GLFW_KEY_K:
                            sky = (sky + 1) % 3;
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

        textureCube = new OGLTexture2D[6];
        System.out.println("Loading textures...");
        try {
            texture = new OGLTexture2D("textures/sky.jpg");
            /*textureCube[0] = new OGLTexture2D("textures/skybox.jpg");
            textureCube[1] = new OGLTexture2D("textures/skybox2.jpg");
            textureCube[2] = new OGLTexture2D("textures/skybox3.jpg");
            textureCube[3] = new OGLTexture2D("textures/skybox4.jpg");
            textureCube[4] = new OGLTexture2D("textures/skybox5.jpg");
            textureCube[5] = new OGLTexture2D("textures/skybox6.jpg");
            */

            /*textureCube[0] = new OGLTexture2D("textures/skyBox_right.jpg");
            textureCube[1] = new OGLTexture2D("textures/skyBox_left.jpg");
            textureCube[2] = new OGLTexture2D("textures/skyBox_top.jpg");
            textureCube[3] = new OGLTexture2D("textures/skyBox_bottom.jpg");
            textureCube[4] = new OGLTexture2D("textures/skyBox_front.jpg");
            textureCube[5] = new OGLTexture2D("textures/skyBox_back.jpg");
            */
            textureCube[0] = new OGLTexture2D("textures/snow_positive_x.jpg");
            textureCube[1] = new OGLTexture2D("textures/snow_negative_x.jpg");
            textureCube[2] = new OGLTexture2D("textures/snow_positive_y.jpg");
            textureCube[3] = new OGLTexture2D("textures/snow_negative_y.jpg");
            textureCube[4] = new OGLTexture2D("textures/snow_positive_z.jpg");
            textureCube[5] = new OGLTexture2D("textures/snow_negative_z.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        camera = new GLCamera();
        camera.setPosition(new Vec3D(10));
        camera.setFirstPerson(true);

        scene();
        skyBox1();
        skyBox2();
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

    private void skyBox1() {
        glNewList(2, GL_COMPILE);
        glPushMatrix();
        glColor3d(0.5, 0.5, 0.5);
        int size = 250;
        glutWireCube(size); //neni nutne, pouze pro znazorneni tvaru skyboxu

        glEnable(GL_TEXTURE_2D);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        textureCube[1].bind(); //-x  (left)
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(-size, -size, -size);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(-size, size, -size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(-size, size, size);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(-size, -size, size);
        glEnd();

        textureCube[0].bind();//+x  (right)
        glBegin(GL_QUADS);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(size, -size, -size);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(size, -size, size);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(size, size, size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(size, size, -size);
        glEnd();

        textureCube[3].bind(); //-y bottom
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(-size, -size, -size);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(size, -size, -size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(size, -size, size);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(-size, -size, size);
        glEnd();

        textureCube[2].bind(); //+y  top
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(-size, size, -size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(size, size, -size);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(size, size, size);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(-size, size, size);
        glEnd();

        textureCube[5].bind(); //-z
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(size, -size, -size);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(-size, -size, -size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(-size, size, -size);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(size, size, -size);
        glEnd();

        textureCube[4].bind(); //+z
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3d(-size, size, size);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3d(-size, -size, size);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3d(size, -size, size);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3d(size, size, size);
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

        glEndList();
    }

    private void skyBox2() {
        glNewList(3, GL_COMPILE);
        glPushMatrix();
        glColor3d(0.5, 0.5, 0.5);
        glutWireSphere(300, 10, 10); //neni nutne, pouze pro znazorneni tvaru skyboxu

        glEnable(GL_TEXTURE_2D);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glEnable(GL_TEXTURE_2D);
        texture.bind();
        glutSolidSphere(300, 30, 30);

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

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
            gluPerspective(90, width / (float) height, 0.1f, 500.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 500.0f);

        if (move) {
            uhel++;
        }

        GLCamera cameraSky = new GLCamera(camera);
        cameraSky.setPosition(new Vec3D());

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glPushMatrix();
        camera.setMatrix();
        glRotatef(uhel, 0, 1, 0);
        glCallList(1);
        glPopMatrix();

        glPushMatrix();
        cameraSky.setMatrix();
        if (sky == 1)
            glCallList(2);
        if (sky == 2)
            glCallList(3);
        glPopMatrix();

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (move)
            text += ", Ani[M] ";
        else
            text += ", Ani[m] ";

        text += "s[K]y box" + sky;

        String textInfo = "position " + camera.getPosition().toString();
        textInfo += String.format(" azimuth %3.1f, zenith %3.1f", azimut, zenit);
        if (sky == 2) {
            textureViewer.view(texture, -1, -1, 0.5);
        }
        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
