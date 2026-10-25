package ossim.ui;

import ossim.util.Parse;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Fixed-size integer matrix editor with row labels (P0, P1, ...) and column labels (R0, R1, ...). */
final class MatrixTable extends JPanel {
    private final String title;
    private final JTable table;
    private final DefaultTableModel model;
    private final int rows;
    private final int cols;

    MatrixTable(String title, int rows, int cols, String rowPrefix) {
        super(new BorderLayout());
        this.title = title;
        this.rows = rows;
        this.cols = cols;
        String[] header = new String[cols + 1];
        header[0] = "";
        for (int j = 0; j < cols; j++) {
            header[j + 1] = "R" + j;
        }
        model = new DefaultTableModel(header, rows) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0;
            }
        };
        for (int i = 0; i < rows; i++) {
            model.setValueAt(rowPrefix + (rows == 1 ? "" : i), i, 0);
            for (int j = 0; j < cols; j++) {
                model.setValueAt("0", i, j + 1);
            }
        }
        table = new JTable(model);
        table.setRowHeight(22);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        setBorder(BorderFactory.createTitledBorder(title));
        var scroll = Ui.scroll(table);
        scroll.setPreferredSize(new Dimension(360, Math.min(rows, 6) * 22 + 28));
        add(scroll, BorderLayout.CENTER);
    }

    /** Reads and validates every cell as a non-negative integer. */
    int[][] values() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        int[][] out = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String raw = String.valueOf(model.getValueAt(i, j + 1));
                out[i][j] = Parse.integer("null".equals(raw) ? "" : raw,
                        title + " [" + model.getValueAt(i, 0) + ", R" + j + "]", 0, Ui.MAX_VALUE);
            }
        }
        return out;
    }

    void setValues(int[][] values) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                model.setValueAt(String.valueOf(values[i][j]), i, j + 1);
            }
        }
    }
}
