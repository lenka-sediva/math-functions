package lvl1basic.p03texture.p01intro;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
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
	boolean mouseButton1 = false;
	// The window handle
	private long window;
	
	OGLBuffers buffers;
	OGLTextRenderer textRenderer;
	
	int shaderProgram, locMat;
	int id;
	
	boolean depthTest = true, cCW = true, renderLine = false;
	Camera cam = new Camera();
	Mat4 proj = new Mat4PerspRH(Math.PI / 4, 1, 0.01, 1000.0);
	
	private void init() throws IOException {
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
		
		if (OGLUtils.getVersionGLSL()<330)
			shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p03texture/p01intro/textureOld");
		else
			shaderProgram = ShaderUtils.loadProgram("/lvl1basic/p03texture/p01intro/texture");
		
		
		glUseProgram(this.shaderProgram);
		
		locMat = glGetUniformLocation(shaderProgram, "mat");

		// load texture using LWJGL objects
		
		createTexture();
		
		cam = cam.withPosition(new Vec3D(5, 5, 2.5))
				.withAzimuth(Math.PI * 1.25)
				.withZenith(Math.PI * -0.125);
		
		glDisable(GL_CULL_FACE); 
		glFrontFace(GL_CCW);
		glEnable(GL_DEPTH_TEST);
		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		
		textRenderer = new OGLTextRenderer(width, height);	
	}
	
	void createTexture() throws IOException {
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer components = BufferUtils.createIntBuffer(1);
        ByteBuffer data = stbi_load_from_memory(ioResourceToByteBuffer("textures/mosaic.jpg", 1024), width, height, components, 4);
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        stbi_image_free(data);
    }

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     *
     * @return the resource data
     *
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null)
            throw new IOException("Classpath resource not found: " + resource);
        File file = new File(url.getFile());
        if (file.isFile()) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            fc.close();
            fis.close();
        } else {
			buffer = BufferUtils.createByteBuffer(bufferSize);
			InputStream source = url.openStream();
			if (source == null)
				throw new FileNotFoundException(resource);
			try {
				byte[] buf = new byte[bufferSize];
				while (true) {
					int bytes = source.read(buf, 0, buf.length);
					if (bytes == -1)
						break;
					if (buffer.remaining() < bytes) {
						int newCapacity = buffer.capacity();
						while ((buffer.position() + bytes) > newCapacity)
							newCapacity *= 2;
						buffer = resizeBuffer(buffer, newCapacity);
					}
					buffer.put(buf, 0, bytes);
				}
				buffer.flip();
			} finally {
				source.close();
			}
        }
        return buffer;
    }

	void createBuffers() {
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
			
			String text = new String(this.getClass().getName() + ": [LMB] camera, WSAD");

			glViewport(0, 0, width, height);
			
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			
			// set the current shader to be used
			glUseProgram(shaderProgram); 
			
			glBindTexture(GL_TEXTURE_2D, id);
			glUniformMatrix4fv(locMat, false,
					ToFloatArray.convert(cam.getViewMatrix().mul(proj)));
			
			// bind and draw
			buffers.draw(GL_TRIANGLES, shaderProgram);
			
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