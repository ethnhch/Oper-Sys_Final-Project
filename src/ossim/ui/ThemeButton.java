package ossim.ui;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Flat rounded button in three weights: PRIMARY (coral fill, used once per screen for Run), SECONDARY
 * (charcoal with outline) and GHOST (text only, for table row actions). Still a {@link JButton}, so keyboard
 * activation, focus and the GUI tests work unchanged.
 */
final class ThemeButton extends JButton {
    enum Kind { PRIMARY, SECONDARY, GHOST }

    private final Kind kind;
    private boolean hover;

    ThemeButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(kind == Kind.PRIMARY ? Theme.BODY_BOLD : kind == Kind.GHOST ? Theme.SMALL_BOLD : Theme.BODY);
        setForeground(kind == Kind.PRIMARY ? Theme.ON_CORAL : kind == Kind.GHOST ? Theme.TEXT_MUTED : Theme.TEXT);
        setMargin(kind == Kind.GHOST ? new Insets(4, 8, 4, 8) : new Insets(7, 14, 7, 14));
        setIconTextGap(7);
        Icon glyph = Glyph.forLabel(text, getForeground());
        if (glyph != null) {
            setIcon(glyph);
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(false);
            }
        });
    }

    private void setHover(boolean value) {
        hover = value;
        if (kind == Kind.GHOST) {
            setForeground(value ? Theme.CORAL_LIGHT : Theme.TEXT_MUTED);
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, kind == Kind.GHOST ? 28 : 34);
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        boolean pressed = getModel().isArmed() && getModel().isPressed();
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        int arc = 10;
        switch (kind) {
            case PRIMARY -> {
                g2.setColor(!isEnabled() ? Theme.LINE : pressed ? Theme.CORAL_DEEP : hover ? Theme.CORAL_LIGHT : Theme.CORAL);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }
            case SECONDARY -> {
                g2.setColor(pressed ? Theme.INSET : hover ? Theme.alpha(Theme.CORAL, 38) : Theme.SURFACE_2);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(hover || pressed ? Theme.CORAL : Theme.LINE);
                g2.drawRoundRect(0, 0, w, h, arc, arc);
            }
            case GHOST -> {
                if (hover || pressed) {
                    g2.setColor(Theme.alpha(Theme.CORAL, pressed ? 60 : 30));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                }
            }
        }
        if (isFocusOwner()) {
            g2.setColor(Theme.alpha(Theme.CORAL_LIGHT, 200));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Small vector icons drawn in code (no image files), chosen from the button label. */
    static final class Glyph implements Icon {
        enum Shape { PLAY, PLUS, MINUS, CROSS, CHECK, GRID, DATA }

        private final Shape shape;
        private final Color color;

        Glyph(Shape shape, Color color) {
            this.shape = shape;
            this.color = color;
        }

        static Glyph forLabel(String label, Color color) {
            if (label.equals("Run")) {
                return new Glyph(Shape.PLAY, color);
            } else if (label.startsWith("Add")) {
                return new Glyph(Shape.PLUS, color);
            } else if (label.startsWith("Remove")) {
                return new Glyph(Shape.MINUS, color);
            } else if (label.equals("Clear")) {
                return new Glyph(Shape.CROSS, color);
            } else if (label.startsWith("Check")) {
                return new Glyph(Shape.CHECK, color);
            } else if (label.startsWith("Build")) {
                return new Glyph(Shape.GRID, color);
            } else if (label.startsWith("Load")) {
                return new Glyph(Shape.DATA, color);
            }
            return null;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g);
            Color paint = c instanceof JButton b ? b.getForeground() : color;
            g2.setColor(paint);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            switch (shape) {
                case PLAY -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(2, 1);
                    p.lineTo(11, 6);
                    p.lineTo(2, 11);
                    p.closePath();
                    g2.fill(p);
                }
                case PLUS -> {
                    g2.drawLine(6, 1, 6, 11);
                    g2.drawLine(1, 6, 11, 6);
                }
                case MINUS -> g2.drawLine(1, 6, 11, 6);
                case CROSS -> {
                    g2.drawLine(2, 2, 10, 10);
                    g2.drawLine(10, 2, 2, 10);
                }
                case CHECK -> {
                    g2.drawLine(1, 6, 4, 10);
                    g2.drawLine(4, 10, 11, 2);
                }
                case GRID -> {
                    g2.drawRect(1, 1, 10, 10);
                    g2.drawLine(6, 1, 6, 11);
                    g2.drawLine(1, 6, 11, 6);
                }
                case DATA -> {
                    g2.drawOval(1, 0, 10, 4);
                    g2.drawLine(1, 2, 1, 10);
                    g2.drawLine(11, 2, 11, 10);
                    g2.drawArc(1, 8, 10, 4, 180, 180);
                }
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}
