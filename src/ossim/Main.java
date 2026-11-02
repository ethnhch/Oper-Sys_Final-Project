package ossim;

import ossim.ui.MainFrame;
import ossim.ui.Theme;

import javax.swing.SwingUtilities;

/** Entry point of the Mini Operating System Simulator. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
