package lvl0fixpipeline.app;

import lvl0fixpipeline.global.AbstractRenderer;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Renderer extends AbstractRenderer {

    public Renderer() {
        super();
    }

    @Override
    public void init() {
        // Nastavení barvy pozadí
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Povolení testu hloubky
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // Jednoduchá projekce
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // Perspektivní projekce (fovy, aspect, near, far)
        float aspect = (float) width / height;
        float fovy = 45f; // field of view v y-ové rovině
        float near = 0.1f;
        float far = 100f;
        float top = (float) Math.tan(Math.toRadians(fovy / 2)) * near;
        float right = top * aspect;
        glFrustum(-right, right, -top, top, near, far);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslatef(0f, 0f, -5f); // posun kamery dozadu
    }

    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Rotující kostka jako test
        glPushMatrix();
        glRotatef((float) (glfwGetTime() * 50), 1f, 1f, 0f);

        glBegin(GL_QUADS);
        // přední strana (Z+)
        glColor3f(1f, 0f, 0f);
        glVertex3f(-1f, -1f, 1f);
        glVertex3f(1f, -1f, 1f);
        glVertex3f(1f, 1f, 1f);
        glVertex3f(-1f, 1f, 1f);

        // zadní strana (Z-)
        glColor3f(0f, 1f, 0f);
        glVertex3f(-1f, -1f, -1f);
        glVertex3f(-1f, 1f, -1f);
        glVertex3f(1f, 1f, -1f);
        glVertex3f(1f, -1f, -1f);

        // levá strana (X-)
        glColor3f(0f, 0f, 1f);
        glVertex3f(-1f, -1f, -1f);
        glVertex3f(-1f, -1f, 1f);
        glVertex3f(-1f, 1f, 1f);
        glVertex3f(-1f, 1f, -1f);

        // pravá strana (X+)
        glColor3f(1f, 1f, 0f);
        glVertex3f(1f, -1f, -1f);
        glVertex3f(1f, 1f, -1f);
        glVertex3f(1f, 1f, 1f);
        glVertex3f(1f, -1f, 1f);

        // horní strana (Y+)
        glColor3f(0f, 1f, 1f);
        glVertex3f(-1f, 1f, -1f);
        glVertex3f(-1f, 1f, 1f);
        glVertex3f(1f, 1f, 1f);
        glVertex3f(1f, 1f, -1f);

        // spodní strana (Y-)
        glColor3f(1f, 0f, 1f);
        glVertex3f(-1f, -1f, -1f);
        glVertex3f(1f, -1f, -1f);
        glVertex3f(1f, -1f, 1f);
        glVertex3f(-1f, -1f, 1f);
        glEnd();

        glPopMatrix();
    }
}