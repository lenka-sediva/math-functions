package lvl1basic.p00.p01withShader;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
/**
 * GLSL sample:<br/>
 * Read and compile shader from string 
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

		System.out.println("OpenGL version " + glGetString(GL_VERSION));
		System.out.println("OpenGL vendor " + glGetString(GL_VENDOR));
		System.out
				.println("OpenGL renderer " + glGetString(GL_RENDERER));
		System.out.println("OpenGL extension "
				+ glGetString(GL_EXTENSIONS));
		
		// Set the clear color
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

		// Create all needed GL resources
		createShaders();
        
        // Fixed pipeline set
		glUseProgram(this.shaderProgram);

	}
    
	void createShaders() {
		CharSequence shaderVertSrc =  
			"#version 150\n"+
			"in vec2 inPosition;"+ // input from the vertex buffer
			"void main() {"+ 
			"	vec2 position = inPosition;"+
			"   position.x += 0.1;"+
			" 	gl_Position = vec4(position, 0.0, 1.0);"+ 
			"}";
		// gl_Position - built-in vertex shader output variable containing
		// vertex position before w-clipping and dehomogenization, must be
		// filled

		String shaderFragSrc =  
			"#version 150\n"+
			"out vec4 outColor;"+ // output from the fragment shader
			"void main() {"+
			" 	outColor = vec4(0.5,0.1,0.8, 1.0);"+ 
			"}";

		// vertex shader
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, shaderVertSrc);
		glCompileShader(vs);
		int compiled = glGetShaderi(vs, GL_COMPILE_STATUS);
        String shaderLog = glGetShaderInfoLog(vs);
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        if (compiled == 0) {
            throw new AssertionError("Could not compile VS shader");
        }
        
		// fragment shader
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, shaderFragSrc);
		glCompileShader(fs);
		compiled = glGetShaderi(fs, GL_COMPILE_STATUS);
        shaderLog = glGetShaderInfoLog(fs);
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        if (compiled == 0) {
            throw new AssertionError("Could not compile FS shader");
        }
        
		// link program
		shaderProgram = glCreateProgram();
		glAttachShader(shaderProgram, vs);
		glAttachShader(shaderProgram, fs);
		glLinkProgram(shaderProgram);
		int linked = glGetProgrami(shaderProgram, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(shaderProgram);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linked == 0) {
            throw new AssertionError("Could not link program");
        }
        
		if (vs > 0) glDetachShader(shaderProgram, vs);
		if (fs > 0) glDetachShader(shaderProgram, fs);
		if (vs > 0) glDeleteShader(vs);
		if (fs > 0) glDeleteShader(fs);
	
	}
	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			
			glViewport(0, 0, width, height);
			
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