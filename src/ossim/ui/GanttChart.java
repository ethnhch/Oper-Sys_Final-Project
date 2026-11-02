package ossim.ui;

import ossim.core.CpuScheduler;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gantt chart drawn to scale on a dark canvas. It stretches to fill the available width (responsive) and scrolls
 * horizontally only when the schedule is too long to fit; labels are hidden when a slice is too narrow.
 */
final class GanttChart extends JPanel implements Scrollable {
    private static final int MARGIN = 24;
    private static final int BAR_Y = 34;
    private static final int BAR_HEIGHT = 46;
    private static final int MIN_WIDTH = 420;

    private final List<CpuScheduler.Slice> slices;
    private final Map<String, Color> colors = new HashMap<>();
    private final int origin;
    private final int total;
    private final int preferredWidth;

    GanttChart(List<CpuScheduler.Slice> slices) {
        this.slices = slices;
        origin = slices.get(0).start();
        total = Math.max(1, slices.get(slices.size() - 1).end() - origin);
        preferredWidth = (int) Math.min(60_000L, Math.max(MIN_WIDTH, total * 28L));
        for (CpuScheduler.Slice s : slices) {
            if (!CpuScheduler.IDLE.equals(s.id())) {
                colors.computeIfAbsent(s.id(), id -> Theme.SERIES.get(colors.size() % Theme.SERIES.size()));
            }
        }
        setPreferredSize(new Dimension(preferredWidth, 130));
        setBackground(Theme.INSET);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Theme.smooth(g);
        g2.setFont(Theme.SMALL_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        double scale = (getWidth() - 2.0 * MARGIN) / total;

        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("TIME →", MARGIN, 18);

        int axisY = BAR_Y + BAR_HEIGHT + 6;
        g2.setColor(Theme.LINE);
        g2.drawLine(MARGIN, axisY, getWidth() - MARGIN, axisY);
        int lastTickEnd = Integer.MIN_VALUE;
        for (CpuScheduler.Slice s : slices) {
            int x = MARGIN + (int) Math.round((s.start() - origin) * scale);
            int w = Math.max(1, (int) Math.round((s.end() - s.start()) * scale));
            boolean idle = CpuScheduler.IDLE.equals(s.id());
            Color fill = idle ? Theme.SURFACE_2 : colors.get(s.id());
            g2.setColor(fill);
            g2.fillRoundRect(x + 1, BAR_Y, Math.max(1, w - 2), BAR_HEIGHT, 8, 8);
            if (idle) {
                g2.setColor(Theme.LINE);
                g2.setStroke(new BasicStroke(1f));
                for (int hx = x - BAR_HEIGHT; hx < x + w; hx += 8) {
                    g2.drawLine(Math.max(hx, x + 1), BAR_Y + Math.max(0, x + 1 - hx), Math.min(hx + BAR_HEIGHT, x + w - 1),
                            BAR_Y + BAR_HEIGHT - Math.max(0, hx + BAR_HEIGHT - (x + w - 1)));
                }
            }
            if (fm.stringWidth(s.id()) + 8 <= w) {
                g2.setColor(idle ? Theme.TEXT_MUTED : Theme.ON_SERIES);
                g2.drawString(s.id(), x + (w - fm.stringWidth(s.id())) / 2, BAR_Y + BAR_HEIGHT / 2 + fm.getAscent() / 2 - 1);
            }
            String tick = String.valueOf(s.start());
            if (x >= lastTickEnd + 4) {
                g2.setColor(Theme.LINE);
                g2.drawLine(x, axisY - 3, x, axisY + 3);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(tick, x - fm.stringWidth(tick) / 2 + 1, axisY + 18);
                lastTickEnd = x + fm.stringWidth(tick) / 2;
            }
        }
        CpuScheduler.Slice last = slices.get(slices.size() - 1);
        int endX = MARGIN + (int) Math.round((last.end() - origin) * scale);
        String end = String.valueOf(last.end());
        g2.setColor(Theme.CORAL_LIGHT);
        g2.drawLine(endX, axisY - 3, endX, axisY + 3);
        g2.drawString(end, endX - fm.stringWidth(end) / 2, axisY + 18);
        g2.dispose();
    }

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
        return Math.max(24, visible.width - 48);
    }

    /** Fill the viewport when the schedule fits; otherwise keep the to-scale width and scroll. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null && getParent().getWidth() >= preferredWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
    }
}
