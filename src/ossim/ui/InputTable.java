package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable table with Add / Remove / Clear: the user decides how many rows (items) to enter.
 * The header shows a live row count, so it is obvious that there is no fixed item limit.
 */
final class InputTable extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final int columns;
    private final Ui.Badge count = new Ui.Badge("0 rows", Theme.Tone.ACCENT);
    private int errorRow = -1;

    InputTable(String title, String... columnNames) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        columns = columnNames.length;
        model = new DefaultTableModel(columnNames, 0);
        table = new Tables.Styled(model, "No rows yet. Click  + Add Row  or load a sample.") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (errorRow >= 0 && errorRow < getRowCount()) {
                    Rectangle r = getCellRect(errorRow, 0, true);
                    Graphics2D g2 = FlatUI.smooth(g);
                    g2.setColor(Theme.c(Theme.Role.DANGER));
                    g2.drawRect(0, r.y, getWidth() - 1, r.height - 1);
                    g2.fillRect(0, r.y, 3, r.height);
                    g2.dispose();
                }
            }
        };
        model.addTableModelListener(e -> {
            count.set(Ui.grouped(model.getRowCount()) + (model.getRowCount() == 1 ? " row" : " rows"), Theme.Tone.ACCENT);
            errorRow = -1;
        });
        table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "ossim.addRow");
        table.getActionMap().put("ossim.addRow", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addRow();
            }
        });

        JPanel head = Ui.clear(new BorderLayout(8, 0));
        JLabel caption = Ui.text(title, Theme.BODY_BOLD, Theme.Role.TEXT);
        caption.setIcon(Icons.of(Icons.Kind.TABLE, 15, Theme.Role.TEXT_MUTED));
        caption.setIconTextGap(7);
        head.add(caption, BorderLayout.WEST);
        head.add(count, BorderLayout.EAST);

        var scroll = Ui.scroll(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.c(Theme.Role.BORDER)));
        Theme.onChange(() -> scroll.setBorder(BorderFactory.createLineBorder(Theme.c(Theme.Role.BORDER))));

        JPanel tools = Ui.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        tools.add(Ui.button("Add Row", Icons.Kind.PLUS, Ui.Variant.GHOST, this::addRow));
        tools.add(Ui.button("Remove Selected", Icons.Kind.MINUS, Ui.Variant.GHOST, this::removeSelected));
        tools.add(Ui.button("Clear", Icons.Kind.TRASH, Ui.Variant.GHOST, this::clear));

        add(head, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(tools, BorderLayout.SOUTH);
        setToolTipText(null);
    }

    private void addRow() {
        stopEditing();
        model.addRow(new Object[columns]);
        int last = model.getRowCount() - 1;
        table.changeSelection(last, 0, false, false);
        table.editCellAt(last, 0);
        if (table.getEditorComponent() != null) {
            table.getEditorComponent().requestFocusInWindow();
        }
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

    /** Marks a row (0-based) as the source of a validation error and scrolls it into view. */
    void flagRow(int row) {
        if (row < 0 || row >= model.getRowCount()) {
            return;
        }
        errorRow = row;
        table.changeSelection(row, 0, false, false);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
        table.repaint();
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
