package ossim.ui;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Segmented control ("Input | Results") used on narrow windows, where the input card and the results no longer fit
 * side by side. A small dot on a segment signals that it has new content the user has not looked at yet.
 */
final class Segmented extends JComponent {
    private final List<String> labels;
    private final IntConsumer onSelect;
    private final boolean[] dot;
    private int selected;

    Segmented(List<String> labels, IntConsumer onSelect) {
        this.labels = labels;
        this.onSelect = onSelect;
        this.dot = new boolean[labels.size()];
        setPreferredSize(new Dimension(10, 40));
        setMinimumSize(new Dimension(10, 40));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = Math.min(labels.size() - 1, e.getX() * labels.size() / Math.max(1, getWidth()));
                select(index);
                onSelect.accept(index);
            }
        });
    }

    void select(int index) {
        selected = index;
        dot[index] = false;
        repaint();
    }

    int selected() {
        return selected;
    }

    void mark(int index) {
        if (index != selected) {
            dot[index] = true;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = FlatUI.smooth(g);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Theme.c(Theme.Role.GRID));
        g2.fillRoundRect(0, 0, w, h, 12, 12);
        int segW = w / labels.size();
        g2.setFont(Theme.BODY_BOLD);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < labels.size(); i++) {
            int x = i * segW;
            if (i == selected) {
                g2.setColor(Theme.c(Theme.Role.SURFACE));
                g2.fillRoundRect(x + 3, 3, segW - 6, h - 6, 9, 9);
                g2.setColor(Theme.c(Theme.Role.BORDER));
                g2.drawRoundRect(x + 3, 3, segW - 7, h - 7, 9, 9);
            }
            String text = labels.get(i);
            int tx = x + (segW - fm.stringWidth(text)) / 2;
            g2.setColor(i == selected ? Theme.c(Theme.Role.ACCENT) : Theme.c(Theme.Role.TEXT_MUTED));
            g2.drawString(text, tx, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (dot[i]) {
                g2.setColor(Theme.c(Theme.Role.ACCENT));
                g2.fillOval(tx + fm.stringWidth(text) + 6, h / 2 - 7, 7, 7);
            }
        }
        g2.dispose();
    }
}
