package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.MathParser;
import lvl0fixpipeline.app.parser.ParseException;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * CONTROL PANEL — Swing UI for the visualizer
 *
 * Components:
 * - Text field for the expression f(x,y,t)
 * - Dropdown with preset functions
 * - Fields for the X/Y ranges
 * - Spinner for the number of samples
 * - Checkbox and slider for the animation
 * - Status line
 * - "About" button
 *
 * All changes are applied to the FunctionRenderer via applySettings()
 */
public class ControlPanel extends JPanel {
    private final FunctionRenderer renderer; // renderer reference, used to pass the settings

    // UI components
    private JTextField tfExpr;
    private JTextField tfXMin, tfXMax;
    private JTextField tfYMin, tfYMax;
    private JSpinner   spinSteps;
    private JLabel     lblStatus;
    private JCheckBox  cbAnimate;
    private JSlider    sliderAnimSpeed;

    // Preset functions
    private static final String[][] PRESETS = {
            {"Sinc",              "sin(sqrt(x*x+y*y)) / (sqrt(x*x+y*y)+0.01)"},
            {"Saddle",            "x*x - y*y"},
            {"sin·cos",           "sin(x) * cos(y)"},
            {"Cone",              "sqrt(x*x+y*y)"},
            {"Ripple",            "sin(x*x+y*y)"},
            {"Mexican hat",       "(1-(x*x+y*y)/4)*exp(-(x*x+y*y)/4)"},
            {"Wave+time",         "sin(x+t)*cos(y+t)"},
            {"Manta",             "sin(x)+cos(y)+sin(x*y)"},
            {"Spiral",            "sin(atan2(y,x)*3+sqrt(x*x+y*y)-t*2)"},
            {"Star",              "sqrt(x*x+y*y)+3*cos(sqrt(x*x+y*y))-3.9"},
    };

