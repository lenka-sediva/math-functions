package lvl0fixpipeline.p02elements;

import lvl0fixpipeline.global.AbstractRenderer;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Basic graphics elements rendering
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {

    private int drawPointMode = 1;
    private int drawLineMode = 1;
    private int drawPolygonMode = 1;

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
                        case GLFW_KEY_P: // body - points
                            drawPointMode++;
                            break;
                        case GLFW_KEY_L: // Cary - lines
                            drawLineMode++;
                            break;
                        case GLFW_KEY_F: // Steny - faces
                            drawPolygonMode++;
                            break;
                    }
                }
            }
        };

        glfwMouseButtonCallback = null; //glfwMouseButtonCallback do nothing

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                //do nothing
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
    public void display() {
        // transformace okna nastavena na rozsah ([0,0]-[1,1]) s konstantnim
        // pomerem podle velikosti okna
        if (3 * width < 2 * height) {
            glViewport(-height * 3 / 2, -height, height * 3, 2 * height);
        } else {
            glViewport(-width, -width * 2 / 3, width * 2, width * 4 / 3);
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

        // model and view matrix initialization
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity(); // identity matrix

        // projection matrix initialization
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity(); // identity matrix

        // Rendering triangle
        glBegin(GL_TRIANGLES);
        glColor3f(1f, 0f, 0f);
        glVertex2f(-1f, -1);
        glColor3f(0f, 1f, 0f);
        glVertex2f(1, 0);
        glColor3f(0f, 0f, 1f);
        glVertex2f(0, 1);
        glEnd();


        drawPoints();
        drawLines();
        drawPolygons();

        String text = this.getClass().getName()
                + ": Mode: " + " [P]oints: " + drawPointMode
                + " [L]ines: " + drawLineMode
                + " [F]aces: " + drawPolygonMode;

        //create and draw text
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, text);
        textRenderer.addStr2D(width - 90, height - 3, " (c) PGRF UHK");
        textRenderer.draw();
    }

    private void drawPoints() {
        // nastaveni michani barev (blending) u 3 a 4 bodu
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        if (drawPointMode % 10 == 0) return;
        // nastaveni velikost bodu
        glPointSize((drawPointMode % 10) * 5);

        // zadani geometrie a barvy
        glBegin(GL_POINTS);
        glColor3f(1, 0.2f, 0.1f);
        glVertex2f(0.4f, 0.4f);
        glColor4f(0.2f, 1.0f, 1.0f, 1.0f);
        glVertex2f(0.7f, .9f);
        glColor4f(0.2f, 1.0f, 1.0f, 0.5f);
        glVertex2f(0.7f, 0.8f);
        glColor4f(0.2f, 1.0f, 1.0f, 1.0f);
        glVertex2f(0.2f, 0.1f);
        glEnd();
        glDisable(GL_BLEND);
    }

    private void drawLines() {
        // nastaveni sirky cary
        glLineWidth(drawLineMode % 10 + 1);
        // povoleni nastaveni vzhledu cary
        glEnable(GL_LINE_STIPPLE);
        // nastaveni vzhledu cary
        glLineStipple(1, (short) (drawLineMode ^ 2));

        // nastaveni zpusobu vykresleni
        switch (drawLineMode % 4) {
            case 0:
                return;
            case 1:
                glBegin(GL_LINES); // samostatne usecky, zadane kazdu novou
                // dvojici bodu
                break;
            case 2:
                glBegin(GL_LINE_STRIP); // posloupnost usecek
                break;
            case 3:
                glBegin(GL_LINE_LOOP); // uzavrena oblast
                break;
        }

        // zadani geometrie a bravy
        glColor3f(1.0f, 0.5f, 0.0f);
        glVertex2f(0.1f, 0.7f);
        glColor3f(0.0f, 0.5f, 1.0f);
        glVertex2f(0.6f, 0.9f);
        glColor3f(0.1f, 1.0f, 0.5f);
        glVertex2f(0.8f, 0.3f);
        glColor3f(0f, 0.5f, 1.0f);
        glVertex2f(0.1f, 0.2f);
        glEnd();

        glDisable(GL_LINE_STIPPLE);
    }

    private void drawPolygons() {
        // volba poradi pro zadani vrcholu (counter clockwise CCW)
        glFrontFace(GL_CCW);
        // glFrontFace(GL_CW);

        // zpusob vykresleni privracenych a odvracenych ploch
        glPolygonMode(GL_FRONT, // GL_FRONT_AND_BACK,GL_FRONT,GL_BACK
                GL_FILL); // GL_LINE,GL_POINT,GL_FILL
        glPolygonMode(GL_BACK, // GL_FRONT_AND_BACK,GL_FRONT,GL_BACK
                GL_LINE); // GL_LINE,GL_POINT,GL_FILL

        // povoleni a nastaveni odstraneni odvracenych ploch
        // glEnable(GL_CULL_FACE);
        // glCullFace(GL_BACK);
        // glCullFace(GL_FRONT);
        // glCullFace(GL_FRONT_AND_BACK);

        // nastaveni zpusobu vykresleni polygonu
        switch (drawPolygonMode % 7) {
            case 0:
                return;
            case 1:
                glBegin(GL_QUADS);
                break;
            case 2:
                glBegin(GL_QUAD_STRIP);
                break;
            case 3:
                glBegin(GL_TRIANGLES);
                break;
            case 4:
                glBegin(GL_TRIANGLE_STRIP);
                break;
            case 5:
                glBegin(GL_TRIANGLE_FAN);
                break;
            case 6:
                glBegin(GL_POLYGON);
                break;
        }
        // zadani geometrie a bravy
        glColor3f(1, 1, 1);
        glVertex2f(0.8f, 0.5f);
        glColor3f(0, 0, 0);
        glVertex2f(0.7f, 0.8f);
        glColor3f(1, 1, 1);
        glVertex2f(0.5f, 0.7f);
        glColor3f(0, 1, 1);
        glVertex2f(0.9f, 0.1f);
        glColor3f(0, 1, 0);
        glVertex2f(0.2f, 0.5f);
        glColor3f(1, 0, 0);
        glVertex2f(0.4f, 0.3f);
        glColor3f(0.5f, 0.2f, 0.6f);
        glVertex2f(0.3f, 0.1f);
        glColor3f(1, 1, 0.5f);
        glVertex2f(0.4f, 0.1f);
        glEnd();

        glDisable(GL_CULL_FACE);
    }

}
