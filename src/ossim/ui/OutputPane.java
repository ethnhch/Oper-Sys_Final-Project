package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.List;

/** RESULTS card: named tabs holding tables or charts, plus a console-style text summary. Shows an empty state before the first run. */
final class OutputPane extends Card {
    private final JTextArea summary = new JTextArea(8, 40);
    private final JTabbedPane tabs = new JTabbedPane();
    private final CardLayout states = new CardLayout();
    private final JPanel body = new JPanel(states);

    OutputPane() {
        super(new BorderLayout(0, 10));
        tabs.setUI(new ThemeTabsUI());
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        summary.setEditable(false);
        summary.setFont(Theme.MONO);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setBackground(Theme.INSET);
        summary.setForeground(Theme.TEXT);
        summary.setSelectionColor(Theme.CORAL);
        summary.setSelectedTextColor(Theme.ON_CORAL);
        summary.setBorder(Theme.padding(10, 12, 10, 12));
        var summaryScroll = Ui.scroll(summary);
        summaryScroll.setPreferredSize(new Dimension(300, 150));
        summaryScroll.setMinimumSize(new Dimension(100, 70));

        JPanel summaryBox = new JPanel(new BorderLayout(0, 6));
        summaryBox.setOpaque(false);
        summaryBox.add(Theme.caption("Summary"), BorderLayout.NORTH);
        summaryBox.add(summaryScroll, BorderLayout.CENTER);

        JPanel results = new JPanel(new BorderLayout(0, 10));
        results.setOpaque(false);
        results.add(tabs, BorderLayout.CENTER);
        results.add(summaryBox, BorderLayout.SOUTH);

        body.setOpaque(false);
        body.add(new EmptyState(), "empty");
        body.add(results, "results");
        add(Theme.caption("Results"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    void reset() {
        tabs.removeAll();
        summary.setText("");
        states.show(body, "results");
    }

    void setSummary(String text) {
        summary.setText(text);
        summary.setCaretPosition(0);
    }

    void addTable(String name, String[] columns, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rows.forEach(model::addRow);
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.setAutoCreateRowSorter(true);
        if (columns.length > 9) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
        tabs.addTab(name, Ui.scroll(table));
    }

    void addComponent(String name, JComponent component) {
        tabs.addTab(name, Ui.scroll(component));
    }

    /** Placeholder shown until the first run, telling the user what to do next. */
    private static final class EmptyState extends JPanel {
        EmptyState() {
            super(new GridBagLayout());
            setOpaque(false);
            JLabel text = new JLabel();
            Runnable refresh = () -> text.setText("<html><div style='text-align:center'>"
                    + "<span style='font-size:13px;color:" + Theme.hex(Theme.TEXT) + "'><b>No results yet</b></span><br><br>"
                    + "<span style='color:" + Theme.hex(Theme.TEXT_MUTED) + "'>Enter your data or press <b>Load Sample</b>,"
                    + "<br>then press <span style='color:" + Theme.hex(Theme.CORAL_LIGHT) + "'><b>Run</b></span> (Ctrl+Enter)."
                    + "</span></div></html>");
            refresh.run();
            Theme.onChange(refresh);
            text.setHorizontalAlignment(SwingConstants.CENTER);
            text.setBorder(BorderFactory.createEmptyBorder(70, 10, 10, 10));
            add(text, new GridBagConstraints());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Theme.smooth(g);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 44;
            g2.setColor(Theme.SURFACE_2);
            g2.fillRoundRect(cx - 34, cy - 26, 68, 52, 14, 14);
            g2.setColor(Theme.CORAL);
            int[] bars = {18, 30, 12, 24};
            for (int i = 0; i < bars.length; i++) {
                g2.fillRoundRect(cx - 22 + i * 12, cy + 16 - bars[i], 8, bars[i], 3, 3);
            }
            g2.dispose();
        }
    }
}
