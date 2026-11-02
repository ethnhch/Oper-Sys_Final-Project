package ossim.ui;

import ossim.core.DiskScheduler;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

/**
 * Head path: cylinders on the horizontal axis, time (movement order) flowing downward. Served moves are coral,
 * return jumps are dashed amber. The chart always fits the available width (responsive) and scrolls vertically.
 */
final class HeadPathChart extends JPanel implements Scrollable {
    private static final int MARGIN = 44;
    private static final int TOP = 54;
    private static final int ROW = 18;
    private static final int LABEL_LIMIT = 60;
    private static final int MIN_WIDTH = 380;

    private final int head;
    private final List<DiskScheduler.Move> moves;
    private final int maxCylinder;

    HeadPathChart(int head, List<DiskScheduler.Move> moves, int maxCylinder) {
        this.head = head;
        this.moves = moves;
        this.maxCylinder = maxCylinder;
        setPreferredSize(new Dimension(MIN_WIDTH, (int) Math.min(40_000L, TOP + 30L + (moves.size() + 1L) * ROW)));
        setBackground(Theme.INSET);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = Theme.smooth(g);
        g2.setFont(Theme.SMALL);
        FontMetrics fm = g2.getFontMetrics();
        Stroke solid = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke dashed = new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f);
        int bottom = TOP + (moves.size() + 1) * ROW;

        // axis with quarter ticks and faint vertical guides
        g2.setColor(Theme.LINE);
        g2.drawLine(MARGIN, 34, getWidth() - MARGIN, 34);
        for (int q = 0; q <= 4; q++) {
            int cylinder = (int) Math.round(maxCylinder * q / 4.0);
            int x = x(cylinder);
            g2.setColor(Theme.LINE_SOFT);
            g2.drawLine(x, 38, x, bottom);
            g2.setColor(Theme.TEXT_MUTED);
            String label = String.valueOf(cylinder);
            g2.drawString(label, x - fm.stringWidth(label) / 2, 26);
        }
        legend(g2, fm);

        int fromX = x(head);
        int fromY = TOP;
        boolean labels = moves.size() <= LABEL_LIMIT;
        for (DiskScheduler.Move move : moves) {
            int toX = x(move.to());
            int toY = fromY + ROW;
            boolean jump = move.kind() == DiskScheduler.Kind.JUMP;
            g2.setStroke(jump ? dashed : solid);
            g2.setColor(jump ? Theme.WARNING : Theme.CORAL);
            g2.drawLine(fromX, fromY, toX, toY);
            fromX = toX;
            fromY = toY;
        }
        // points on top of the lines
        fromY = TOP;
        dot(g2, fm, x(head), fromY, "head " + head, labels, true);
        for (DiskScheduler.Move move : moves) {
            fromY += ROW;
            dot(g2, fm, x(move.to()), fromY, String.valueOf(move.to()), labels, false);
        }
        g2.dispose();
    }

    private void legend(Graphics2D g2, FontMetrics fm) {
        int x = getWidth() - MARGIN - 190;
        if (x < MARGIN + 60) {
            return;
        }
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Theme.CORAL);
        g2.drawLine(x, 10, x + 18, 10);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("head movement", x + 24, 14);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f));
        g2.setColor(Theme.WARNING);
        g2.drawLine(x + 120, 10, x + 138, 10);
        g2.setColor(Theme.TEXT_MUTED);
        g2.drawString("jump", x + 144, 14);
    }

    private static void dot(Graphics2D g2, FontMetrics fm, int x, int y, String label, boolean showLabel, boolean start) {
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(start ? Theme.CORAL_LIGHT : Theme.TEXT);
        int r = start ? 5 : 3;
        g2.fillOval(x - r, y - r, 2 * r, 2 * r);
        g2.setColor(Theme.INSET);
        g2.drawOval(x - r, y - r, 2 * r, 2 * r);
        if (showLabel) {
            g2.setColor(start ? Theme.CORAL_LIGHT : Theme.TEXT_MUTED);
            g2.drawString(label, x + 8, y + fm.getAscent() / 2 - 1);
        }
    }

    private int x(int cylinder) {
        return MARGIN + (int) Math.round(cylinder * (getWidth() - 2.0 * MARGIN) / maxCylinder);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return ROW;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(ROW, visible.height - ROW);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() != null && getParent().getWidth() >= MIN_WIDTH;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
    }
}
