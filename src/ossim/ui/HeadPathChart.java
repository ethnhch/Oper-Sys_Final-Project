package ossim.ui;

import ossim.core.DiskScheduler;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Head path: cylinders on the horizontal axis, service order flowing downward. Serves are solid accent lines,
 * travel to the disk edge is muted and return jumps are dashed red. Hover a point for the step details.
 */
final class HeadPathChart extends Chart {
    private static final int LEFT = 56;
    private static final int RIGHT = 36;
    private static final int TOP = 64;
    private static final int LABEL_LIMIT = 40;

    private final int head;
    private final List<DiskScheduler.Move> moves;
    private final int maxCylinder;

    HeadPathChart(int head, List<DiskScheduler.Move> moves, int maxCylinder) {
        super(560, (int) Math.min(40_000L, TOP + 50L + Math.max(1, moves.size()) * (moves.size() <= LABEL_LIMIT ? 30L : 12L)));
        this.head = head;
        this.moves = moves;
        this.maxCylinder = maxCylinder;
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        int plotW = width - LEFT - RIGHT;
        double row = Math.max(1.0, (height - TOP - 40.0) / Math.max(1, moves.size()));
        boolean labels = moves.size() <= LABEL_LIMIT;

        // header and legend
        g.setFont(Theme.SMALL_BOLD);
        label(g, "CYLINDER", LEFT, 18, Theme.Role.TEXT_MUTED);
        g.setFont(Theme.SMALL);
        int lx = width - RIGHT - 330;
        lx = legend(g, Math.max(LEFT + 90, lx), 18, Theme.c(Theme.Role.ACCENT), "Serve request", false);
        lx = legend(g, lx, 18, Theme.c(Theme.Role.TEXT_FAINT), "To disk edge", false);
        legend(g, lx, 18, Theme.c(Theme.Role.DANGER), "Return jump", false);

        // vertical grid + axis labels
        double step = niceStep(maxCylinder, Math.max(2, plotW / 90));
        for (double c = 0; c <= maxCylinder; c += step) {
            int x = (int) x(c, plotW);
            g.setColor(Theme.c(Theme.Role.GRID));
            g.drawLine(x, TOP - 10, x, height - 24);
            g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
            centered(g, String.valueOf((long) c), x, TOP - 18);
        }
        g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
        g.drawLine(LEFT, TOP - 10, width - RIGHT, TOP - 10);
        int edge = (int) x(maxCylinder, plotW);
        g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
        centered(g, String.valueOf(maxCylinder), edge, TOP - 18);

        // order axis on the left
        g.setColor(Theme.c(Theme.Role.TEXT_FAINT));
        g.drawString("start", 10, TOP + 4);

        double fromX = x(head, plotW);
        double fromY = TOP;
        int stepNo = 0;
        for (DiskScheduler.Move move : moves) {
            stepNo++;
            double toX = x(move.to(), plotW);
            double toY = TOP + stepNo * row;
            switch (move.kind()) {
                case JUMP -> {
                    g.setStroke(DASHED);
                    g.setColor(Theme.c(Theme.Role.DANGER));
                }
                case EDGE -> {
                    g.setStroke(new BasicStroke(1.6f));
                    g.setColor(Theme.c(Theme.Role.TEXT_FAINT));
                }
                default -> {
                    g.setStroke(new BasicStroke(labels ? 2.2f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(Theme.c(Theme.Role.ACCENT));
                }
            }
            g.draw(new Line2D.Double(fromX, fromY, toX, toY));
            if (labels) {
                g.setColor(Theme.c(Theme.Role.TEXT_FAINT));
                g.drawString(String.valueOf(stepNo), 14, (int) toY + 4);
            }
            fromX = toX;
            fromY = toY;
        }

        // points on top of the lines
        g.setStroke(HAIRLINE);
        point(g, x(head, plotW), TOP, "Head " + head, labels, Theme.c(Theme.Role.TEXT), true, false);
        tip(new Ellipse2D.Double(x(head, plotW) - 7, TOP - 7, 14, 14), "<b>Initial head</b><br>cylinder " + head);
        stepNo = 0;
        for (DiskScheduler.Move move : moves) {
            stepNo++;
            double px = x(move.to(), plotW);
            double py = TOP + stepNo * row;
            boolean serve = move.kind() == DiskScheduler.Kind.SERVE;
            point(g, px, py, String.valueOf(move.to()), labels,
                    serve ? Theme.c(Theme.Role.ACCENT) : move.kind() == DiskScheduler.Kind.JUMP
                            ? Theme.c(Theme.Role.DANGER) : Theme.c(Theme.Role.TEXT_FAINT), false, move.from() > move.to());
            tip(new Ellipse2D.Double(px - 7, py - 7, 14, 14), "<b>Step " + stepNo + "</b><br>" + move.from() + " → "
                    + move.to() + "<br>distance " + move.distance() + "<br>"
                    + (serve ? "serve request" : move.kind() == DiskScheduler.Kind.JUMP ? "return jump" : "travel to edge"));
        }
    }

    private void point(Graphics2D g, double x, double y, String text, boolean showLabel, Color color, boolean start,
                       boolean labelLeft) {
        double r = showLabel ? (start ? 6 : 4.5) : 2;
        g.setColor(Theme.c(Theme.Role.SURFACE));
        g.fill(new Ellipse2D.Double(x - r - 1.5, y - r - 1.5, 2 * r + 3, 2 * r + 3));
        g.setColor(color);
        g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        if (showLabel) {
            g.setFont(start ? Theme.SMALL_BOLD : Theme.SMALL);
            g.setColor(Theme.c(Theme.Role.TEXT));
            int tw = g.getFontMetrics().stringWidth(text);
            g.drawString(text, labelLeft ? (int) (x - r - 6 - tw) : (int) (x + r + 5), (int) (y + 4));
            g.setFont(Theme.SMALL);
        }
    }

    private double x(double cylinder, int plotW) {
        return LEFT + cylinder * plotW / Math.max(1, maxCylinder);
    }
}
