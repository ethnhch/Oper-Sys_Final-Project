package ossim.ui;

import ossim.core.PageReplacement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Classic textbook frame diagram: one column per reference, one row per frame. The page loaded on a fault is
 * outlined, hits are marked green and faults red underneath, and the evicted page is listed per step.
 */
final class FrameGrid extends Chart {
    static final int STEP_LIMIT = 400;
    private static final int CELL = 30;
    private static final int LEFT = 70;
    private static final int TOP = 58;

    private final List<PageReplacement.Step> steps;
    private final int frames;

    FrameGrid(PageReplacement.Result result, int frames) {
        super(LEFT + Math.min(result.steps().size(), STEP_LIMIT) * CELL + 30, TOP + frames * CELL + 96);
        this.steps = result.steps().subList(0, Math.min(result.steps().size(), STEP_LIMIT));
        this.frames = frames;
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        g.setFont(Theme.SMALL_BOLD);
        label(g, "REFERENCE", 12, TOP - 14, Theme.Role.TEXT_MUTED);
        for (int f = 0; f < frames; f++) {
            label(g, "Frame " + (f + 1), 12, TOP + f * CELL + CELL / 2 + 5, Theme.Role.TEXT_MUTED);
        }
        int resultY = TOP + frames * CELL + 8;
        label(g, "RESULT", 12, resultY + 17, Theme.Role.TEXT_MUTED);
        label(g, "EVICTED", 12, resultY + 45, Theme.Role.TEXT_MUTED);

        for (int i = 0; i < steps.size(); i++) {
            PageReplacement.Step s = steps.get(i);
            int x = LEFT + i * CELL;
            // reference header
            g.setFont(Theme.BODY_BOLD);
            g.setColor(Theme.c(Theme.Role.TEXT));
            centered(g, String.valueOf(s.page()), x + CELL / 2, TOP - 14);
            g.setFont(Theme.SMALL);
            g.setColor(Theme.c(Theme.Role.TEXT_FAINT));
            centered(g, String.valueOf(s.index() + 1), x + CELL / 2, TOP - 34);

            for (int f = 0; f < frames; f++) {
                int y = TOP + f * CELL;
                Integer page = f < s.frames().size() ? s.frames().get(f) : null;
                Rectangle cell = new Rectangle(x + 2, y + 2, CELL - 4, CELL - 4);
                if (page == null) {
                    g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
                    g.fillRoundRect(cell.x, cell.y, cell.width, cell.height, 7, 7);
                    continue;
                }
                Color base = Theme.series(page);
                boolean loadedHere = !s.hit() && page == s.page() && isNewIn(i, f);
                g.setColor(Theme.mix(base, Theme.c(Theme.Role.SURFACE), loadedHere ? 0.1 : 0.72));
                g.fillRoundRect(cell.x, cell.y, cell.width, cell.height, 7, 7);
                if (loadedHere) {
                    g.setColor(Theme.c(Theme.Role.TEXT));
                    g.setStroke(new BasicStroke(1.6f));
                    g.drawRoundRect(cell.x, cell.y, cell.width - 1, cell.height - 1, 7, 7);
                    g.setStroke(HAIRLINE);
                }
                g.setFont(loadedHere ? Theme.BODY_BOLD : Theme.BODY);
                g.setColor(loadedHere ? Theme.textOn(Theme.mix(base, Theme.c(Theme.Role.SURFACE), 0.1)) : Theme.c(Theme.Role.TEXT));
                centered(g, String.valueOf(page), x + CELL / 2, y + CELL / 2 + 5);
            }
            // hit / fault marker
            Color tone = s.hit() ? Theme.c(Theme.Role.SUCCESS) : Theme.c(Theme.Role.DANGER);
            g.setColor(s.hit() ? Theme.c(Theme.Role.SUCCESS_SOFT) : Theme.c(Theme.Role.DANGER_SOFT));
            g.fillRoundRect(x + 4, resultY + 2, CELL - 8, 22, 8, 8);
            g.setColor(tone);
            g.setFont(Theme.SMALL_BOLD);
            centered(g, s.hit() ? "H" : "F", x + CELL / 2, resultY + 17);
            g.setFont(Theme.SMALL);
            g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
            centered(g, s.evicted() == null ? "" : String.valueOf(s.evicted()), x + CELL / 2, resultY + 45);
            tip(new Rectangle(x, TOP - 50, CELL, height), "<b>Step " + (s.index() + 1) + " · page " + s.page() + "</b><br>"
                    + (s.hit() ? "HIT (already in memory)" : "FAULT" + (s.evicted() == null ? " (free frame used)"
                    : " · evicted page " + s.evicted())) + "<br>frames: " + s.frames().toString().replace("null", "-"));
        }
        g.setFont(Theme.SMALL);
        int ly = height - 14;
        int lx = legend(g, LEFT, ly, Theme.c(Theme.Role.SUCCESS_SOFT), "H = hit", false);
        lx = legend(g, lx, ly, Theme.c(Theme.Role.DANGER_SOFT), "F = page fault", false);
        legend(g, lx, ly, Theme.c(Theme.Role.SURFACE_ALT), "outlined = page loaded at this step", false);
    }

    /** True when frame f changed at step i (so the highlighted cell is the one the fault filled). */
    private boolean isNewIn(int i, int f) {
        if (i == 0) {
            return true;
        }
        List<Integer> before = steps.get(i - 1).frames();
        Integer prev = f < before.size() ? before.get(f) : null;
        Integer now = steps.get(i).frames().get(f);
        return prev == null ? now != null : !prev.equals(now);
    }
}
