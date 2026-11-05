package ossim.ui;

import ossim.util.InputException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base layout shared by every module: a header (module number, title, description), an INPUT card and a
 * RESULTS card. Wide screens show the two cards side by side; below {@link Theme#BP_STACK} they stack vertically.
 * Every button action runs through {@link #action}, so invalid input becomes a message and never a crash.
 */
abstract class ModulePanel extends JPanel {
    private static final Pattern TITLE = Pattern.compile("^<b>(.*?)</b>\\s*-\\s*(.*)$", Pattern.DOTALL);

    protected final OutputPane output = new OutputPane();
    private final JLabel eyebrow = Theme.caption("Module");
    private final JLabel title = Theme.label("", Theme.TITLE, Theme.NEON_BRIGHT);
    private final JLabel description = new JLabel();
    private final String descriptionHtml;
    private final String moduleName;
    private JSplitPane split;
    private Card inputCard;
    private StackPanel stack;
    private JScrollPane stackScroll;
    private JButton primary;
    private BiConsumer<Boolean, String> statusListener;
    private boolean stacked;
    private boolean dividerPending;

    protected ModulePanel(String description) {
        super(new BorderLayout(0, Theme.GAP));
        setBackground(Theme.BG);
        setBorder(Theme.padding(18, 20, 16, 20));
        Matcher m = TITLE.matcher(description);
        String name = m.matches() ? m.group(1) : getClass().getSimpleName();
        String body = m.matches() ? m.group(2) : description;
        descriptionHtml = body.isEmpty() ? body : Character.toUpperCase(body.charAt(0)) + body.substring(1);
        moduleName = name;
        title.setText(name.toUpperCase(Locale.ROOT));
        this.description.setForeground(Theme.TEXT_MUTED);
        this.description.setFont(Theme.BODY);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{eyebrow, title, this.description}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(c);
        }
        eyebrow.setForeground(Theme.NEON_LIGHT);
        title.setBorder(Theme.padding(2, 0, 4, 0));
        add(header, BorderLayout.NORTH);
        updateDescription(600);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsive();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> applyPendingDivider());
            }
        });
    }

    /** Called by the main window so the header can show "MODULE 03 · MEMORY MANAGEMENT". */
    final void setModuleInfo(int number, String group) {
        eyebrow.setText(String.format("MODULE %02d  ·  %s", number, group.toUpperCase(Locale.ROOT)));
    }

    final void setStatusListener(BiConsumer<Boolean, String> listener) {
        statusListener = listener;
    }

    /** Places the main input component and the control rows in the INPUT card, the output in the RESULTS card. */
    protected final void arrange(JComponent main, JComponent... controls) {
        Card left = new Card(new BorderLayout(0, 10));
        left.setMinimumSize(new Dimension(300, 220));
        left.add(Theme.caption("Input"), BorderLayout.NORTH);
        left.add(main, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        for (JComponent control : controls) {
            control.setAlignmentX(Component.LEFT_ALIGNMENT);
            south.add(control);
        }
        left.add(south, BorderLayout.SOUTH);
        hookClear(left);
        output.setMinimumSize(new Dimension(300, 240));
        inputCard = left;

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setUI(new GripSplitUI());
        split.setBorder(null);
        split.setOpaque(false);
        split.setContinuousLayout(true);
        split.setDividerSize(Theme.GAP + 2);
        split.setResizeWeight(0.44);
        // a proportional divider location only works once the split pane has a size, so apply it on resize
        split.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyPendingDivider();
            }
        });
        stack = new StackPanel();
        stackScroll = new JScrollPane(stack);
        stackScroll.setBorder(null);
        stackScroll.setOpaque(false);
        stackScroll.getViewport().setOpaque(false);
        stackScroll.getVerticalScrollBar().setUnitIncrement(24);
        stackScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        useSideBySide();
    }

    /** Wide layout: INPUT and RESULTS side by side, separated by a draggable grip. */
    private void useSideBySide() {
        stack.removeAll();
        remove(stackScroll);
        split.setLeftComponent(inputCard);
        split.setRightComponent(output);
        add(split, BorderLayout.CENTER);
        dividerPending = true;
        SwingUtilities.invokeLater(this::applyPendingDivider);
    }

    /**
     * JSplitPane ignores a proportional location set before it is on screen, so the 44 % split is applied
     * once the split is showing and sized (after a resize, or when a hidden module is opened).
     */
    private void applyPendingDivider() {
        if (dividerPending && split.isShowing() && split.getWidth() > 0) {
            dividerPending = false;
            SwingUtilities.invokeLater(() -> split.setDividerLocation(0.44));
        }
    }

    /** Narrow layout: the cards stack vertically and the module scrolls, like a phone layout. */
    private void useStacked() {
        // clear only the two sides: removeAll() would also remove the split pane's own divider
        split.setLeftComponent(null);
        split.setRightComponent(null);
        remove(split);
        stack.add(inputCard);
        stack.add(output);
        add(stackScroll, BorderLayout.CENTER);
    }

    /** Responsive behaviour: stack the cards on narrow widths, and re-wrap the description to the new width. */
    private void applyResponsive() {
        int width = getWidth();
        updateDescription(width - getInsets().left - getInsets().right);
        boolean narrow = width < Theme.BP_STACK;
        title.setFont(narrow ? Theme.TITLE.deriveFont(21f) : Theme.TITLE);
        if (split != null && narrow != stacked) {
            stacked = narrow;
            if (narrow) {
                useStacked();
            } else {
                useSideBySide();
            }
        }
        // wrapped control rows depend on the new width, so recompute preferred sizes
        SwingUtilities.invokeLater(() -> {
            invalidateTree(this);
            validate();
            repaint();
        });
    }

    /** revalidate() only invalidates ancestors; wrapped rows need their cached sizes cleared all the way down. */
    private static void invalidateTree(java.awt.Container container) {
        container.invalidate();
        for (Component child : container.getComponents()) {
            if (child instanceof java.awt.Container inner) {
                invalidateTree(inner);
            }
        }
    }

    /** Vertical stack used in the narrow layout: input card at its natural height, results card tall enough to read. */
    private final class StackPanel extends JPanel implements Scrollable {
        StackPanel() {
            super(null);
            setOpaque(false);
        }

        private int inputHeight() {
            return Math.max(380, Math.min(inputCard.getPreferredSize().height, 620));
        }

        private int outputHeight() {
            return Math.max(520, stackScroll.getViewport().getHeight() - 20);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h1 = inputHeight();
            inputCard.setBounds(0, 0, w, h1);
            output.setBounds(0, h1 + Theme.GAP, w, outputHeight());
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(320, inputHeight() + Theme.GAP + outputHeight());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return Math.max(24, r.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Swing's HTML renderer scales CSS px by 1.3, so the wrap width is converted back to screen pixels. */
    private void updateDescription(int width) {
        int cssWidth = (int) (Math.max(240, Math.min(width - 8, 1100)) / 1.3);
        description.setText("<html><div style='width:" + cssWidth + "px'>"
                + descriptionHtml + "</div></html>");
    }

    /** Creates a button whose task is guarded; the button labelled "Run" becomes the neon primary action. */
    protected final JButton action(String text, Runnable task) {
        boolean isPrimary = text.equals("Run");
        JButton button = Ui.button(text, isPrimary ? ThemeButton.Kind.PRIMARY : ThemeButton.Kind.SECONDARY,
                () -> guarded(text, task));
        if (isPrimary) {
            primary = button;
        }
        return button;
    }

    /** Pressing Clear on any input table also empties the results, so a cleared module never shows stale output. */
    private void hookClear(java.awt.Container root) {
        if (root instanceof InputTable table) {
            table.setOnClear(output::showEmpty);
        }
        for (Component c : root.getComponents()) {
            if (c instanceof java.awt.Container inner) {
                hookClear(inner);
            }
        }
    }

    /** Keyboard shortcut target (Ctrl+Enter / F5). */
    final void runPrimary() {
        if (primary != null) {
            primary.doClick();
        }
    }

    private void guarded(String label, Runnable task) {
        long start = System.nanoTime();
        Cursor previous = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            task.run();
            status(true, moduleName + " — " + label + " completed in " + (System.nanoTime() - start) / 1_000_000 + " ms");
        } catch (InputException ex) {
            status(false, "Invalid input: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            status(false, "Unexpected error: " + ex);
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(previous);
        }
    }

    private void status(boolean ok, String message) {
        if (statusListener != null) {
            statusListener.accept(ok, message);
        }
    }

    /** Transparent split divider with a small grip, so the two cards float on the background. */
    private static final class GripSplitUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = Theme.smooth(g);
                    g2.setColor(Theme.LINE);
                    int w = getWidth();
                    int h = getHeight();
                    boolean vertical = splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT;
                    for (int i = -1; i <= 1; i++) {
                        int x = vertical ? w / 2 - 2 : w / 2 + i * 8 - 2;
                        int y = vertical ? h / 2 + i * 8 - 2 : h / 2 - 2;
                        g2.fillOval(x, y, 4, 4);
                    }
                    g2.dispose();
                }

                @Override
                public void setBorder(javax.swing.border.Border border) {
                    super.setBorder(BorderFactory.createEmptyBorder());
                }
            };
        }
    }
}
