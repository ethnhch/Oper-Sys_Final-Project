package ossim.ui;

import ossim.util.Parse;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/** Fixed-size integer matrix editor with row labels (P0, P1, ...) and column labels (R0, R1, ...). */
final class MatrixTable extends JPanel {
    private final String title;
    private final JTable table;
    private final DefaultTableModel model;
    private final int rows;
    private final int cols;

    MatrixTable(String title, int rows, int cols, String rowPrefix) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 0));
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
        table = new Tables.Styled(model, null);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                label.setFont(Theme.SMALL_BOLD);
                label.setForeground(Theme.c(Theme.Role.TEXT_MUTED));
                label.setBackground(Theme.c(Theme.Role.SURFACE_ALT));
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return label;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(64);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.setAutoResizeMode(cols > 8 ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_ALL_COLUMNS);

        JLabel caption = Ui.text(title + "  ·  " + rows + " × " + cols, Theme.BODY_BOLD, Theme.Role.TEXT);
        var scroll = Ui.scroll(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.c(Theme.Role.BORDER)));
        Theme.onChange(() -> scroll.setBorder(BorderFactory.createLineBorder(Theme.c(Theme.Role.BORDER))));
        scroll.setPreferredSize(new Dimension(360, Math.min(rows, 6) * 28 + 36));
        add(caption, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        setAlignmentX(LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.min(rows, 6) * 28 + 80));
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
                try {
                    out[i][j] = Parse.integer("null".equals(raw) ? "" : raw,
                            title + " [" + model.getValueAt(i, 0) + ", R" + j + "]", 0, Ui.MAX_VALUE);
                } catch (RuntimeException e) {
                    table.changeSelection(i, j + 1, false, false);
                    throw e;
                }
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
