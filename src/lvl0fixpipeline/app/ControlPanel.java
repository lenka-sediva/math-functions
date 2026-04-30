package lvl0fixpipeline.app;

import lvl0fixpipeline.app.parser.MathParser;
import lvl0fixpipeline.app.parser.ParseException;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * OVLÁDACÍ PANEL — Swing UI pro vizualizér
 * 
 * Komponenty:
 * - Textové pole pro zadání výrazu f(x,y,t)
 * - Dropdown se předvoleným funkcemi
 * - Pole pro X/Y rozsahy
 * - Spinner pro počet vzorkování
 * - Checkbox a slider pro animaci
 * - Status řádka
 * - Tlačítko "O projektu"
 * 
 * Veškeré změny se aplikují na FunctionRenderer přes applySettings()
 */
public class ControlPanel extends JPanel {
    private final FunctionRenderer renderer; // reference na renderer, pro předávání nastavení

    // UI komponenty
    private JTextField tfExpr;
    private JTextField tfXMin, tfXMax;
    private JTextField tfYMin, tfYMax;
    private JSpinner   spinSteps;
    private JLabel     lblStatus;
    private JCheckBox  cbAnimate;
    private JSlider    sliderAnimSpeed;

    // Přednastavené funkce
    private static final String[][] PRESETS = {
            {"Sinc",              "sin(sqrt(x*x+y*y)) / (sqrt(x*x+y*y)+0.01)"},
            {"Sedlo",             "x*x - y*y"},
            {"sin·cos",           "sin(x) * cos(y)"},
            {"Kužel",             "sqrt(x*x+y*y)"},
            {"Vlnění",            "sin(x*x+y*y)"},
            {"Mexický klobouk",   "(1-(x*x+y*y)/4)*exp(-(x*x+y*y)/4)"},
            {"Vlna+čas",          "sin(x+t)*cos(y+t)"},
            {"Manta",             "sin(x)+cos(y)+sin(x*y)"},
            {"Spirála",           "sin(atan2(y,x)*3+sqrt(x*x+y*y)-t*2)"},
            {"Hvězda",            "sqrt(x*x+y*y)+3*cos(sqrt(x*x+y*y))-3.9"},
    };

