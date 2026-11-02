package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/** Small Swing helpers shared by all module panels (all of them produce Charcoal &amp; Coral themed parts). */
final class Ui {
    /** Upper bound for a single numeric value (never for the number of items) so sums stay inside the int range. */
    static final int MAX_VALUE = 1_000_000;
    static final Font MONO = Theme.MONO;

    private Ui() { }

    static JButton button(String text, Runnable action) {
        return button(text, ThemeButton.Kind.SECONDARY, action);
    }

    static JButton button(String text, ThemeButton.Kind kind, Runnable action) {
        JButton button = new ThemeButton(text, kind);
        button.addActionListener(e -> action.run());
        return button;
    }

    /** A row of controls that wraps onto extra lines when the window is narrow (responsive). */
    static JPanel flow(Component... parts) {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        panel.setOpaque(false);
        for (Component part : parts) {
            if (part instanceof JLabel label) {
                label.setForeground(Theme.TEXT_MUTED);
            }
            ThemeFields.style(part);
            panel.add(part);
        }
        return panel;
    }

    static JPanel labeled(String label, JComponent field) {
        return flow(new JLabel(label), field);
    }

    static JTextArea textArea(int rows, String initial) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(MONO);
        area.setBackground(Theme.INSET);
        area.setForeground(Theme.TEXT);
        area.setCaretColor(Theme.CORAL_LIGHT);
        area.setSelectionColor(Theme.CORAL);
        area.setSelectedTextColor(Theme.ON_CORAL);
        area.setBorder(Theme.padding(8, 10, 8, 10));
        area.setText(initial);
        return area;
    }

    static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(Theme.hairline(1, 1, 1, 1, true));
        scroll.getViewport().setBackground(Theme.INSET);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** Joins at most {@code limit} items; the rest is summarised so huge datasets keep the summary readable. */
    static String preview(List<String> items, int limit, String separator) {
        if (items.size() <= limit) {
            return String.join(separator, items);
        }
        return String.join(separator, items.subList(0, limit)) + " ... (" + items.size() + " total)";
    }

    static String two(double value) {
        return String.format("%.2f", value);
    }
}
