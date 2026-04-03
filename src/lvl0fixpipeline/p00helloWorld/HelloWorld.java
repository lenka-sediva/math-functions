package lvl0fixpipeline.p00helloWorld;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * <br/>
 * Rendering without shaders, using fixed pipeline <br/>
 * Requires LWJGL3
 *
 * @author PGRF FIM UHK
 * @version 3.1
 * @since 2020-01-20
 */
public class HelloWorld {

	// The window handle
	private long window;

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
					window,
					(vidmode.width() - pWidth.get(0)) / 2,
					(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(0);

		// Make the window visible
		glfwShowWindow(window);

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		System.out.println("OpenGL version " + glGetString(GL_VERSION));
		System.out.println("OpenGL vendor " + glGetString(GL_VENDOR));
		System.out.println("OpenGL renderer " + glGetString(GL_RENDERER));
		System.out.println("OpenGL extension " + glGetString(GL_EXTENSIONS));

		System.out.println("Red bits " + glGetInteger(GL_RED_BITS));
		System.out.println("Get bits " + glGetInteger(GL_GREEN_BITS));
		System.out.println("Blue bits " + glGetInteger(GL_BLUE_BITS));
		System.out.println("Alpha bits " + glGetInteger(GL_ALPHA_BITS));
		System.out.println("Depth bits " + glGetInteger(GL_DEPTH_BITS));
		System.out.println("Stencil bits " + glGetInteger(GL_STENCIL_BITS));
		// Set the clear color
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

		// Fixed pipeline set
		glUseProgram(0);
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			// Rendering triangle by fixed pipeline
			glBegin(GL_TRIANGLES);
			glColor3f(1f, 0f, 0f);
			glVertex2f(-1f, -1);
			glColor3f(0f, 1f, 0f);
			glVertex2f(1, 0);
			glColor3f(0f, 0f, 1f);
			glVertex2f(0, 1);
			glEnd();

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public void run() {
		try {
			System.out.println("Hello LWJGL " + Version.getVersion() + "!");
			init();

			loop();

			// Free the window callbacks and destroy the window
			glfwFreeCallbacks(window);
			glfwDestroyWindow(window);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			// Terminate GLFW and free the error callback
			glfwTerminate();
			glfwSetErrorCallback(null).free();
		}
	}

	public static void main(String[] args) {
		new HelloWorld().run();
	}

}
