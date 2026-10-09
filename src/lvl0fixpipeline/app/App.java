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
 * HLAVNÍ APLIKACE — Swing okno s embeddovaným OpenGL renderem
 *
 * Struktura:
 * - Vlevo: OpenGL canvas (GLFW okno vložené do AWT)
 * - Vpravo: Swing ovládací panel pro parametry
 *
 * Technologie:
 * - GLFW okno běží v samostatném vlákně
 * - Synchronizace mezi Swing UI a GL vláknem přes volatile pole
 * - GLFW okno je bez dekorací a přesně se překrývá se Swing komponentou
 * - Na Windows je GLFW okno "vlastněné" Swing oknem → drží se nad ním,
 *   ale ne nad ostatními aplikacemi (jinde se použije GLFW_FLOATING)
 */
public class App extends JFrame {
	// postranní panel
	private static final int PANEL_WIDTH  = 350;
	// GL okno
	private static final int GL_WIDTH     = 900;
	private static final int GL_HEIGHT    = 725;
	// titulek hlavního okna (podle něj se na Windows dohledá jeho HWND)
	private static final String TITLE     = "Vizualizace funkcí f(x,y)  —  LWJGL / OpenGL";

	private final FunctionRenderer renderer;
	private GLThread     glThread;
	private final JPanel       glPlaceholder; // Swing panel držící místo pro GL okno

	/**
	 * Vstupní bod aplikace — spustí App v EDT
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	/**
	 * Konstruktor — vytvoří a inicializuje UI
	 */
	public App() {
		super(TITLE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		getContentPane().setBackground(new Color(20, 20, 28));

		// Vytvoří renderer (bude spuštěn později z GL vlákna)
		renderer = new FunctionRenderer();

		// Placeholder panel — do něho se překryje GLFW okno
		glPlaceholder = new JPanel();
		glPlaceholder.setPreferredSize(new Dimension(GL_WIDTH, GL_HEIGHT));
		glPlaceholder.setBackground(new Color(12, 12, 18));
		glPlaceholder.setMinimumSize(new Dimension(400, 300));

		// Ovládací panel vpravo
		ControlPanel controlPanel = new ControlPanel(renderer);
		controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, GL_HEIGHT));

