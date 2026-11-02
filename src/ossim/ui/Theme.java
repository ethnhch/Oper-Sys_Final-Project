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
            rgb(0xF6F2F0), rgb(0xFFFFFF), rgb(0xFBF7F5), rgb(0x262D33), rgb(0x323A41),
            rgb(0x4A3A3A), rgb(0xECE3DF), rgb(0xD8CBC6), rgb(0x262D33), rgb(0x4F565D),
            rgb(0x6B7178), rgb(0xC24A32), rgb(0xA63D28), rgb(0xFCEAE6), rgb(0xFFFFFF),
            rgb(0x1E7A4C), rgb(0xE3F4EA), rgb(0x9A5B00), rgb(0xFBF0DC), rgb(0xB42345),
            rgb(0xFBE6EA), rgb(0xF1EBE8), rgb(0xFBDDD5)
    };

    private static final Color[] DARK = {
            rgb(0x1E2328), rgb(0x262D33), rgb(0x2C343B), rgb(0x171B1F), rgb(0x232A30),
            rgb(0x3A2C2A), rgb(0x353D45), rgb(0x46505A), rgb(0xE9ECEE), rgb(0xA9B0B6),
            rgb(0x8C949B), rgb(0xF07A65), rgb(0xF7937F), rgb(0x3B2A28), rgb(0x1E2328),
            rgb(0x4CC38A), rgb(0x1C3329), rgb(0xF2B544), rgb(0x3A3020), rgb(0xFF6B81),
            rgb(0x3D2229), rgb(0x30383F), rgb(0x4A3330)
    };

    /** Categorical series colours (process IDs, pages, programs). Chosen to stay distinct in both themes. */
    private static final Color[] SERIES_LIGHT = {
            rgb(0xE36A53), rgb(0x2A9D8F), rgb(0xE9A23B), rgb(0x4F6D8F), rgb(0x7FA36B),
            rgb(0x9B5DA5), rgb(0x3E9BD4), rgb(0xD6577C), rgb(0xA69B3A), rgb(0x5C6B7A)
    };
    private static final Color[] SERIES_DARK = {
            rgb(0xF07A65), rgb(0x3FBFAE), rgb(0xF2B544), rgb(0x7C9CC2), rgb(0x9CC285),
            rgb(0xBE85C8), rgb(0x64B5E8), rgb(0xEE7C9C), rgb(0xC9BC5A), rgb(0x8A99A8)
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
        try {
            java.util.prefs.Preferences.userNodeForPackage(Theme.class).putBoolean(PREF_DARK, value);
        } catch (RuntimeException ignored) {
            // preferences unavailable (locked-down machine): the choice simply is not remembered
        }
    }

    private static final String PREF_DARK = "darkMode";
    private static Runnable toggler;

    /** The main window registers how to switch theme, so a button anywhere in the UI can ask for it. */
    static void setToggler(Runnable value) {
        toggler = value;
    }

    static void requestToggle() {
        if (toggler != null) {
            toggler.run();
        } else {
            setDark(!dark);
        }
    }

    /**
     * Start-up mode: {@code -Dossim.theme=dark|light} wins, otherwise the mode the user picked last time
     * (remembered with java.util.prefs), otherwise light.
     */
    private static boolean initialDark() {
        String forced = System.getProperty("ossim.theme");
        if (forced != null) {
            return forced.equalsIgnoreCase("dark");
        }
        try {
            return java.util.prefs.Preferences.userNodeForPackage(Theme.class).getBoolean(PREF_DARK, false);
        } catch (RuntimeException e) {
            return false;
        }
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
        return luminance > 0.6 ? rgb(0x262D33) : Color.WHITE;
    }

    /** Installs the cross-platform base look and feel, then overrides its defaults with the current tokens. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // Metal ships with every JDK; if it somehow fails the default look and feel is used.
        }
        dark = initialDark();
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
