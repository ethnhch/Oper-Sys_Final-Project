package ossim.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.Icon;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/** Themes the input widgets (text fields, drop-downs, check boxes) that the module panels create. */
final class ThemeFields {
    private ThemeFields() { }

    /** Applies the matching style to a component if it is one of the supported input widgets. */
    static void style(Component c) {
        if (c instanceof JTextField field) {
            field(field);
        } else if (c instanceof JComboBox<?> combo) {
            combo(combo);
        } else if (c instanceof JCheckBox box) {
            checkBox(box);
        }
    }

    static void field(JTextField field) {
        field.setUI(new BasicTextFieldUI());
        field.setOpaque(true);
        field.setBackground(Theme.INSET);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.NEON_LIGHT);
        field.setSelectionColor(Theme.NEON);
        field.setSelectedTextColor(Theme.ON_NEON);
        field.setFont(Theme.BODY);
        field.setBorder(new FieldBorder(field));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.repaint();
                field.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.repaint();
            }
        });
        Dimension d = field.getPreferredSize();
        field.setPreferredSize(new Dimension(Math.max(d.width, 64), 32));
    }

    static void combo(JComboBox<?> combo) {
        combo.setUI(new ComboUI());
        combo.setOpaque(true);
        combo.setBackground(Theme.INSET);
        combo.setForeground(Theme.TEXT);
        combo.setFont(Theme.BODY);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(new FieldBorder(combo));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, false);
                list.setBackground(Theme.SURFACE_2);
                setBorder(Theme.padding(5, 8, 5, 8));
                setFont(Theme.BODY);
                setOpaque(index >= 0);
                setBackground(selected ? Theme.NEON : Theme.SURFACE_2);
                setForeground(selected && index >= 0 ? Theme.ON_NEON : Theme.TEXT);
                return this;
            }
        });
        combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                combo.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                combo.repaint();
            }
        });
        Dimension d = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(Math.max(d.width + 12, 120), 32));
    }

    static void checkBox(JCheckBox box) {
        box.setOpaque(false);
        box.setForeground(Theme.TEXT_MUTED);
        box.setFont(Theme.BODY);
        box.setFocusPainted(false);
        box.setIconTextGap(8);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.setIcon(new CheckIcon());
    }

    /** Rounded inset field; the outline turns neon while focused. */
    private static final class FieldBorder extends javax.swing.border.AbstractBorder {
        private final JComponent owner;

        FieldBorder(JComponent owner) {
            this.owner = owner;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = Theme.smooth(g);
            // mask the square corners of the opaque field with the card color so the field looks rounded
            java.awt.geom.Area corners = new java.awt.geom.Area(new Rectangle(x, y, w, h));
            corners.subtract(new java.awt.geom.Area(new java.awt.geom.RoundRectangle2D.Float(x, y, w - 1, h - 1, 10, 10)));
            g2.setColor(Theme.CHARCOAL);
            g2.fill(corners);
            boolean focused = owner.isFocusOwner() || owner instanceof JComboBox<?> cb && cb.isPopupVisible();
            g2.setColor(focused ? Theme.NEON : Theme.LINE);
            g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(5, 10, 5, 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(5, 10, 5, 8);
            return insets;
        }
    }

    /** Flat drop-down: chevron arrow, charcoal popup list with neon selection. */
    private static final class ComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Theme.smooth(g);
                    g2.setColor(comboBox.isPopupVisible() ? Theme.NEON_LIGHT : Theme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    if (comboBox.isPopupVisible()) {
                        g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy + 2, cy - 2, cy + 2}, 3);
                    } else {
                        g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    }
                    g2.dispose();
                }
            };
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setFocusable(false);
            arrow.setOpaque(false);
            arrow.setPreferredSize(new Dimension(24, 24));
            return arrow;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // transparent: the rounded field border paints the background
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Object value = comboBox.getSelectedItem();
            Graphics2D g2 = Theme.smooth(g);
            g2.setFont(comboBox.getFont());
            g2.setColor(comboBox.isEnabled() ? Theme.TEXT : Theme.TEXT_FAINT);
            String text = value == null ? "" : value.toString();
            int y = bounds.y + (bounds.height + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(text, bounds.x + 2, y);
            g2.dispose();
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox);
            popup.setBorder(Theme.hairline(1, 1, 1, 1, false));
            popup.getList().setBackground(Theme.SURFACE_2);
            popup.getList().setSelectionBackground(Theme.NEON);
            popup.getList().setSelectionForeground(Theme.ON_NEON);
            return popup;
        }
    }

    /** Rounded check box drawn in the palette (neon when checked). */
    private static final class CheckIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            boolean on = c instanceof AbstractButton b && b.isSelected();
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(on ? Theme.NEON : Theme.INSET);
            g2.fillRoundRect(x, y, 16, 16, 5, 5);
            g2.setColor(on ? Theme.NEON : Theme.LINE);
            g2.drawRoundRect(x, y, 15, 15, 5, 5);
            if (on) {
                g2.setColor(Theme.ON_NEON);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 4, x + 7, x + 12}, new int[]{y + 8, y + 11, y + 5}, 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
