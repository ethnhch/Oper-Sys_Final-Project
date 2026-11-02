package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Table styling shared by input and result tables: taller rows, zebra striping, right-aligned numbers,
 * status values rendered as coloured pills, and a friendly empty state.
 */
final class Tables {
    /** Columns whose values are verdicts and are drawn as coloured pills. */
    private static final Set<String> STATUS_COLUMNS = Set.of("status", "result", "action", "counted", "verdict");

    private Tables() { }

    /** A JTable that fills its viewport and shows a hint when it has no rows. */
    static class Styled extends JTable {
        private final String emptyText;

        Styled(TableModel model, String emptyText) {
            super(model);
            this.emptyText = emptyText;
            setFillsViewportHeight(true);
            setRowHeight(30);
            setShowVerticalLines(false);
            setShowHorizontalLines(true);
            setIntercellSpacing(new Dimension(0, 1));
            setFont(Theme.BODY);
            setDefaultRenderer(Object.class, new Cell());
            getTableHeader().setDefaultRenderer(new Header());
            getTableHeader().setPreferredSize(new Dimension(10, 34));
            getTableHeader().setReorderingAllowed(false);
            putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            JTextField editor = new JTextField();
            editor.setFont(Theme.BODY);
            editor.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.c(Theme.Role.ACCENT)),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            DefaultCellEditor cellEditor = new DefaultCellEditor(editor);
            cellEditor.setClickCountToStart(1);
            setDefaultEditor(Object.class, cellEditor);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setGridColor(Theme.c(Theme.Role.GRID));
        }

        @Override
        public Color getGridColor() {
            return Theme.c(Theme.Role.GRID);
        }

        @Override
        public Color getBackground() {
            return Theme.c(Theme.Role.SURFACE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0 && emptyText != null) {
                Graphics2D g2 = FlatUI.smooth(g);
                g2.setFont(Theme.BODY);
                g2.setColor(Theme.c(Theme.Role.TEXT_FAINT));
                FontMetrics fm = g2.getFontMetrics();
                int y = Math.max(40, getParent() == null ? 40 : getParent().getHeight() / 2);
                g2.drawString(emptyText, (getWidth() - fm.stringWidth(emptyText)) / 2, y);
                g2.dispose();
            }
        }
    }

    static Theme.Tone toneFor(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        if (v.startsWith("hit") || v.startsWith("ok") || v.startsWith("allocated") || v.startsWith("granted")
                || v.equals("yes") || v.startsWith("serve") || v.equals("safe")) {
            return Theme.Tone.SUCCESS;
        }
        if (v.startsWith("fault") || v.startsWith("error") || v.startsWith("page fault") || v.startsWith("external")
                || v.startsWith("denied") || v.equals("unsafe")) {
            return Theme.Tone.DANGER;
        }
        if (v.startsWith("internal") || v.startsWith("not enough") || v.startsWith("return jump") || v.startsWith("must wait")
                || v.equals("no")) {
            return Theme.Tone.WARNING;
        }
        return Theme.Tone.NEUTRAL;
    }

    /** Body cells: zebra rows, numbers right-aligned in tabular style, verdict columns as pills. */
    private static final class Cell extends DefaultTableCellRenderer {
        private boolean pill;
        private Theme.Tone tone = Theme.Tone.NEUTRAL;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            String name = table.getColumnName(column).toLowerCase(Locale.ROOT);
            String text = value == null ? "" : value.toString();
            boolean numeric = value instanceof Number || text.matches("-?\\d+(\\.\\d+)?");
            pill = STATUS_COLUMNS.contains(name) && !text.isEmpty() && !"-".equals(text);
            tone = pill ? toneFor(text) : Theme.Tone.NEUTRAL;
            setHorizontalAlignment(numeric && !pill ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setFont(numeric ? Theme.MONO.deriveFont(12.5f) : Theme.BODY);
            Color base = row % 2 == 0 ? Theme.c(Theme.Role.SURFACE) : Theme.c(Theme.Role.SURFACE_ALT);
            setBackground(selected ? Theme.c(Theme.Role.SELECTION) : base);
            Color fg = Theme.c(Theme.Role.TEXT);
            if (!pill && name.equals("step") && text.contains("blocked")) {
                fg = Theme.c(Theme.Role.DANGER);
            } else if (!pill && name.equals("step") && text.contains("finish")) {
                fg = Theme.c(Theme.Role.SUCCESS);
            } else if ("-".equals(text) || "idle".equals(text)) {
                fg = Theme.c(Theme.Role.TEXT_FAINT);
            }
            setForeground(fg);
            setToolTipText(text.length() > 28 ? text : null);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!pill) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(Theme.SMALL_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int max = getWidth() - 34;
            if (fm.stringWidth(text) > max) {
                while (text.length() > 1 && fm.stringWidth(text + "…") > max) {
                    text = text.substring(0, text.length() - 1);
                }
                text = text + "…";
            }
            Color fill = tone == Theme.Tone.NEUTRAL ? Theme.c(Theme.Role.GRID) : Theme.toneSoft(tone);
            Color fg = tone == Theme.Tone.NEUTRAL ? Theme.c(Theme.Role.TEXT_MUTED) : Theme.tone(tone);
            Ui.pill(g2, text, 10, getHeight() / 2, fill, fg);
            g2.dispose();
        }
    }

    /** Header cells: small bold muted captions with a hairline underline and a sort arrow. */
    private static final class Header extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            String text = value == null ? "" : value.toString();
            String arrow = "";
            RowSorter<?> sorter = table.getRowSorter();
            if (sorter != null && !sorter.getSortKeys().isEmpty()) {
                RowSorter.SortKey key = sorter.getSortKeys().get(0);
                if (key.getColumn() == table.convertColumnIndexToModel(column)) {
                    arrow = key.getSortOrder() == SortOrder.ASCENDING ? "  ▲" : key.getSortOrder() == SortOrder.DESCENDING ? "  ▼" : "";
                }
            }
            label.setText(text + arrow);
            label.setFont(Theme.SMALL_BOLD);
            label.setForeground(Theme.c(Theme.Role.TEXT_MUTED));
            label.setBackground(Theme.c(Theme.Role.SURFACE_ALT));
            label.setOpaque(true);
            boolean numeric = false;
            if (table.getRowCount() > 0) {
                Object first = table.getValueAt(0, column);
                numeric = first instanceof Number || (first != null && first.toString().matches("-?\\d+(\\.\\d+)?"));
            }
            String name = text.toLowerCase(Locale.ROOT);
            label.setHorizontalAlignment(numeric && !STATUS_COLUMNS.contains(name) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.c(Theme.Role.BORDER)),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            return label;
        }
    }

    static TableCellRenderer cellRenderer() {
        return new Cell();
    }

    /** Sets sensible widths: narrow for short numeric columns, wide for free-text columns. */
    static void fitColumns(JTable table, List<Object[]> sample) {
        FontMetrics fm = table.getFontMetrics(Theme.BODY);
        int limit = Math.min(sample.size(), 200);
        for (int c = 0; c < table.getColumnCount(); c++) {
            int width = fm.stringWidth(table.getColumnName(c)) + 40;
            for (int r = 0; r < limit; r++) {
                Object v = sample.get(r)[c];
                width = Math.max(width, fm.stringWidth(v == null ? "" : v.toString()) + 34);
            }
            table.getColumnModel().getColumn(c).setPreferredWidth(Math.min(width, 520));
        }
    }
}
