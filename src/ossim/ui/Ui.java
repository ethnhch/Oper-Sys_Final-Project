package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.List;

/** Shared building blocks of the design system: buttons, fields, cards, badges and layout helpers. */
final class Ui {
    /** Upper bound for a single numeric value (never for the number of items) so sums stay inside the int range. */
    static final int MAX_VALUE = 1_000_000;
    static final String ERROR_KEY = "ossim.error";

    private Ui() { }

    // ------------------------------------------------------------------ buttons

    enum Variant { PRIMARY, SECONDARY, GHOST }

    /** Rounded button in one of three emphasis levels; hover and press states are painted, not images. */
    static class Btn extends JButton {
        private final Variant variant;

        Btn(String text, Icons.Kind icon, Variant variant) {
            super(text);
            this.variant = variant;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.BODY_BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(7, text.isEmpty() ? 8 : 14, 7, text.isEmpty() ? 8 : 14));
            setIconTextGap(7);
            if (icon != null) {
                setIcon(Icons.of(icon, 15, null));
            }
        }

        @Override
        public Color getForeground() {
            if (variant == null) {
                return super.getForeground();
            }
            if (!isEnabled()) {
                return Theme.c(Theme.Role.TEXT_FAINT);
            }
            return switch (variant) {
                case PRIMARY -> Theme.c(Theme.Role.ON_ACCENT);
                case SECONDARY -> Theme.c(Theme.Role.TEXT);
                case GHOST -> getModel().isRollover() ? Theme.c(Theme.Role.TEXT) : Theme.c(Theme.Role.TEXT_MUTED);
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            int w = getWidth();
            int h = getHeight();
            switch (variant) {
                case PRIMARY -> {
                    g2.setColor(pressed ? Theme.mix(Theme.c(Theme.Role.ACCENT_HOVER), Color.BLACK, 0.15)
                            : hover ? Theme.c(Theme.Role.ACCENT_HOVER) : Theme.c(Theme.Role.ACCENT));
                    g2.fillRoundRect(0, 0, w, h, 9, 9);
                }
                case SECONDARY -> {
                    g2.setColor(pressed ? Theme.c(Theme.Role.GRID) : hover ? Theme.c(Theme.Role.SURFACE_ALT) : Theme.c(Theme.Role.SURFACE));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 9, 9);
                    g2.setColor(hover ? Theme.c(Theme.Role.BORDER_STRONG) : Theme.c(Theme.Role.BORDER));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 9, 9);
                }
                case GHOST -> {
                    if (hover || pressed) {
                        g2.setColor(pressed ? Theme.c(Theme.Role.GRID) : Theme.c(Theme.Role.SURFACE_ALT));
                        g2.fillRoundRect(0, 0, w, h, 9, 9);
                    }
                }
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.mix(Theme.c(Theme.Role.ACCENT), Theme.c(Theme.Role.SURFACE), 0.45));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static JButton button(String text, Runnable action) {
        return button(text, null, Variant.SECONDARY, action);
    }

