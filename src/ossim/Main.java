package ossim;

import ossim.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point of the Mini Operating System Simulator. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default Swing look and feel
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
