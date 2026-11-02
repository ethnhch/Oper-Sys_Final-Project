package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The main menu: brand, a Home entry, the eight modules grouped by OS area, and the theme switch.
 * The sidebar stays dark in both themes so the navigation reads as a separate layer from the work area.
 * On narrow windows it collapses to an icon rail (labels move into tooltips); clicking the logo toggles it by hand.
 * The menu scrolls when the window is too short to show every entry.
 */
final class Sidebar extends JPanel {
    static final int WIDTH = 248;
    static final int RAIL = 68;
    private static final Color TEXT = new Color(0xC9D1DC);
    private static final Color TEXT_DIM = new Color(0x7D8899);
    private static final Color TEXT_ON = Color.WHITE;

    private final List<NavItem> items = new ArrayList<>();
    private final IntConsumer onSelect;
    private final NavItem themeItem;
    private final Footnote footnote = new Footnote();
    private boolean compact;

    /** One navigation entry: index -1 = home, 0..7 = modules. */
    record Entry(int index, String label, Icons.Kind icon, String group, String shortcut) { }

    Sidebar(List<Entry> entries, IntConsumer onSelect, Runnable onToggleTheme, Runnable onToggleCompact) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setPreferredSize(new Dimension(WIDTH, 600));
        setBorder(BorderFactory.createEmptyBorder(18, 12, 14, 12));

        JPanel list = Ui.clear(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.add(new Brand(onToggleCompact));
        list.add(Box.createVerticalStrut(18));
        String group = null;
        for (Entry e : entries) {
            if (e.group() != null && !e.group().equals(group)) {
                group = e.group();
                list.add(Box.createVerticalStrut(10));
                list.add(new GroupCaption(group));
            }
            NavItem item = new NavItem(e.label(), e.icon(), e.shortcut(), () -> onSelect.accept(e.index()));
            item.index = e.index();
            items.add(item);
            list.add(item);
        }
        JPanel listTop = Ui.clear(new BorderLayout());
        listTop.add(list, BorderLayout.NORTH);
        var scroll = Ui.scroll(listTop);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = Ui.clear(null);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        themeItem = new NavItem("Dark mode", Icons.Kind.MOON, "Ctrl+D", onToggleTheme);
        bottom.add(themeItem);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(footnote);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Switches between the full sidebar and the icon rail. */
    void setCompact(boolean value) {
        compact = value;
        setPreferredSize(new Dimension(compact ? RAIL : WIDTH, 600));
        setBorder(BorderFactory.createEmptyBorder(18, compact ? 10 : 12, 14, compact ? 10 : 12));
        footnote.setVisible(!compact);
        revalidate();
        repaint();
    }

    boolean isCompact() {
        return compact;
    }

    /** Hides the shortcut footnote when the window is short, so the menu itself keeps the space. */
    void adaptHeight(int height) {
        footnote.setVisible(!compact && height >= 760);
    }

    private static boolean compact(Component c) {
        Sidebar s = (Sidebar) javax.swing.SwingUtilities.getAncestorOfClass(Sidebar.class, c);
        return s != null && s.compact;
    }

    void select(int index) {
        for (NavItem item : items) {
            item.active = item.index == index;
            item.repaint();
        }
    }

    void themeChanged(boolean dark) {
        themeItem.label = dark ? "Light mode" : "Dark mode";
        themeItem.kind = dark ? Icons.Kind.SUN : Icons.Kind.MOON;
        themeItem.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Theme.c(Theme.Role.SIDEBAR));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.isDark() ? Theme.c(Theme.Role.BORDER) : Theme.c(Theme.Role.SIDEBAR));
        g.fillRect(getWidth() - 1, 0, 1, getHeight());
    }

    /** Clickable, hoverable navigation row. */
    private static final class NavItem extends JComponent {
        private String label;
        private Icons.Kind kind;
        private final String shortcut;
        private boolean active;
        private boolean hover;
        private int index;

        NavItem(String label, Icons.Kind kind, String shortcut, Runnable action) {
            this.label = label;
            this.kind = kind;
            this.shortcut = shortcut;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(40, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(label + (shortcut == null ? "" : "  (" + shortcut + ")"));
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

                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            if (active || hover) {
                g2.setColor(active ? Theme.c(Theme.Role.SIDEBAR_ACTIVE) : Theme.c(Theme.Role.SIDEBAR_HOVER));
                g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 10, 10);
            }
            if (active) {
                g2.setColor(Theme.c(Theme.Role.ACCENT));
                g2.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
            }
            Color fg = active ? TEXT_ON : hover ? TEXT_ON : TEXT;
            boolean rail = compact(this);
            int iconX = rail ? (getWidth() - 18) / 2 : 12;
            Icons.paint(g2, kind, iconX, (getHeight() - 18) / 2, 18, active ? Theme.c(Theme.Role.ACCENT_HOVER) : fg);
            if (rail) {
                g2.dispose();
                return;
            }
            g2.setFont(active ? Theme.BODY_BOLD : Theme.BODY);
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, 40, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (shortcut != null && hover && !active && fm.stringWidth(label) + 40 + 60 < getWidth()) {
                g2.setFont(Theme.SMALL);
                g2.setColor(TEXT_DIM);
                FontMetrics sm = g2.getFontMetrics();
                g2.drawString(shortcut, getWidth() - sm.stringWidth(shortcut) - 10,
                        (getHeight() - sm.getHeight()) / 2 + sm.getAscent());
            }
            g2.dispose();
        }
    }

    private static final class GroupCaption extends JComponent {
        private final String text;

        GroupCaption(String text) {
            this.text = text.toUpperCase();
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(40, compact(this) ? 10 : 26);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, compact(this) ? 10 : 26);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            if (compact(this)) {
                g2.setColor(new Color(0x3A434B));
                g2.fillRect(10, 5, getWidth() - 20, 1);
            } else {
                g2.setFont(Theme.SMALL_BOLD);
                g2.setColor(TEXT_DIM);
                g2.drawString(text, 12, 18);
            }
            g2.dispose();
        }
    }

    private static final class Brand extends JComponent {
        Brand(Runnable onToggle) {
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Collapse or expand the menu");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onToggle.run();
                }
            });
            setPreferredSize(new Dimension(40, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            int x = compact(this) ? (getWidth() - 36) / 2 : 8;
            g2.setPaint(new java.awt.GradientPaint(x, 4, new Color(0xE36A53), x + 36, 40, new Color(0xF47A66)));
            g2.fillRoundRect(x, 4, 36, 36, 11, 11);
            Icons.paint(g2, Icons.Kind.CPU, x + 8, 12, 20, Color.WHITE);
            if (compact(this)) {
                g2.dispose();
                return;
            }
            g2.setFont(Theme.H2.deriveFont(16f));
            g2.setColor(TEXT_ON);
            g2.drawString("MiniOS", 54, 20);
            g2.setFont(Theme.SMALL);
            g2.setColor(TEXT_DIM);
            g2.drawString("Operating System Simulator", 54, 37);
            g2.dispose();
        }
    }

    private static final class Footnote extends JComponent {
        Footnote() {
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(40, 36));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setFont(Theme.SMALL);
            g2.setColor(TEXT_DIM);
            g2.drawString("Ctrl+1…8 modules · Ctrl+0 home", 12, 14);
            g2.drawString("Ctrl+Enter run · Java " + Runtime.version().feature() + " / Swing", 12, 30);
            g2.dispose();
        }
    }
}
