package lvl0fixpipeline.app;

import org.lwjgl.opengl.GL;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Jedno Swing okno:
 * - vlevo OpenGL canvas (GLFW embedded do AWT)
 * - vpravo ovládací panel
 *
 * Technika:
 * - GLFW window s GLFW_VISIBLE=false
 * - vložení jeho nativní handle do AWT Canvas přes GLFWNativeWin32/X11/Cocoa
 */
public class App extends JFrame {

	private static final int PANEL_WIDTH  = 260;
	private static final int GL_WIDTH     = 900;
	private static final int GL_HEIGHT    = 700;

	private FuncRenderer renderer;
	private GLThread     glThread;

	// Panel, který drží místo pro GL okno
	private JPanel       glPlaceholder;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	public App() {
		super("Vizualizace funkcí f(x,y)  —  LWJGL / OpenGL");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		getContentPane().setBackground(new Color(20, 20, 28));

		renderer = new FuncRenderer();

		// Placeholder – sem se překryje GLFW okno
		glPlaceholder = new JPanel();
		glPlaceholder.setPreferredSize(new Dimension(GL_WIDTH, GL_HEIGHT));
		glPlaceholder.setBackground(new Color(12, 12, 18));
		glPlaceholder.setMinimumSize(new Dimension(400, 300));

		// Pravý panel s ovládáním
		ControlPanel controlPanel = new ControlPanel(renderer);
		controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, GL_HEIGHT));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				glPlaceholder, controlPanel);
		split.setDividerLocation(GL_WIDTH);
		split.setDividerSize(4);
		split.setBackground(new Color(40, 40, 55));
		split.setBorder(null);
		split.setResizeWeight(1.0); // GL plocha dostane přebytečný prostor

		add(split, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		// Spustí GL vlákno až po zobrazení okna (potřebuje souřadnice)
		addComponentListener(new ComponentAdapter() {
			boolean started = false;
			@Override public void componentShown(ComponentEvent e)  { startGL(); }
			@Override public void componentMoved(ComponentEvent e)  { syncGLWindow(); }
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }
			private void startGL() {
				if (started) return;
				started = true;
				glThread = new GLThread();
				glThread.setDaemon(true);
				glThread.start();
			}
		});

		// Pohyb/resize → synchronizace pozice GL okna
		glPlaceholder.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }
		});

		// Zavření → ukončení GL vlákna
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				if (glThread != null) glThread.requestStop();
			}
		});
	}

	// Přesune GLFW okno přesně na místo glPlaceholder na obrazovce
	void syncGLWindow() {
		if (glThread == null || !glThread.isWindowCreated()) return;
		Point loc = glPlaceholder.getLocationOnScreen();
		int   w   = glPlaceholder.getWidth();
		int   h   = glPlaceholder.getHeight();
		// Musí se volat z GL vlákna (GLFW není thread-safe)
		glThread.requestReposition(loc.x, loc.y, w, h);
	}

	//  GL vlákno — celá GLFW smyčka tady
	class GLThread extends Thread {

		private volatile boolean stopRequested = false;
		private volatile boolean windowCreated = false;
		private volatile int pendingX = -1, pendingY = -1;
		private volatile int pendingW = -1, pendingH = -1;
		private long window;

		void requestStop() { stopRequested = true; }
		boolean isWindowCreated() { return windowCreated; }

		void requestReposition(int x, int y, int w, int h) {
			pendingX = x; pendingY = y;
			pendingW = w; pendingH = h;
		}

		@Override
		public void run() {
			// Init GLFW
			if (!glfwInit()) throw new RuntimeException("Cannot init GLFW");

			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE,   GLFW_TRUE);
			glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE); // resize řídíme sami
			glfwWindowHint(GLFW_DECORATED, GLFW_FALSE); // bez titulku/rámečku
			glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);

			Point loc = glPlaceholder.getLocationOnScreen();
			int   w   = glPlaceholder.getWidth();
			int   h   = glPlaceholder.getHeight();

			window = glfwCreateWindow(
					Math.max(w, 100), Math.max(h, 100),
					"GL", 0L, 0L);
			if (window == 0L) throw new RuntimeException("Cannot create GLFW window");

			glfwSetWindowPos(window, loc.x, loc.y);

			// Callbacky
			glfwSetKeyCallback        (window, renderer.getGlfwKeyCallback());
			glfwSetWindowSizeCallback (window, renderer.getGlfwWindowSizeCallback());
			glfwSetMouseButtonCallback(window, renderer.getGlfwMouseButtonCallback());
			glfwSetCursorPosCallback  (window, renderer.getGlfwCursorPosCallback());
			glfwSetScrollCallback     (window, renderer.getGlfwScrollCallback());

			// Fokus při kliknutí na placeholder
			glPlaceholder.addMouseListener(new MouseAdapter() {
				@Override public void mousePressed(MouseEvent e) {
					glfwFocusWindow(window);
				}
			});

			glfwMakeContextCurrent(window);
			glfwSwapInterval(1);
			GL.createCapabilities();

			renderer.init();
			windowCreated = true;

			// Ihned synchronizuje pozici
			syncGLWindow();

			while (!stopRequested && !glfwWindowShouldClose(window)) {
				// Aplikuj čekající přemístění/resize
				if (pendingX >= 0) {
					glfwSetWindowPos (window, pendingX, pendingY);
					glfwSetWindowSize(window, Math.max(pendingW, 100), Math.max(pendingH, 100));
					renderer.setWidth(Math.max(pendingW, 100));
					renderer.setHeight(Math.max(pendingH, 100));
					pendingX = -1;
				}

				renderer.display();
				glfwSwapBuffers(window);
				glfwPollEvents();
			}

			renderer.dispose();
			glfwDestroyWindow(window);
			glfwTerminate();
			System.exit(0);
		}
	}
}