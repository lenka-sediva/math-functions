package lvl0fixpipeline.app;

import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.User32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * MAIN APPLICATION — Swing window with an embedded OpenGL renderer
 *
 * Layout:
 * - Left: OpenGL canvas (GLFW window overlaid on the AWT window)
 * - Right: Swing control panel for the parameters
 *
 * Technology:
 * - The GLFW window runs in a separate thread
 * - Swing UI and the GL thread are synchronized through volatile fields
 * - The GLFW window has no decorations and exactly overlays a Swing component
 * - On Windows the GLFW window is "owned" by the Swing window → it stays above it,
 *   but not above other applications (elsewhere GLFW_FLOATING is used)
 */
public class App extends JFrame {
	// side panel
	private static final int PANEL_WIDTH  = 350;
	// GL window
	private static final int GL_WIDTH     = 900;
	private static final int GL_HEIGHT    = 725;
	// main window title (used to look up its HWND on Windows)
	private static final String TITLE     = "Function Visualizer f(x,y)  —  LWJGL / OpenGL";

	private final FunctionRenderer renderer;
	private GLThread     glThread;
	private final JPanel       glPlaceholder; // Swing panel that reserves space for the GL window

	/**
	 * Application entry point — starts App on the EDT
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	/**
	 * Constructor — creates and initializes the UI
	 */
	public App() {
		super(TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		getContentPane().setBackground(new Color(20, 20, 28));

		// Create the renderer (started later from the GL thread)
		renderer = new FunctionRenderer();

		// Placeholder panel — the GLFW window is overlaid on it
		glPlaceholder = new JPanel();
		glPlaceholder.setPreferredSize(new Dimension(GL_WIDTH, GL_HEIGHT));
		glPlaceholder.setBackground(new Color(12, 12, 18));
		glPlaceholder.setMinimumSize(new Dimension(400, 300));

		// Control panel on the right
		ControlPanel controlPanel = new ControlPanel(renderer);
		controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, GL_HEIGHT));

