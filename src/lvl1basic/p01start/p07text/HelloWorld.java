package lvl1basic.p01start.p07text;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import lwjglutils.ShaderUtils;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLTextRenderer;
import lwjglutils.OGLUtils;

import java.awt.*;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * GLSL sample:<br/>
 * Print texts and draw two different geometries with two different shader programs using depth buffer<br/>
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
	
	OGLBuffers buffers, buffers2;
	OGLTextRenderer textRenderer, textRenderer2, textRenderer3;
	
	int shaderProgram, shaderProgram2, locTime, locTime2;

	long frame = 0;
	double fps;
	Boolean depthTest = false;
	long oldFPSmils;

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
			if ( key == GLFW_KEY_D && action == GLFW_RELEASE ){
				depthTest = !depthTest;
				System.out.println("Depth test is " + (depthTest ? "on" : "off"));
			}
		});

		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && 
                		(HelloWorld.this.width != width || HelloWorld.this.height != height)) {
                	HelloWorld.this.width = width;
                	HelloWorld.this.height = height;
                	if (textRenderer != null) {
						textRenderer.resize(width, height);
						textRenderer2 = new OGLTextRenderer(width, height, new Font("SansSerif", Font.PLAIN, height/20));
						textRenderer3.resize(width, height);

					}
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
			assert vidmode != null;
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
		//GLUtil.setupDebugMessageCallback();
		OGLUtils.printOGLparameters();
		OGLUtils.printLWJLparameters();
		OGLUtils.printJAVAparameters();
		
		// Set the clear color
		glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

		createBuffers();
		
		shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p01start/p06depthbuffer/start");
		shaderProgram2 = ShaderUtils.loadProgram("/lvl1basic/p01start/p06depthbuffer/start2");
		
		// Shader program set
		glUseProgram(this.shaderProgram);
		
		// internal OpenGL ID of a shader uniform (constant during one draw call
		// - constant value for all processed vertices or pixels) variable
		locTime = glGetUniformLocation(shaderProgram, "time");
		locTime2 = glGetUniformLocation(shaderProgram2, "time");

		textRenderer = new OGLTextRenderer(width, height);
		textRenderer2 = new OGLTextRenderer(width, height, new Font("SansSerif", Font.PLAIN, 20));
		textRenderer3 = new OGLTextRenderer(width, height);
		textRenderer3.setColor(Color.BLUE);
		textRenderer3.setBackgroundColor(Color.LIGHT_GRAY);

	}
	
	void createBuffers() {
		float[] vertexBufferData = {
			-1, -1, 	0.7f, 0, 0, 
			 1,  0,		0, 0.7f, 0,
			 0,  1,		0, 0, 0.7f 
		};
		int[] indexBufferData = { 0, 1, 2 };

		// vertex binding description, concise version
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 2), // 2 floats
				new OGLBuffers.Attrib("inColor", 3) // 3 floats
		};
		buffers = new OGLBuffers(vertexBufferData, attributes,
				indexBufferData);
		
		float[] vertexBufferDataPos = {
				-1, 1, 
				0.5f, 0,
				-0.5f, -1 
			};
			float[] vertexBufferDataCol = {
				0, 1, 1, 
				1, 0, 1,
				1, 1, 1 
			};
			OGLBuffers.Attrib[] attributesPos = {
					new OGLBuffers.Attrib("inPosition", 2),
			};
			OGLBuffers.Attrib[] attributesCol = {
					new OGLBuffers.Attrib("inColor", 3)
			};
			
			buffers2 = new OGLBuffers(vertexBufferDataPos, attributesPos,
					indexBufferData);
			buffers2.addVertexBuffer(vertexBufferDataCol, attributesCol);
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {

			// fps calculation every ten frames
			if (frame%10 == 0) {
				long mils = System.currentTimeMillis();
				fps = 10000 / (double) (mils - oldFPSmils + 1);
				oldFPSmils = mils;
			}




			String text = this.getClass().getName();
			if (depthTest){
				//enable depth test
				glEnable(GL_DEPTH_TEST);
				text += ": [D]epth test";
			}else{
				//disable depth test
				glDisable(GL_DEPTH_TEST);
				text += ": [d]epth test";
			}	
			
			glViewport(0, 0, width, height);
			
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			frame += 1;
			
			// set the current shader to be used
			glUseProgram(shaderProgram); 
			glUniform1f(locTime, frame/100.f); // correct shader must be set before this
			
			// bind and draw
			buffers.draw(GL_TRIANGLES, shaderProgram);
			
			// set the current shader to be used
			glUseProgram(shaderProgram2); 
			glUniform1f(locTime2, frame/100.f); // correct shader must be set before this
			
			// bind and draw
			buffers2.draw(GL_TRIANGLES, shaderProgram2);
			
			//create and draw text
			textRenderer.clear();
			textRenderer2.setColor(Color.YELLOW);
			textRenderer2.setBackgroundColor(new Color(255, 255, 255, 0));
			for(int i=1; i<=10; i++) {
				textRenderer2.setRotationAngle(Math.PI*2.*i/10.);
				textRenderer2.setScale(0.2 * (1 + i));
				textRenderer2.addStr2D(width / 2, height / 2, "HelloWorld");
			}
			textRenderer3.addStr2D(3, 60, "áčéěíňůúřšťýž");
			textRenderer3.addStr2D(3, 40, "ABCČDĎEÉĚFGHIÍJKLĽMNŇOÓPQRŘSŠTŤUÚVWXYÝZŽ");
			textRenderer3.addStr2D(3, height-3, "0123456789");
			
			textRenderer.addStr2D(3, height-23, "frame: " + frame +
					 String.format("; fps: %4.1f", fps));

			textRenderer.addStr2D(3, 20, text);
			textRenderer.addStr2D(width-90, height-3, " (c) PGRF UHK");
			textRenderer.draw();
			
			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwSwapBuffers(window); // swap the color buffers
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