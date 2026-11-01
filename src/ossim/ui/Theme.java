package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Design tokens for the whole interface: colours (light and dark), the type scale, spacing and corner radius.
 * Every custom component reads its colours from here at paint time, so switching theme repaints everything.
 */
public final class Theme {

    /** Semantic colour roles. Components ask for a role, never for a hard-coded colour. */
    enum Role {
        BG, SURFACE, SURFACE_ALT, SIDEBAR, SIDEBAR_HOVER, SIDEBAR_ACTIVE, BORDER, BORDER_STRONG,
        TEXT, TEXT_MUTED, TEXT_FAINT, ACCENT, ACCENT_HOVER, ACCENT_SOFT, ON_ACCENT,
        SUCCESS, SUCCESS_SOFT, WARNING, WARNING_SOFT, DANGER, DANGER_SOFT, GRID, SELECTION
    }

    /** Tone of a stat tile, badge or banner. */
    enum Tone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER }

    private static final Color[] LIGHT = {
            rgb(0xF4F6FA), rgb(0xFFFFFF), rgb(0xF8FAFC), rgb(0x111827), rgb(0x1F2937), rgb(0x2B3A55),
            rgb(0xE3E7EE), rgb(0xCBD2DC),
            rgb(0x111827), rgb(0x4B5563), rgb(0x6B7280),
            rgb(0x2563EB), rgb(0x1D4ED8), rgb(0xE6EEFF), rgb(0xFFFFFF),
            rgb(0x15803D), rgb(0xE5F6EB), rgb(0xB45309), rgb(0xFDF2E1), rgb(0xC62828), rgb(0xFDE8E8),
            rgb(0xEEF1F5), rgb(0xDCE7FE)
    };

    private static final Color[] DARK = {
            rgb(0x0D1117), rgb(0x161B22), rgb(0x1C222B), rgb(0x090C10), rgb(0x161B22), rgb(0x1F2D45),
            rgb(0x2A313C), rgb(0x3A4250),
            rgb(0xE6EAF0), rgb(0xA3ADBB), rgb(0x848E9D),
            rgb(0x4C8DFF), rgb(0x6EA2FF), rgb(0x18263D), rgb(0x0B1220),
            rgb(0x3FB868), rgb(0x12291B), rgb(0xE3A23B), rgb(0x2E2312), rgb(0xF2645A), rgb(0x34171A),
            rgb(0x222933), rgb(0x22385E)
    };

    /** Categorical series colours (process IDs, pages, programs). Chosen to stay distinct in both themes. */
    private static final Color[] SERIES_LIGHT = {
            rgb(0x3B82F6), rgb(0x10B981), rgb(0xF59E0B), rgb(0x8B5CF6), rgb(0xEF4444),
            rgb(0x06B6D4), rgb(0xEC4899), rgb(0x84CC16), rgb(0xF97316), rgb(0x6366F1)
    };
    private static final Color[] SERIES_DARK = {
            rgb(0x60A5FA), rgb(0x34D399), rgb(0xFBBF24), rgb(0xA78BFA), rgb(0xF87171),
            rgb(0x22D3EE), rgb(0xF472B6), rgb(0xA3E635), rgb(0xFB923C), rgb(0x818CF8)
    };

    static final int RADIUS = 10;
    static final int GAP = 12;

    static final String FAMILY = pickFamily();
    static final String MONO_FAMILY = pickMono();
    static final Font BODY = new Font(FAMILY, Font.PLAIN, 13);
    static final Font BODY_BOLD = new Font(FAMILY, Font.BOLD, 13);
    static final Font SMALL = new Font(FAMILY, Font.PLAIN, 12);
    static final Font SMALL_BOLD = new Font(FAMILY, Font.BOLD, 11);
    static final Font H1 = new Font(FAMILY, Font.BOLD, 22);
    static final Font H2 = new Font(FAMILY, Font.BOLD, 15);
    static final Font STAT = new Font(FAMILY, Font.BOLD, 22);
    static final Font MONO = new Font(MONO_FAMILY, Font.PLAIN, 12);

    private static boolean dark;
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private Theme() { }

    static Color c(Role role) {
        return (dark ? DARK : LIGHT)[role.ordinal()];
    }

    static Color series(int index) {
        Color[] palette = dark ? SERIES_DARK : SERIES_LIGHT;
        return palette[Math.floorMod(index, palette.length)];
    }

    /** Stable colour for a label such as "P3" or "J12": numbered names map to consecutive palette slots. */
    static Color seriesFor(String id) {
        String digits = id.replaceAll("\\D", "");
        if (!digits.isEmpty() && digits.length() < 9) {
            return series(Integer.parseInt(digits) - 1);
        }
        return series(id.hashCode());
    }

    static Color tone(Tone tone) {
        return switch (tone) {
            case NEUTRAL -> c(Role.TEXT);
            case ACCENT -> c(Role.ACCENT);
            case SUCCESS -> c(Role.SUCCESS);
            case WARNING -> c(Role.WARNING);
            case DANGER -> c(Role.DANGER);
        };
    }

    static Color toneSoft(Tone tone) {
        return switch (tone) {
            case NEUTRAL -> c(Role.SURFACE_ALT);
            case ACCENT -> c(Role.ACCENT_SOFT);
            case SUCCESS -> c(Role.SUCCESS_SOFT);
            case WARNING -> c(Role.WARNING_SOFT);
            case DANGER -> c(Role.DANGER_SOFT);
        };
    }

    static boolean isDark() {
        return dark;
    }

    static void setDark(boolean value) {
        dark = value;
        apply();
        LISTENERS.forEach(Runnable::run);
    }

    static void onChange(Runnable listener) {
        LISTENERS.add(listener);
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Readable text colour (dark or white) on top of the given fill. */
    static Color textOn(Color fill) {
        double luminance = (0.299 * fill.getRed() + 0.587 * fill.getGreen() + 0.114 * fill.getBlue()) / 255;
        return luminance > 0.6 ? rgb(0x111827) : Color.WHITE;
    }

    /** Installs the cross-platform base look and feel, then overrides its defaults with the current tokens. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // Metal ships with every JDK; if it somehow fails the default look and feel is used.
        }
        apply();
    }

    private static void apply() {
        put("Panel.background", c(Role.BG));
        put("Label.foreground", c(Role.TEXT));
        put("control", c(Role.SURFACE));
        put("text", c(Role.SURFACE));
        put("Viewport.background", c(Role.SURFACE));
        put("ScrollPane.background", c(Role.SURFACE));
        put("TextField.background", c(Role.SURFACE));
        put("TextField.foreground", c(Role.TEXT));
        put("TextField.caretForeground", c(Role.TEXT));
        put("TextField.selectionBackground", c(Role.SELECTION));
        put("TextField.selectionForeground", c(Role.TEXT));
        put("TextField.inactiveForeground", c(Role.TEXT_MUTED));
        put("TextArea.background", c(Role.SURFACE));
        put("TextArea.foreground", c(Role.TEXT));
        put("TextArea.caretForeground", c(Role.TEXT));
        put("TextArea.selectionBackground", c(Role.SELECTION));
        put("TextArea.selectionForeground", c(Role.TEXT));
        put("Table.background", c(Role.SURFACE));
        put("Table.foreground", c(Role.TEXT));
        put("Table.gridColor", c(Role.GRID));
        put("Table.selectionBackground", c(Role.SELECTION));
        put("Table.selectionForeground", c(Role.TEXT));
        put("Table.focusCellHighlightBorder", BorderFactory.createLineBorder(c(Role.ACCENT)));
        put("TableHeader.background", c(Role.SURFACE_ALT));
        put("TableHeader.foreground", c(Role.TEXT_MUTED));
        put("ComboBox.background", c(Role.SURFACE));
        put("ComboBox.foreground", c(Role.TEXT));
        put("ComboBox.selectionBackground", c(Role.ACCENT_SOFT));
        put("ComboBox.selectionForeground", c(Role.TEXT));
        put("ComboBox.buttonBackground", c(Role.SURFACE));
        put("List.background", c(Role.SURFACE));
        put("List.foreground", c(Role.TEXT));
        put("List.selectionBackground", c(Role.ACCENT_SOFT));
        put("List.selectionForeground", c(Role.TEXT));
        put("CheckBox.background", c(Role.SURFACE));
        put("CheckBox.foreground", c(Role.TEXT));
        put("ToolTip.background", c(Role.SIDEBAR));
        put("ToolTip.foreground", rgb(0xF3F4F6));
        put("ToolTip.border", BorderFactory.createEmptyBorder(6, 9, 6, 9));
        put("PopupMenu.border", BorderFactory.createLineBorder(c(Role.BORDER)));
        put("SplitPane.background", c(Role.BG));
        put("SplitPane.border", BorderFactory.createEmptyBorder());
        put("ScrollPane.border", BorderFactory.createEmptyBorder());
        put("TabbedPane.contentBorderInsets", new InsetsUIResource(0, 0, 0, 0));
        put("TabbedPane.tabAreaInsets", new InsetsUIResource(0, 4, 0, 4));
        put("OptionPane.background", c(Role.SURFACE));
        put("OptionPane.messageForeground", c(Role.TEXT));

        for (String key : Set.of("Label.font", "Button.font", "TextField.font", "ComboBox.font", "CheckBox.font",
                "Table.font", "List.font", "TabbedPane.font", "ToolTip.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "TableHeader.font", "TitledBorder.font")) {
            UIManager.put(key, new FontUIResource(BODY));
        }
        UIManager.put("TableHeader.font", new FontUIResource(SMALL_BOLD));
        UIManager.put("TextArea.font", new FontUIResource(MONO));

        UIManager.put("TabbedPaneUI", FlatUI.Tabs.class.getName());
        UIManager.put("ScrollBarUI", FlatUI.ScrollBar.class.getName());
        UIManager.put("ComboBoxUI", FlatUI.Combo.class.getName());
        UIManager.put("SplitPaneUI", FlatUI.Split.class.getName());
        UIManager.put("CheckBoxUI", FlatUI.Check.class.getName());
    }

    private static void put(String key, Object value) {
        UIManager.put(key, value instanceof Color color ? new ColorUIResource(color) : value);
    }

    private static Color rgb(int hex) {
        return new Color(hex);
    }

    private static String pickFamily() {
        return first(List.of("Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans", "DejaVu Sans"),
                Font.SANS_SERIF);
    }

    private static String pickMono() {
        return first(List.of("Cascadia Mono", "Consolas", "JetBrains Mono", "SF Mono", "Menlo", "DejaVu Sans Mono"),
                Font.MONOSPACED);
    }

    private static String first(List<String> wanted, String fallback) {
        try {
            Set<String> installed = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String name : wanted) {
                if (installed.contains(name)) {
                    return name;
                }
            }
        } catch (RuntimeException ignored) {
            // headless environments without fonts fall back to the logical font
        }
        return fallback;
    }
}
