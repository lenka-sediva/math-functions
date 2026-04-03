package lvl0fixpipeline.p13culling;

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

import static lvl0fixpipeline.global.GluUtils.*;
import static lvl0fixpipeline.global.GlutUtils.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * It shows the possibilities of moving light sources in the scene
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

    private boolean wire = true, ccw = true;
    private int front=0, back=1, model = 4, cull = 0;
    private boolean mouseButton1 = false;
    private boolean per = true, move = true;
    private OGLTexture2D texture;

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
                        case GLFW_KEY_W, GLFW_KEY_S, GLFW_KEY_A, GLFW_KEY_D -> deltaTrans = 0.001f;
                        case GLFW_KEY_P -> per = !per;
                        case GLFW_KEY_M -> move = !move;

                        //case GLFW_KEY_V -> wire = !wire;
                        case GLFW_KEY_V -> model = (++model);
                        case GLFW_KEY_N -> front = (++front)%3;
                        case GLFW_KEY_B -> back = (++back)%3;
                        case GLFW_KEY_C -> cull = ++cull % 4;
                        case GLFW_KEY_X -> ccw = !ccw;

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
        camera.setPosition(new Vec3D(11.62, 11.57, 11.60));
        azimut = (float) Math.toDegrees(-0.79);
        zenit = (float) Math.toDegrees(-0.65);
        camera.setFirstPerson(true);
    }


    private void scene() {
        glDisable(GL_LIGHTING);
        glPushMatrix();
        if ((model % 2 ) == 0){
            glDisable(GL_TEXTURE_2D);
            glColor3f(1, 1, 1);
        } else {
            glEnable(GL_TEXTURE_2D);
            texture.bind();
        }
        switch (model % 8) {
            case 0-> glutWireSphere(5, 16, 16);
            case 1-> glutSolidSphere(5, 16, 16);
            case 2-> glutWireCube(5);
            case 3-> glutSolidCube(5);
            case 4-> glutWireCylinder(3, 5, 16,16);
            case 5-> glutSolidCylinder(3, 5, 16,16);
            case 6-> glutWireTorus(3, 5, 16,16);
            case 7-> glutSolidTorus(3, 5, 16,16);
        }
        glPopMatrix();
    }

    private void drawBall(float r, float g, float b, float x, float y, float z) {
        glPushMatrix();
        glTranslatef(x, y, z);
        glDisable(GL_TEXTURE_2D);
        glColor3f(r, g, b);
        glutWireSphere(1, 8, 8);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    @Override
    public void display() {
        camera.setAzimuth(Math.toRadians(azimut));
        camera.setZenith(Math.toRadians(zenit));
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);

        String textInfo = "[v] model, fro[n]t face:";

        switch (front) {
            case 0 -> {
                glPolygonMode(GL_FRONT, GL_FILL);
                textInfo += " fill ";
            }
            case 1 -> {
                glPolygonMode(GL_FRONT, GL_LINE);
                textInfo += " line ";
            }
            case 2 -> {
                glPolygonMode(GL_FRONT, GL_POINT);
                textInfo += " point ";
            }
        }

        textInfo += ", [b]ack face:";

        switch (back) {
            case 0 -> {
                glPolygonMode(GL_BACK, GL_FILL);
                textInfo += " fill ";
            }
            case 1 -> {
                glPolygonMode(GL_BACK, GL_LINE);
                textInfo += " line ";
            }
            case 2 -> {
                glPolygonMode(GL_BACK, GL_POINT);
                textInfo += " point ";
            }
        }

        String textInfo2 = "[C]ull: ";
        switch (cull) {
            case 0 -> {
                glDisable(GL_CULL_FACE);
                textInfo2 += "disable, ";
            }
            case 1 ->{
                glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT);
                textInfo2 += "front, ";
            }
            case 2 ->{
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
                textInfo2 += "back, ";
            }
            case 3 ->{
                glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT_AND_BACK);
                textInfo2 += "front and back, ";
            }
        }
        if (ccw){
            glFrontFace(GL_CCW);
            textInfo2 += "CCW[X]cw ";
        }
        else {
            glFrontFace(GL_CW);
            textInfo2 += "ccw[X]CW ";
        }
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

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        camera.setMatrix();
        glRotatef(uhel, 0, 0, 1);
        scene();
        glPopMatrix();

        glPushMatrix();
        camera.setMatrix();
        drawBall(1, 1, 1, 0, 0, 0);
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
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(3, 60, textInfo2);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
