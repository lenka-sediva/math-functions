package lvl1basic.p01start.p02attribute;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import lwjglutils.ShaderUtils;
import lwjglutils.OGLUtils;

import java.nio.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
/**
 * GLSL sample:<br/>
 * Adding another attribute (color) to vertices via vertex buffer<br/>
 * Requires LWJGL3
 * 
 * @author PGRF FIM UHK
 * @version 3.0
 * @since 2019-07-11
 */
public class HelloWorld {

	int width = 300, height = 300;

	// The window handle
	private long window;
	
	int vertexBuffer, indexBuffer;
	
	int shaderProgram;
	
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
		});

		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && 
                		(HelloWorld.this.width != width || HelloWorld.this.height != height)) {
                	HelloWorld.this.width = width;
                	HelloWorld.this.height = height;
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
		glfwSwapInterval(0);

		// Make the window visible
		glfwShowWindow(window);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		OGLUtils.printOGLparameters();
		OGLUtils.printLWJLparameters();
		OGLUtils.printJAVAparameters();
		
		// Set the clear color
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

		createBuffers();
		
		shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p01start/attribute.vert",
				"/lvl1basic/p01start/attribute.frag",
				null,null,null,null); 
		
		// Shader program set
		glUseProgram(this.shaderProgram);

	}
	
	void createBuffers() {
		// create and fill vertex buffer data
		float[] vertexBufferData = {
				-1, -1, 	0.7f, 0, 0, 
				 1,  0,		0, 0.7f, 0,
				 0,  1,		0, 0, 0.7f  
		};
		// create buffer required for sending data to a native library
		FloatBuffer vertexBufferBuffer = (FloatBuffer) BufferUtils.createFloatBuffer(vertexBufferData.length)
				.put(vertexBufferData).rewind();
		
		vertexBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glBufferData(GL_ARRAY_BUFFER, vertexBufferBuffer, GL_STATIC_DRAW);

		// create and fill index buffer data (element buffer in OpenGL terminology)
		short[] indexBufferData = { 0, 1, 2 };
		
		// create buffer required for sending data to a native library
		ShortBuffer indexBufferBuffer = (ShortBuffer) BufferUtils.createShortBuffer(indexBufferData.length)
				.put(indexBufferData).rewind();

		indexBuffer = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferBuffer,
				GL_STATIC_DRAW);
	}

	void bindBuffers() {
		// internal OpenGL ID of a vertex shader input variable
				int locPosition = glGetAttribLocation(shaderProgram, "inPosition"); 
				int locColor = glGetAttribLocation(shaderProgram, "inColor");
				glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
				glEnableVertexAttribArray(locPosition);
				glVertexAttribPointer(
						locPosition, // shader variable ID
						2, // number of components (coordinates, color channels,...)
						GL_FLOAT, // component data type
						false, // normalize integer data to [0,1]
						20, // size of a vertex in bytes
						0); // number of bytes from vertex from vertex start to the first component
				glEnableVertexAttribArray(locColor);
				glVertexAttribPointer(locColor, 3, GL_FLOAT, false, 20, 8);
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			
			glViewport(0, 0, width, height);
			
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			// set the current shader to be used, could have been done only once (in
			// init) in this sample (only one shader used)
			glUseProgram(shaderProgram); 
			// to use the default shader of the "fixed pipeline", call
			// glUseProgram(0);

			
			// bind the vertex and index buffer to shader, could have been done only
			// once (in init) in this sample (only one geometry used)
			bindBuffers();
			// draw
			glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_SHORT, 0);

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