    public ControlPanel(FunctionRenderer renderer) {
        super();
        this.renderer = renderer;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // ScrollPane with the inner content + bottom bar with a button
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    /**
     * Creates the bottom bar with the "About" button and the axis color legend
     */
    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        bottom.setPreferredSize(new Dimension(0, 36));

        // Axis legend — left
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.X_AXIS));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JLabel lx = new JLabel("■ X = X axis");
        lx.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lx.setForeground(new Color(255, 64, 64));

        JLabel ly = new JLabel("■ Y = Y axis");
        ly.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ly.setForeground(new Color(64, 255, 64));

        JLabel lz = new JLabel("■ Z = Z axis");
        lz.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lz.setForeground(new Color(90, 140, 255));

        legend.add(lx);
        legend.add(Box.createRigidArea(new Dimension(8, 0)));
        legend.add(ly);
        legend.add(Box.createRigidArea(new Dimension(8, 0)));
        legend.add(lz);

        // About button — right
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JButton btnInfo = new JButton("About");
        btnInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnInfo.setFocusPainted(false);
        btnInfo.setBackground(INPUT);
        btnInfo.setForeground(FG);
        btnInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInfo.addActionListener(e -> showInfoDialog());
        right.add(btnInfo);

        bottom.add(legend, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);

        return bottom;
    }

    /**
     * Shows the About dialog
     */
    private void showInfoDialog() {
        JOptionPane pane = new JOptionPane(
                """
                Author: Lenka Šedivá
                Project: 3D visualization of math functions
                Year: 2026

                The application renders user-defined
                functions f(x, y, t) using OpenGL (LWJGL)
                """,
                JOptionPane.INFORMATION_MESSAGE
        );
        JDialog dialog = pane.createDialog(SwingUtilities.getWindowAncestor(this), "About");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    /**
     * Creates the inner panel with all the controls
     */
    private JPanel buildInner() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Title
        JLabel title = new JLabel("Function f(x, y, t)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(130, 180, 255));
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(title);

        // Text field for the expression
        tfExpr = monoField(renderer.getExpression(), 22);
        root.add(tfExpr);
        root.add(vgap(4));

        // Presets
        root.add(sectionLabel("Presets"));
        String[] names = new String[PRESETS.length + 1];
        names[0] = "— select —";
        for (int i = 0; i < PRESETS.length; i++) names[i+1] = PRESETS[i][0];
        JComboBox<String> cbPresets = new JComboBox<>(names);
        styleCombo(cbPresets);

        // When the user picks a preset → put its expression into the text field
        cbPresets.addActionListener(e -> {
            int s = cbPresets.getSelectedIndex();
            if (s > 0) tfExpr.setText(PRESETS[s-1][1]);
        });
        root.add(cbPresets);
        root.add(vgap(10));

        // Axis ranges
        root.add(sectionLabel("X / Y axis range"));
        JPanel rangeGrid = new JPanel(new GridLayout(3, 2, 6, 5));
        rangeGrid.setOpaque(false);
        rangeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        rangeGrid.setAlignmentX(LEFT_ALIGNMENT);
        rangeGrid.add(smallLabel("X min")); rangeGrid.add(smallLabel("X max"));
        tfXMin = rangeField(fmt(renderer.getXMin()));
        tfXMax = rangeField(fmt(renderer.getXMax()));
        rangeGrid.add(tfXMin); rangeGrid.add(tfXMax);
        rangeGrid.add(smallLabel("Y min")); rangeGrid.add(smallLabel("Y max"));
        root.add(rangeGrid);

        // Second grid row for Y
        JPanel rangeGrid2 = new JPanel(new GridLayout(1, 2, 6, 5));
        rangeGrid2.setOpaque(false);
        rangeGrid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        rangeGrid2.setAlignmentX(LEFT_ALIGNMENT);
        tfYMin = rangeField(fmt(renderer.getYMin()));
        tfYMax = rangeField(fmt(renderer.getYMax()));
        rangeGrid2.add(tfYMin); rangeGrid2.add(tfYMax);
        root.add(rangeGrid2);
        root.add(vgap(10));

        // Sampling
        root.add(sectionLabel("Sampling (steps)"));

        JPanel stepsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        stepsRow.setOpaque(false);
        stepsRow.setAlignmentX(LEFT_ALIGNMENT);

        // Spinner with the number of steps
        SpinnerNumberModel sm = new SpinnerNumberModel(renderer.getSteps(), 10, 300, 10);
        spinSteps = new JSpinner(sm);
        spinSteps.setPreferredSize(new Dimension(80, 28));
        styleSpinner(spinSteps);

        // "Apply changes" button
        JButton btnApply = accentButtonSmall("Apply changes");
        btnApply.addActionListener(e -> applySettings());

        // Put the spinner and the button on one row
        stepsRow.add(spinSteps);
        stepsRow.add(Box.createRigidArea(new Dimension(8, 0))); // gap between
        stepsRow.add(btnApply);
        root.add(stepsRow);
        root.add(vgap(12));

        // Animation
        root.add(sectionLabel("Animation  (variable t)"));

        JPanel animRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        animRow.setOpaque(false);
        animRow.setAlignmentX(LEFT_ALIGNMENT);

        // "Enable" checkbox
        cbAnimate = new JCheckBox("Enable");
        cbAnimate.setForeground(FG);
        cbAnimate.setBackground(BG);
        cbAnimate.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        animRow.add(cbAnimate);
        animRow.add(Box.createRigidArea(new Dimension(8, 0))); // gap between
        animRow.add(smallLabel("Speed:"));

        // Speed slider (1-20)
        sliderAnimSpeed = new JSlider(1, 20, 5);
        sliderAnimSpeed.setPreferredSize(new Dimension(90, 25));
        sliderAnimSpeed.setOpaque(false);

        animRow.add(sliderAnimSpeed);
        root.add(animRow);

        // Checkbox listener — checks that 't' is present and turns the animation on/off
        cbAnimate.addActionListener(e -> {
            if (cbAnimate.isSelected()) {
                // Check that the parameter 't' is present
                String expr = tfExpr.getText().trim();
                if (expr.isEmpty()) {
                    status("Error: enter an expression first", true);
                    cbAnimate.setSelected(false);
                    return;
                }

                try {
                    MathParser parser = new MathParser(expr);
                    boolean hasTimeVariable = parser.containsTimeVariable();

                    if (!hasTimeVariable) {
                        status("Error: the expression does not contain 't'", true);
                        cbAnimate.setSelected(false);
                        return;
                    }

                    // OK — the expression contains t
                    status("Animation on — " + expr, false);
                } catch (ParseException pe) {
                    status("Error: " + pe.getMessage(), true);
                    cbAnimate.setSelected(false);
                    return;
                }
            } else {
                // Animation off
                status("Animation off", false);
            }

            // Finally apply the settings
            renderer.setAnimating(cbAnimate.isSelected(), sliderAnimSpeed.getValue() * 0.2f);
        });
        root.add(vgap(5));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 80));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        root.add(sep);
        root.add(vgap(5));

        // Keyboard shortcuts
        // Section 1 — Camera controls
        root.add(sectionLabel("Camera controls"));
        for (String hint : new String[]{
                "Left mouse — rotate camera",
                "Right mouse — pan camera",
                "Mouse wheel — zoom",
                "W/S/A/D — move camera",
                "Q/E — turn camera",
                "R — reset camera",
                "XYZ — views from the axes",
                "P — toggle ortho/perspective"
        }) {
            root.add(hintLabel(hint));
        }

        // Section 2 — Display
        root.add(sectionLabel("Display"));
        for (String hint : new String[]{
                "M — wireframe",
                "N — surface normals",
                "O — coordinate axes",
                "K — ground grid"
        }) {
            root.add(hintLabel(hint));
        }

        root.add(vgap(5));

        // Status
        lblStatus = new JLabel("Ready.");
        lblStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(100, 200, 130));
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        root.add(lblStatus);

        return root;
    }

    /**
     * APPLIES ALL CHANGES — validates the input and sends it to the renderer
     */
    private void applySettings() {
        try {
            // Check the expression
            String expr = tfExpr.getText().trim();
            if (expr.isEmpty()) { status("Error: empty expression", true); return; }

            // Parse the numeric values
            float x0 = Float.parseFloat(tfXMin.getText().trim());
            float x1 = Float.parseFloat(tfXMax.getText().trim());
            float y0 = Float.parseFloat(tfYMin.getText().trim());
            float y1 = Float.parseFloat(tfYMax.getText().trim());
            int   s  = (Integer) spinSteps.getValue();

            // Validate the ranges
            if (x0 >= x1) { status("X min must be < X max", true); return; }
            if (y0 >= y1) { status("Y min must be < Y max", true); return; }

            // Validate the expression
            MathParser parser;
            try {
                parser = new MathParser(expr);
                parser.evaluate(0, 0, 0);
            } catch (ParseException pe) {
                status("Error: " + pe.getMessage(), true); return;
            }

            // Everything is OK → send it to the renderer
            renderer.applySettings(expr, x0, x1, y0, y1, s);
            renderer.setAnimating(cbAnimate.isSelected(), sliderAnimSpeed.getValue() * 0.2f);

            status("OK: " + expr, false);

        } catch (NumberFormatException e) {
            status("Invalid number", true);
        }

        renderer.resetCamera(true);
    }

    // Sets the text and color of the status label
    private void status(String msg, boolean err) {
        lblStatus.setText(msg);
        lblStatus.setForeground(err ? new Color(255, 100, 100) : new Color(100, 200, 130));
    }

    // Helper — formats a float (without trailing zeros)
    private static String fmt(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    // Colors
    private static final Color BG     = new Color(22, 22, 30);
    private static final Color FG     = new Color(200, 200, 220);
    private static final Color INPUT  = new Color(38, 38, 52);
    private static final Color ACCENT = new Color(80, 130, 255);
    private static final Color BORDER = new Color(60, 60, 85);

    // HELPER COMPONENTS
    // Section heading — blue bold text
    private JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(new Color(110, 160, 255));
        l.setBorder(new EmptyBorder(2, 0, 3, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    
    // Small label — for captions
    private JLabel smallLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(FG);
        return l;
    }
    
    // Hint label — monospace, white
    private JLabel hintLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(Color.white);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    
    // Monospace text field — for entering expressions
    private JTextField monoField(String text, int cols) {
        JTextField tf = new JTextField(text, cols);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tf.setBackground(INPUT);
        tf.setForeground(new Color(160, 230, 160));
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        return tf;
    }
    
    // Text field for numeric ranges
    private JTextField rangeField(String text) {
        JTextField tf = new JTextField(text, 5);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tf.setBackground(INPUT);
        tf.setForeground(FG);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        return tf;
    }
    
    // Large accent button with a hover effect
    private JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Hover effect — changes the color
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(100, 150, 255)); }
            public void mouseExited (MouseEvent e) { b.setBackground(ACCENT); }
        });
        return b;
    }
    
    // Styles a ComboBox
    private void styleCombo(JComboBox<?> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(INPUT);
        cb.setForeground(FG);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cb.setAlignmentX(LEFT_ALIGNMENT);
    }
    
    // Styles a Spinner
    private void styleSpinner(JSpinner sp) {
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(INPUT);
            de.getTextField().setForeground(FG);
            de.getTextField().setFont(new Font("Monospaced", Font.PLAIN, 12));
        }
    }
    
    // Creates a vertical gap between components
    private Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    // Small accent button — accentButton with a smaller size
    private JButton accentButtonSmall(String text) {
        JButton b = accentButton(text);
        b.setMaximumSize(new Dimension(240, 32));
        b.setPreferredSize(new Dimension(240, 32));
        return b;
    }
}