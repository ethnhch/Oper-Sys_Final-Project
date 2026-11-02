package ossim.ui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Main window: charcoal header, grouped sidebar menu (modules 1-8), the active module, and a status bar.
 * Responsive: the sidebar collapses to badges below {@link Theme#BP_SIDEBAR}; each module stacks its input and
 * results below {@link Theme#BP_STACK}. Shortcuts: Ctrl+1..8 switch module, Ctrl+Enter / F5 run, Ctrl+B menu,
 * Ctrl+T light/dark mode (also the moon/sun switch in the header; the choice is remembered).
 */
public final class MainFrame extends JFrame {
    private record Module(String group, String name, Supplier<ModulePanel> factory) { }

    private static final List<Module> MODULES = List.of(
            new Module("Process management", "CPU Scheduling", CpuPanel::new),
            new Module("Process management", "Deadlock · Banker's", BankerPanel::new),
            new Module("Memory management", "Paging", PagingPanel::new),
            new Module("Memory management", "Page Replacement", PageReplacementPanel::new),
            new Module("Memory management", "Segmentation", SegmentationPanel::new),
            new Module("Memory management", "Fragmentation", FragmentationPanel::new),
            new Module("Storage", "Disk Scheduling", DiskPanel::new),
            new Module("System", "Multiprogramming", MultiprogrammingPanel::new));

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final List<ModulePanel> panels = new ArrayList<>();
    private final Sidebar sidebar = new Sidebar(this::show);
    private final StatusBar status = new StatusBar();
    private final JLabel crumb = Theme.label("", Theme.BODY, Theme.TEXT_MUTED);
    private final JLabel subtitle = Theme.label("OPERATING SYSTEM SIMULATOR", Theme.SMALL_BOLD, Theme.TEXT_MUTED);
    private int current;
    private Boolean compactOverride;
    private boolean lastAutoCompact;

    public MainFrame() {
        super("Mini OS Simulator");
        content.setBackground(Theme.BG);
        String group = "";
        for (int i = 0; i < MODULES.size(); i++) {
            Module m = MODULES.get(i);
            if (!m.group().equals(group)) {
                group = m.group();
                sidebar.addGroup(group);
            }
            sidebar.addItem(m.name());
            ModulePanel panel = m.factory().get();
            panel.setModuleInfo(i + 1, m.group());
            panel.setStatusListener(status::show);
            panels.add(panel);
            content.add(panel, String.valueOf(i));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        installShortcuts(root);
        Theme.polishTree(root);
        show(0);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 560));
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1440, (int) (screen.width * 0.9)), Math.min(900, (int) (screen.height * 0.9)));
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsive();
            }
        });
        lastAutoCompact = getWidth() < Theme.BP_SIDEBAR;
        sidebar.setCompact(lastAutoCompact);
    }

    private JComponent header() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.CHARCOAL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                Theme.hairline(0, 0, 1, 0, true), Theme.padding(10, 14, 10, 20)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        JComponent menu = new MenuToggle();
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleSidebar();
            }
        });
        brand.add(menu);
        brand.add(new LogoMark());
        brand.add(Theme.label("MINI OS", Theme.BRAND, Theme.CORAL_BRIGHT));
        brand.add(subtitle);
        bar.add(brand, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(crumb);
        right.add(new ModeSwitch(this::setTheme));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void installShortcuts(JComponent root) {
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        for (int i = 0; i < MODULES.size(); i++) {
            int index = i;
            bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, ctrl), "module" + i, () -> show(index));
        }
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ctrl), "run", () -> panels.get(current).runPrimary());
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run-f5", () -> panels.get(current).runPrimary());
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_B, ctrl), "sidebar", this::toggleSidebar);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_T, ctrl), "theme",
                () -> setTheme(Theme.mode() == Theme.Mode.DARK ? Theme.Mode.LIGHT : Theme.Mode.DARK));
    }

    private static void bind(JComponent root, KeyStroke key, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void show(int index) {
        current = index;
        cards.show(content, String.valueOf(index));
        sidebar.select(index);
        Module m = MODULES.get(index);
        crumb.setText(m.group() + "  /  " + m.name());
        status.hint("Module " + (index + 1) + " of " + MODULES.size() + " · " + m.name());
    }

    /** Switches light/dark mode live (inputs and results are kept) and remembers the choice for the next launch. */
    private void setTheme(Theme.Mode mode) {
        if (mode == Theme.mode()) {
            return;
        }
        Theme.setMode(mode);
        savedMode(mode);
        status.show(true, (mode == Theme.Mode.DARK ? "Dark" : "Light") + " mode \u2014 press Ctrl+T to switch back");
    }

    /** Reads the saved appearance (dark by default). */
    public static Theme.Mode savedMode() {
        try {
            return Theme.Mode.valueOf(java.util.prefs.Preferences.userNodeForPackage(MainFrame.class)
                    .get("theme", Theme.Mode.DARK.name()));
        } catch (RuntimeException e) {
            return Theme.Mode.DARK;
        }
    }

    private static void savedMode(Theme.Mode mode) {
        try {
            java.util.prefs.Preferences.userNodeForPackage(MainFrame.class).put("theme", mode.name());
        } catch (RuntimeException ignored) {
            // preferences unavailable (e.g. read-only home); the choice simply is not remembered
        }
    }

    private void toggleSidebar() {
        compactOverride = !sidebar.isCompact();
        sidebar.setCompact(compactOverride);
    }

    /** Collapses the sidebar automatically when crossing the breakpoint; a manual toggle wins until the next crossing. */
    private void applyResponsive() {
        boolean autoCompact = getWidth() < Theme.BP_SIDEBAR;
        if (autoCompact != lastAutoCompact) {
            compactOverride = null;
            lastAutoCompact = autoCompact;
        }
        boolean compact = compactOverride != null ? compactOverride : autoCompact;
        if (compact != sidebar.isCompact()) {
            sidebar.setCompact(compact);
        }
        subtitle.setVisible(getWidth() >= 900);
        crumb.setVisible(getWidth() >= 820);
    }

    /** Hamburger button that toggles the sidebar (also Ctrl+B). */
    private static final class MenuToggle extends JComponent {
        private boolean hover;

        MenuToggle() {
            setPreferredSize(new Dimension(34, 34));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Collapse / expand menu (Ctrl+B)");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            if (hover) {
                g2.setColor(Theme.SURFACE_2);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            g2.setColor(hover ? Theme.CORAL_LIGHT : Theme.TEXT_MUTED);
            for (int i = 0; i < 3; i++) {
                g2.fillRoundRect(9, 11 + i * 5, 16, 2, 2, 2);
            }
            g2.dispose();
        }
    }

    /** Two-segment moon / sun switch in the header; the active mode is filled with coral. */
    private static final class ModeSwitch extends JComponent {
        private static final int W = 72;
        private static final int H = 32;
        private int hover = -1;

        ModeSwitch(java.util.function.Consumer<Theme.Mode> onChange) {
            setPreferredSize(new Dimension(W, H));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Dark / light mode (Ctrl+T)");
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onChange.accept(e.getX() < W / 2 ? Theme.Mode.DARK : Theme.Mode.LIGHT);
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    hover = e.getX() < W / 2 ? 0 : 1;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(Theme.SURFACE_2);
            g2.fillRoundRect(0, 0, W - 1, H - 1, H, H);
            g2.setColor(Theme.LINE);
            g2.drawRoundRect(0, 0, W - 1, H - 1, H, H);
            int active = Theme.mode() == Theme.Mode.DARK ? 0 : 1;
            for (int i = 0; i < 2; i++) {
                int x = 3 + i * (W / 2 - 3);
                if (i == active) {
                    g2.setColor(Theme.CORAL);
                    g2.fillRoundRect(x, 3, W / 2 - 3, H - 7, H - 7, H - 7);
                } else if (i == hover) {
                    g2.setColor(Theme.CORAL_TINT);
                    g2.fillRoundRect(x, 3, W / 2 - 3, H - 7, H - 7, H - 7);
                }
                g2.setColor(i == active ? Theme.ON_CORAL : Theme.TEXT_MUTED);
                int cx = x + (W / 2 - 3) / 2;
                int cy = H / 2 - 1;
                if (i == 0) {
                    java.awt.geom.Area moon = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(cx - 6, cy - 6, 12, 12));
                    moon.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(cx - 2, cy - 9, 12, 12)));
                    g2.fill(moon);
                } else {
                    g2.fillOval(cx - 3, cy - 3, 7, 7);
                    g2.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    for (int r = 0; r < 8; r++) {
                        double a = Math.PI / 4 * r;
                        g2.drawLine(cx + (int) Math.round(Math.cos(a) * 6), cy + (int) Math.round(Math.sin(a) * 6),
                                cx + (int) Math.round(Math.cos(a) * 8), cy + (int) Math.round(Math.sin(a) * 8));
                    }
                }
            }
            g2.dispose();
        }
    }

    /** Brand mark: a charcoal tile with a coral half, echoing the two-tone palette. */
    private static final class LogoMark extends JComponent {
        LogoMark() {
            setPreferredSize(new Dimension(30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(Theme.CORAL);
            g2.fillRoundRect(2, 2, 26, 26, 9, 9);
            g2.setColor(Theme.CHARCOAL);
            g2.fillRoundRect(8, 8, 14, 7, 4, 4);
            g2.setColor(Theme.CORAL_LIGHT);
            g2.fillRoundRect(8, 17, 14, 5, 4, 4);
            g2.dispose();
        }
    }
}