    public ControlPanel(FunctionRenderer renderer) {
        super();
        this.renderer = renderer;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // ScrollPane s vnitřním obsahem + spodní bar s tlačítkem
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BG);
        add(scroll, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    /**
     * Vytvoří spodní lištu s tlačítkem "O projektu" a legendou barev pro osy
     */
    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        bottom.setPreferredSize(new Dimension(0, 36));

        // Legenda os — vlevo
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.X_AXIS));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JLabel lx = new JLabel("■ X = osa X");
        lx.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lx.setForeground(new Color(255, 64, 64));

        JLabel ly = new JLabel("■ Y = osa Y");
        ly.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ly.setForeground(new Color(64, 255, 64));

        JLabel lz = new JLabel("■ Z = osa Z");
        lz.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lz.setForeground(new Color(90, 140, 255));

        legend.add(lx);
        legend.add(Box.createRigidArea(new Dimension(8, 0)));
        legend.add(ly);
        legend.add(Box.createRigidArea(new Dimension(8, 0)));
        legend.add(lz);

        // Tlačítko O projektu — vpravo
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        JButton btnInfo = new JButton("O projektu");
        btnInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnInfo.setFocusPainted(false);
        btnInfo.setBackground(INPUT);
        btnInfo.setForeground(FG);
        btnInfo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btnInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInfo.addActionListener(_ -> showInfoDialog());
        right.add(btnInfo);

        bottom.add(legend, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);

        return bottom;
    }

    /**
     * Zobrazí informační dialog o projektu
     */
    private void showInfoDialog() {
        JOptionPane pane = new JOptionPane(
                """
                Autorka: Lenka Šedivá
                Projekt: 3D vizualizace matematických funkcí
                Rok: 2026
                
                Aplikace umožňuje vykreslovat uživatelem zadané
                funkce f(x, y, t) pomocí OpenGL (LWJGL)
                """,
                JOptionPane.INFORMATION_MESSAGE
        );
        JDialog dialog = pane.createDialog(SwingUtilities.getWindowAncestor(this), "O projektu");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    /**
     * Vytvoří vnitřek panelu se všemi ovládacími prvky
     */
    private JPanel buildInner() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Nadpis
        JLabel title = new JLabel("Předpis funkce f(x, y, t)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(130, 180, 255));
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(title);

        // Textové pole pro výraz
        tfExpr = monoField(renderer.getExpression(), 22);
        root.add(tfExpr);
        root.add(vgap(4));

        // Předvolby
        root.add(sectionLabel("Předvolby"));
        String[] names = new String[PRESETS.length + 1];
        names[0] = "— vyberte —";
        for (int i = 0; i < PRESETS.length; i++) names[i+1] = PRESETS[i][0];
        JComboBox<String> cbPresets = new JComboBox<>(names);
        styleCombo(cbPresets);

        // Když uživatel vybere z dropdown → dá to výraz do textového pole
        cbPresets.addActionListener(_ -> {
            int s = cbPresets.getSelectedIndex();
            if (s > 0) tfExpr.setText(PRESETS[s-1][1]);
        });
        root.add(cbPresets);
        root.add(vgap(10));

        // Rozsahy os
        root.add(sectionLabel("Rozsah os X / Y"));
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

        // Druhý řádek gridů pro Y
        JPanel rangeGrid2 = new JPanel(new GridLayout(1, 2, 6, 5));
        rangeGrid2.setOpaque(false);
        rangeGrid2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        rangeGrid2.setAlignmentX(LEFT_ALIGNMENT);
        tfYMin = rangeField(fmt(renderer.getYMin()));
        tfYMax = rangeField(fmt(renderer.getYMax()));
        rangeGrid2.add(tfYMin); rangeGrid2.add(tfYMax);
        root.add(rangeGrid2);
        root.add(vgap(10));

        // Vzorkování
        root.add(sectionLabel("Vzorkování (kroků)"));

        JPanel stepsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        stepsRow.setOpaque(false);
        stepsRow.setAlignmentX(LEFT_ALIGNMENT);

        // Spinner s počtem kroků
        SpinnerNumberModel sm = new SpinnerNumberModel(renderer.getSteps(), 10, 300, 10);
        spinSteps = new JSpinner(sm);
        spinSteps.setPreferredSize(new Dimension(80, 28));
        styleSpinner(spinSteps);

        // Tlačítko "Aplikovat změny"
        JButton btnApply = accentButtonSmall("Aplikovat změny");
        btnApply.addActionListener(_ -> applySettings());

        // Přidá spinner a tlačítko do jednoho řádku
        stepsRow.add(spinSteps);
        stepsRow.add(Box.createRigidArea(new Dimension(8, 0))); // mezera mezi
        stepsRow.add(btnApply);
        root.add(stepsRow);
        root.add(vgap(12));

        // Animace
        root.add(sectionLabel("Animace  (proměnná t)"));

        JPanel animRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        animRow.setOpaque(false);
        animRow.setAlignmentX(LEFT_ALIGNMENT);

        // Checkbox "Zapnout"
        cbAnimate = new JCheckBox("Zapnout");
        cbAnimate.setForeground(FG);
        cbAnimate.setBackground(BG);
        cbAnimate.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        animRow.add(cbAnimate);
        animRow.add(Box.createRigidArea(new Dimension(8, 0))); // mezera mezi
        animRow.add(smallLabel("Rychlost:"));

        // Slider pro rychlost (1-20)
        sliderAnimSpeed = new JSlider(1, 20, 5);
        sliderAnimSpeed.setPreferredSize(new Dimension(90, 25));
        sliderAnimSpeed.setOpaque(false);

        animRow.add(sliderAnimSpeed);
        root.add(animRow);

        // Listener na checkbox — kontroluje přítomnost 't' a zapne/vypne animaci
        cbAnimate.addActionListener(_ -> {
            if (cbAnimate.isSelected()) {
                // Kontrola přítomnosti parametru 't'
                String expr = tfExpr.getText().trim();
                if (expr.isEmpty()) {
                    status("Chyba: Nejdřív zadejte výraz", true);
                    cbAnimate.setSelected(false);
                    return;
                }

                try {
                    MathParser parser = new MathParser(expr);
                    boolean hasTimeVariable = parser.containsTimeVariable();

                    if (!hasTimeVariable) {
                        status("Chyba: Parametr 't' není v předpisu", true);
                        cbAnimate.setSelected(false);
                        return;
                    }

                    // OK — je t v předpisu
                    status("Animace zapnutá — " + expr, false);
                } catch (ParseException pe) {
                    status("Chyba: " + pe.getMessage(), true);
                    cbAnimate.setSelected(false);
                    return;
                }
            } else {
                // Animace vypnutá
                status("Animace vypnutá", false);
            }

            // Nakonec aplikuj nastavení
            renderer.setAnimating(cbAnimate.isSelected(), sliderAnimSpeed.getValue() * 0.2f);
        });
        root.add(vgap(5));

        // Oddělovač
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 60, 80));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        root.add(sep);
        root.add(vgap(5));

        // Klávesové zkratky
        // Sekce 1 — Ovládání kamery
        root.add(sectionLabel("Ovládání kamery"));
        for (String hint : new String[]{
                "Levá myš — rotace kamery",
                "Pravá myš — posun kamery",
                "Kolečko — zoom",
                "W/S/A/D — pohyb kamery",
                "Q/E — rozhlížení kamery",
                "R — reset kamery",
                "XYZ — pohledy z os",
                "P — přepnutí pohledu ortho/perspektiva"
        }) {
            root.add(hintLabel(hint));
        }

        // Sekce 2 — Zobrazení
        root.add(sectionLabel("Zobrazení"));
        for (String hint : new String[]{
                "M — drátový model",
                "N — normály povrchu",
                "O — souřadnicové osy",
                "K — podkladová mřížka"
        }) {
            root.add(hintLabel(hint));
        }

        root.add(vgap(5));

        // Status
        lblStatus = new JLabel("Připraveno.");
        lblStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(100, 200, 130));
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        root.add(lblStatus);

        return root;
    }

    /**
     * APLIKUJE VŠECHNY ZMĚNY — validuje input a pošle do rendereru
     */
    private void applySettings() {
        try {
            // Ověří výraz
            String expr = tfExpr.getText().trim();
            if (expr.isEmpty()) { status("Chyba: prázdný výraz", true); return; }

            // Parsuje číselné hodnoty
            float x0 = Float.parseFloat(tfXMin.getText().trim());
            float x1 = Float.parseFloat(tfXMax.getText().trim());
            float y0 = Float.parseFloat(tfYMin.getText().trim());
            float y1 = Float.parseFloat(tfYMax.getText().trim());
            int   s  = (Integer) spinSteps.getValue();

            // Validace rozsahů
            if (x0 >= x1) { status("X min musí být < X max", true); return; }
            if (y0 >= y1) { status("Y min musí být < Y max", true); return; }

            // Ověření výrazu
            MathParser parser;
            try {
                parser = new MathParser(expr);
                parser.evaluate(0, 0, 0);
            } catch (ParseException pe) {
                status("Chyba: " + pe.getMessage(), true); return;
            }

            // Vše je OK → pošle do rendereru
            renderer.applySettings(expr, x0, x1, y0, y1, s);
            renderer.setAnimating(cbAnimate.isSelected(), sliderAnimSpeed.getValue() * 0.2f);

            status("OK: " + expr, false);

        } catch (NumberFormatException e) {
            status("Neplatná číselná hodnota", true);
        }

        renderer.resetCamera(true);
    }

    // Nastaví text a barvu status labelu
    private void status(String msg, boolean err) {
        lblStatus.setText(msg);
        lblStatus.setForeground(err ? new Color(255, 100, 100) : new Color(100, 200, 130));
    }

    // Pomocná funkce — formátuje float (bez zbytečných nul za čárkou)
    private static String fmt(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.valueOf(v);
    }

    // Barvy
    private static final Color BG     = new Color(22, 22, 30);
    private static final Color FG     = new Color(200, 200, 220);
    private static final Color INPUT  = new Color(38, 38, 52);
    private static final Color ACCENT = new Color(80, 130, 255);
    private static final Color BORDER = new Color(60, 60, 85);

    // POMOCNÉ KOMPONENTY
    // Nadpis sekce — modrý, tučný text
    private JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(new Color(110, 160, 255));
        l.setBorder(new EmptyBorder(2, 0, 3, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    
    // Malý label — pro popisky
    private JLabel smallLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(FG);
        return l;
    }
    
    // Hint label — monospace, bílý
    private JLabel hintLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(Color.white);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    
    // Textové pole s monospace fontem — pro zadávání výrazů
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
    
    // Textové pole pro numerické rozsahy
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
    
    // Velké accent tlačítko s hover efektem
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

        // Hover efekt — změní barvu
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(100, 150, 255)); }
            public void mouseExited (MouseEvent e) { b.setBackground(ACCENT); }
        });
        return b;
    }
    
    // Formátuje ComboBox
    private void styleCombo(JComboBox<?> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cb.setBackground(INPUT);
        cb.setForeground(FG);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        cb.setAlignmentX(LEFT_ALIGNMENT);
    }
    
    // Formátuje Spinner
    private void styleSpinner(JSpinner sp) {
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(INPUT);
            de.getTextField().setForeground(FG);
            de.getTextField().setFont(new Font("Monospaced", Font.PLAIN, 12));
        }
    }
    
    // Vytvoří vertikální mezeru mezi komponentami
    private Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    // Malé accent tlačítko — upravené accentButton s menší velikostí
    private JButton accentButtonSmall(String text) {
        JButton b = accentButton(text);
        b.setMaximumSize(new Dimension(240, 32));
        b.setPreferredSize(new Dimension(240, 32));
        return b;
    }
}