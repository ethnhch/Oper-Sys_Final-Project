package ossim.ui;

import ossim.core.CpuScheduler;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Gantt chart drawn to scale; labels are hidden when a slice is too narrow to hold them. */
final class GanttChart extends JPanel {
    private static final int MARGIN = 20;
    private static final int BAR_Y = 24;
    private static final int BAR_HEIGHT = 44;

    private final List<CpuScheduler.Slice> slices;
    private final int origin;
    private final double scale;

    GanttChart(List<CpuScheduler.Slice> slices) {
        this.slices = slices;
        origin = slices.get(0).start();
        int total = slices.get(slices.size() - 1).end() - origin;
        int width = (int) Math.min(60_000L, Math.max(700L, total * 28L));
        scale = (width - 2.0 * MARGIN) / total;
        setPreferredSize(new Dimension(width, 100));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics();
        for (CpuScheduler.Slice s : slices) {
            int x = MARGIN + (int) Math.round((s.start() - origin) * scale);
            int w = Math.max(1, (int) Math.round((s.end() - s.start()) * scale));
            g2.setColor(CpuScheduler.IDLE.equals(s.id()) ? new Color(225, 225, 225) : colorFor(s.id()));
            g2.fillRect(x, BAR_Y, w, BAR_HEIGHT);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(x, BAR_Y, w, BAR_HEIGHT);
            if (fm.stringWidth(s.id()) + 6 <= w) {
                g2.drawString(s.id(), x + (w - fm.stringWidth(s.id())) / 2, BAR_Y + BAR_HEIGHT / 2 + 5);
            }
            String tick = String.valueOf(s.start());
            if (fm.stringWidth(tick) + 4 <= w) {
                g2.drawString(tick, x, BAR_Y + BAR_HEIGHT + 16);
            }
        }
        CpuScheduler.Slice last = slices.get(slices.size() - 1);
        int endX = MARGIN + (int) Math.round((last.end() - origin) * scale);
        g2.drawString(String.valueOf(last.end()), endX - 8, BAR_Y + BAR_HEIGHT + 16);
    }

    private static Color colorFor(String id) {
        float hue = ((id.hashCode() & 0x7fffffff) % 360) / 360f;
        return Color.getHSBColor(hue, 0.35f, 0.95f);
    }
}
