package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * "Charcoal &amp; Coral" design system: one place for every color, font, spacing value and shared renderer,
 * so all eight module screens look identical. No algorithm code lives here.
 */
public final class Theme {
    // ---- Palette (sampled from the Charcoal & Coral reference) -------------------------------------
    /** Light or dark appearance; switchable at runtime with {@link #setMode}. */
    public enum Mode { DARK, LIGHT }

    /**
     * Every color token with its dark and light value. Values are unique within each mode, which lets
     * {@link #setMode} recolor existing components by swapping old token values for new ones.
     */
    private enum Token {
        BG(0x1C2125, 0xF5EDE9), CHARCOAL(0x262C31, 0xFFFEFD), SURFACE_2(0x2F363C, 0xF3E7E2), INSET(0x1A1E21, 0xFBF7F5),
        ROW_ALT(0x20252A, 0xF7F0ED), LINE(0x3A424A, 0xE2D3CC), LINE_SOFT(0x30373D, 0xEDE2DD),
        CORAL(0xE26853, 0xD85A44), CORAL_BRIGHT(0xEE6C55, 0xD9573F), CORAL_LIGHT(0xF57A67, 0xC24E39),
        CORAL_DEEP(0xC9563F, 0xB04532), CORAL_TINT(0x43312F, 0xFBE1DA),
        TEXT(0xECE7E4, 0x262C31), TEXT_MUTED(0xA3A9AE, 0x5E666D), TEXT_FAINT(0x6E767D, 0x8F979D),
        ON_CORAL(0x1B2024, 0xFFFFFF), SUCCESS(0x7FCBA8, 0x2E8B63), WARNING(0xF2A65A, 0xB86A1C);

        private final Color dark;
        private final Color light;

        Token(int dark, int light) {
            this.dark = new Color(dark);
            this.light = new Color(light);
        }

        Color of(Mode mode) {
            return mode == Mode.DARK ? dark : light;
        }
    }

    private static Mode mode = Mode.DARK;
    private static final List<Runnable> LISTENERS = new java.util.ArrayList<>();

    /** Deepest background behind the cards. */
    static Color BG;
    /** Charcoal (dark mode) / white (light mode): header, sidebar and card surfaces. */
    static Color CHARCOAL;
    /** Raised surface: hover states, table headers. */
    static Color SURFACE_2;
    /** Recessed surface: tables, text areas, input fields, chart canvases. */
    static Color INSET;
    /** Alternate (zebra) table row. */
    static Color ROW_ALT;
    static Color LINE;
    static Color LINE_SOFT;
    /** Coral: the single accent (primary buttons, selection, headings). */
    static Color CORAL;
    static Color CORAL_BRIGHT;
    /** Coral used for text accents (lighter on dark, darker on light, for contrast). */
    static Color CORAL_LIGHT;
    static Color CORAL_DEEP;
    /** Coral tint over the surface, for active nav items and tabs. */
    static Color CORAL_TINT;
    static Color TEXT;
    static Color TEXT_MUTED;
    static Color TEXT_FAINT;
    /** Text drawn on top of coral fills. */
    static Color ON_CORAL;
    static Color SUCCESS;
    static Color WARNING;
    /** Label color on the light series fills of the Gantt chart (same in both modes). */
    static final Color ON_SERIES = new Color(0x1B2024);

    static {
        assign(Mode.DARK);
    }

    private static void assign(Mode m) {
        mode = m;
        BG = Token.BG.of(m);
        CHARCOAL = Token.CHARCOAL.of(m);
        SURFACE_2 = Token.SURFACE_2.of(m);
        INSET = Token.INSET.of(m);
        ROW_ALT = Token.ROW_ALT.of(m);
        LINE = Token.LINE.of(m);
        LINE_SOFT = Token.LINE_SOFT.of(m);
        CORAL = Token.CORAL.of(m);
        CORAL_BRIGHT = Token.CORAL_BRIGHT.of(m);
        CORAL_LIGHT = Token.CORAL_LIGHT.of(m);
        CORAL_DEEP = Token.CORAL_DEEP.of(m);
        CORAL_TINT = Token.CORAL_TINT.of(m);
        TEXT = Token.TEXT.of(m);
        TEXT_MUTED = Token.TEXT_MUTED.of(m);
        TEXT_FAINT = Token.TEXT_FAINT.of(m);
        ON_CORAL = Token.ON_CORAL.of(m);
        SUCCESS = Token.SUCCESS.of(m);
        WARNING = Token.WARNING.of(m);
    }

