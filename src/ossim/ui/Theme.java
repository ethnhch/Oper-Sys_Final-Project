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
 * "Neon Matrix, Cyber Light &amp; Cyber Midnight" design system: one place for every color, font, spacing value and shared renderer,
 * so all eight module screens look identical. No algorithm code lives here.
 */
public final class Theme {
    // ---- Palette (Neon Matrix cyber interface) -------------------------------------
    /** Light or dark appearance; switchable at runtime with {@link #setMode}. */
    public enum Mode { CYBER_GREEN, CYBER_LIGHT, CYBER_MIDNIGHT }

    /**
     * Every color token with its theme values. Values are unique within each mode, which lets
     * {@link #setMode} recolor existing components by swapping old token values for new ones.
     */
    private enum Token {
        // Neon Matrix: near-black surfaces, electric green controls and high-contrast text.
        BG(0x080E0B, 0xF1F7F2, 0x08121B), CHARCOAL(0x0D1712, 0xFFFFFF, 0x101F2D),
        SURFACE_2(0x14241A, 0xE5EFE7, 0x1A3042), INSET(0x060B08, 0xFAFCFA, 0x07101A),
        ROW_ALT(0x0B1510, 0xEDF4EE, 0x0D1B29), LINE(0x244A32, 0xD0E0D3, 0x2D5068),
        LINE_SOFT(0x193624, 0xE0EAE2, 0x203B50),
        NEON(0x27E879, 0x398653, 0x36D9C2), NEON_BRIGHT(0x48FF91, 0x286C43, 0x63F3DE),
        NEON_LIGHT(0x9AFFBD, 0x245B39, 0x9AF7E9), NEON_DEEP(0x159B4C, 0x1D5935, 0x168E86),
        NEON_TINT(0x102B1B, 0xE0F0E4, 0x123A3B),
        TEXT(0xE6F8EC, 0x20382A, 0xE5F2FA), TEXT_MUTED(0xA1BDAA, 0x5D7564, 0xA8C0D0),
        TEXT_FAINT(0x6D8D78, 0x819688, 0x7893A7), ON_NEON(0x041009, 0xFFFFFF, 0x061713),
        SUCCESS(0x65E995, 0x34794B, 0x63E6B8), WARNING(0xFFC56E, 0x98601D, 0xFFC56E);

        private final Color green;
        private final Color light;
        private final Color midnight;

        Token(int green, int light, int midnight) {
            this.green = new Color(green);
            this.light = new Color(light);
            this.midnight = new Color(midnight);
        }

        Color of(Mode mode) {
            return switch (mode) {
                case CYBER_GREEN -> green;
                case CYBER_LIGHT -> light;
                case CYBER_MIDNIGHT -> midnight;
            };
        }
    }

    private static Mode mode = Mode.CYBER_GREEN;
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
    /** Neon green: the single accent (primary buttons, selection, headings). */
    static Color NEON;
    static Color NEON_BRIGHT;
    /** Neon green used for text accents (lighter on dark, darker on light, for contrast). */
    static Color NEON_LIGHT;
    static Color NEON_DEEP;
    /** Neon tint over the surface, for active nav items and tabs. */
    static Color NEON_TINT;
    static Color TEXT;
    static Color TEXT_MUTED;
    static Color TEXT_FAINT;
    /** Text drawn on top of neon fills. */
    static Color ON_NEON;
    static Color SUCCESS;
    static Color WARNING;
    /** Label color on the series fills of the Gantt chart (same in both modes). */
    static final Color ON_SERIES = new Color(0x1B2024);

    static {
        assign(Mode.CYBER_GREEN);
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
        NEON = Token.NEON.of(m);
        NEON_BRIGHT = Token.NEON_BRIGHT.of(m);
        NEON_LIGHT = Token.NEON_LIGHT.of(m);
        NEON_DEEP = Token.NEON_DEEP.of(m);
        NEON_TINT = Token.NEON_TINT.of(m);
        TEXT = Token.TEXT.of(m);
        TEXT_MUTED = Token.TEXT_MUTED.of(m);
        TEXT_FAINT = Token.TEXT_FAINT.of(m);
        ON_NEON = Token.ON_NEON.of(m);
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
            table.setSelectionBackground(NEON);
            table.setSelectionForeground(ON_NEON);
            table.setGridColor(LINE_SOFT);
        } else if (c instanceof javax.swing.text.JTextComponent text) {
            text.setCaretColor(NEON_LIGHT);
            text.setSelectionColor(NEON);
            text.setSelectedTextColor(ON_NEON);
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

    /** Distinct series colors that sit well next to neon on charcoal (Gantt slices, legends). */
    static final List<Color> SERIES = List.of(
            new Color(0x739B7D), new Color(0xD2A86D), new Color(0x8BC9A0), new Color(0x78A9C8),
            new Color(0xA6A0C8), new Color(0xD7C77A), new Color(0xC58F9D), new Color(0x69B8B5),
            new Color(0x8DB99A), new Color(0xA8C68C));

    // ---- Spacing & shape ------------------------------------------------------------------------
    static final int RADIUS = 14;
    static final int GAP = 12;
    static final int ROW_HEIGHT = 28;

    // ---- Responsive breakpoints (frame / panel widths in pixels) ---------------------------------
    /** Below this frame width the sidebar collapses to icon badges. */
    static final int BP_SIDEBAR = 1050;
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
     * Installs the Neon Matrix, Cyber Light &amp; Cyber Midnight palette in the given mode on top of Nimbus (bundled with every JDK, so no
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
        put("nimbusBase", mode == Mode.CYBER_GREEN ? SURFACE_2 : (mode == Mode.CYBER_LIGHT ? new Color(0x398653) : new Color(0x1A5260)));
        put("nimbusBlueGrey", LINE);
        put("nimbusFocus", NEON);
        put("nimbusLightBackground", INSET);
        put("nimbusSelectionBackground", NEON);
        put("nimbusSelectedText", ON_NEON);
        put("nimbusSelection", NEON);
        put("nimbusOrange", NEON);
        put("nimbusGreen", SUCCESS);
        put("nimbusRed", NEON_DEEP);
        put("nimbusInfoBlue", new Color(0x6FA8DC));
        put("nimbusDisabledText", TEXT_FAINT);
        put("nimbusBorder", LINE);
        put("text", TEXT);
        put("textForeground", TEXT);
        put("textBackground", NEON);
        put("textHighlight", NEON);
        put("textHighlightText", ON_NEON);
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
        table.setSelectionBackground(NEON);
        table.setSelectionForeground(ON_NEON);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(SURFACE_2);
        table.getTableHeader().setPreferredSize(new Dimension(0, ROW_HEIGHT + 4));
    }

    /** Text cells: zebra rows, numbers right-aligned, status words colored (fault/error in neon, hit/ok in green). */
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
                setBackground(NEON);
                setForeground(ON_NEON);
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
            return NEON_LIGHT;
        }
        if (t.equals("HIT") || t.equals("OK") || t.equals("ALLOCATED") || t.equals("SAFE") || t.startsWith("GRANTED")) {
            return SUCCESS;
        }
        if (t.startsWith("INTERNAL") || t.startsWith("RETURN JUMP") || t.startsWith("WARNING")) {
            return WARNING;
        }
        return null;
    }

    /** Column headers: raised charcoal, neon caption, sort direction arrow. */
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
            setForeground(NEON_LIGHT);
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
