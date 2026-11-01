package ossim.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Horizontal bar chart used to compare algorithms on one metric. The best bar (lowest or highest, depending on the
 * metric) is drawn in the success colour and tagged "best", the currently selected algorithm is outlined.
 */
final class BarChart extends Chart {
    private static final int ROW = 30;
    private static final int TOP = 34;

    private final String title;
    private final List<String> labels;
    private final List<Double> values;
    private final boolean lowerIsBetter;
    private final String selected;
    private final boolean integers;

    BarChart(String title, List<String> labels, List<Double> values, boolean lowerIsBetter, String selected) {
        super(440, TOP + labels.size() * ROW + 14);
        this.title = title;
        this.labels = labels;
        this.values = values;
        this.lowerIsBetter = lowerIsBetter;
        this.selected = selected;
        this.integers = values.stream().allMatch(v -> v == Math.rint(v));
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        g.setFont(Theme.SMALL_BOLD);
        label(g, title.toUpperCase(), 12, 20, Theme.Role.TEXT_MUTED);
        g.setFont(Theme.SMALL);
        label(g, lowerIsBetter ? "lower is better" : "higher is better", 12 + g.getFontMetrics(Theme.SMALL_BOLD)
                .stringWidth(title.toUpperCase()) + 12, 20, Theme.Role.TEXT_FAINT);

        FontMetrics fm = g.getFontMetrics(Theme.BODY);
        int labelW = 0;
        for (String l : labels) {
            labelW = Math.max(labelW, fm.stringWidth(l));
        }
        labelW = Math.min(labelW + 20, width / 3);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double best = lowerIsBetter ? values.stream().mapToDouble(Double::doubleValue).min().orElse(0) : max;
        int barX = 12 + labelW;
        int barMax = Math.max(40, width - barX - 110);

        for (int i = 0; i < labels.size(); i++) {
            int y = TOP + i * ROW;
            double v = values.get(i);
            boolean isBest = v == best;
            boolean isSelected = labels.get(i).equals(selected);
            g.setFont(isSelected ? Theme.BODY_BOLD : Theme.BODY);
            g.setColor(Theme.c(isSelected ? Theme.Role.TEXT : Theme.Role.TEXT_MUTED));
            g.drawString(labels.get(i), 12, y + 18);

            g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
            g.fillRoundRect(barX, y + 5, barMax, 18, 8, 8);
            int w = max <= 0 ? 0 : (int) Math.round(v / max * barMax);
            Color fill = isBest ? Theme.c(Theme.Role.SUCCESS)
                    : Theme.mix(Theme.c(Theme.Role.ACCENT), Theme.c(Theme.Role.SURFACE), isSelected ? 0 : 0.45);
            g.setColor(fill);
            g.fillRoundRect(barX, y + 5, Math.max(w, 4), 18, 8, 8);
            if (isSelected) {
                g.setColor(Theme.c(Theme.Role.TEXT));
                g.drawRoundRect(barX, y + 5, Math.max(w, 4), 18, 8, 8);
            }
            String text = integers ? String.valueOf((long) v) : Ui.two(v);
            g.setFont(Theme.MONO);
            g.setColor(Theme.c(Theme.Role.TEXT));
            int tx = barX + Math.max(w, 4) + 8;
            g.drawString(text, tx, y + 18);
            if (isBest) {
                g.setFont(Theme.SMALL_BOLD);
                Ui.pill(g, "best", tx + g.getFontMetrics(Theme.MONO).stringWidth(text) + 8, y + 14,
                        Theme.c(Theme.Role.SUCCESS_SOFT), Theme.c(Theme.Role.SUCCESS));
            }
            tip(new Rectangle(0, y, width, ROW), "<b>" + labels.get(i) + "</b><br>" + title + ": " + text
                    + (isBest ? "<br>best in this comparison" : ""));
        }
    }
}