		// SplitPane: GL on the left, controls on the right
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				glPlaceholder, controlPanel);
		split.setDividerLocation(GL_WIDTH);
		split.setDividerSize(4);
		split.setBackground(new Color(40, 40, 55));
		split.setBorder(null);
		split.setResizeWeight(1.0); // the GL window gets the extra space on resize

		add(split, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		// Window shown listener — starts the GL thread once the coordinates are known
		addComponentListener(new ComponentAdapter() {
			boolean started = false;
			@Override public void componentShown(ComponentEvent e)  { startGL(); }
			@Override public void componentMoved(ComponentEvent e)  { syncGLWindow(); }
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }

			/** Starts the GL thread only once */
			private void startGL() {
				if (started) return;
				started = true;
				glThread = new GLThread();
				glThread.setDaemon(true);
				glThread.start();
			}
		});

		// GL placeholder change listener — keeps the GL window position in sync
		glPlaceholder.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }
		});

		// Window close listener
		addWindowListener(new WindowAdapter() {
			/** The user is closing the window → stop the GL thread */
			@Override public void windowClosing(WindowEvent e) {
				if (glThread != null) glThread.requestStop();
			}
		});
	}

	/**
	 * Moves and resizes the GLFW window so it exactly overlays glPlaceholder
	 * (called from the Swing EDT, the request is handed over to the GL thread)
	 */
	void syncGLWindow() {
		if (glThread == null || !glThread.isWindowCreated()) return;
		Rectangle r = placeholderDeviceBounds();
		// Hand the request over to the GL thread (the GLFW API is not thread-safe)
		glThread.requestReposition(r.x, r.y, r.width, r.height);
	}

	/**
	 * Returns the position and size of glPlaceholder in physical screen pixels.
	 *
	 * Swing works in logical coordinates (smaller with Windows scaling, e.g. 125 %),
	 * GLFW in physical pixels → without conversion the GL window would end up
	 * shifted to the top left and smaller.
	 */
	private Rectangle placeholderDeviceBounds() {
		Point loc = glPlaceholder.getLocationOnScreen();
		GraphicsConfiguration gc = glPlaceholder.getGraphicsConfiguration();
		if (gc == null) gc = getGraphicsConfiguration();
		AffineTransform t = gc.getDefaultTransform();
		Rectangle screen = gc.getBounds(); // the monitor origin is already in physical pixels
		double sx = t.getScaleX(), sy = t.getScaleY();
		int x = (int) Math.round(screen.x + (loc.x - screen.x) * sx);
		int y = (int) Math.round(screen.y + (loc.y - screen.y) * sy);
		int w = (int) Math.round(glPlaceholder.getWidth()  * sx);
		int h = (int) Math.round(glPlaceholder.getHeight() * sy);
		return new Rectangle(x, y, w, h);
	}

	/**
	 * Windows: sets the Swing window as the owner of the GLFW window.
	 * An owned window always stays above its owner, hides with it when minimized
	 * and stays below other applications when the user switches to them.
	 *
	 * @return true if the owner was set successfully
	 */
	private static boolean setWin32Owner(long glfwWindow) {
		long findWindowW = User32.getLibrary().getFunctionAddress("FindWindowW");
		if (findWindowW == 0L) return false;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			long owner = JNI.callPPP(
					memAddress(stack.UTF16("SunAwtFrame")),
					memAddress(stack.UTF16(TITLE)),
					findWindowW);
			if (owner == 0L) return false;
			long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
			User32.SetWindowLongPtr(null, hwnd, User32.GWL_HWNDPARENT, owner);
			return true;
		}
	}

	/**
	 * INNER THREAD — runs the GLFW event loop
	 *
	 * Communication with the Swing EDT is synchronized through volatile fields
	 */
	class GLThread extends Thread {
		private volatile boolean stopRequested  = false;
		private volatile boolean windowCreated  = false;
		private volatile boolean pending = false; // is a reposition request waiting?
		private volatile int  pendingX, pendingY; // coordinates can be negative (monitor on the left)
		private volatile int  pendingW, pendingH;
		private long window;

		// Setter for stopRequested
		void requestStop() { stopRequested = true; }

		// Getter for windowCreated
		boolean isWindowCreated() { return windowCreated; }

		// Stores a pending reposition request
		void requestReposition(int x, int y, int w, int h) {
			pendingX = x; pendingY = y;
			pendingW = w; pendingH = h;
			pending = true;
		}

		@Override
		public void run() {
			// GLFW initialization
			if (!glfwInit()) throw new RuntimeException("Cannot init GLFW");

			// Window hints for the new window
			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE,       GLFW_FALSE); // shown only after it is positioned
			glfwWindowHint(GLFW_RESIZABLE,     GLFW_FALSE); // the size is controlled from Swing
			glfwWindowHint(GLFW_DECORATED,     GLFW_FALSE); // no title bar or border
			glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE); // does not take focus automatically

			// Initial position and size from the placeholder (physical pixels)
			Rectangle r = placeholderDeviceBounds();

			// Create the GLFW window
			window = glfwCreateWindow(
					Math.max(r.width, 100), Math.max(r.height, 100),
					"GL", 0L, 0L);
			if (window == 0L) throw new RuntimeException("Cannot create GLFW window");

			// Keep the GL window above the Swing window: via the owner on Windows,
			// elsewhere (or if the owner cannot be found) as "floating"
			boolean owned = Platform.get() == Platform.WINDOWS && setWin32Owner(window);
			if (!owned) glfwSetWindowAttrib(window, GLFW_FLOATING, GLFW_TRUE);

			// Move the window into place and only then show it
			glfwSetWindowPos(window, r.x, r.y);
			glfwShowWindow(window);

			// Callbacks
			glfwSetKeyCallback        (window, renderer.getGlfwKeyCallback());
			glfwSetWindowSizeCallback (window, renderer.getGlfwWindowSizeCallback());
			glfwSetMouseButtonCallback(window, renderer.getGlfwMouseButtonCallback());
			glfwSetCursorPosCallback  (window, renderer.getGlfwCursorPosCallback());
			glfwSetScrollCallback     (window, renderer.getGlfwScrollCallback());

			// Click in the placeholder → the GL window gets focus
			glPlaceholder.addMouseListener(new MouseAdapter() {
				@Override public void mousePressed(MouseEvent e) {
					glfwFocusWindow(window);
				}
			});

			// OpenGL initialization
			glfwMakeContextCurrent(window);
			glfwSwapInterval(1); // vsync ON
			GL.createCapabilities(); // loads the OpenGL function pointers

			renderer.init(); // initializes the renderer (lighting, camera, etc.)
			windowCreated = true; // signals to Swing that the window is ready
			syncGLWindow(); // initial position sync

			// Main loop
			while (!stopRequested && !glfwWindowShouldClose(window)) {
				// Apply a pending move/resize from Swing
				if (pending) {
					pending = false;
					glfwSetWindowPos (window, pendingX, pendingY);
					glfwSetWindowSize(window, Math.max(pendingW, 100), Math.max(pendingH, 100));
					// Tell the renderer about the new size
					renderer.setWidth(Math.max(pendingW, 100));
					renderer.setHeight(Math.max(pendingH, 100));
				}

				// Main render call
				renderer.display();
				glfwSwapBuffers(window);
				glfwPollEvents(); // processes keyboard, mouse, etc.
			}

			// Shutdown and cleanup
			renderer.dispose();
			glfwDestroyWindow(window);
			glfwTerminate();
			System.exit(0);
		}
	}
}