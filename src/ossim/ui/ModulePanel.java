package ossim.ui;

import ossim.util.InputException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Page template shared by every module, so all eight screens look and behave the same:
 * <ul>
 *   <li>header: icon, title, category and a one-paragraph description;</li>
 *   <li>left card: the inputs, the options and a sticky action bar (Load Sample, Load Large, Run);</li>
 *   <li>right side: the {@link OutputPane} with stat tiles, chart/table tabs and the explanation.</li>
 * </ul>
 * Every button runs through {@link #guarded}, so invalid input becomes an inline message that points at the faulty
 * field or table row, and never a crash.
 */
abstract class ModulePanel extends JPanel {
    protected final OutputPane output = new OutputPane();

    final String title;
    final String category;
    final Icons.Kind icon;

    private final Banner banner = new Banner();
    private final JPanel actionBar = Ui.clear(new BorderLayout(8, 0));
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final Map<String, InputTable> tables = new LinkedHashMap<>();
    private JButton runButton;
    private String lastError;
    private boolean unexpected;

    /** Content width below which input and results are shown as "Input | Results" tabs instead of side by side. */
    static final int STACK_BELOW = 960;
    /** Window height below which the module description is hidden to give the work area more room. */
    static final int SHORT_BELOW = 680;

    private final JPanel centerHost = Ui.clear(new BorderLayout());
    private final java.awt.CardLayout stackCards = new java.awt.CardLayout();
    private final JPanel stackBody = Ui.clear(stackCards);
    private final Segmented segments = new Segmented(java.util.List.of("Input", "Results"), this::showPane);
    private final JPanel stack = Ui.clear(new BorderLayout(0, Theme.GAP));
    private JTextArea descriptionArea;
    private Ui.Card inputCard;
    private JSplitPane split;
    private boolean stacked;

    protected ModulePanel(String title, Icons.Kind icon, String category, String description) {
        super(new BorderLayout(0, Theme.GAP));
        this.title = title;
        this.icon = icon;
        this.category = category;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = Ui.clear(new BorderLayout(14, 0));
        header.add(new IconTile(icon), BorderLayout.WEST);
        JPanel titles = Ui.clear(new BorderLayout(0, 4));
        JPanel titleRow = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.add(Ui.text(title, Theme.H1, Theme.Role.TEXT));
        titleRow.add(javax.swing.Box.createHorizontalStrut(12));
        titleRow.add(new Ui.Badge(category, Theme.Tone.ACCENT));
        titles.add(titleRow, BorderLayout.NORTH);
        descriptionArea = wrapping(description);
        titles.add(descriptionArea, BorderLayout.CENTER);
        header.add(titles, BorderLayout.CENTER);
        JPanel headerRight = Ui.clear(new BorderLayout());
        headerRight.add(Ui.themeButton(), BorderLayout.NORTH);
        header.add(headerRight, BorderLayout.EAST);

        JPanel north = Ui.clear(new BorderLayout(0, 10));
        north.add(header, BorderLayout.NORTH);
        north.add(banner, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        stack.add(segments, BorderLayout.NORTH);
        stack.add(stackBody, BorderLayout.CENTER);
        add(centerHost, BorderLayout.CENTER);
    }

    /** Multi-line muted text that wraps to the available width. */
    static JTextArea wrapping(String text) {
        JTextArea area = new JTextArea(text) {
            @Override
            public java.awt.Color getForeground() {
                return Theme.c(Theme.Role.TEXT_MUTED);
            }
        };
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(Theme.BODY);
        area.setBorder(BorderFactory.createEmptyBorder());
        return area;
    }

    // ------------------------------------------------------------------ layout

    /** Places the main input component and the option rows in the input card, results on the right. */
    protected final void arrange(JComponent main, JComponent... options) {
        Ui.Card input = new Ui.Card("Input", Icons.Kind.FILE);
        JLabel hint = Ui.text("Unlimited rows · values ≤ " + Ui.grouped(Ui.MAX_VALUE), Theme.SMALL, Theme.Role.TEXT_FAINT);
        input.setHeaderRight(hint);
        JPanel content = Ui.clear(new BorderLayout(0, 10));
        content.add(main, BorderLayout.CENTER);
        if (options.length > 0) {
            JPanel south = Ui.clear(null);
            south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
            JLabel caption = Ui.caption("Options");
            caption.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(new Divider());
            south.add(caption);
            for (JComponent option : options) {
                option.setAlignmentX(Component.LEFT_ALIGNMENT);
                south.add(option);
            }
            content.add(south, BorderLayout.SOUTH);
        }
        input.add(content, BorderLayout.CENTER);
        JPanel bar = Ui.clear(new BorderLayout(0, 10));
        bar.add(new Divider(), BorderLayout.NORTH);
        bar.add(actionBar, BorderLayout.CENTER);
        input.add(bar, BorderLayout.SOUTH);
        input.setMinimumSize(new Dimension(380, 300));

        inputCard = input;
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.36);
        split.setOpaque(false);
        split.setContinuousLayout(true);
        applyStacked(false, true);
    }

    // ------------------------------------------------------------------ responsive layout

    /**
     * Called by the main window whenever the space for this screen changes. Wide: inputs and results side by side.
     * Narrow: a segmented "Input | Results" switch shows one at a time. Short: the description is hidden and the
     * page margins shrink.
     */
    void adapt(int width, int height) {
        boolean compactSpacing = width < STACK_BELOW || height < SHORT_BELOW;
        int v = compactSpacing ? 12 : 20;
        int h = compactSpacing ? 14 : 24;
        setBorder(BorderFactory.createEmptyBorder(v, h, v, h));
        if (descriptionArea != null) {
            descriptionArea.setVisible(height >= SHORT_BELOW);
        }
        if (split != null) {
            applyStacked(width < STACK_BELOW, false);
            output.adapt(stacked ? width - 2 * h : (int) ((width - 2 * h) * 0.62));
        }
    }

    private void applyStacked(boolean value, boolean force) {
        if (value == stacked && !force) {
            return;
        }
        stacked = value;
        centerHost.removeAll();
        if (stacked) {
            stackBody.add(inputCard, "0");
            stackBody.add(output, "1");
            centerHost.add(stack, BorderLayout.CENTER);
            showPane(segments.selected());
        } else {
            split.setLeftComponent(inputCard);
            split.setRightComponent(output);
            centerHost.add(split, BorderLayout.CENTER);
        }
        centerHost.revalidate();
        centerHost.repaint();
    }

    private void showPane(int index) {
        segments.select(index);
        stackCards.show(stackBody, String.valueOf(index));
    }

    /** True when the narrow "Input | Results" layout is active (used by the GUI tests). */
    boolean isStacked() {
        return stacked;
    }

    /** Index of the visible pane in the narrow layout: 0 = Input, 1 = Results. */
    int visiblePane() {
        return segments.selected();
    }

    /** Fills the sticky action bar: secondary sample buttons on the left, the primary Run button on the right. */
    protected final void actions(Runnable sample, String largeLabel, Runnable large, Runnable run) {
        JButton sampleButton = Ui.button("Load Sample", Icons.Kind.FILE, Ui.Variant.SECONDARY,
                () -> guarded(() -> {
                    sample.run();
                    banner.info("Sample data loaded. Press Run to simulate.");
                }, false));
        sampleButton.setToolTipText("Small textbook dataset whose answer can be checked by hand");
        JButton largeButton = Ui.button(largeLabel, Icons.Kind.DATABASE, Ui.Variant.SECONDARY,
                () -> guarded(() -> {
                    large.run();
                    banner.info("Large random dataset loaded to prove there is no item limit. Press Run to simulate.");
                }, false));
        largeButton.setToolTipText("Hundreds to thousands of random items");
        runButton = Ui.button("Run", Icons.Kind.PLAY, Ui.Variant.PRIMARY, () -> guarded(run, true));
        runButton.setToolTipText("Run the simulation (Ctrl+Enter)");
        JPanel samples = Ui.clear(new WrapLayout(java.awt.FlowLayout.LEFT, 8, 4));
        samples.add(sampleButton);
        samples.add(largeButton);
        actionBar.add(samples, BorderLayout.CENTER);
        JPanel right = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.add(runButton);
        actionBar.add(right, BorderLayout.EAST);
    }

    /** A button that runs through the same validation path, e.g. Build Tables or Check Request. */
    protected final JButton action(String text, Icons.Kind iconKind, Runnable task) {
        return Ui.button(text, iconKind, Ui.Variant.SECONDARY, () -> guarded(task, true));
    }

    // ------------------------------------------------------------------ inputs with error targeting

    /**
     * Text field registered under the label that {@code Parse} uses in its messages, so a validation error about it
     * outlines the field in red and moves the focus there.
     */
    protected final JTextField field(String parseLabel, String text, int columns) {
        JTextField f = Ui.field(text, columns);
        fields.put(parseLabel, f);
        return f;
    }

    /** Free-text list input (reference string, requests, addresses) with a live item count. */
    protected final JComponent listInput(String caption, String hint, JTextArea area, String parseLabel) {
        JScrollPane box = Ui.textBox(area);
        fields.put(parseLabel, box);
        Ui.Badge count = new Ui.Badge("0 values", Theme.Tone.ACCENT);
        Runnable recount = () -> {
            String t = area.getText().trim();
            int n = t.isEmpty() ? 0 : t.split("[\\s,;]+").length;
            count.set(Ui.grouped(n) + (n == 1 ? " value" : " values"), Theme.Tone.ACCENT);
        };
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                recount.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                recount.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                recount.run();
            }
        });
        JPanel panel = Ui.clear(new BorderLayout(0, 6));
        JPanel head = Ui.clear(new BorderLayout(8, 0));
        JLabel title = Ui.text(caption, Theme.BODY_BOLD, Theme.Role.TEXT);
        head.add(title, BorderLayout.WEST);
        head.add(count, BorderLayout.EAST);
        JPanel top = Ui.clear(new BorderLayout(0, 2));
        top.add(head, BorderLayout.NORTH);
        top.add(Ui.text(hint, Theme.SMALL, Theme.Role.TEXT_FAINT), BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        panel.add(box, BorderLayout.CENTER);
        return panel;
    }

    /** Registers a table whose validation messages start with {@code rowPrefix + rowNumber}, e.g. "Row 3 ...". */
    protected final InputTable table(InputTable table, String rowPrefix) {
        tables.put(rowPrefix, table);
        return table;
    }

    // ------------------------------------------------------------------ guarded execution

    private void guarded(Runnable task, boolean isRun) {
        clearFlags();
        long start = System.nanoTime();
        try {
            task.run();
            lastError = null;
            unexpected = false;
            if (isRun) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                banner.success("Done in " + ms + " ms. Results, charts and the comparison are updated.");
                if (stacked) {
                    showPane(1);
                }
            }
        } catch (InputException ex) {
            lastError = ex.getMessage();
            unexpected = false;
            banner.error("Please fix the input", ex.getMessage());
            if (stacked) {
                showPane(0);
            }
            flag(ex.getMessage());
        } catch (RuntimeException ex) {
            lastError = String.valueOf(ex);
            unexpected = true;
            banner.error("Unexpected error", "The simulation stopped safely: " + ex);
        }
    }

    private void flag(String message) {
        for (Map.Entry<String, JComponent> e : fields.entrySet()) {
            if (message.startsWith(e.getKey())) {
                JComponent target = e.getValue();
                target.putClientProperty(Ui.ERROR_KEY, Boolean.TRUE);
                target.repaint();
                Component focus = target instanceof JScrollPane sp ? sp.getViewport().getView() : target;
                focus.requestFocusInWindow();
                return;
            }
        }
        for (Map.Entry<String, InputTable> e : tables.entrySet()) {
            Matcher m = Pattern.compile("^" + Pattern.quote(e.getKey()) + "(\\d+)\\b").matcher(message);
            if (m.find()) {
                e.getValue().flagRow(Integer.parseInt(m.group(1)) - 1);
                return;
            }
        }
    }

    private void clearFlags() {
        for (JComponent c : fields.values()) {
            if (c.getClientProperty(Ui.ERROR_KEY) != null) {
                c.putClientProperty(Ui.ERROR_KEY, null);
                c.repaint();
            }
        }
    }

    JButton runButton() {
        return runButton;
    }

    /** Message of the last validation problem, or null after a successful action (used by the GUI tests). */
    String lastError() {
        return lastError;
    }

    boolean unexpectedError() {
        return unexpected;
    }

    // ------------------------------------------------------------------ small components

    /** Rounded tinted square holding the module icon. */
    static final class IconTile extends JComponent {
        private final Icons.Kind kind;

        IconTile(Icons.Kind kind) {
            this.kind = kind;
            setPreferredSize(new Dimension(48, 48));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(Theme.c(Theme.Role.ACCENT_SOFT));
            g2.fillRoundRect(0, 0, 48, 48, 14, 14);
            Icons.paint(g2, kind, 12, 12, 24, Theme.c(Theme.Role.ACCENT));
            g2.dispose();
        }
    }

    /** Hairline separator. */
    static final class Divider extends JComponent {
        Divider() {
            setPreferredSize(new Dimension(10, 9));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.c(Theme.Role.BORDER));
            g.fillRect(0, 4, getWidth(), 1);
        }
    }

    /** Inline, non-blocking message strip: red for errors (stays), green for success and blue for info (fade out). */
    static final class Banner extends JPanel {
        private final JLabel headline = new JLabel() {
            @Override
            public java.awt.Color getForeground() {
                return tone == null ? Theme.c(Theme.Role.TEXT) : Theme.tone(tone);
            }
        };
        private final JTextArea detail = wrapping("");
        private final Timer hide = new Timer(5000, e -> setVisible(false));
        private Theme.Tone tone = Theme.Tone.ACCENT;
        private Icons.Kind kind = Icons.Kind.INFO;

        Banner() {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setVisible(false);
            setBorder(BorderFactory.createEmptyBorder(10, 46, 10, 10));
            hide.setRepeats(false);
            headline.setFont(Theme.BODY_BOLD);
            detail.setFont(Theme.BODY);
            JPanel text = Ui.clear(new BorderLayout(0, 2));
            text.add(headline, BorderLayout.NORTH);
            text.add(detail, BorderLayout.CENTER);
            add(text, BorderLayout.CENTER);
            JButton close = Ui.button("", Icons.Kind.MINUS, Ui.Variant.GHOST, () -> setVisible(false));
            close.setToolTipText("Dismiss");
            close.setIcon(new javax.swing.Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = FlatUI.smooth(g);
                    g2.setColor(Theme.c(Theme.Role.TEXT_MUTED));
                    g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 3, y + 3, x + 11, y + 11);
                    g2.drawLine(x + 11, y + 3, x + 3, y + 11);
                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return 14;
                }

                @Override
                public int getIconHeight() {
                    return 14;
                }
            });
            JPanel east = Ui.clear(new BorderLayout());
            east.add(close, BorderLayout.NORTH);
            add(east, BorderLayout.EAST);
        }

        void error(String title, String message) {
            show(Theme.Tone.DANGER, Icons.Kind.ALERT, title, message, false);
        }

        void success(String message) {
            show(Theme.Tone.SUCCESS, Icons.Kind.CHECK, "Simulation complete", message, true);
        }

        void info(String message) {
            show(Theme.Tone.ACCENT, Icons.Kind.INFO, "Ready", message, true);
        }

        private void show(Theme.Tone newTone, Icons.Kind newKind, String title, String message, boolean autoHide) {
            tone = newTone;
            kind = newKind;
            headline.setText(title);
            detail.setText(message);
            hide.stop();
            if (autoHide) {
                hide.start();
            }
            setVisible(true);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            g2.setColor(Theme.toneSoft(tone));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(Theme.mix(Theme.tone(tone), Theme.toneSoft(tone), 0.6));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            Icons.paint(g2, kind, 15, 11, 20, Theme.tone(tone));
            g2.dispose();
        }
    }
}
