package lvl0fixpipeline.p01start;

import lvl0fixpipeline.global.AbstractRenderer;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

import static org.lwjgl.opengl.GL11.*;

/**
 * Simple scene rendering
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class Renderer extends AbstractRenderer {
    int angle = 0; // 0 na začátek, aby byl inicializovaný

    public Renderer() {
        super();

        glfwWindowSizeCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                // přizpůsobení změny velikost okna
                if (w > 0 && h > 0) {
                    width = w;
                    height = h;
                }
            }
        };

        /*used default glfwKeyCallback */

        glfwMouseButtonCallback = null; //glfwMouseButtonCallback do nothing

        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                System.out.println("glfwCursorPosCallback"); // hýbání myší
            }
        };

        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
                //do nothing
            }
        };
    }

    // na začátku, stane se jen jednou
    @Override
    public void init() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    // neustále se opakuje (loop), dokud není zavřeno okno
    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

        // model and view matrix initialization
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity(); // identity matrix

        // projection matrix initialization
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity(); // identity matrix

        //glTranslatef(1,0,0);
        //glScalef(0.5f, 0.5f, 1); // zmenšení
        angle++; // bude se zvětšovat každým překreslením
        glRotatef(angle, 0, 0, 1); // rotace kolem osy z (0,0,1) o úhel angle

        // Rendering triangle by fixed pipeline
        // stavový automat = nastaví barvu, která platí, dokud se nezmění na jinou
        // výchozí barva, pokud by se nenastavila, je bílá (1, 1, 1)
        glBegin(GL_TRIANGLES);
        // RGB trojúhelník
        glColor3f(1f, 0f, 0f); // 3 = počet, f = float (d by byl double)
        glVertex2f(-1f, -1); // souřadnice v NDC (-1 a -1 je vlevo dole)
        glColor3f(0f, 1f, 0f);
        glVertex2f(1, 0);
        glColor3f(0f, 0f, 1f);
        glVertex2f(0, 1);

        // CMY trojúhelník
        glColor3f(0f, 1f, 1f); // C
        glVertex2f(-1,0);
        glColor3f(1f, 0f, 1f); // M
        glVertex2f(1,1);
        glColor3f(1f, 1f, 0f); // Y
        glVertex2f(0,-1);
        glEnd();
    }

}
