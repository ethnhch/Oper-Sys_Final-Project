package ossim.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every chart: themed background, hover tooltips from regions registered while painting,
 * "nice" axis ticks, and scroll behaviour that stretches to the viewport when the chart is narrower than it.
 */
abstract class Chart extends JPanel implements Scrollable {
    static final Stroke DASHED = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
            new float[]{6f, 5f}, 0f);
    static final Stroke HAIRLINE = new BasicStroke(1f);

    private final List<Shape> hitShapes = new ArrayList<>();
    private final List<String> hitTips = new ArrayList<>();

    Chart(int preferredWidth, int preferredHeight) {
        setOpaque(true);
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public Color getBackground() {
        return Theme.c(Theme.Role.SURFACE);
    }

    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        hitShapes.clear();
        hitTips.clear();
        Graphics2D g2 = FlatUI.smooth(g);
        g2.setFont(Theme.SMALL);
        draw(g2, getWidth(), getHeight());
        g2.dispose();
    }

    /** Subclasses paint here; the graphics context is antialiased and already set to the small font. */
    protected abstract void draw(Graphics2D g, int width, int height);

    /** Registers a hover tooltip for a region painted in this frame. Later regions win. */
    protected final void tip(Shape shape, String text) {
        hitShapes.add(shape);
        hitTips.add(text);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = hitShapes.size() - 1; i >= 0; i--) {
            if (hitShapes.get(i).contains(e.getPoint())) {
                return "<html>" + hitTips.get(i) + "</html>";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing helpers

    static double niceStep(double range, int targetTicks) {
        if (range <= 0) {
            return 1;
        }
        double raw = range / Math.max(1, targetTicks);
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double residual = raw / magnitude;
        double nice = residual > 5 ? 10 : residual > 2 ? 5 : residual > 1 ? 2 : 1;
        return Math.max(1, nice * magnitude);
    }

    static void label(Graphics2D g, String text, int x, int y, Theme.Role role) {
        g.setColor(Theme.c(role));
        g.drawString(text, x, y);
    }

    static void centered(Graphics2D g, String text, int cx, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, baseline);
    }

    /** Small legend swatch plus caption; returns the x where the next entry should start. */
    static int legend(Graphics2D g, int x, int y, Color fill, String text, boolean hatched) {
        g.setColor(fill);
        g.fillRoundRect(x, y - 9, 12, 12, 4, 4);
        if (hatched) {
            hatch(g, new Rectangle(x, y - 9, 12, 12), Theme.mix(fill, Color.BLACK, 0.25));
        }
        g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
        g.setFont(Theme.SMALL);
        g.drawString(text, x + 18, y + 1);
        return x + 18 + g.getFontMetrics().stringWidth(text) + 18;
    }

    /** Diagonal hatching clipped to the rectangle, used for idle time and wasted memory. */
    static void hatch(Graphics2D g, Rectangle r, Color color) {
        Shape oldClip = g.getClip();
        Stroke oldStroke = g.getStroke();
        g.clip(r);
        g.setColor(color);
        g.setStroke(HAIRLINE);
        for (int x = r.x - r.height; x < r.x + r.width; x += 6) {
            g.drawLine(x, r.y + r.height, x + r.height, r.y);
        }
        g.setClip(oldClip);
        g.setStroke(oldStroke);
    }

    static String fit(Graphics2D g, String text, int width) {
        FontMetrics fm = g.getFontMetrics();
        return fm.stringWidth(text) + 6 <= width ? text : null;
    }

    // ------------------------------------------------------------------ Scrollable

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? visible.width - 40 : visible.height - 40;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null && getParent().getWidth() > getPreferredSize().width;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }
}
