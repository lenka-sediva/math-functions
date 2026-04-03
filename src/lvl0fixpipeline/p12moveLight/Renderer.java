package lvl0fixpipeline.p12moveLight;

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
import java.util.ArrayList;
import java.util.List;

import static lvl0fixpipeline.global.GluUtils.gluPerspective;
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

    private boolean mouseButton1 = false;
    private boolean per = true, move = true;
    private boolean light1 = true, light2 = true, light3 = true;
    private boolean light4 = true, light5 = true, light6 = true, light7 = true, infinity = false;
    private OGLTexture2D texture;
    private String textInfo;

    private GLCamera camera;

    class Light{
        int lightID;
        boolean enable;
        float r,g,b;

        public Light(int lightID, boolean enable, float r, float g, float b) {
            this.lightID = lightID;
            this.enable = enable;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private List<Light> lights = new ArrayList<>();

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
                        case GLFW_KEY_P -> per = !per;
                        case GLFW_KEY_M -> move = !move;
                        case GLFW_KEY_I -> infinity = !infinity;
                        case GLFW_KEY_W, GLFW_KEY_S, GLFW_KEY_A,  GLFW_KEY_D -> deltaTrans = 0.001f;
                        case GLFW_KEY_1 -> lights.get(0).enable = !lights.get(0).enable;
                        case GLFW_KEY_2 -> lights.get(1).enable = !lights.get(1).enable;
                        case GLFW_KEY_3 -> lights.get(2).enable = !lights.get(2).enable;
                        case GLFW_KEY_4 -> lights.get(3).enable = !lights.get(3).enable;
                        case GLFW_KEY_5 -> lights.get(4).enable = !lights.get(4).enable;
                        case GLFW_KEY_6 -> lights.get(5).enable = !lights.get(5).enable;
                        case GLFW_KEY_7 -> lights.get(6).enable = !lights.get(6).enable;
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
        camera.setPosition(new Vec3D(11.62, 11.57, 11.60));
        azimut = (float) Math.toDegrees(-0.79);
        zenit = (float) Math.toDegrees(-0.65);
        camera.setFirstPerson(true);

        float[] mat_dif = new float[]{1, 1, 1, 1};
        float[] mat_spec = new float[]{1, 1, 1, 1};
        float[] mat_amb = new float[]{.1f, .1f, .1f, 1};

        glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, mat_amb);
        glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, mat_dif);
        glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, mat_spec);
        glMaterialfv(GL_FRONT_AND_BACK, GL_EMISSION, new float[]{0, 0, 0, 1});

        setLight(GL_LIGHT0, 1, 0, 0);
        setLight(GL_LIGHT1, 0, 1, 0);
        setLight(GL_LIGHT2, 0, 0, 1);
        setLight(GL_LIGHT3, 0, 1, 1);
        setLight(GL_LIGHT4, 1, 1, 0);
        setLight(GL_LIGHT5, 1, 0, 1);
        setLight(GL_LIGHT6, 1, 0.5f, 0.5f);
    }

    private void setLight(int light, float r, float g, float b) {
        float[] light_amb = new float[]{0, 0, 0, 1};//{ 0.0f*r, .1f*g, .1f*b, 1 };
        float[] light_dif = new float[]{r, g, b, 1};//{ 0.1f*r, .2f*g, .2f*b, 1 };
        float[] light_spec = new float[]{0, 0, 0, 1};//{ 0.1f*r, .3f*g, .3f*b, 1 };

        glLightfv(light, GL_AMBIENT, light_amb);
        glLightfv(light, GL_DIFFUSE, light_dif);
        glLightfv(light, GL_SPECULAR, light_spec);

        lights.add(new Light(light, true, r,g,b));
    }

    private void trasformEarth() {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);

        glRotatef(uhel, 0, 0, 1);
    }

    private void drawEarth() {
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glColor3f(0, 0, 0);
        texture.bind();
        glutSolidSphere(4, 16, 16);
    }

    private void transformSatellite() {
        glTranslatef(6, 6, 0);
        glRotatef(-uhel*5f, 1, 0f, 1);
    }

    private void drawSatellite() {
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        texture.bind();
        glColor3f(0.5f, 1, 0.5f);
        glutSolidSphere(0.5, 16, 16); //draw satellite
    }

    private void drawBall(float r, float g, float b, float x, float y, float z) {
        glPushMatrix();
        glTranslatef(x, y, z);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glColor3f(r, g, b);
        glutWireSphere(0.5, 8, 8);
        glPopMatrix();
    }

    private void enableLight(int lightIdx) {
        float[] lightPosition = {0, 0, 0, 1};
        Light light =  lights.get(lightIdx-1);
        if (light.enable) {
            glEnable(light.lightID);
            glLightfv(light.lightID, GL_POSITION, lightPosition);
            glPushMatrix();
            drawBall(light.r, light.g, light.b, 0, 0, 0 );
            glPopMatrix();
            textInfo += (lightIdx);
        }

    }

    @Override
    public void display() {
        camera.setAzimuth(Math.toRadians(azimut));
        camera.setZenith(Math.toRadians(zenit));
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        String text = this.getClass().getName() + ": [lmb] move";
        textInfo = "Light: ";

        trans += deltaTrans;
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            gluPerspective(60, width / (float) height, 0.1f, 500.0f);
        else
            glOrtho(-10 * width / (float) height,
                    10 * width / (float) height,
                    -10, 10, 0.1f, 500.0f);

        if (move) {uhel++; }

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        //we are in view (camera) space

        glPushMatrix();
        //static light moving together with camera
        glTranslatef(-0.5f, -0.5f, -10f);
        enableLight(1);
        glPopMatrix();

        glPushMatrix();
        //dynamic light moving relatively to camera
        glTranslatef((float) (10 * Math.sin(Math.toRadians(uhel / 2))), 0, -5f);
        enableLight(2);
        glPopMatrix();

        camera.setMatrix(); //set view transformation

        //we are in model space
        drawBall(1,1,1,0,0,0); //origin in camera space

        glPushMatrix();
            // static light in model space
            glTranslatef(5, 0, 5);
            enableLight(3);
        glPopMatrix();

        glPushMatrix();
            // light moving independently in model space
            glTranslatef(0, (float) (10 * Math.sin(Math.toRadians(uhel / 5))), 5);
            enableLight(4);
        glPopMatrix();

        trasformEarth();

        glPushMatrix();
            // light moving (rotating) together with object (Earth)
            glTranslatef(-6, -6, 0);
            enableLight(5);
        glPopMatrix();

        glPushMatrix();
            // light moving (rotating) together and independent on object
            glTranslatef(0, 7 + (float) (2 * Math.sin(Math.toRadians(uhel * 5))), 0);
            enableLight(6);
        glPopMatrix();

        glPushMatrix();
            transformSatellite();
            glPushMatrix();
                glTranslatef(0, 2, 0);
                // light moving (rotating) together with rotating object (satelite)
                enableLight(7);
            glPopMatrix();
            drawSatellite();
        glPopMatrix();

        drawEarth();


        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHT1);
        glDisable(GL_LIGHT2);
        glDisable(GL_LIGHT3);
        glDisable(GL_LIGHT4);
        glDisable(GL_LIGHT5);
        glDisable(GL_LIGHT6);

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (move)
            text += ", Ani[M] ";
        else
            text += ", Ani[m] ";

        /*if (infinity)
            text += ", [I]nfinity light position  ";
        else
            text += ", [i]nfinity light position ";
        */
        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(3, 60, camera.toString());
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
