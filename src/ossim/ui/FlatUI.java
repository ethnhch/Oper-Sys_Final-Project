package ossim.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Flat UI delegates that replace the dated parts of the Metal look and feel: underline tabs, thin scroll bars,
 * a borderless combo box, a hairline split divider and a rounded check box. Registered in {@link Theme#install()}.
 */
public final class FlatUI {
    private FlatUI() { }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** Underline-style tabs: muted labels, the selected one in the accent colour with a 2px bar. */
    public static final class Tabs extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new Tabs();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(9, 12, 9, 12);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(0, 2, 0, 2);
            tabPane.setOpaque(false);
        }

        @Override
        protected int calculateTabHeight(int placement, int index, int fontHeight) {
            return fontHeight + 20;
        }

        @Override
        protected int calculateTabWidth(int placement, int index, FontMetrics metrics) {
            return super.calculateTabWidth(placement, index, metrics) + 6;
        }

        @Override
        protected void paintTabArea(Graphics g, int placement, int selectedIndex) {
            Rectangle area = new Rectangle(0, 0, tabPane.getWidth(), calculateTabAreaHeight(placement, runCount, maxTabHeight));
            g.setColor(Theme.c(Theme.Role.BORDER));
            g.fillRect(area.x, area.y + area.height - 1, area.width, 1);
            super.paintTabArea(g, placement, selectedIndex);
        }

        @Override
        protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) {
            if (getRolloverTab() == index && !selected) {
                Graphics2D g2 = smooth(g);
                g2.setColor(Theme.c(Theme.Role.SURFACE_ALT));
                g2.fillRoundRect(x + 2, y + 4, w - 4, h - 8, 8, 8);
                g2.dispose();
            }
        }

        @Override
        protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) {
            if (selected) {
                g.setColor(Theme.c(Theme.Role.ACCENT));
                g.fillRect(x + 6, y + h - 2, w - 12, 2);
            }
        }

        @Override
        protected void paintText(Graphics g, int placement, java.awt.Font font, FontMetrics metrics, int index,
                                 String title, Rectangle textRect, boolean selected) {
            Graphics2D g2 = smooth(g);
            g2.setFont(selected ? Theme.BODY_BOLD : Theme.BODY);
            g2.setColor(selected ? Theme.c(Theme.Role.ACCENT) : Theme.c(Theme.Role.TEXT_MUTED));
            g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index,
                                           Rectangle iconRect, Rectangle textRect, boolean selected) {
            // focus is shown by the accent underline
        }

        @Override
        protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            // content sits flush inside the surrounding card
        }
    }

    /** Thin rounded scroll bar thumb without arrow buttons. */
    public static final class ScrollBar extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) {
            return new ScrollBar();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            scrollbar.setOpaque(false);
            scrollBarWidth = 11;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private static JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(isThumbRollover() || isDragging ? Theme.c(Theme.Role.TEXT_FAINT) : Theme.c(Theme.Role.BORDER_STRONG));
            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            int pad = 3;
            if (vertical) {
                g2.fillRoundRect(r.x + pad, r.y + 2, r.width - 2 * pad, r.height - 4, 6, 6);
            } else {
                g2.fillRoundRect(r.x + 2, r.y + pad, r.width - 4, r.height - 2 * pad, 6, 6);
            }
            g2.dispose();
        }
    }

    /** Combo box drawn as a rounded field with a chevron. */
    public static final class Combo extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) {
            return new Combo();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            comboBox.setBorder(new Ui.RoundBorder(comboBox, false));
            comboBox.setOpaque(false);
            comboBox.setFont(Theme.BODY);
        }

        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton();
            b.setUI(new BasicButtonUI());
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setFocusable(false);
            b.setIcon(Icons.of(Icons.Kind.CHEVRON_DOWN, 12, Theme.Role.TEXT_MUTED));
            return b;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = smooth(g);
            g2.setColor(Theme.c(Theme.Role.SURFACE));
            g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 8, 8);
            g2.dispose();
            super.paint(g, c);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // the rounded fill painted in paint() is the background
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Object value = comboBox.getSelectedItem();
            Graphics2D g2 = smooth(g);
            g2.setFont(comboBox.getFont());
            g2.setColor(comboBox.isEnabled() ? Theme.c(Theme.Role.TEXT) : Theme.c(Theme.Role.TEXT_FAINT));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(value == null ? "" : value.toString(), bounds.x + 2,
                    bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Hairline split-pane divider. */
    public static final class Split extends BasicSplitPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new Split();
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                @Override
                public void paint(Graphics g) {
                    g.setColor(Theme.c(Theme.Role.BG));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    Graphics2D g2 = smooth(g);
                    g2.setColor(Theme.c(Theme.Role.BORDER_STRONG));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    for (int i = -1; i <= 1; i++) {
                        g2.fillOval(cx - 1, cy + i * 6 - 1, 3, 3);
                    }
                    g2.dispose();
                }

                @Override
                public void setBorder(javax.swing.border.Border b) {
                    // no border
                }
            };
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            splitPane.setDividerSize(Theme.GAP);
            splitPane.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    /** Rounded check box with an accent fill when selected. */
    public static final class Check extends BasicCheckBoxUI {
        public static ComponentUI createUI(JComponent c) {
            return new Check();
        }

        @Override
        protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);
            b.setOpaque(false);
            b.setIconTextGap(8);
            icon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    AbstractButton button = (AbstractButton) c;
                    Graphics2D g2 = smooth(g);
                    boolean on = button.isSelected();
                    g2.setColor(on ? Theme.c(Theme.Role.ACCENT) : Theme.c(Theme.Role.SURFACE));
                    g2.fillRoundRect(x, y, 16, 16, 5, 5);
                    g2.setColor(on ? Theme.c(Theme.Role.ACCENT) : Theme.c(Theme.Role.BORDER_STRONG));
                    g2.drawRoundRect(x, y, 15, 15, 5, 5);
                    if (on) {
                        g2.setColor(Theme.c(Theme.Role.ON_ACCENT));
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
            };
        }

        @Override
        public Icon getDefaultIcon() {
            return icon;
        }
    }
}
