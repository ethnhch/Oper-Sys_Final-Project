package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Left navigation ("main menu"): grouped module entries with numbered badges. In compact mode (narrow windows)
 * only the badges remain, with the module name as a tooltip.
 */
final class Sidebar extends JPanel {
    static final int EXPANDED_WIDTH = 236;
    static final int COMPACT_WIDTH = 68;

    private final List<Item> items = new ArrayList<>();
    private final List<JComponent> groupLabels = new ArrayList<>();
    private final JPanel list = new JPanel();
    private final IntConsumer onSelect;
    private final JComponent footer;
    private int selected = -1;
    private boolean compact;
    private boolean keyboardFocus;

    Sidebar(IntConsumer onSelect) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setBackground(Theme.CHARCOAL);
        setBorder(Theme.hairline(0, 0, 0, 1, true));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(Theme.padding(10, 10, 10, 10));
        add(list, BorderLayout.NORTH);
        footer = Theme.label("Mini OS Simulator · Java Swing", Theme.SMALL, Theme.TEXT_FAINT);
        footer.setBorder(Theme.padding(10, 20, 14, 10));
        add(footer, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));
    }

    void addGroup(String name) {
        JComponent label = Theme.caption(name);
        label.setForeground(Theme.TEXT_FAINT);
        label.setBorder(Theme.padding(list.getComponentCount() == 0 ? 6 : 16, 10, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        groupLabels.add(label);
        list.add(label);
    }

    void addItem(String name) {
        Item item = new Item(items.size(), name);
        items.add(item);
        list.add(item);
        list.add(Box.createVerticalStrut(2));
    }

    void select(int index) {
        selected = index;
        items.forEach(Component::repaint);
    }

    boolean isCompact() {
        return compact;
    }

    void setCompact(boolean value) {
        compact = value;
        for (JComponent label : groupLabels) {
            label.setVisible(!value);
        }
        footer.setVisible(!value);
        for (Item item : items) {
            item.setToolTipText(value ? item.name : null);
        }
        setPreferredSize(new Dimension(value ? COMPACT_WIDTH : EXPANDED_WIDTH, 0));
        revalidate();
        repaint();
    }

    /** One clickable, keyboard-focusable menu entry. */
    private final class Item extends JComponent {
        private final int index;
        private final String name;
        private boolean hover;

        Item(int index, String name) {
            this.index = index;
            this.name = name;
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setName(name);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelect.accept(Item.this.index);
                }

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
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    keyboardFocus = true;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> onSelect.accept(Item.this.index);
                        case KeyEvent.VK_DOWN -> focusItem(Item.this.index + 1);
                        case KeyEvent.VK_UP -> focusItem(Item.this.index - 1);
                        default -> { }
                    }
                }
            });
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    // show the focus ring only for keyboard traversal, not for the initial window focus
                    keyboardFocus = e.getCause() == java.awt.event.FocusEvent.Cause.TRAVERSAL_FORWARD
                            || e.getCause() == java.awt.event.FocusEvent.Cause.TRAVERSAL_BACKWARD
                            || e.getCause() == java.awt.event.FocusEvent.Cause.TRAVERSAL
                            || e.getCause() == java.awt.event.FocusEvent.Cause.UNKNOWN && keyboardFocus;
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
        }

        private void focusItem(int i) {
            if (i >= 0 && i < items.size()) {
                items.get(i).requestFocusInWindow();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(compact ? COMPACT_WIDTH - 20 : EXPANDED_WIDTH - 20, 40);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            boolean active = index == selected;
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setColor(Theme.CORAL_TINT);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(Theme.CORAL);
                g2.fillRoundRect(0, 8, 3, h - 16, 3, 3);
            } else if (hover) {
                g2.setColor(Theme.SURFACE_2);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }
            if (isFocusOwner() && keyboardFocus) {
                g2.setColor(Theme.alpha(Theme.CORAL_LIGHT, 170));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            // numbered badge
            String number = String.format("%02d", index + 1);
            int badge = 28;
            int bx = compact ? (w - badge) / 2 : 10;
            int by = (h - badge) / 2;
            g2.setColor(active ? Theme.CORAL : Theme.SURFACE_2);
            g2.fillRoundRect(bx, by, badge, badge, 9, 9);
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(active ? Theme.ON_CORAL : Theme.TEXT_MUTED);
            g2.drawString(number, bx + (badge - fm.stringWidth(number)) / 2, by + (badge + fm.getAscent() - fm.getDescent()) / 2);
            if (!compact) {
                g2.setFont(active ? Theme.BODY_BOLD : Theme.BODY);
                fm = g2.getFontMetrics();
                g2.setColor(active ? Theme.CORAL_LIGHT : hover ? Theme.TEXT : Theme.TEXT_MUTED);
                g2.drawString(name, bx + badge + 12, (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }
}
