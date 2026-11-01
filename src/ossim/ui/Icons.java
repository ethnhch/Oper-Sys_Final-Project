package ossim.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Vector line icons drawn with Java2D on a 24x24 grid (no image files, so they stay sharp at any scale and
 * follow the current theme colour).
 */
final class Icons {
    enum Kind {
        HOME, CPU, LOCK, GRID, SWAP, LAYERS, BLOCKS, DISK, STACK, PLAY, FILE, DATABASE, PLUS, MINUS, TRASH,
        MOON, SUN, CHECK, ALERT, INFO, CHEVRON_DOWN, ARROW_RIGHT, TABLE, SHIELD, KEYBOARD
    }

    private Icons() { }

    /** Icon painted in the given role colour, or in the host component's foreground when role is null. */
    static Icon of(Kind kind, int size, Theme.Role role) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Color color = role != null ? Theme.c(role) : (c != null ? c.getForeground() : Theme.c(Theme.Role.TEXT));
                paint((Graphics2D) g, kind, x, y, size, color);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    static void paint(Graphics2D target, Kind kind, int x, int y, int size, Color color) {
        Graphics2D g = FlatUI.smooth(target);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case HOME -> {
                g.draw(poly(3, 11, 12, 3, 21, 11));
                g.draw(poly(5, 10, 5, 21, 19, 21, 19, 10));
                g.draw(poly(10, 21, 10, 15, 14, 15, 14, 21));
            }
            case CPU -> {
                g.draw(new RoundRectangle2D.Double(5, 5, 14, 14, 3, 3));
                g.draw(new RoundRectangle2D.Double(9, 9, 6, 6, 1, 1));
                for (int p : new int[]{9, 15}) {
                    g.draw(new Line2D.Double(p, 2, p, 5));
                    g.draw(new Line2D.Double(p, 19, p, 22));
                    g.draw(new Line2D.Double(2, p, 5, p));
                    g.draw(new Line2D.Double(19, p, 22, p));
                }
            }
            case LOCK -> {
                g.draw(new RoundRectangle2D.Double(4, 11, 16, 10, 3, 3));
                g.draw(new Arc2D.Double(7, 3, 10, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(7, 9, 7, 11));
                g.draw(new Line2D.Double(17, 9, 17, 11));
                g.fill(new Ellipse2D.Double(10.5, 14.5, 3, 3));
            }
            case GRID -> {
                g.draw(new RoundRectangle2D.Double(3, 3, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(14, 3, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(3, 14, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(14, 14, 7, 7, 2, 2));
            }
            case SWAP -> {
                g.draw(poly(4, 8, 20, 8));
                g.draw(poly(16, 4, 20, 8, 16, 12));
                g.draw(poly(20, 16, 4, 16));
                g.draw(poly(8, 12, 4, 16, 8, 20));
            }
            case LAYERS -> {
                g.draw(poly(12, 3, 21, 8, 12, 13, 3, 8, 12, 3));
                g.draw(poly(3, 12, 12, 17, 21, 12));
                g.draw(poly(3, 16, 12, 21, 21, 16));
            }
            case BLOCKS -> {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 3, 3));
                g.draw(new Line2D.Double(9, 4, 9, 20));
                g.draw(new Line2D.Double(15, 4, 15, 20));
                g.fill(new RoundRectangle2D.Double(4, 5, 5, 14, 2, 2));
                g.fill(new RoundRectangle2D.Double(15, 11, 5, 8, 2, 2));
            }
            case DISK -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                g.draw(new Line2D.Double(14.5, 14.5, 18, 18));
            }
            case STACK -> {
                g.draw(new RoundRectangle2D.Double(3, 3, 18, 5, 2, 2));
                g.draw(new RoundRectangle2D.Double(3, 10, 18, 5, 2, 2));
                g.draw(new RoundRectangle2D.Double(3, 17, 18, 4, 2, 2));
                g.fill(new Ellipse2D.Double(5.5, 4.5, 2, 2));
                g.fill(new Ellipse2D.Double(5.5, 11.5, 2, 2));
            }
            case PLAY -> {
                Path2D p = poly(7, 4, 19, 12, 7, 20, 7, 4);
                g.fill(p);
                g.draw(p);
            }
            case FILE -> {
                g.draw(poly(14, 3, 6, 3, 6, 21, 18, 21, 18, 7, 14, 3, 14, 7, 18, 7));
                g.draw(new Line2D.Double(9, 12, 15, 12));
                g.draw(new Line2D.Double(9, 16, 15, 16));
            }
            case DATABASE -> {
                g.draw(new Ellipse2D.Double(4, 3, 16, 5));
                g.draw(poly(4, 5.5, 4, 18.5));
                g.draw(poly(20, 5.5, 20, 18.5));
                g.draw(new Arc2D.Double(4, 9.5, 16, 5, 180, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4, 16, 16, 5, 180, 180, Arc2D.OPEN));
            }
            case PLUS -> {
                g.draw(new Line2D.Double(12, 5, 12, 19));
                g.draw(new Line2D.Double(5, 12, 19, 12));
            }
            case MINUS -> g.draw(new Line2D.Double(5, 12, 19, 12));
            case TRASH -> {
                g.draw(poly(4, 7, 20, 7));
                g.draw(poly(9, 7, 9, 4, 15, 4, 15, 7));
                g.draw(poly(6, 7, 7, 21, 17, 21, 18, 7));
            }
            case MOON -> g.draw(moon());
            case SUN -> {
                g.draw(new Ellipse2D.Double(8, 8, 8, 8));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    g.draw(new Line2D.Double(12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7, 12 + Math.cos(a) * 9.5,
                            12 + Math.sin(a) * 9.5));
                }
            }
            case CHECK -> g.draw(poly(5, 12.5, 10, 17, 19, 7));
            case ALERT -> {
                g.draw(poly(12, 3, 22, 20, 2, 20, 12, 3));
                g.draw(new Line2D.Double(12, 9, 12, 13.5));
                g.fill(new Ellipse2D.Double(10.8, 15.8, 2.4, 2.4));
            }
            case INFO -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g.draw(new Line2D.Double(12, 11, 12, 16.5));
                g.fill(new Ellipse2D.Double(10.8, 6.8, 2.4, 2.4));
            }
            case CHEVRON_DOWN -> g.draw(poly(6, 9, 12, 15, 18, 9));
            case ARROW_RIGHT -> {
                g.draw(new Line2D.Double(4, 12, 19, 12));
                g.draw(poly(13, 6, 19, 12, 13, 18));
            }
            case TABLE -> {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 3, 3));
                g.draw(new Line2D.Double(3, 9.5, 21, 9.5));
                g.draw(new Line2D.Double(3, 15, 21, 15));
                g.draw(new Line2D.Double(10, 4, 10, 20));
            }
            case SHIELD -> {
                g.draw(poly(12, 3, 20, 6, 20, 12, 12, 21, 4, 12, 4, 6, 12, 3));
                g.draw(poly(8.5, 12, 11, 14.5, 15.5, 9.5));
            }
            case KEYBOARD -> {
                g.draw(new RoundRectangle2D.Double(2, 6, 20, 12, 3, 3));
                for (int kx = 6; kx <= 18; kx += 4) {
                    g.fill(new Ellipse2D.Double(kx - 1, 9, 2, 2));
                }
                g.draw(new Line2D.Double(8, 14.5, 16, 14.5));
            }
        }
        g.dispose();
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    private static Path2D moon() {
        Path2D p = new Path2D.Double();
        p.moveTo(20, 14.5);
        p.curveTo(18.5, 19, 13.5, 21.5, 9, 20);
        p.curveTo(4, 18, 2, 12.5, 4, 8);
        p.curveTo(5, 5.5, 7, 4, 9.5, 3.2);
        p.curveTo(7.5, 8, 9.5, 13, 13.5, 15);
        p.curveTo(15.5, 16, 18, 15.8, 20, 14.5);
        return p;
    }
}
