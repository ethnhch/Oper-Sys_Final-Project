package ossim.ui;

import javax.swing.LookAndFeel;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

/** Result tabs as flat "pills": muted labels, the active tab filled with a neon tint and a neon underline. */
final class ThemeTabsUI extends BasicTabbedPaneUI {

    @Override
    protected void installDefaults() {
        LookAndFeel.installColorsAndFont(tabPane, "TabbedPane.background", "TabbedPane.foreground", "TabbedPane.font");
        tabPane.setFont(Theme.BODY_BOLD);
        tabPane.setBackground(Theme.CHARCOAL);
        tabPane.setForeground(Theme.TEXT_MUTED);
        tabPane.setOpaque(false);
        highlight = Theme.LINE;
        lightHighlight = Theme.LINE;
        shadow = Theme.LINE;
        darkShadow = Theme.LINE;
        focus = Theme.NEON;
        textIconGap = 4;
        tabRunOverlay = 0;
        tabInsets = new Insets(7, 14, 7, 14);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 8, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    /** Chevron buttons for the single scrolling tab row used when the card is narrow. */
    @Override
    protected javax.swing.JButton createScrollButton(int direction) {
        return new javax.swing.plaf.basic.BasicArrowButton(direction, Theme.CHARCOAL, Theme.LINE, Theme.LINE, Theme.LINE) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = Theme.smooth(g);
                g2.setColor(Theme.CHARCOAL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(isEnabled() ? (getModel().isRollover() ? Theme.NEON_LIGHT : Theme.TEXT_MUTED) : Theme.LINE);
                g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int d = getDirection() == WEST ? 1 : -1;
                g2.drawPolyline(new int[]{cx + 2 * d, cx - 2 * d, cx + 2 * d}, new int[]{cy - 4, cy, cy + 4}, 3);
                g2.dispose();
            }

            @Override
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(22, 30);
            }
        };
    }

    @Override
    protected int calculateTabHeight(int placement, int index, int fontHeight) {
        return fontHeight + tabInsets.top + tabInsets.bottom + 4;
    }

    @Override
    protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) {
        Graphics2D g2 = Theme.smooth(g);
        if (selected) {
            g2.setColor(Theme.NEON_TINT);
            g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 10, 10);
            g2.setColor(Theme.NEON);
            g2.fillRoundRect(x + 10, y + h - 3, w - 20, 3, 3, 3);
        } else if (index == getRolloverTab()) {
            g2.setColor(Theme.SURFACE_2);
            g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, 10, 10);
        }
        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics, int index, String title,
                             Rectangle textRect, boolean selected) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setFont(font);
        g2.setColor(selected ? Theme.NEON_LIGHT : index == getRolloverTab() ? Theme.TEXT : Theme.TEXT_MUTED);
        g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected) {
        // flat design: no tab outline
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index, Rectangle iconRect,
                                       Rectangle textRect, boolean selected) {
        // the neon underline already marks the active tab
    }

    @Override
    protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
        Rectangle area = getTabBounds(tabPane, 0);
        int y = area == null ? 0 : maxTabBottom();
        g.setColor(Theme.LINE_SOFT);
        g.drawLine(0, y, tabPane.getWidth(), y);
    }

    private int maxTabBottom() {
        int bottom = 0;
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            Rectangle r = getTabBounds(tabPane, i);
            bottom = Math.max(bottom, r.y + r.height);
        }
        return bottom + 3;
    }
}
