package ossim.ui;

import ossim.core.Multiprogramming;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Multiprogramming system status over time: a CPU lane showing which program runs, a step chart of memory in use
 * against total memory, and a lane counting programs that wait for memory. Hover any moment for the details.
 */
final class MemoryTimeline extends Chart {
    private static final int LEFT = 70;
    private static final int RIGHT = 24;
    private static final int CPU_Y = 40;
    private static final int CPU_H = 30;
    private static final int MEM_Y = 112;
    private static final int MEM_H = 170;

    private final Multiprogramming.Result result;
    private final int start;
    private final int end;

    MemoryTimeline(Multiprogramming.Result result) {
        super(timelineWidth(result), MEM_Y + MEM_H + 96);
        this.result = result;
        List<Multiprogramming.Snapshot> t = result.timeline();
        start = t.isEmpty() ? 0 : t.get(0).start();
        end = t.isEmpty() ? 1 : Math.max(start + 1, t.get(t.size() - 1).end());
    }

    private static int timelineWidth(Multiprogramming.Result r) {
        List<Multiprogramming.Snapshot> t = r.timeline();
        long span = t.isEmpty() ? 1 : t.get(t.size() - 1).end() - t.get(0).start();
        return (int) Math.min(40_000L, Math.max(560L, span * 8L));
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        int plotW = width - LEFT - RIGHT;
        double scale = plotW / (double) (end - start);
        int total = result.totalMemory();

        g.setFont(Theme.SMALL_BOLD);
        label(g, "CPU", 14, CPU_Y + 20, Theme.Role.TEXT_MUTED);
        label(g, "MEMORY IN USE", 14, MEM_Y - 12, Theme.Role.TEXT_MUTED);
        g.setFont(Theme.SMALL);
        label(g, "of " + Ui.grouped(total) + " units", 14 + g.getFontMetrics(Theme.SMALL_BOLD).stringWidth("MEMORY IN USE") + 10,
                MEM_Y - 12, Theme.Role.TEXT_FAINT);
        label(g, "SYSTEM STATUS OVER TIME", LEFT, 22, Theme.Role.TEXT_FAINT);

        g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
        g.fillRoundRect(LEFT, CPU_Y, plotW, CPU_H, 8, 8);
        g.fillRoundRect(LEFT, MEM_Y, plotW, MEM_H, 8, 8);

        // gridlines at 25 % steps
        for (int p = 0; p <= 100; p += 25) {
            int y = MEM_Y + MEM_H - (int) Math.round(p / 100.0 * MEM_H);
            g.setColor(Theme.c(Theme.Role.GRID));
            g.drawLine(LEFT, y, width - RIGHT, y);
            g.setColor(Theme.c(Theme.Role.TEXT_FAINT));
            g.drawString(p + "%", LEFT - 34, y + 4);
        }

        Path2D area = new Path2D.Double();
        area.moveTo(LEFT, MEM_Y + MEM_H);
        int waitingY = MEM_Y + MEM_H + 22;
        int maxWaiting = 1;
        for (Multiprogramming.Snapshot s : result.timeline()) {
            maxWaiting = Math.max(maxWaiting, s.waiting().size());
        }
        for (Multiprogramming.Snapshot s : result.timeline()) {
            int x0 = LEFT + (int) Math.round((s.start() - start) * scale);
            int x1 = LEFT + (int) Math.round((s.end() - start) * scale);
            int w = Math.max(1, x1 - x0);
            boolean idle = Multiprogramming.IDLE.equals(s.running());
            Rectangle cpu = new Rectangle(x0, CPU_Y + 2, w, CPU_H - 4);
            if (idle) {
                hatch(g, cpu, Theme.c(Theme.Role.BORDER_STRONG));
            } else {
                Color fill = Theme.seriesFor(s.running());
                g.setColor(fill);
                g.fillRect(cpu.x, cpu.y, Math.max(1, cpu.width - 1), cpu.height);
                String text = fit(g, s.running(), w);
                if (text != null) {
                    g.setColor(Theme.textOn(fill));
                    g.setFont(Theme.SMALL_BOLD);
                    centered(g, text, x0 + w / 2, CPU_Y + 20);
                    g.setFont(Theme.SMALL);
                }
            }
            double y = MEM_Y + MEM_H - (double) s.memoryUsed() / total * MEM_H;
            area.lineTo(x0, y);
            area.lineTo(x1, y);

            if (!s.waiting().isEmpty()) {
                int bh = (int) Math.round(18.0 * s.waiting().size() / maxWaiting);
                g.setColor(Theme.mix(Theme.c(Theme.Role.WARNING), Theme.c(Theme.Role.SURFACE), 0.3));
                g.fillRect(x0, waitingY + 18 - bh, Math.max(1, w - 1), bh);
            }
            tip(new Rectangle(x0, CPU_Y, w, waitingY + 20 - CPU_Y), "<b>t = " + s.start() + " – " + s.end() + "</b><br>"
                    + "running: " + s.running() + "<br>in memory (" + s.resident().size() + "): "
                    + abbreviate(s.resident()) + "<br>memory " + s.memoryUsed() + " / " + total + " ("
                    + Ui.two(s.memoryUsed() * 100.0 / total) + " %)<br>waiting for memory: "
                    + (s.waiting().isEmpty() ? "none" : abbreviate(s.waiting())));
        }
        area.lineTo(LEFT + plotW, MEM_Y + MEM_H);
        area.closePath();
        g.setColor(Theme.mix(Theme.c(Theme.Role.ACCENT), Theme.c(Theme.Role.SURFACE), 0.7));
        g.fill(area);
        g.setColor(Theme.c(Theme.Role.ACCENT));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(area);
        g.setStroke(HAIRLINE);

        g.setFont(Theme.SMALL_BOLD);
        label(g, "WAITING", 14, waitingY + 15, Theme.Role.WARNING);

        // time axis
        int axisY = waitingY + 30;
        g.setFont(Theme.SMALL);
        g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
        g.drawLine(LEFT, axisY, width - RIGHT, axisY);
        double step = niceStep(end - start, Math.max(2, plotW / 80));
        for (double t = Math.ceil(start / step) * step; t <= end; t += step) {
            int x = LEFT + (int) Math.round((t - start) * scale);
            g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
            g.drawLine(x, axisY, x, axisY + 4);
            g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
            centered(g, String.valueOf((long) t), x, axisY + 17);
        }
        int ly = axisY + 40;
        int lx = legend(g, LEFT, ly, Theme.series(0), "Program on CPU", false);
        lx = legend(g, lx, ly, Theme.mix(Theme.c(Theme.Role.ACCENT), Theme.c(Theme.Role.SURFACE), 0.7), "Memory in use", false);
        legend(g, lx, ly, Theme.mix(Theme.c(Theme.Role.WARNING), Theme.c(Theme.Role.SURFACE), 0.3),
                "Waiting for memory", false);
    }

    private static String abbreviate(List<String> ids) {
        return ids.isEmpty() ? "-" : Ui.preview(ids, 8, ", ");
    }
}
