package ossim.ui;

import ossim.core.CpuScheduler;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Gantt chart drawn to scale. Each process keeps one colour, idle CPU time is hatched, the time axis uses
 * evenly spaced ticks, and hovering a slice shows its start, end and duration.
 */
final class GanttChart extends Chart {
    private static final int LEFT = 24;
    private static final int RIGHT = 24;
    private static final int BAR_Y = 46;
    private static final int BAR_HEIGHT = 52;
    private static final int LEGEND_LIMIT = 16;

    private final List<CpuScheduler.Slice> slices;
    private final int origin;
    private final int end;

    GanttChart(List<CpuScheduler.Slice> slices) {
        super((int) Math.min(60_000L, Math.max(480L, (slices.get(slices.size() - 1).end() - slices.get(0).start()) * 8L)), 190);
        this.slices = slices;
        origin = slices.get(0).start();
        end = slices.get(slices.size() - 1).end();
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        int total = Math.max(1, end - origin);
        double scale = (width - LEFT - RIGHT) / (double) total;
        FontMetrics fm = g.getFontMetrics();

        g.setFont(Theme.SMALL_BOLD);
        label(g, "CPU TIMELINE", LEFT, 22, Theme.Role.TEXT_MUTED);
        g.setFont(Theme.SMALL);
        label(g, slices.size() + " slices · time " + origin + " to " + end, LEFT + 100, 22, Theme.Role.TEXT_FAINT);

        // lane background
        g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
        g.fillRoundRect(LEFT, BAR_Y, width - LEFT - RIGHT, BAR_HEIGHT, 10, 10);

        for (CpuScheduler.Slice s : slices) {
            int x = LEFT + (int) Math.round((s.start() - origin) * scale);
            int w = Math.max(1, (int) Math.round((s.end() - s.start()) * scale));
            Rectangle r = new Rectangle(x, BAR_Y, w, BAR_HEIGHT);
            boolean idle = CpuScheduler.IDLE.equals(s.id());
            if (idle) {
                hatch(g, r, Theme.c(Theme.Role.BORDER_STRONG));
            } else {
                Color fill = Theme.seriesFor(s.id());
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Double(x + 0.5, BAR_Y, Math.max(1, w - 1), BAR_HEIGHT, w > 8 ? 8 : 2, w > 8 ? 8 : 2));
                String text = fit(g, s.id(), w);
                if (text != null) {
                    g.setFont(Theme.BODY_BOLD);
                    g.setColor(Theme.textOn(fill));
                    if (g.getFontMetrics().stringWidth(text) + 6 <= w) {
                        centered(g, text, x + w / 2, BAR_Y + BAR_HEIGHT / 2 + 5);
                    }
                    g.setFont(Theme.SMALL);
                }
            }
            tip(r, "<b>" + (idle ? "CPU idle" : s.id()) + "</b><br>" + s.start() + " → " + s.end()
                    + "<br>duration " + (s.end() - s.start()));
        }

        // axis with nice ticks plus the exact slice boundaries when there is room
        int axisY = BAR_Y + BAR_HEIGHT + 10;
        g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
        g.drawLine(LEFT, axisY, width - RIGHT, axisY);
        boolean boundaries = slices.size() <= 60 && scale * minDuration() >= fm.stringWidth(String.valueOf(end)) + 6;
        if (boundaries) {
            for (CpuScheduler.Slice s : slices) {
                tick(g, LEFT + (int) Math.round((s.start() - origin) * scale), axisY, String.valueOf(s.start()));
            }
            tick(g, LEFT + (int) Math.round((end - origin) * scale), axisY, String.valueOf(end));
        } else {
            double step = niceStep(total, Math.max(2, (width - LEFT - RIGHT) / 80));
            for (double t = Math.ceil(origin / step) * step; t <= end; t += step) {
                tick(g, LEFT + (int) Math.round((t - origin) * scale), axisY, String.valueOf((long) t));
            }
        }

        // legend
        Set<String> ids = new LinkedHashSet<>();
        for (CpuScheduler.Slice s : slices) {
            ids.add(s.id());
        }
        int lx = LEFT;
        int ly = axisY + 44;
        if (ids.size() <= LEGEND_LIMIT) {
            for (String id : ids) {
                boolean idle = CpuScheduler.IDLE.equals(id);
                lx = legend(g, lx, ly, idle ? Theme.c(Theme.Role.SURFACE_ALT) : Theme.seriesFor(id), idle ? "CPU idle" : id, idle);
            }
        } else {
            label(g, ids.size() + " processes · hover a slice for details · scroll sideways for the full timeline",
                    lx, ly, Theme.Role.TEXT_FAINT);
        }
    }

    private int minDuration() {
        int min = Integer.MAX_VALUE;
        for (CpuScheduler.Slice s : slices) {
            min = Math.min(min, s.end() - s.start());
        }
        return min;
    }

    private static void tick(Graphics2D g, int x, int axisY, String text) {
        g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
        g.drawLine(x, axisY, x, axisY + 5);
        g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
        centered(g, text, x, axisY + 19);
    }
}
