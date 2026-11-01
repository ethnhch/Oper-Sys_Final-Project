package ossim.ui;

import ossim.core.Fragmentation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Memory map for the fragmentation module: one bar per block, scaled to the largest block. Process allocations are
 * coloured, internal fragmentation (fixed partitions) is hatched orange and free holes are left empty.
 */
final class MemoryMap extends Chart {
    static final int BLOCK_LIMIT = 300;
    private static final int ROW = 34;
    private static final int TOP = 40;
    private static final int LEFT = 110;

    private final Fragmentation.Result result;
    private final List<List<Fragmentation.Allocation>> perBlock = new ArrayList<>();
    private final int shown;
    private final int maxSize;

    MemoryMap(Fragmentation.Result result) {
        super(560, TOP + Math.min(result.blocks().size(), BLOCK_LIMIT) * ROW + 70);
        this.result = result;
        shown = Math.min(result.blocks().size(), BLOCK_LIMIT);
        int max = 1;
        for (Fragmentation.BlockState b : result.blocks()) {
            perBlock.add(new ArrayList<>());
            max = Math.max(max, b.size());
        }
        maxSize = max;
        for (Fragmentation.Allocation a : result.allocations()) {
            if (a.block() >= 0) {
                perBlock.get(a.block()).add(a);
            }
        }
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        boolean fixed = result.mode() == Fragmentation.Mode.FIXED;
        g.setFont(Theme.SMALL_BOLD);
        label(g, "MEMORY MAP · " + result.strategy().toString().toUpperCase() + " · " + result.mode().toString().toUpperCase(),
                12, 20, Theme.Role.TEXT_MUTED);
        int barW = Math.max(120, width - LEFT - 160);

        for (int i = 0; i < shown; i++) {
            Fragmentation.BlockState b = result.blocks().get(i);
            int y = TOP + i * ROW;
            int w = (int) Math.round((double) b.size() / maxSize * barW);
            g.setFont(Theme.BODY_BOLD);
            label(g, "B" + (i + 1), 12, y + 19, Theme.Role.TEXT);
            g.setFont(Theme.SMALL);
            label(g, Ui.grouped(b.size()), 46, y + 19, Theme.Role.TEXT_MUTED);

            Rectangle whole = new Rectangle(LEFT, y + 4, Math.max(w, 6), ROW - 10);
            g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
            g.fillRoundRect(whole.x, whole.y, whole.width, whole.height, 8, 8);
            g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
            g.drawRoundRect(whole.x, whole.y, whole.width, whole.height, 8, 8);

            int x = LEFT;
            for (Fragmentation.Allocation a : perBlock.get(i)) {
                int aw = (int) Math.round((double) a.request().size() / maxSize * barW);
                Rectangle seg = new Rectangle(x + 1, y + 5, Math.max(aw - 1, 2), ROW - 11);
                Color fill = Theme.seriesFor(a.request().id());
                g.setColor(fill);
                g.fillRoundRect(seg.x, seg.y, seg.width, seg.height, 6, 6);
                String text = fit(g, a.request().id(), seg.width);
                if (text != null) {
                    g.setColor(Theme.textOn(fill));
                    g.setFont(Theme.SMALL_BOLD);
                    centered(g, text, seg.x + seg.width / 2, y + 19);
                    g.setFont(Theme.SMALL);
                }
                tip(seg, "<b>" + a.request().id() + "</b> in B" + (i + 1) + "<br>size " + a.request().size()
                        + (fixed ? "<br>internal fragmentation " + a.internalWaste() : ""));
                x += aw;
            }
            int rest = LEFT + w - x;
            if (rest > 1) {
                Rectangle hole = new Rectangle(x + 1, y + 5, rest - 2, ROW - 11);
                boolean wasted = fixed && !perBlock.get(i).isEmpty();
                if (wasted) {
                    g.setColor(Theme.c(Theme.Role.WARNING_SOFT));
                    g.fillRoundRect(hole.x, hole.y, hole.width, hole.height, 6, 6);
                    hatch(g, hole, Theme.mix(Theme.c(Theme.Role.WARNING), Theme.c(Theme.Role.SURFACE), 0.35));
                }
                tip(hole, "<b>B" + (i + 1) + "</b><br>" + (wasted ? "internal fragmentation " : "free hole ") + b.free());
            }
            g.setFont(Theme.SMALL);
            String note = perBlock.get(i).isEmpty() ? "empty · " + Ui.grouped(b.free()) + " free"
                    : (fixed ? Ui.grouped(b.free()) + " wasted" : Ui.grouped(b.free()) + " free");
            label(g, note, LEFT + Math.max(w, 6) + 10, y + 19,
                    fixed && !perBlock.get(i).isEmpty() && b.free() > 0 ? Theme.Role.WARNING : Theme.Role.TEXT_FAINT);
        }

        int ly = TOP + shown * ROW + 24;
        int lx = legend(g, LEFT, ly, Theme.series(0), "Allocated process", false);
        if (fixed) {
            lx = legend(g, lx, ly, Theme.c(Theme.Role.WARNING_SOFT), "Internal fragmentation", true);
        }
        legend(g, lx, ly, Theme.c(Theme.Role.SURFACE_ALT), "Free hole", false);
        if (result.blocks().size() > shown) {
            label(g, "Showing the first " + shown + " of " + result.blocks().size() + " blocks (see the Memory Map table for all).",
                    LEFT, ly + 22, Theme.Role.TEXT_FAINT);
        }
    }
}
