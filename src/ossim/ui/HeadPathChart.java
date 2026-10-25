package ossim.ui;

import ossim.core.DiskScheduler;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/** Head path: cylinders on the horizontal axis, time (movement order) flowing downward. Jumps are dashed red. */
final class HeadPathChart extends JPanel {
    private static final int MARGIN = 40;
    private static final int ROW = 16;
    private static final int LABEL_LIMIT = 60;

    private final int head;
    private final List<DiskScheduler.Move> moves;
    private final int maxCylinder;

    HeadPathChart(int head, List<DiskScheduler.Move> moves, int maxCylinder) {
        this.head = head;
        this.moves = moves;
        this.maxCylinder = maxCylinder;
        setPreferredSize(new Dimension(800, (int) Math.min(40_000L, 70L + (moves.size() + 1L) * ROW)));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke solid = new BasicStroke(1.5f);
        Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("0", MARGIN - 4, 18);
        g2.drawString(String.valueOf(maxCylinder), getWidth() - MARGIN - 8, 18);
        g2.drawLine(MARGIN, 24, getWidth() - MARGIN, 24);

        int fromX = x(head);
        int fromY = 40;
        boolean labels = moves.size() <= LABEL_LIMIT;
        dot(g2, fromX, fromY, String.valueOf(head), labels);
        for (DiskScheduler.Move move : moves) {
            int toX = x(move.to());
            int toY = fromY + ROW;
            boolean jump = move.kind() == DiskScheduler.Kind.JUMP;
            g2.setStroke(jump ? dashed : solid);
            g2.setColor(jump ? Color.RED : new Color(30, 90, 200));
            g2.drawLine(fromX, fromY, toX, toY);
            dot(g2, toX, toY, String.valueOf(move.to()), labels);
            fromX = toX;
            fromY = toY;
        }
    }

    private void dot(Graphics2D g2, int x, int y, String label, boolean showLabel) {
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Color.BLACK);
        g2.fillOval(x - 2, y - 2, 5, 5);
        if (showLabel) {
            g2.drawString(label, x + 5, y + 4);
        }
    }

    private int x(int cylinder) {
        return MARGIN + (int) Math.round(cylinder * (getWidth() - 2.0 * MARGIN) / maxCylinder);
    }
}
