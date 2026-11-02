package ossim.ui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/** Rounded surface (charcoal in dark mode, white in light mode) with a hairline outline: the building block of every screen. */
class Card extends JPanel {
    Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(Theme.padding(14, 16, 14, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(Theme.CHARCOAL);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS, Theme.RADIUS);
        g2.setColor(Theme.LINE_SOFT);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS, Theme.RADIUS);
        g2.dispose();
    }
}
