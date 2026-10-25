package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Editable table with Add / Remove / Clear: the user decides how many rows (items) to enter. */
final class InputTable extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final int columns;

    InputTable(String title, String... columnNames) {
        super(new BorderLayout(4, 4));
        columns = columnNames.length;
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable(model);
        table.setRowHeight(22);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        setBorder(BorderFactory.createTitledBorder(title));
        add(Ui.scroll(table), BorderLayout.CENTER);
        add(Ui.flow(Ui.button("Add Row", this::addRow),
                Ui.button("Remove Selected", this::removeSelected),
                Ui.button("Clear", this::clear)), BorderLayout.SOUTH);
    }

    private void addRow() {
        stopEditing();
        model.addRow(new Object[columns]);
        int last = model.getRowCount() - 1;
        table.changeSelection(last, 0, false, false);
    }

    private void removeSelected() {
        stopEditing();
        int[] selected = table.getSelectedRows();
        for (int i = selected.length - 1; i >= 0; i--) {
            model.removeRow(table.convertRowIndexToModel(selected[i]));
        }
    }

    private void clear() {
        stopEditing();
        model.setRowCount(0);
    }

    void setRows(List<String[]> rows) {
        stopEditing();
        model.setRowCount(0);
        for (String[] row : rows) {
            model.addRow(row);
        }
    }

    /** Returns trimmed cell text per row; trailing completely blank rows are ignored. */
    List<String[]> rows() {
        stopEditing();
        List<String[]> rows = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String[] cells = new String[columns];
            for (int c = 0; c < columns; c++) {
                Object value = model.getValueAt(r, c);
                cells[c] = value == null ? "" : value.toString().trim();
            }
            rows.add(cells);
        }
        while (!rows.isEmpty() && isBlank(rows.get(rows.size() - 1))) {
            rows.remove(rows.size() - 1);
        }
        return rows;
    }

    private static boolean isBlank(String[] cells) {
        for (String cell : cells) {
            if (!cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void stopEditing() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }
}
