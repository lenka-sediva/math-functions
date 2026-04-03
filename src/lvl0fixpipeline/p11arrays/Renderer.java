package lvl0fixpipeline.p11arrays;

import lvl0fixpipeline.global.AbstractRenderer;
import lvl0fixpipeline.global.GLCamera;
import lvl0fixpipeline.global.Teapot;
import lwjglutils.OGLModelOBJ;
import lwjglutils.OGLTexture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.Vec3D;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static lvl0fixpipeline.global.GluUtils.gluPerspective;
import static lvl0fixpipeline.global.GlutUtils.glutWireCube;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Shows a different definition of solids using vertex and index arrays
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
    private boolean per = true, move = false, wire = true;
    private int arrayMode = 6, objMode = 0;

    private GLCamera camera;

    private int vaoId, vboId, iboId, vaoIdOBJ, vaoIdTeapot;
    OGLModelOBJ model;
    OGLTexture2D texture;

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
                        case GLFW_KEY_B:
                            objMode++;
                            break;
                        case GLFW_KEY_O:
                            arrayMode++;
                            break;
                        case GLFW_KEY_I:
                            wire = !wire;
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
        glClearColor(0.3f, 0.1f, 0.1f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glFrontFace(GL_CW);
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_FILL);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        camera = new GLCamera();
        camera.setPosition(new Vec3D(0));
        camera.setFirstPerson(false);
        camera.setRadius(5);
        scene();
    }

    private void scene() {
        int[] indices = new int[]{0, 2, 1, 3, 7, 5, 6, 4, 0, 1};
        //3*n values for triangles
        //n+2 values for triangle_strip
        //n+2 values for triangle_fan
        float[] buffer = new float[] // 8 vertices: x,y,z,r,g,b;
                {       -1, -1, -1,      1f, 1f, 1f,
                        -1,  1, -1,      0f, 0f, 1f,
                        -1, -1,  1,      0f, 1f, 1f,
                        -1,  1,  1,      0f, 1f, 0f,
                         1, -1,  1,      1f, 1f, 0f,
                         1,  1,  1,      1f, 0f, 0f,
                         1, -1, -1,      1f, 0f, 1f,
                         1,  1, -1,      .5f, .5f, .5f};

        FloatBuffer vertexBufferDataBuffer = (FloatBuffer) BufferUtils
                .createFloatBuffer(buffer.length)
                .put(buffer)
                .rewind();
        //FloatBuffer vertexBufferDataBuffer = (FloatBuffer) MemoryUtil.memAllocFloat(buffer.length).put(buffer).flip();


        IntBuffer indexBufferDataBuffer = (IntBuffer) BufferUtils
                .createIntBuffer(indices.length)
                .put(indices)
                .rewind();

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBufferDataBuffer, GL_STATIC_DRAW);

        glVertexPointer(3, GL_FLOAT, 6 * 4, 0);
        glColorPointer(3, GL_FLOAT, 6 * 4, 3 * 4);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        iboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferDataBuffer, GL_STATIC_DRAW);

        //model = new OGLModelOBJ("/obj/teapot.obj");
        model= new OGLModelOBJ("/obj/ElephantBody.obj");
        //model= new OGLModelOBJ("/obj/TexturedCube.obj");
        //model = new OGLModelOBJ("/obj/ducky.obj");
        vaoIdOBJ = glGenVertexArrays();
        glBindVertexArray(vaoIdOBJ);

        FloatBuffer fb = model.getVerticesBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glVertexPointer(4, GL_FLOAT, 4 * 4, 0);
        }
        fb = model.getNormalsBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glColorPointer(3, GL_FLOAT, 3 * 4, 0);
            glNormalPointer(GL_FLOAT, 3 * 4, 0);
        }
        fb = model.getTexCoordsBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glTexCoordPointer(2, GL_FLOAT, 2 * 4, 0);
        }



        System.out.println("Loading textures...");
        try {
            texture = new OGLTexture2D("textures/globe.jpg"); // vzhledem k adresari res v projektu
        } catch (IOException e) {
            e.printStackTrace();
        }

        vaoIdTeapot = glGenVertexArrays();
        glBindVertexArray(vaoIdTeapot);
        Teapot teapot = new Teapot();
        fb = teapot.getVerticesBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glVertexPointer(4, GL_FLOAT, 4 * 4, 0);
        }
        fb = teapot.getNormalsBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glColorPointer(3, GL_FLOAT, 3 * 4, 0);
            glNormalPointer(GL_FLOAT, 3 * 4, 0);
        }
        fb = teapot.getTexCoordsBuffer();
        if (fb != null) {
            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            fb.rewind();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
            glTexCoordPointer(2, GL_FLOAT, 2 * 4, 0);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        String text = this.getClass().getName() + ": [lmb] move";
        String textInfo = "";

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
        glLoadIdentity();
        camera.setMatrix();

        glRotatef(uhel, 0, 1, 0);

        glColor3f(0.5f, 0.5f, 1);

        glutWireCube(2.2);


        if (!wire)
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        else
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        switch (arrayMode % 7) {
            case 0:
                glBegin(GL_TRIANGLES);
                glColor3f(1f, 0f, 0f);
                glVertex2f(-1f, -1);
                glColor3f(0f, 1f, 0f);
                glVertex2f(1, 0);
                glColor3f(0f, 0f, 1f);
                glVertex2f(0, 1);
                glEnd();
                textInfo += "Array m[O]de : Simple triangle ";
                break;
            case 1:
                glBindVertexArray(vaoId);
                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);

                glBegin(GL_TRIANGLES);
                glArrayElement(0);
                glArrayElement(6);
                glArrayElement(7);
                glEnd();

                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_COLOR_ARRAY);
                glBindVertexArray(0);
                textInfo += "Array m[O]de : ArrayElement ";
                break;
            case 2:
                glBindVertexArray(vaoId);
                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);

                glDrawArrays(GL_TRIANGLE_STRIP, 0, 8);

                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_COLOR_ARRAY);
                glBindVertexArray(0);
                textInfo += "Array m[O]de : DrawArrays ";
                break;
            case 3:
                glBindVertexArray(vaoId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_INDEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);

                glDrawElements(GL_TRIANGLES, 9, GL_UNSIGNED_INT, 0);
                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_COLOR_ARRAY);
                glDisableClientState(GL_INDEX_ARRAY);
                glBindVertexArray(0);
                textInfo += "Array m[O]de : glDrawElements ";
                break;
            case 4:
                glBindVertexArray(vaoId);
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);
                glEnableClientState(GL_INDEX_ARRAY);

                glDrawRangeElements(GL_TRIANGLE_STRIP, 0, 3, 6, GL_UNSIGNED_INT, 4);
                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_COLOR_ARRAY);
                glDisableClientState(GL_INDEX_ARRAY);
                glBindVertexArray(0);
                textInfo += "Array m[O]de : glDrawRangeElements ";
                break;
            case 5:
                text += ", O[B]J mode: " + objMode % 4;
                glBindVertexArray(vaoIdOBJ);
                glEnableClientState(GL_VERTEX_ARRAY);
                switch (objMode % 4) {
                    case 0:
                        text += " constant color";
                        glColor3f(1,1,0);
                        break;
                    case 1:
                        text += " normals as color";
                        glEnableClientState(GL_COLOR_ARRAY);
                        break;
                    case 2:
                        text += " lighting";
                        float[] light_position = {1,1,1,0};
                        glLightfv(GL_LIGHT0, GL_POSITION, light_position);
                        glEnable(GL_LIGHTING);
                        glEnable(GL_LIGHT0);
                        glEnable(GL_NORMALIZE);
                        glEnableClientState(GL_NORMAL_ARRAY);
                        break;
                    case 3:
                        text += " texture mapping";
                        glEnable(GL_TEXTURE_2D);
                        texture.bind();
                        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
                        break;
                }
                glScalef(0.02f,0.02f,0.02f);

                glDrawArrays(model.getTopology(), 0, model.getVerticesBuffer().limit());
                glDisableClientState(GL_COLOR_ARRAY);
                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_NORMAL_ARRAY);
                glDisableClientState(GL_TEXTURE_COORD_ARRAY);
                textInfo += "Array m[O]de : OBJ model";
                glDisable(GL_TEXTURE_2D);
                glDisable(GL_LIGHTING);
                glBindVertexArray(0);
                break;
            case 6:
                text += ", teapot [B]: " + objMode % 4;
                glBindVertexArray(vaoIdTeapot);
                glEnableClientState(GL_VERTEX_ARRAY);
                switch (objMode % 4) {
                    case 0:
                        text += " constant color";
                        glColor3f(1,1,0);
                        break;
                    case 1:
                        text += " normals as color";
                        glEnableClientState(GL_COLOR_ARRAY);
                        break;
                    case 2:
                        text += " lighting";
                        float[] light_position = {1,1,1,0};
                        glLightfv(GL_LIGHT0, GL_POSITION, light_position);
                        glEnable(GL_LIGHTING);
                        glEnable(GL_LIGHT0);
                        glEnable(GL_NORMALIZE);
                        glEnableClientState(GL_NORMAL_ARRAY);
                        break;
                    case 3:
                        text += " texture mapping";
                        glEnable(GL_TEXTURE_2D);
                        texture.bind();
                        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
                        break;
                }
                glScalef(0.2f,0.2f,0.2f);

                glDrawArrays(model.getTopology(), 0, model.getVerticesBuffer().limit());
                glDisableClientState(GL_COLOR_ARRAY);
                glDisableClientState(GL_VERTEX_ARRAY);
                glDisableClientState(GL_NORMAL_ARRAY);
                glDisableClientState(GL_TEXTURE_COORD_ARRAY);
                textInfo += "Array m[O]de : OBJ model";
                glDisable(GL_TEXTURE_2D);
                glDisable(GL_LIGHTING);
                glBindVertexArray(0);
                break;
        }

        glDisable(GL_VERTEX_ARRAY);
        glDisable(GL_COLOR_ARRAY);
        glDisable(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);

        if (per)
            text += ", [P]ersp ";
        else
            text += ", [p]ersp ";

        if (move)
            text += ", Ani[M] ";
        else
            text += ", Ani[m] ";

        if (wire)
            text += ", W[I]re ";
        else
            text += ", W[i]re ";

        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(3, 40, textInfo);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

}
