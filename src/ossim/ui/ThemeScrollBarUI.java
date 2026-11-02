package ossim.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Thin, flat scrollbar: no arrow buttons, rounded charcoal thumb that turns coral while dragged. */
public final class ThemeScrollBarUI extends BasicScrollBarUI {

    /** Factory called by {@link javax.swing.UIManager} (registered in {@link Theme#install}). */
    public static ComponentUI createUI(JComponent c) {
        return new ThemeScrollBarUI();
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = Theme.LINE;
        trackColor = Theme.INSET;
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
        Dimension zero = new Dimension(0, 0);
        b.setPreferredSize(zero);
        b.setMinimumSize(zero);
        b.setMaximumSize(zero);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(Theme.INSET);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = Theme.smooth(g);
        g2.setColor(isDragging ? Theme.CORAL : isThumbRollover() ? Theme.TEXT_FAINT : Theme.LINE);
        int inset = 2;
        int arc = Math.min(r.width, r.height) - 2 * inset;
        g2.fillRoundRect(r.x + inset, r.y + inset, r.width - 2 * inset, r.height - 2 * inset, arc, arc);
        g2.dispose();
    }
}