    static Mode mode() {
        return mode;
    }

    /** Registers a callback for parts that cache theme-dependent content (e.g. HTML text). */
    static void onChange(Runnable listener) {
        LISTENERS.add(listener);
    }

    /**
     * Switches between light and dark at runtime without rebuilding any screen (typed input is kept):
     * custom-painted parts read the tokens at paint time; colors stored on components are swapped token by token.
     */
    static void setMode(Mode next) {
        if (next == mode) {
            return;
        }
        java.util.Map<Integer, Color> swap = new java.util.HashMap<>();
        for (Token t : Token.values()) {
            swap.put(t.of(mode).getRGB(), t.of(next));
        }
        assign(next);
        putDefaults();
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            recolor(window, swap);
            window.repaint();
        }
        LISTENERS.forEach(Runnable::run);
    }

    private static void recolor(Component c, java.util.Map<Integer, Color> swap) {
        Color bg = swapped(c.getBackground(), swap);
        if (bg != null) {
            c.setBackground(bg);
        }
        Color fg = swapped(c.getForeground(), swap);
        if (fg != null) {
            c.setForeground(fg);
        }
        polish(c);
        if (c instanceof JTable table) {
            table.setSelectionBackground(CORAL);
            table.setSelectionForeground(ON_CORAL);
            table.setGridColor(LINE_SOFT);
        } else if (c instanceof javax.swing.text.JTextComponent text) {
            text.setCaretColor(CORAL_LIGHT);
            text.setSelectionColor(CORAL);
            text.setSelectedTextColor(ON_CORAL);
        }
        if (c instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                recolor(child, swap);
            }
        }
    }

    /** Gives titled borders (input group captions) the current text color; Nimbus does not update them itself. */
    static void polish(Component c) {
        if (c instanceof JComponent jc) {
            Border border = jc.getBorder();
            if (border instanceof javax.swing.border.CompoundBorder compound) {
                border = compound.getOutsideBorder();
            }
            if (border instanceof javax.swing.border.TitledBorder titled) {
                titled.setTitleColor(TEXT);
                titled.setTitleFont(SMALL_BOLD);
            }
        }
    }

    /** Applies {@link #polish} to a whole component tree (used once when the window is built). */
    static void polishTree(Component c) {
        polish(c);
        if (c instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                polishTree(child);
            }
        }
    }

    private static Color swapped(Color color, java.util.Map<Integer, Color> swap) {
        return color == null ? null : swap.get(color.getRGB());
    }

    /** Distinct series colors that sit well next to coral on charcoal (Gantt slices, legends). */
    static final List<Color> SERIES = List.of(
            new Color(0xE26853), new Color(0xF2A65A), new Color(0x7FCBA8), new Color(0x6FA8DC),
            new Color(0xC38FD6), new Color(0xF4D35E), new Color(0xE88AA2), new Color(0x5FC2C9),
            new Color(0xF57A67), new Color(0xA8C66C));

    // ---- Spacing & shape ------------------------------------------------------------------------
    static final int RADIUS = 14;
    static final int GAP = 12;
    static final int ROW_HEIGHT = 28;

    // ---- Responsive breakpoints (frame / panel widths in pixels) ---------------------------------
    /** Below this frame width the sidebar collapses to icon badges. */
    static final int BP_SIDEBAR = 1180;
    /** Below this module width inputs and results stack vertically instead of side by side. */
    static final int BP_STACK = 880;

    // ---- Typography -----------------------------------------------------------------------------
    static final String FAMILY = pickFamily("Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans");
    static final Font BODY = new Font(FAMILY, Font.PLAIN, 13);
    static final Font BODY_BOLD = BODY.deriveFont(Font.BOLD);
    static final Font SMALL = BODY.deriveFont(11.5f);
    static final Font SMALL_BOLD = SMALL.deriveFont(Font.BOLD);
    static final Font TITLE = new Font(FAMILY, Font.BOLD, 26);
    static final Font BRAND = new Font(FAMILY, Font.BOLD, 17);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Theme() { }

    private static String pickFamily(String... preferred) {
        try {
            List<String> installed = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String name : preferred) {
                if (installed.contains(name)) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
            // headless environments without font configuration
        }
        return Font.SANS_SERIF;
    }

    /**
     * Installs the Charcoal &amp; Coral palette in the given mode on top of Nimbus (bundled with every JDK, so no
     * dependencies). Must run before any component is created.
     */
    public static void install(Mode initial) {
        assign(initial);
        putDefaults();
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // keep the default look and feel; custom-painted parts still use the palette
        }
        putDefaults();
        UIManager.put("defaultFont", new FontUIResource(BODY));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                hairline(1, 1, 1, 1, true), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        UIManager.put("TitledBorder.border", new RoundBorder(10, new Insets(6, 6, 6, 6)));
        UIManager.put("TitledBorder.font", new FontUIResource(SMALL_BOLD));
        UIManager.put("TextField.contentMargins", new InsetsUIResource(4, 8, 4, 8));
        UIManager.put("TextArea.contentMargins", new InsetsUIResource(6, 8, 6, 8));
        // Thin, flat scrollbars everywhere.
        UIManager.put("ScrollBarUI", ThemeScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.minimumThumbSize", new Dimension(24, 24));
        UIManager.put("ScrollBar.maximumThumbSize", new Dimension(4096, 4096));
        UIManager.put("ScrollPane.border", hairline(1, 1, 1, 1, true));
        UIManager.put("Table.showGrid", Boolean.FALSE);
    }

    /** Color defaults for Nimbus and for components created later (dialogs, tooltips). */
    private static void putDefaults() {
        put("control", CHARCOAL);
        put("info", SURFACE_2);
        put("nimbusBase", mode == Mode.DARK ? SURFACE_2 : new Color(0x8A7F7A));
        put("nimbusBlueGrey", LINE);
        put("nimbusFocus", CORAL);
        put("nimbusLightBackground", INSET);
        put("nimbusSelectionBackground", CORAL);
        put("nimbusSelectedText", ON_CORAL);
        put("nimbusSelection", CORAL);
        put("nimbusOrange", CORAL);
        put("nimbusGreen", SUCCESS);
        put("nimbusRed", CORAL_DEEP);
        put("nimbusInfoBlue", new Color(0x6FA8DC));
        put("nimbusDisabledText", TEXT_FAINT);
        put("nimbusBorder", LINE);
        put("text", TEXT);
        put("textForeground", TEXT);
        put("textBackground", CORAL);
        put("textHighlight", CORAL);
        put("textHighlightText", ON_CORAL);
        put("menuText", TEXT);
        put("infoText", TEXT);
        put("controlText", TEXT);
        put("Panel.background", CHARCOAL);
        put("OptionPane.background", CHARCOAL);
        put("OptionPane.messageForeground", TEXT);
        put("Label.foreground", TEXT);
        put("Table.background", INSET);
        put("Table.alternateRowColor", ROW_ALT);
        put("Table.gridColor", LINE_SOFT);
        put("TextArea.background", INSET);
        put("ToolTip.background", SURFACE_2);
        put("ToolTip.foreground", TEXT);
        put("TitledBorder.titleColor", TEXT);
    }

    /** Hex string of a token for HTML text, e.g. {@code #ECE7E4}. */
    static String hex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    /** Hairline border on the given sides that always paints in the current mode's line color. */
    static Border hairline(int top, int left, int bottom, int right, boolean soft) {
        return new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(soft ? LINE_SOFT : LINE);
                g.fillRect(x, y, w, top);
                g.fillRect(x, y + h - bottom, w, bottom);
                g.fillRect(x, y, left, h);
                g.fillRect(x + w - right, y, right, h);
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(top, left, bottom, right);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                insets.set(top, left, bottom, right);
                return insets;
            }
        };
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    /** Returns {@code c} with the given alpha (0..255). */
    static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Small uppercase section caption ("RESULTS", "INPUT", ...). */
    static JLabel caption(String text) {
        return label(text.toUpperCase(Locale.ROOT), SMALL_BOLD, TEXT_MUTED);
    }

    /** Applies the shared table look: row height, header, selection and status-aware cell colors. */
    static void styleTable(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE_SOFT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(INSET);
        table.setForeground(TEXT);
        table.setSelectionBackground(CORAL);
        table.setSelectionForeground(ON_CORAL);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(SURFACE_2);
        table.getTableHeader().setPreferredSize(new Dimension(0, ROW_HEIGHT + 4));
    }

    /** Text cells: zebra rows, numbers right-aligned, status words colored (fault/error in coral, hit/ok in green). */
    private static final class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setFont(BODY);
            String text = value == null ? "" : value.toString();
            boolean numeric = value instanceof Number || text.equals("-") || text.matches("-?[0-9][0-9.,]*( ?%)?");
            setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
            // full text on hover when a narrow column truncates it
            setToolTipText(getFontMetrics(BODY_BOLD).stringWidth(text) + 20
                    > table.getColumnModel().getColumn(column).getWidth() ? text : null);
            if (selected) {
                setBackground(CORAL);
                setForeground(ON_CORAL);
                return this;
            }
            setBackground(row % 2 == 0 ? INSET : ROW_ALT);
            Color status = statusColor(text);
            setForeground(status == null ? TEXT : status);
            if (status != null) {
                setFont(BODY_BOLD);
            }
            return this;
        }
    }

    /** Maps result words produced by the core classes to a semantic color, or null for ordinary text. */
    static Color statusColor(String text) {
        String t = text.toUpperCase(Locale.ROOT);
        if (t.contains("FAULT") || t.startsWith("ERROR") || t.contains("EXTERNAL") || t.startsWith("NOT ")
                || t.contains("BLOCKED") || t.contains("UNSAFE") || t.startsWith("DENIED")) {
            return CORAL_LIGHT;
        }
        if (t.equals("HIT") || t.equals("OK") || t.equals("ALLOCATED") || t.equals("SAFE") || t.startsWith("GRANTED")) {
            return SUCCESS;
        }
        if (t.startsWith("INTERNAL") || t.startsWith("RETURN JUMP") || t.startsWith("WARNING")) {
            return WARNING;
        }
        return null;
    }

    /** Column headers: raised charcoal, coral caption, sort direction arrow. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            String text = value == null ? "" : value.toString();
            RowSorter<?> sorter = table.getRowSorter();
            if (sorter != null && !sorter.getSortKeys().isEmpty()
                    && sorter.getSortKeys().get(0).getColumn() == table.convertColumnIndexToModel(column)) {
                text += sorter.getSortKeys().get(0).getSortOrder() == SortOrder.ASCENDING ? "  ▲" : "  ▼";
            }
            super.getTableCellRendererComponent(table, text, false, false, row, column);
            setFont(SMALL_BOLD);
            setForeground(CORAL_LIGHT);
            setBackground(SURFACE_2);
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    hairline(0, 0, 2, 1, false), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            return this;
        }
    }

    /** Header renderer usable by other components that want the same look. */
    static TableCellRenderer headerRenderer() {
        return new HeaderRenderer();
    }

    /** Antialiased rounded outline (current line color) with inner padding. */
    static final class RoundBorder extends AbstractBorder {
        private final int radius;
        private final Insets pad;

        RoundBorder(int radius, Insets pad) {
            this.radius = radius;
            this.pad = pad;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = smooth(g);
            g2.setColor(LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(pad.top, pad.left, pad.bottom, pad.right);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(pad.top, pad.left, pad.bottom, pad.right);
            return insets;
        }
    }

    static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** Makes a component and (when it is a container of plain panels) its background transparent. */
    static <T extends JComponent> T clear(T component) {
        component.setOpaque(false);
        return component;
    }
}
