package ossim.ui;

import ossim.core.Paging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page-to-frame mapping diagram: logical pages on the left, physical frames on the right, a curve for every
 * page-table entry. Pages referenced by a faulting address appear in red with no curve.
 */
final class PageMap extends Chart {
    static final int LIMIT = 32;
    private static final int ROW = 30;
    private static final int TOP = 54;

    private final List<Paging.Entry> entries;
    private final int frames;
    private final List<Long> faultPages;
    private final int pageSize;

    PageMap(List<Paging.Entry> entries, int frames, int pageSize, List<Long> faultPages) {
        super(460, TOP + Math.max(Math.min(rows(entries, faultPages), LIMIT), Math.min(frames, LIMIT)) * ROW + 70);
        this.entries = entries;
        this.frames = frames;
        this.pageSize = pageSize;
        this.faultPages = faultPages;
    }

    private static int rows(List<Paging.Entry> entries, List<Long> faults) {
        return entries.size() + (int) faults.stream().distinct().count();
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        int leftX = 40;
        int boxW = 150;
        int rightX = width - boxW - 40;
        g.setFont(Theme.SMALL_BOLD);
        label(g, "LOGICAL PAGES", leftX, TOP - 18, Theme.Role.TEXT_MUTED);
        label(g, "PHYSICAL FRAMES", rightX, TOP - 18, Theme.Role.TEXT_MUTED);
        g.setFont(Theme.SMALL);
        label(g, "page size " + Ui.grouped(pageSize), leftX + 110, TOP - 18, Theme.Role.TEXT_FAINT);

        int shownFrames = Math.min(frames, LIMIT);
        Map<Integer, Integer> frameToPage = new HashMap<>();
        for (Paging.Entry e : entries) {
            frameToPage.put(e.frame(), e.page());
        }
        for (int f = 0; f < shownFrames; f++) {
            Rectangle r = new Rectangle(rightX, TOP + f * ROW, boxW, ROW - 6);
            Integer page = frameToPage.get(f);
            g.setColor(page == null ? Theme.c(Theme.Role.SURFACE_ALT) : Theme.mix(Theme.series(page), Theme.c(Theme.Role.SURFACE), 0.6));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Theme.c(Theme.Role.TEXT));
            g.setFont(Theme.BODY_BOLD);
            g.drawString("Frame " + f, r.x + 10, r.y + 17);
            g.setFont(Theme.SMALL);
            g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
            String range = Ui.grouped((long) f * pageSize) + "+";
            g.drawString(page == null ? "free" : range, r.x + boxW - 10 - g.getFontMetrics().stringWidth(page == null ? "free" : range),
                    r.y + 17);
            tip(r, "<b>Frame " + f + "</b><br>" + (page == null ? "free" : "holds page " + page)
                    + "<br>physical " + ((long) f * pageSize) + " – " + ((long) (f + 1) * pageSize - 1));
        }

        int row = 0;
        for (Paging.Entry e : entries) {
            if (row >= LIMIT) {
                break;
            }
            Rectangle r = new Rectangle(leftX, TOP + row * ROW, boxW, ROW - 6);
            Color c = Theme.series(e.page());
            g.setColor(c);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Theme.textOn(c));
            g.setFont(Theme.BODY_BOLD);
            g.drawString("Page " + e.page(), r.x + 10, r.y + 17);
            if (e.frame() < shownFrames) {
                int fy = TOP + e.frame() * ROW + (ROW - 6) / 2;
                int py = r.y + r.height / 2;
                g.setColor(Theme.mix(c, Theme.c(Theme.Role.SURFACE), 0.15));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int mid = (leftX + boxW + rightX) / 2;
                g.draw(new CubicCurve2D.Double(leftX + boxW, py, mid, py, mid, fy, rightX, fy));
                g.fillOval(rightX - 4, fy - 3, 7, 7);
                g.setStroke(HAIRLINE);
            }
            tip(r, "<b>Page " + e.page() + "</b> → frame " + e.frame() + "<br>logical " + ((long) e.page() * pageSize)
                    + " – " + ((long) (e.page() + 1) * pageSize - 1));
            row++;
        }
        for (Long p : faultPages.stream().distinct().toList()) {
            if (row >= LIMIT) {
                break;
            }
            Rectangle r = new Rectangle(leftX, TOP + row * ROW, boxW, ROW - 6);
            g.setColor(Theme.c(Theme.Role.DANGER_SOFT));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Theme.c(Theme.Role.DANGER));
            g.setFont(Theme.BODY_BOLD);
            g.drawString("Page " + p, r.x + 10, r.y + 17);
            g.setFont(Theme.SMALL);
            g.drawString("not mapped", r.x + boxW + 10, r.y + 17);
            tip(r, "<b>Page " + p + "</b><br>not in the page table → page fault");
            row++;
        }
        g.setFont(Theme.SMALL);
        int ly = height - 18;
        if (entries.size() > LIMIT || frames > LIMIT) {
            label(g, "Diagram shows the first " + LIMIT + " pages and frames; the table lists every translation.",
                    leftX, ly, Theme.Role.TEXT_FAINT);
        } else {
            int lx = legend(g, leftX, ly, Theme.series(0), "Mapped page", false);
            lx = legend(g, lx, ly, Theme.c(Theme.Role.DANGER_SOFT), "Page fault (unmapped)", false);
            legend(g, lx, ly, Theme.c(Theme.Role.SURFACE_ALT), "Free frame", false);
        }
    }
}