    static JButton button(String text, Icons.Kind icon, Variant variant, Runnable action) {
        Btn b = new Btn(text, icon, variant);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Sun / moon button that switches between light and dark mode; the icon shows the mode you will get. */
    static JButton themeButton() {
        Btn b = new Btn("", null, Variant.GHOST) {
            @Override
            public String getToolTipText() {
                return Theme.isDark() ? "Switch to light mode (Ctrl+D)" : "Switch to dark mode (Ctrl+D)";
            }
        };
        b.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Icons.paint((Graphics2D) g, Theme.isDark() ? Icons.Kind.SUN : Icons.Kind.MOON, x, y, 18,
                        Theme.c(Theme.Role.TEXT_MUTED));
            }

            @Override
            public int getIconWidth() {
                return 18;
            }

            @Override
            public int getIconHeight() {
                return 18;
            }
        });
        b.setToolTipText("Switch theme");
        b.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        b.addActionListener(e -> Theme.requestToggle());
        return b;
    }

    // ------------------------------------------------------------------ fields

    /** Rounded outline used by fields and combo boxes; turns accent on focus and red when flagged invalid. */
    static final class RoundBorder extends AbstractBorder {
        private final JComponent owner;
        private final boolean roomy;

        RoundBorder(JComponent owner, boolean roomy) {
            this.owner = owner;
            this.roomy = roomy;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = FlatUI.smooth(g);
            boolean error = Boolean.TRUE.equals(owner.getClientProperty(ERROR_KEY));
            boolean focus = owner.isFocusOwner() || (owner instanceof JScrollPane sp && sp.getViewport().getView() != null
                    && sp.getViewport().getView().isFocusOwner());
            if (focus || error) {
                Color ring = error ? Theme.c(Theme.Role.DANGER) : Theme.c(Theme.Role.ACCENT);
                g2.setColor(Theme.mix(ring, Theme.c(Theme.Role.SURFACE), 0.7));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 9, 9);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(ring);
            } else {
                g2.setColor(Theme.c(Theme.Role.BORDER_STRONG));
            }
            g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 8, 8);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return roomy ? new Insets(7, 10, 7, 10) : new Insets(5, 9, 5, 6);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets i = getBorderInsets(c);
            insets.set(i.top, i.left, i.bottom, i.right);
            return insets;
        }
    }

    /** Text field with a rounded fill and outline. Typing clears an earlier validation highlight. */
    static final class Field extends JTextField {
        Field(String text, int columns) {
            super(text, columns);
            setOpaque(false);
            setFont(Theme.BODY);
            setBorder(new RoundBorder(this, true));
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
            getDocument().addDocumentListener(clearErrorOnEdit(this));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(isEditable() ? Theme.c(Theme.Role.SURFACE) : Theme.c(Theme.Role.SURFACE_ALT));
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 9, 9);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            setOpaque(false);
            setBorder(new RoundBorder(this, true));
        }
    }

    static DocumentListener clearErrorOnEdit(JComponent target) {
        return new DocumentListener() {
            private void clear() {
                if (Boolean.TRUE.equals(target.getClientProperty(ERROR_KEY))) {
                    target.putClientProperty(ERROR_KEY, null);
                    target.repaint();
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clear();
            }
        };
    }

    static JTextField field(String text, int columns) {
        return new Field(text, columns);
    }

    static JTextArea textArea(int rows, String initial) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.MONO);
        area.setText(initial);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return area;
    }

    /** Text area inside a rounded outline; the outline is the error target for validation. */
    static JScrollPane textBox(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new RoundBorder(scroll, false));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                scroll.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                scroll.repaint();
            }
        });
        area.getDocument().addDocumentListener(clearErrorOnEdit(scroll));
        return scroll;
    }

    static JScrollPane scroll(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ------------------------------------------------------------------ layout

    /** Transparent panel, so the card or page colour shows through. */
    static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel flow(Component... parts) {
        JPanel panel = clear(new WrapLayout(FlowLayout.LEFT, 8, 4));
        for (Component part : parts) {
            panel.add(part);
        }
        return panel;
    }

    /** A labelled form control: small caption above the field. */
    static JPanel form(String label, JComponent field) {
        JPanel panel = clear(new BorderLayout(0, 5));
        panel.add(text(label, Theme.SMALL_BOLD, Theme.Role.TEXT_MUTED), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    /** One row of form controls laid out left to right. */
    static JPanel row(Component... parts) {
        JPanel panel = clear(new WrapLayout(FlowLayout.LEFT, 12, 6));
        for (Component part : parts) {
            panel.add(part);
        }
        return panel;
    }

    static JPanel labeled(String label, JComponent field) {
        return row(form(label, field));
    }

    static JLabel text(String text, Font font, Theme.Role role) {
        JLabel label = new JLabel(text) {
            @Override
            public Color getForeground() {
                return Theme.c(role);
            }
        };
        label.setFont(font);
        return label;
    }

    /** Uppercase section caption used inside cards. */
    static JLabel caption(String text) {
        JLabel label = text(text.toUpperCase(), Theme.SMALL_BOLD, Theme.Role.TEXT_FAINT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 4, 2, 4));
        return label;
    }

    /** Rounded surface with a hairline border and an optional header row. */
    static final class Card extends JPanel {
        private final JPanel header;

        Card(String title, Icons.Kind icon) {
            super(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            header = clear(new BorderLayout(8, 0));
            if (title != null) {
                JLabel label = text(title, Theme.H2, Theme.Role.TEXT);
                if (icon != null) {
                    label.setIcon(Icons.of(icon, 17, Theme.Role.ACCENT));
                    label.setIconTextGap(8);
                }
                header.add(label, BorderLayout.WEST);
                add(header, BorderLayout.NORTH);
            }
        }

        void setHeaderRight(JComponent component) {
            header.add(component, BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(Theme.c(Theme.Role.SURFACE));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS * 2, Theme.RADIUS * 2);
            g2.setColor(Theme.c(Theme.Role.BORDER));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.RADIUS * 2, Theme.RADIUS * 2);
            g2.dispose();
        }
    }

    /** Small rounded pill with a tinted background, e.g. "Memory" or "SAFE". */
    static final class Badge extends JLabel {
        private Theme.Tone tone;

        Badge(String text, Theme.Tone tone) {
            super(text, SwingConstants.CENTER);
            this.tone = tone;
            setFont(Theme.SMALL_BOLD);
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        }

        void set(String text, Theme.Tone newTone) {
            setText(text);
            tone = newTone;
            repaint();
        }

        @Override
        public Color getForeground() {
            return tone == null ? super.getForeground() : Theme.tone(tone);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(Theme.toneSoft(tone));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Draws a pill (rounded label) directly onto a chart or cell. Returns its width. */
    static int pill(Graphics2D g2, String text, int x, int centerY, Color fill, Color fg) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 14;
        int h = fm.getHeight() + 2;
        g2.setColor(fill);
        g2.fillRoundRect(x, centerY - h / 2, w, h, h, h);
        g2.setColor(fg);
        g2.drawString(text, x + 7, centerY - h / 2 + 1 + fm.getAscent());
        return w;
    }

    static Dimension size(int w, int h) {
        return new Dimension(w, h);
    }

    static Icon icon(Icons.Kind kind, int size, Theme.Role role) {
        return Icons.of(kind, size, role);
    }

    // ------------------------------------------------------------------ text

    /** Joins at most {@code limit} items; the rest is summarised so huge datasets keep the summary readable. */
    static String preview(List<String> items, int limit, String separator) {
        if (items.size() <= limit) {
            return String.join(separator, items);
        }
        return String.join(separator, items.subList(0, limit)) + " ... (" + items.size() + " total)";
    }

    static String two(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    static String grouped(long value) {
        return String.format("%,d", value);
    }
}
