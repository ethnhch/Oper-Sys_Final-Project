package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

/** Result area: a text summary plus any number of named tabs holding tables or charts. */
final class OutputPane extends JPanel {
    private final JTextArea summary = new JTextArea(9, 40);
    private final JTabbedPane tabs = new JTabbedPane();

    OutputPane() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Results"));
        summary.setEditable(false);
        summary.setFont(Ui.MONO);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        var summaryScroll = Ui.scroll(summary);
        summaryScroll.setPreferredSize(new Dimension(300, 150));
        add(tabs, BorderLayout.CENTER);
        add(summaryScroll, BorderLayout.SOUTH);
    }

    void reset() {
        tabs.removeAll();
        summary.setText("");
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
        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        if (columns.length > 9) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
        tabs.addTab(name, Ui.scroll(table));
    }

    void addComponent(String name, JComponent component) {
        tabs.addTab(name, Ui.scroll(component));
    }
}
