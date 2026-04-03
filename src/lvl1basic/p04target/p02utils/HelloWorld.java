package lvl1basic.p04target.p02utils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import lwjglutils.ShaderUtils;
import lwjglutils.ToFloatArray;
import transforms.Camera;
import transforms.Mat4;
import transforms.Mat4PerspRH;
import transforms.Mat4Scale;
import transforms.Vec3D;
import lwjglutils.OGLBuffers;
import lwjglutils.OGLRenderTarget;
import lwjglutils.OGLTextRenderer;
import lwjglutils.OGLTexture2D;
import lwjglutils.OGLUtils;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
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
	
	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, locMat;
	
	OGLTexture2D texture;
	
	Camera cam = new Camera();
	Mat4 proj = new Mat4PerspRH(Math.PI / 4, 1, 1, 10.0);

	OGLRenderTarget renderTarget;
	OGLTexture2D.Viewer textureViewer;
	
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
                	proj = new Mat4PerspRH(Math.PI / 4, height / (double) width, 1, 10.0);
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

		GLUtil.setupDebugMessageCallback();

		OGLUtils.printOGLparameters();
		
		glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

		createBuffers();

		shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p04target/p02utils/texture");

		glUseProgram(this.shaderProgram);
		
		locMat = glGetUniformLocation(shaderProgram, "mat");
		
		renderTarget = new OGLRenderTarget(512, 512,1);

		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);
		
		try {
			texture = new OGLTexture2D("textures/mosaic.jpg");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		glDisable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);

		textureViewer = new OGLTexture2D.Viewer();
		
		textRenderer = new OGLTextRenderer(width, height);	
	}
	
	
	void createBuffers() {
		// vertices are not shared among triangles (and thus faces) so each face
		// can have a correct normal in all vertices
		// also because of this, the vertices can be directly drawn as GL_TRIANGLES
		// (three and three vertices form one face) 
		// triangles defined in index buffer
				float[] cube = {
						// bottom (z-) face
						1, 0, 0,	0, 0, -1, 	1, 0,
						0, 0, 0,	0, 0, -1,	0, 0, 
						1, 1, 0,	0, 0, -1,	1, 1, 
						0, 1, 0,	0, 0, -1,	0, 1, 
						// top (z+) face
						1, 0, 1,	0, 0, 1,	1, 0, 
						0, 0, 1,	0, 0, 1,	0, 0, 
						1, 1, 1,	0, 0, 1,	1, 1,
						0, 1, 1,	0, 0, 1,	0, 1,
						// x+ face
						1, 1, 0,	1, 0, 0,	1, 0,
						1, 0, 0,	1, 0, 0,	0, 0, 
						1, 1, 1,	1, 0, 0,	1, 1,
						1, 0, 1,	1, 0, 0,	0, 1,
						// x- face
						0, 1, 0,	-1, 0, 0,	1, 0,
						0, 0, 0,	-1, 0, 0,	0, 0, 
						0, 1, 1,	-1, 0, 0,	1, 1,
						0, 0, 1,	-1, 0, 0,	0, 1,
						// y+ face
						1, 1, 0,	0, 1, 0,	1, 0,
						0, 1, 0,	0, 1, 0,	0, 0, 
						1, 1, 1,	0, 1, 0,	1, 1,
						0, 1, 1,	0, 1, 0,	0, 1,
						// y- face
						1, 0, 0,	0, -1, 0,	1, 0,
						0, 0, 0,	0, -1, 0,	0, 0, 
						1, 0, 1,	0, -1, 0,	1, 1,
						0, 0, 1,	0, -1, 0,	0, 1
				};

				int[] indexBufferData = new int[36];
				for (int i = 0; i<6; i++){
					indexBufferData[i*6] = i*4;
					indexBufferData[i*6 + 1] = i*4 + 1;
					indexBufferData[i*6 + 2] = i*4 + 2;
					indexBufferData[i*6 + 3] = i*4 + 1;
					indexBufferData[i*6 + 4] = i*4 + 2;
					indexBufferData[i*6 + 5] = i*4 + 3;
				}
				
				
		OGLBuffers.Attrib[] attributes = {
				new OGLBuffers.Attrib("inPosition", 3),
				new OGLBuffers.Attrib("inNormal", 3),
				new OGLBuffers.Attrib("inTextureCoordinates", 2)
		};

		buffers = new OGLBuffers(cube, attributes, indexBufferData);

		System.out.println(buffers.toString());
	}

	private void loop() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {

			//glEnable(GL_DEPTH_TEST);
			glViewport(0, 0, width, height);
			
			// set our render target (texture)
			renderTarget.bind();

			glClearColor(0.7f, 1.0f, 1.0f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			
			texture.bind(shaderProgram, "textureID", 0);
			
			glUniformMatrix4fv(locMat, false,
					ToFloatArray.convert(cam.getViewMatrix().mul(proj).mul(new Mat4Scale((double)width / height, 1, 1))));
			
			// bind and draw
			buffers.draw(GL_TRIANGLES, shaderProgram);
			
			// set the default render target (screen)
			glBindFramebuffer(GL_FRAMEBUFFER, 0);
			glViewport(0, 0, width, height);

			glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			
			// use the result of the previous draw as a texture for the next
			//renderTarget.bindColorTexture(shaderProgram, "textureID", 0);
			renderTarget.getColorTexture().bind(shaderProgram, "textureID", 0);
			// use the depth buffer from the previous draw as a texture for the next
			renderTarget.bindDepthTexture(shaderProgram, "textureID", 0);

			glUniformMatrix4fv(locMat, false,
					ToFloatArray.convert(cam.getViewMatrix().mul(proj)));
			buffers.draw(GL_TRIANGLES, shaderProgram);
			
			String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

			//OGLTexture2D depthTexture = renderTarget.getDepthTexture();
			//depthTexture.bind();
			//glTexParameteri(GL_TEXTURE_2D,
			//		GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);

			textureViewer.view(texture, -1, -1, 0.5);
			textureViewer.view(renderTarget.getColorTexture(), -1, -0.5, 0.5);
			renderTarget.getDepthTexture().bind();
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
			textureViewer.view(renderTarget.getDepthTexture(), -1, 0, 0.5);
			
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