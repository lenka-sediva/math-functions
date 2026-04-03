package lvl1basic.p02geometry.p02strip;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import lwjglutils.ShaderUtils;
import lwjglutils.ToFloatArray;
import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Vec3D;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLTextRenderer;
import lwjglutils.OGLUtils;

import java.awt.event.KeyEvent;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * GLSL sample:<br/>
 * Draw two different geometries with two different shader programs<br/>
 * Requires LWJGL3
 * 
 * @author PGRF FIM UHK
 * @version 3.0
 * @since 2019-07-11
 */

public class HelloWorld {

	int width = 300, height = 300;
	double ox, oy;
	private boolean mouseButton1 = false;
	
	// The window handle
	private long window;

	OGLBuffers buffers2, buffers3, buffers, buffers4;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, locMat;

	boolean renderLine = false;
	int mode = 0;

	Camera cam = new Camera();
	Mat4 proj = new Mat4PerspRH(Math.PI / 4, 1, 0.01, 1000.0);

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			if (action == GLFW_PRESS || action == GLFW_REPEAT){
				switch (key) {
				case GLFW_KEY_W:
					cam = cam.forward(1);
					break;
				case GLFW_KEY_D:
					cam = cam.right(1);
					break;
				case GLFW_KEY_S:
					cam = cam.backward(1);
					break;
				case GLFW_KEY_A:
					cam = cam.left(1);
					break;
				case GLFW_KEY_LEFT_CONTROL:
					cam = cam.down(1);
					break;
				case GLFW_KEY_LEFT_SHIFT:
					cam = cam.up(1);
					break;
				case GLFW_KEY_SPACE:
					cam = cam.withFirstPerson(!cam.getFirstPerson());
					break;
				case GLFW_KEY_R:
					cam = cam.mulRadius(0.9f);
					break;
				case GLFW_KEY_F:
					cam = cam.mulRadius(1.1f);
					break;
				case GLFW_KEY_P:
					renderLine = !renderLine;
					break;
				case KeyEvent.VK_M:
					mode++;
					break;
			
				}
			}
		});
		
		glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
				if (mouseButton1) {
					cam = cam.addAzimuth((double) Math.PI * (ox - x) / width)
							.addZenith((double) Math.PI * (oy - y) / width);
					ox = x;
					oy = y;
				}
        	}
        });
		
		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback () {
			
			@Override
			public void invoke(long window, int button, int action, int mods) {
				mouseButton1 = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;
				
				if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS){
					mouseButton1 = true;
					DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
					DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
					glfwGetCursorPos(window, xBuffer, yBuffer);
					ox = xBuffer.get(0);
					oy = yBuffer.get(0);
				}
				
				if (button==GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE){
					mouseButton1 = false;
					DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
					DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
					glfwGetCursorPos(window, xBuffer, yBuffer);
					double x = xBuffer.get(0);
					double y = yBuffer.get(0);
					cam = cam.addAzimuth((double) Math.PI * (ox - x) / width)
	        				.addZenith((double) Math.PI * (oy - y) / width);
					ox = x;
					oy = y;
	        	}
			}
			
		});
		
		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int w, int h) {
                if (w > 0 && h > 0 && 
                		(w != width || h != height)) {
                	width = w;
                	height = h;
                	proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 0.01, 1000.0);
                	if (textRenderer != null)
                		textRenderer.resize(width, height);
               }
            }
        });
		
		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
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
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		OGLUtils.printOGLparameters();
		
		glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

		createBuffers();
		
		shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p02geometry/p02strip/simple");
		
		glUseProgram(this.shaderProgram);
		
		locMat = glGetUniformLocation(shaderProgram, "mat");
		
		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);
		
		glDisable(GL_CULL_FACE); 
		glFrontFace(GL_CCW);
		glEnable(GL_DEPTH_TEST);
		textRenderer = new OGLTextRenderer(width, height);	
	}

	void createBuffers() {
		float[] strip = {
				// first triangle
				1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, -1, 1, 1, 0, 0, 0, -1,
				// second triangle
				0, 0, 0, 0, -1, 0, 1, 1, 0, 0, -1, 0, 0, 1, 0, 0, -1, 0,
				// 3st triangle
				1, 1, 0, -1, 0, 0, 0, 1, 0, -1, 0, 0, 1, 2, 0, -1, 0, 0,
				// 4th triangle
				0, 1, 0, 0, 1, 0, 1, 2, 0, 0, 1, 0, 0, 2, 0, 0, 1, 0,
				// 5th triangle
				1, 2, 0, 0, 0, 1, 0, 2, 0, 0, 0, 1, 1, 3, 0, 0, 0, 1,
				// 6th triangle
				0, 2, 0, 1, 0, 0, 1, 3, 0, 1, 0, 0, 0, 3, 0, 1, 1, 1,

		};

		OGLBuffers.Attrib[] attributes = { new OGLBuffers.Attrib("inPosition", 3),
				new OGLBuffers.Attrib("inNormal", 3) };

		// create geometry without index buffer as the triangle list
		buffers = new OGLBuffers(strip, attributes, null);

		int[] indexBufferData = new int[9];
		for (int i = 0; i < 9; i += 3) {
			indexBufferData[i] = 2 * i;
			indexBufferData[i + 1] = 2 * i + 1;
			indexBufferData[i + 2] = 2 * i + 2;
		}
		// create geometry with index buffer as the triangle list [0, 1, 2, 6, 7, 8, 12, 13, 14]
		buffers2 = new OGLBuffers(strip, attributes, indexBufferData);

		int[] indexBufferData2 = { 0, 1, 2, 5, 8, 11, 14, 17 };
		// create geometry with index buffer as the triangle strip
		buffers3 = new OGLBuffers(strip, attributes, indexBufferData2);

		int[] indexBufferData3 = { 0, 1, 2, 5, 65535, 12, 13, 14, 17 };
		// create geometry with index buffer as the triangle strip with restart index
		buffers4 = new OGLBuffers(strip, attributes, indexBufferData3);
		System.out.println("buffers \n " + buffers.toString());
		System.out.println("buffers \n " + buffers2.toString());
		System.out.println("buffers \n " + buffers3.toString());
		System.out.println("buffers \n " + buffers4.toString());
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {

						glViewport(0, 0, width, height);

			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the
																// framebuffer

			// set the current shader to be used
			glUseProgram(shaderProgram);

			glUniformMatrix4fv(locMat, false, ToFloatArray.convert(cam.getViewMatrix().mul(proj)));

			String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

			if (!renderLine) {
				glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
				text += ", [p]olygon: fill";
			} else {
				glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
				text += ", [p]olygon: line";
			}

			// bind and draw
			switch (mode % 11) {
			case 0:
				text += ", [m]ode: all triangles of triangle list, without index buffer";
				buffers.draw(GL_TRIANGLES, shaderProgram);
				break;
			case 1:
				text += ", [m]ode: first 3 triangles of triangle list, without index buffer";
				// number of vertices
				buffers.draw(GL_TRIANGLES, shaderProgram, 9);
				break;
			case 2:
				text += ", [m]ode: 3rd, 4th and 5th triangles of triangle list, without index buffer";
				// number of vertices, index of the first vertex
				buffers.draw(GL_TRIANGLES, shaderProgram, 9, 6);
				break;
			case 3:
				text += ", [m]ode: odd triangles of triangle list, with defined index buffer";
				buffers2.draw(GL_TRIANGLES, shaderProgram);
				break;
			case 4:
				text += ", [m]ode: 1st and 2nd odd triangles of triangle list, with defined index buffer";
				// number of vertices
				buffers2.draw(GL_TRIANGLES, shaderProgram, 6);
				break;
			case 5:
				text += ", [m]ode: 2nd and 3rd odd triangles of triangle list, with defined index buffer";
				// number of vertices, index of the first vertex
				buffers2.draw(GL_TRIANGLES, shaderProgram, 6, 3);
				break;
			case 6:
				text += ", [m]ode: all triangles of triangle strip, with defined index buffer";
				buffers3.draw(GL_TRIANGLE_STRIP, shaderProgram);
				break;
			case 7:
				text += ", [m]ode: first 3 triangles of triangle strip, with defined index buffer";
				// number of vertices
				buffers3.draw(GL_TRIANGLE_STRIP, shaderProgram, 5);
				break;
			case 8:
				text += ", [m]ode: 3rd and 4th triangles of triangle strip, with defined index buffer and range";
				// number of vertices, index of the first vertex
				buffers3.draw(GL_TRIANGLE_STRIP, shaderProgram, 4, 2);
				break;
			case 9:
				text += ", [m]ode: 1st-2nd and 5th-6th triangles of triangle strip, with defined index buffer and primitive restart index";
				// number of vertices, index of the first vertex
				glEnable(GL_PRIMITIVE_RESTART);
				glPrimitiveRestartIndex(65535);
				buffers4.draw(GL_TRIANGLE_STRIP, shaderProgram);
				glDisable(GL_PRIMITIVE_RESTART);
				break;
			case 10:
				text += ", [m]ode: 1st and 4-6 triangles of triangle strip, with defined index buffer, primitive restart index and range";
				// number of vertices, index of the first vertex
				glEnable(GL_PRIMITIVE_RESTART);
				glPrimitiveRestartIndex(65535);
				buffers4.draw(GL_TRIANGLE_STRIP, shaderProgram,8,1);
				glDisable(GL_PRIMITIVE_RESTART);
				break;
			}
			
			textRenderer.clear();
			textRenderer.addStr2D(3, 20, text);
			textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
			textRenderer.draw();
			
			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public void run() {
		try {
			init();

			loop();

			// Free the window callbacks and destroy the window
			glfwFreeCallbacks(window);
			glfwDestroyWindow(window);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			// Terminate GLFW and free the error callback
			glDeleteProgram(shaderProgram);
			glfwTerminate();
			glfwSetErrorCallback(null).free();
		}

	}

	public static void main(String[] args) {
		new HelloWorld().run();
	}

}