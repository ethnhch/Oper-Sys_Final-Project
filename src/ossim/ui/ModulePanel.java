package ossim.ui;

import ossim.util.InputException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Base layout shared by every module: description on top, inputs on the left, results on the right.
 * Every button action runs through {@link #action}, so invalid input becomes a message and never a crash.
 */
abstract class ModulePanel extends JPanel {
    protected final OutputPane output = new OutputPane();

    protected ModulePanel(String description) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JLabel("<html><body style='width:900px'>" + description + "</body></html>"), BorderLayout.NORTH);
    }

    /** Places the main input component and the control rows on the left, the output on the right. */
    protected final void arrange(JComponent main, JComponent... controls) {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setMinimumSize(new Dimension(400, 200));
        left.add(main, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        for (JComponent control : controls) {
            control.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(control);
        }
        left.add(south, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, output);
        split.setResizeWeight(0.38);
        add(split, BorderLayout.CENTER);
    }

    protected final JButton action(String text, Runnable task) {
        return Ui.button(text, () -> guarded(task));
    }

    private void guarded(Runnable task) {
        try {
            task.run();
        } catch (InputException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
