package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Bottom bar: result of the last action (green = ok, neon = invalid input) and the keyboard shortcuts. */
final class StatusBar extends JPanel {
    private final JLabel message = Theme.label("Ready", Theme.SMALL, Theme.TEXT_MUTED);
    private final JLabel keys = Theme.label("Ctrl+1–8 module  ·  Ctrl+Enter / F5 run  ·  Ctrl+B menu  ·  Ctrl+T theme",
            Theme.SMALL, Theme.TEXT_FAINT);
    private final Dot dot = new Dot();
    private final Timer fade = new Timer(6000, e -> reset());
    private String hint = "Ready";

    StatusBar() {
        super(new BorderLayout(8, 0));
        setBackground(Theme.CHARCOAL);
        setBorder(BorderFactory.createCompoundBorder(
                Theme.hairline(1, 0, 0, 0, true), Theme.padding(6, 16, 6, 16)));
        JPanel left = new JPanel(new BorderLayout(8, 0));
        left.setOpaque(false);
        left.add(dot, BorderLayout.WEST);
        left.add(message, BorderLayout.CENTER);
        add(left, BorderLayout.CENTER);
        add(keys, BorderLayout.EAST);
        fade.setRepeats(false);
        Theme.onChange(this::reset);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                keys.setVisible(getWidth() > 900);
            }
        });
    }

    /** Shows an action outcome; it falls back to the module hint after a few seconds. */
    void show(boolean ok, String text) {
        message.setText(text);
        message.setForeground(ok ? Theme.TEXT : Theme.NEON_LIGHT);
        dot.color = ok ? Theme.SUCCESS : Theme.NEON;
        dot.repaint();
        fade.restart();
    }

    void hint(String text) {
        hint = text;
        if (!fade.isRunning()) {
            reset();
        }
    }

    private void reset() {
        message.setText(hint);
        message.setForeground(Theme.TEXT_MUTED);
        dot.color = Theme.TEXT_FAINT;
        dot.repaint();
    }

    private static final class Dot extends JPanel {
        private java.awt.Color color = Theme.TEXT_FAINT;

        Dot() {
            setOpaque(false);
            setPreferredSize(new java.awt.Dimension(10, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setColor(color);
            g2.fillOval(1, (getHeight() - 8) / 2, 8, 8);
            g2.dispose();
        }
    }
}