		// SplitPane: vlevo GL, vpravo ovládání
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				glPlaceholder, controlPanel);
		split.setDividerLocation(GL_WIDTH);
		split.setDividerSize(4);
		split.setBackground(new Color(40, 40, 55));
		split.setBorder(null);
		split.setResizeWeight(1.0); // GL okno dostane přebytečný prostor při resizu

		add(split, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		// Listener na zobrazení okna — spustí GL vlákno až po zjištění souřadnic
		addComponentListener(new ComponentAdapter() {
			boolean started = false;
			@Override public void componentShown(ComponentEvent e)  { startGL(); }
			@Override public void componentMoved(ComponentEvent e)  { syncGLWindow(); }
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }

			/** Startuje GL vlákno jen jednou */
			private void startGL() {
				if (started) return;
				started = true;
				glThread = new GLThread();
				glThread.setDaemon(true);
				glThread.start();
			}
		});

		// Listener na changes v GL placeholderu — synchronizuje pozici GL okna
		glPlaceholder.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e){ syncGLWindow(); }
		});

		// Listener na zavření okna a minimalizaci
		addWindowListener(new WindowAdapter() {
			/** Uživatel zavírá okno → zastaví GL vlákno */
			@Override public void windowClosing(WindowEvent e) {
				if (glThread != null) glThread.requestStop();
			}
		});
	}

	/**
	 * Přesune a změní velikost GLFW okna, aby se přesně překrýval s glPlaceholder
	 * (volá se z Swing EDT, požadavek se předá GL vláknu)
	 */
	void syncGLWindow() {
		if (glThread == null || !glThread.isWindowCreated()) return;
		Rectangle r = placeholderDeviceBounds();
		// Předá požadavek GL vláknu (GLFW API není thread-safe)
		glThread.requestReposition(r.x, r.y, r.width, r.height);
	}

	/**
	 * Vrátí pozici a velikost glPlaceholder ve fyzických pixelech obrazovky.
	 *
	 * Swing pracuje v logických souřadnicích (při škálování Windows např. 125 %
	 * jsou menší), GLFW ve fyzických pixelech → bez přepočtu by GL okno
	 * skončilo posunuté doleva nahoru a bylo by menší.
	 */
	private Rectangle placeholderDeviceBounds() {
		Point loc = glPlaceholder.getLocationOnScreen();
		GraphicsConfiguration gc = glPlaceholder.getGraphicsConfiguration();
		if (gc == null) gc = getGraphicsConfiguration();
		AffineTransform t = gc.getDefaultTransform();
		Rectangle screen = gc.getBounds(); // počátek monitoru je už ve fyzických pixelech
		double sx = t.getScaleX(), sy = t.getScaleY();
		int x = (int) Math.round(screen.x + (loc.x - screen.x) * sx);
		int y = (int) Math.round(screen.y + (loc.y - screen.y) * sy);
		int w = (int) Math.round(glPlaceholder.getWidth()  * sx);
		int h = (int) Math.round(glPlaceholder.getHeight() * sy);
		return new Rectangle(x, y, w, h);
	}

	/**
	 * Windows: nastaví Swing okno jako vlastníka (owner) GLFW okna.
	 * Vlastněné okno je vždy nad vlastníkem, při minimalizaci se skryje s ním
	 * a při přepnutí do jiné aplikace zůstane pod ní.
	 *
	 * @return true, pokud se vlastníka podařilo nastavit
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
	 * VNITŘNÍ VLÁKNO — běží GLFW event loop
	 *
	 * Komunikace se Swing EDTem je synchronizovaná přes volatile pole
	 */
	class GLThread extends Thread {
		private volatile boolean stopRequested  = false;
		private volatile boolean windowCreated  = false;
		private volatile boolean pending = false; // čeká požadavek na přemístění?
		private volatile int  pendingX, pendingY; // souřadnice mohou být záporné (monitor vlevo)
		private volatile int  pendingW, pendingH;
		private long window;

		// Setter pro stopRequested
		void requestStop() { stopRequested = true; }

		// Getter pro windowCreated
		boolean isWindowCreated() { return windowCreated; }

		// Nastaví pending repositioning request
		void requestReposition(int x, int y, int w, int h) {
			pendingX = x; pendingY = y;
			pendingW = w; pendingH = h;
			pending = true;
		}

		@Override
		public void run() {
			// Inicializace GLFW
			if (!glfwInit()) throw new RuntimeException("Cannot init GLFW");

			// Nastavení hints pro nové okno
			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE,       GLFW_FALSE); // zobrazí se až po umístění
			glfwWindowHint(GLFW_RESIZABLE,     GLFW_FALSE); // velikost řídíme sami ze Swingu
			glfwWindowHint(GLFW_DECORATED,     GLFW_FALSE); // bez titulku a rámu
			glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE); // nezískává focus automaticky

			// Zjistí počáteční pozici a velikost z placeholderu (fyzické pixely)
			Rectangle r = placeholderDeviceBounds();

			// Vytvoří GLFW okno
			window = glfwCreateWindow(
					Math.max(r.width, 100), Math.max(r.height, 100),
					"GL", 0L, 0L);
			if (window == 0L) throw new RuntimeException("Cannot create GLFW window");

			// Udrží GL okno nad Swing oknem: na Windows přes vlastníka,
			// jinde (nebo když se vlastníka nepodaří najít) jako "floating"
			boolean owned = Platform.get() == Platform.WINDOWS && setWin32Owner(window);
			if (!owned) glfwSetWindowAttrib(window, GLFW_FLOATING, GLFW_TRUE);

			// Umístí okno na správné místo a teprve pak ho zobrazí
			glfwSetWindowPos(window, r.x, r.y);
			glfwShowWindow(window);

			// Callbacky
			glfwSetKeyCallback        (window, renderer.getGlfwKeyCallback());
			glfwSetWindowSizeCallback (window, renderer.getGlfwWindowSizeCallback());
			glfwSetMouseButtonCallback(window, renderer.getGlfwMouseButtonCallback());
			glfwSetCursorPosCallback  (window, renderer.getGlfwCursorPosCallback());
			glfwSetScrollCallback     (window, renderer.getGlfwScrollCallback());

			// Listener na klik v placeholderu — dostane focus GL okno
			glPlaceholder.addMouseListener(new MouseAdapter() {
				@Override public void mousePressed(MouseEvent e) {
					glfwFocusWindow(window);
				}
			});

			// Inicializace OpenGL
			glfwMakeContextCurrent(window);
			glfwSwapInterval(1); // vsync ON
			GL.createCapabilities(); // načte OpenGL function pointery

			renderer.init(); // inicializuje renderer (nastavení osvětlení, kamery atd.)
			windowCreated = true; // signál pro Swing, že okno je připraveno
			syncGLWindow(); // počáteční synchronizace pozice

			// Hlavní loop
			while (!stopRequested && !glfwWindowShouldClose(window)) {
				// Zpracuje čekající přemístění/resize ze Swingu
				if (pending) {
					pending = false;
					glfwSetWindowPos (window, pendingX, pendingY);
					glfwSetWindowSize(window, Math.max(pendingW, 100), Math.max(pendingH, 100));
					// Informuje renderer o nové velikosti
					renderer.setWidth(Math.max(pendingW, 100));
					renderer.setHeight(Math.max(pendingH, 100));
				}

				// Hlavní renderovací volání
				renderer.display();
				glfwSwapBuffers(window);
				glfwPollEvents(); // zpracuje keyboard, mouse atd.
			}

			// Ukončení a úklid
			renderer.dispose();
			glfwDestroyWindow(window);
			glfwTerminate();
			System.exit(0);
		}
	}
}