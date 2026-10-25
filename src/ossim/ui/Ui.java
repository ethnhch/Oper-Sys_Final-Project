package ossim.ui;

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

/** Small Swing helpers shared by all module panels. */
final class Ui {
    /** Upper bound for a single numeric value (never for the number of items) so sums stay inside the int range. */
    static final int MAX_VALUE = 1_000_000;
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Ui() { }

    static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }

    static JPanel flow(Component... parts) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        for (Component part : parts) {
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
        area.setFont(MONO);
        area.setText(initial);
        return area;
    }

    static JScrollPane scroll(JComponent component) {
        return new JScrollPane(component);
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
