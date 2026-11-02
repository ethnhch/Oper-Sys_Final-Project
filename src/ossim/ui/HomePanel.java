package ossim.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Landing screen (also the project overview for the presentation): what the simulator does, how every module
 * screen is organised, and one card per module that opens it.
 */
final class HomePanel extends JPanel implements Scrollable {

    /** Static description of a module card. */
    record ModuleInfo(String number, String title, Icons.Kind icon, String category, String blurb, List<String> algorithms) { }

    private final JPanel steps = Ui.clear(new GridLayout(1, 3, Theme.GAP, Theme.GAP));
    private final JPanel grid = Ui.clear(new GridLayout(0, 2, Theme.GAP, Theme.GAP));
    private final JPanel guide = Ui.clear(new GridLayout(1, 4, Theme.GAP, Theme.GAP));

    HomePanel(List<ModuleInfo> modules, IntConsumer open) {
        super(new BorderLayout(0, 20));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        JPanel hero = Ui.clear(new BorderLayout(0, 6));
        JPanel titleRow = Ui.clear(new BorderLayout(12, 0));
        titleRow.add(Ui.text("Mini Operating System Simulator", Theme.H1.deriveFont(26f), Theme.Role.TEXT), BorderLayout.CENTER);
        titleRow.add(appearanceSwitch(), BorderLayout.EAST);
        hero.add(titleRow, BorderLayout.NORTH);
        hero.add(ModulePanel.wrapping("Eight core OS concepts, each with real calculations, step-by-step traces, charts and a "
                + "side-by-side comparison of every algorithm. Enter as many items as you like: the only limit is your "
                + "computer's memory."), BorderLayout.CENTER);

        steps.add(step("1", "Pick a module", "Use the menu on the left or press Ctrl+1 to Ctrl+8."));
        steps.add(step("2", "Enter or load data", "Type rows, or press Load Sample (textbook) or Load Large (thousands)."));
        steps.add(step("3", "Run and compare", "Press Run (Ctrl+Enter). Read the stat tiles, charts and the comparison tab."));

        JPanel top = Ui.clear(new BorderLayout(0, 20));
        top.add(hero, BorderLayout.NORTH);
        top.add(steps, BorderLayout.CENTER);

        for (int i = 0; i < modules.size(); i++) {
            int index = i;
            grid.add(new ModuleCard(modules.get(i), () -> open.accept(index)));
        }
        JPanel gridWrap = Ui.clear(new BorderLayout(0, 10));
        gridWrap.add(Ui.caption("Modules"), BorderLayout.NORTH);
        gridWrap.add(grid, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(gridWrap, BorderLayout.CENTER);
        add(layoutGuide(), BorderLayout.SOUTH);
    }

    /** "Light | Dark" switch on the overview, kept in sync with the sidebar item, header buttons and Ctrl+D. */
    private static JComponent appearanceSwitch() {
        Segmented sw = new Segmented(List.of("Light", "Dark"), index -> {
            if ((index == 1) != Theme.isDark()) {
                Theme.requestToggle();
            }
        });
        sw.setPreferredSize(new Dimension(180, 36));
        sw.setToolTipText("Appearance (Ctrl+D)");
        sw.select(Theme.isDark() ? 1 : 0);
        Theme.onChange(() -> sw.select(Theme.isDark() ? 1 : 0));
        JPanel wrap = Ui.clear(new BorderLayout());
        wrap.add(sw, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent step(String number, String title, String text) {
        Ui.Card card = new Ui.Card(null, null);
        JPanel row = Ui.clear(new BorderLayout(12, 0));
        JLabel n = new Ui.Badge(number, Theme.Tone.ACCENT);
        n.setFont(Theme.H2);
        n.setPreferredSize(new Dimension(34, 34));
        JPanel left = Ui.clear(new BorderLayout());
        left.add(n, BorderLayout.NORTH);
        row.add(left, BorderLayout.WEST);
        JPanel texts = Ui.clear(new BorderLayout(0, 3));
        texts.add(Ui.text(title, Theme.BODY_BOLD, Theme.Role.TEXT), BorderLayout.NORTH);
        texts.add(ModulePanel.wrapping(text), BorderLayout.CENTER);
        row.add(texts, BorderLayout.CENTER);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    /** Reflows the overview: module cards in 2 columns or 1, quick-start steps and guide in a row or stacked. */
    void adapt(int width) {
        int pad = width < 900 ? 16 : 32;
        setBorder(BorderFactory.createEmptyBorder(pad + 4, pad, pad, pad));
        regrid(grid, 0, width >= 1000 ? 2 : 1);
        regrid(steps, width >= 860 ? 1 : 0, width >= 860 ? 3 : 1);
        regrid(guide, width >= 1000 ? 1 : 0, width >= 1000 ? 4 : width >= 600 ? 2 : 1);
    }

    private static void regrid(JPanel panel, int rows, int cols) {
        GridLayout g = (GridLayout) panel.getLayout();
        if (g.getRows() != rows || g.getColumns() != cols) {
            panel.setLayout(new GridLayout(rows, cols, Theme.GAP, Theme.GAP));
            panel.revalidate();
        }
    }

    private JComponent layoutGuide() {
        Ui.Card card = new Ui.Card("Every module screen has the same layout", Icons.Kind.INFO);
        JPanel cols = guide;
        cols.add(guideItem("Header", "What the module does and the rules it follows."));
        cols.add(guideItem("Input card", "Tables you can grow row by row, list boxes, options, then Load / Run."));
        cols.add(guideItem("Stat tiles + tabs", "Key numbers first, then charts, step tables and the algorithm comparison."));
        cols.add(guideItem("Errors", "Bad input never crashes: a red message names the field or row and outlines it."));
        card.add(cols, BorderLayout.CENTER);
        return card;
    }

    private static JComponent guideItem(String title, String text) {
        JPanel p = Ui.clear(new BorderLayout(0, 3));
        p.add(Ui.text(title, Theme.BODY_BOLD, Theme.Role.ACCENT), BorderLayout.NORTH);
        p.add(ModulePanel.wrapping(text), BorderLayout.CENTER);
        return p;
    }

    /** Clickable module card with hover lift. */
    private static final class ModuleCard extends JPanel {
        private boolean hover;

        ModuleCard(ModuleInfo info, Runnable open) {
            super(new BorderLayout(14, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Open " + info.title() + " (Ctrl+" + info.number() + ")");

            JPanel west = Ui.clear(new BorderLayout());
            west.add(new ModulePanel.IconTile(info.icon()), BorderLayout.NORTH);
            add(west, BorderLayout.WEST);

            JPanel head = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
            head.add(Ui.text(info.number() + "  " + info.title(), Theme.H2, Theme.Role.TEXT));
            head.add(javax.swing.Box.createHorizontalStrut(10));
            head.add(new Ui.Badge(info.category(), Theme.Tone.NEUTRAL));
            JPanel body = Ui.clear(new BorderLayout(0, 8));
            body.add(head, BorderLayout.NORTH);
            body.add(ModulePanel.wrapping(info.blurb()), BorderLayout.CENTER);
            JPanel chips = Ui.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
            for (String a : info.algorithms()) {
                chips.add(new Ui.Badge(a, Theme.Tone.ACCENT));
            }
            body.add(chips, BorderLayout.SOUTH);
            add(body, BorderLayout.CENTER);
            JLabel arrow = new JLabel(Icons.of(Icons.Kind.ARROW_RIGHT, 18, Theme.Role.TEXT_FAINT));
            add(arrow, BorderLayout.EAST);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!contains(javax.swing.SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), ModuleCard.this))) {
                        hover = false;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    open.run();
                }
            };
            addMouseListener(mouse);
            for (java.awt.Component c : allChildren(this)) {
                c.addMouseListener(mouse);
            }
        }

        private static List<java.awt.Component> allChildren(java.awt.Container root) {
            List<java.awt.Component> out = new java.util.ArrayList<>();
            for (java.awt.Component c : root.getComponents()) {
                out.add(c);
                if (c instanceof java.awt.Container inner) {
                    out.addAll(allChildren(inner));
                }
            }
            return out;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = FlatUI.smooth(g);
            int arc = Theme.RADIUS * 2;
            if (hover) {
                g2.setColor(Theme.mix(Theme.c(Theme.Role.BORDER), Theme.c(Theme.Role.BG), 0.4));
                g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 3, arc, arc);
            }
            g2.setColor(Theme.c(Theme.Role.SURFACE));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - (hover ? 3 : 1), arc, arc);
            g2.setColor(hover ? Theme.c(Theme.Role.ACCENT) : Theme.c(Theme.Role.BORDER));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - (hover ? 3 : 1), arc, arc);
            g2.dispose();
        }
    }

    // Scrollable: track the viewport width so text wraps instead of scrolling sideways.
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return r.height - 40;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }
}
