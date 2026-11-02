package ossim.ui;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Drives each real panel through its buttons without a display.
 * Valid input must run without any error message and must produce results; invalid input must produce an inline
 * validation message (never an unexpected exception), and the faulty field must be flagged when one is named.
 */
public final class GuiSmokeTest {
    private static int failures;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        List<Supplier<ModulePanel>> panels = List.of(CpuPanel::new, BankerPanel::new, PagingPanel::new,
                PageReplacementPanel::new, SegmentationPanel::new, FragmentationPanel::new, DiskPanel::new,
                MultiprogrammingPanel::new);
        for (Supplier<ModulePanel> factory : panels) {
            String name = factory.get().getClass().getSimpleName();
            validRun(name, factory.get(), "Run");
            validRun(name, factory.get(), "Load Large", "Run");
            invalidRun(name, factory.get());
            narrowRun(name, factory.get());
        }
        System.out.println(failures == 0 ? "\nGUI smoke test: ALL OK" : "\nGUI smoke test: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void validRun(String name, ModulePanel panel, String... clicks) {
        long start = System.nanoTime();
        try {
            for (String label : clicks) {
                click(panel, label);
            }
            boolean ok = panel.lastError() == null && panel.output.hasResults();
            report(ok, name + " valid [" + String.join(" > ", clicks) + "] " + (System.nanoTime() - start) / 1_000_000 + " ms"
                    + (ok ? "" : " -> unexpected message: " + panel.lastError()));
        } catch (RuntimeException e) {
            report(false, name + " valid [" + String.join(" > ", clicks) + "] threw " + e);
        }
    }

    /** Narrow window: the screen must switch to "Input | Results" tabs and jump to Results after a run. */
    private static void narrowRun(String name, ModulePanel panel) {
        try {
            panel.adapt(700, 600);
            boolean stacked = panel.isStacked();
            click(panel, "Run");
            boolean ok = stacked && panel.lastError() == null && panel.visiblePane() == 1 && panel.output.hasResults();
            panel.adapt(1300, 900);
            ok &= !panel.isStacked();
            report(ok, name + " responsive: narrow layout stacks, Run shows Results, wide layout restores split");
        } catch (RuntimeException e) {
            report(false, name + " responsive layout threw " + e);
        }
    }

    private static void invalidRun(String name, ModulePanel panel) {
        for (Component c : all(panel)) {
            if (c instanceof JTextArea area && area.isEditable()) {
                area.setText("");
            } else if (c instanceof JTextField field) {
                field.setText("abc");
            }
        }
        for (AbstractButton b : buttons(panel, "Clear")) {
            b.doClick();
        }
        try {
            click(panel, panel instanceof BankerPanel ? "Build Tables" : "Run");
            if (panel.lastError() == null) {
                report(false, name + " invalid input was silently accepted");
            } else if (panel.unexpectedError()) {
                report(false, name + " invalid input caused an unexpected error: " + panel.lastError());
            } else {
                boolean flagged = false;
                for (Component c : all(panel)) {
                    flagged |= c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty(Ui.ERROR_KEY));
                }
                report(true, name + " invalid input -> inline message" + (flagged ? " + field outlined" : "")
                        + ": \"" + panel.lastError() + "\"");
            }
        } catch (RuntimeException e) {
            report(false, name + " invalid input crashed with " + e);
        }
    }

    private static void click(Container root, String labelStart) {
        List<AbstractButton> found = buttons(root, labelStart);
        if (found.isEmpty()) {
            throw new IllegalStateException("No button starting with '" + labelStart + "'");
        }
        found.get(0).doClick();
    }

    private static List<AbstractButton> buttons(Container root, String labelStart) {
        List<AbstractButton> out = new ArrayList<>();
        for (Component c : all(root)) {
            if (c instanceof JButton b && b.getText().startsWith(labelStart)) {
                out.add(b);
            }
        }
        return out;
    }

    private static List<Component> all(Container root) {
        List<Component> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            out.add(c);
            if (c instanceof Container inner) {
                out.addAll(all(inner));
            }
        }
        return out;
    }

    private static void report(boolean ok, String message) {
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "PASS  " : "FAIL  ") + message);
    }
}
