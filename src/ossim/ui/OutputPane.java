package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.List;

/**
 * Result area, read top to bottom: key metrics as stat tiles, then tabs holding charts and tables,
 * then a plain-language explanation with the formulas used. Before the first run an empty state explains what to do.
 */
final class OutputPane extends JPanel {
    private static final String EMPTY = "empty";
    private static final String RESULTS = "results";

    private final JPanel stats = Ui.clear(new GridLayout(1, 0, Theme.GAP, 0));
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextArea summary = new JTextArea(6, 40);
    private final CardLayout cards = new CardLayout();
    private final JPanel body = Ui.clear(cards);

    OutputPane() {
        super(new BorderLayout(0, Theme.GAP));
        setOpaque(false);
        summary.setEditable(false);
        summary.setFont(Theme.MONO);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        Ui.Card tabCard = new Ui.Card(null, null);
        tabCard.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
        tabCard.add(tabs, BorderLayout.CENTER);

        Ui.Card explain = new Ui.Card("How it was calculated", Icons.Kind.INFO);
        var summaryScroll = Ui.scroll(summary);
        summaryScroll.setOpaque(false);
        summaryScroll.getViewport().setOpaque(false);
        explain.add(summaryScroll, BorderLayout.CENTER);
        explain.setMinimumSize(new Dimension(200, 90));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabCard, explain);
        split.setResizeWeight(0.78);
        split.setOpaque(false);
        split.setContinuousLayout(true);

        JPanel results = Ui.clear(new BorderLayout(0, Theme.GAP));
        results.add(stats, BorderLayout.NORTH);
        results.add(split, BorderLayout.CENTER);

        body.add(emptyState(), EMPTY);
        body.add(results, RESULTS);
        add(body, BorderLayout.CENTER);
        cards.show(body, EMPTY);
    }

    private static JComponent emptyState() {
        Ui.Card card = new Ui.Card(null, null);
        JPanel inner = Ui.clear(new GridLayout(0, 1, 0, 6));
        JLabel icon = new JLabel(Icons.of(Icons.Kind.PLAY, 40, Theme.Role.ACCENT), SwingConstants.CENTER);
        JLabel title = Ui.text("No results yet", Theme.H2, Theme.Role.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel hint = Ui.text("Check the inputs on the left, then press Run (Ctrl+Enter).", Theme.BODY, Theme.Role.TEXT_MUTED);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel hint2 = Ui.text("Charts, tables and a comparison of every algorithm will appear here.", Theme.SMALL,
                Theme.Role.TEXT_FAINT);
        hint2.setHorizontalAlignment(SwingConstants.CENTER);
        inner.add(icon);
        inner.add(title);
        inner.add(hint);
        inner.add(hint2);
        JPanel center = Ui.clear(new java.awt.GridBagLayout());
        center.add(inner);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    void reset() {
        tabs.removeAll();
        stats.removeAll();
        summary.setText("");
        cards.show(body, RESULTS);
    }

    boolean hasResults() {
        return tabs.getTabCount() > 0;
    }

    void setSummary(String text) {
        summary.setText(text);
        summary.setCaretPosition(0);
        if (tabs.getTabCount() > 0) {
            tabs.setSelectedIndex(0);
        }
        revalidate();
        repaint();
    }

    /** Adds a key metric tile above the result tabs. */
    void addStat(String label, String value, Theme.Tone tone, String hint) {
        stats.add(new StatTile(label, value, tone, hint));
    }

    void addStat(String label, String value, Theme.Tone tone) {
        addStat(label, value, tone, null);
    }

    void addTable(String name, String[] columns, List<Object[]> rows) {
        tabs.addTab(name, tableScroll(columns, rows));
    }

    void addComponent(String name, JComponent component) {
        tabs.addTab(name, Ui.scroll(component));
    }

    /** A chart above the table that holds the same numbers (used for algorithm comparisons). */
    void addChartAndTable(String name, JComponent chart, String[] columns, List<Object[]> rows) {
        JPanel panel = Ui.clear(new BorderLayout(0, 8));
        panel.add(chart, BorderLayout.NORTH);
        panel.add(tableScroll(columns, rows), BorderLayout.CENTER);
        tabs.addTab(name, panel);
    }

    private static JComponent tableScroll(String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rows.forEach(model::addRow);
        JTable table = new Tables.Styled(model, "Nothing to show.");
        table.setAutoCreateRowSorter(true);
        if (columns.length > 7) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
        Tables.fitColumns(table, rows);
        return Ui.scroll(table);
    }

    /** Key metric: caption, large value in the tone colour and an optional hint underneath. */
    private static final class StatTile extends JPanel {
        private final Theme.Tone tone;

        StatTile(String label, String value, Theme.Tone tone, String hint) {
            super(new BorderLayout(0, 2));
            this.tone = tone;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 14));
            JLabel caption = Ui.text(label.toUpperCase(), Theme.SMALL_BOLD, Theme.Role.TEXT_MUTED);
            JLabel number = new JLabel(value) {
                @Override
                public java.awt.Color getForeground() {
                    return tone == Theme.Tone.NEUTRAL ? Theme.c(Theme.Role.TEXT) : Theme.tone(tone);
                }
            };
            number.setFont(Theme.STAT);
            add(caption, BorderLayout.NORTH);
            add(number, BorderLayout.CENTER);
            if (hint != null) {
                add(Ui.text(hint, Theme.SMALL, Theme.Role.TEXT_FAINT), BorderLayout.SOUTH);
            }
            setToolTipText(label + ": " + value + (hint == null ? "" : " (" + hint + ")"));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            int arc = Theme.RADIUS * 2;
            g2.setColor(Theme.c(Theme.Role.SURFACE));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(Theme.c(Theme.Role.BORDER));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(tone == Theme.Tone.NEUTRAL ? Theme.c(Theme.Role.BORDER_STRONG) : Theme.tone(tone));
            g2.fillRoundRect(0, 12, 4, getHeight() - 24, 4, 4);
            g2.dispose();
        }
    }
}
