package ossim.ui;

import ossim.core.Segmentation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Physical memory layout for segmentation. The top lane draws each segment from base to base + limit; segments that
 * overlap are pushed to a second lane. Valid translations are marked with a triangle at their physical address,
 * and the table on the right lists every segment as base / limit.
 */
final class SegmentMap extends Chart {
    private static final int LEFT = 24;
    private static final int RIGHT = 24;
    private static final int LANE = 40;
    private static final int TOP = 44;
    private static final int MARKER_LIMIT = 400;

    private final List<Segmentation.Segment> segments;
    private final List<Segmentation.Translation> translations;
    private final List<Integer> lanes = new ArrayList<>();
    private final int laneCount;
    private final long end;

    SegmentMap(List<Segmentation.Segment> segments, List<Segmentation.Translation> translations) {
        super(Math.max(420, Math.min(segments.size(), 400) * 26), 0);
        this.segments = new ArrayList<>(segments);
        this.segments.sort(Comparator.comparingInt(Segmentation.Segment::base));
        this.translations = translations;
        long max = 1;
        List<Long> laneEnds = new ArrayList<>();
        for (Segmentation.Segment s : this.segments) {
            long segEnd = (long) s.base() + s.limit();
            max = Math.max(max, segEnd);
            int lane = 0;
            while (lane < laneEnds.size() && laneEnds.get(lane) > s.base()) {
                lane++;
            }
            if (lane == laneEnds.size()) {
                laneEnds.add(segEnd);
            } else {
                laneEnds.set(lane, segEnd);
            }
            lanes.add(lane);
        }
        laneCount = Math.max(1, laneEnds.size());
        end = max;
        setPreferredSize(new java.awt.Dimension(getPreferredSize().width, TOP + Math.min(laneCount, 6) * LANE + 110));
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        int plotW = width - LEFT - RIGHT;
        g.setFont(Theme.SMALL_BOLD);
        label(g, "PHYSICAL MEMORY  0 – " + Ui.grouped(end), LEFT, 22, Theme.Role.TEXT_MUTED);
        int shownLanes = Math.min(laneCount, 6);
        int lanesBottom = TOP + shownLanes * LANE;

        g.setColor(Theme.c(Theme.Role.SURFACE_ALT));
        g.fillRoundRect(LEFT, TOP, plotW, shownLanes * LANE - 6, 10, 10);

        for (int i = 0; i < segments.size(); i++) {
            int lane = lanes.get(i);
            if (lane >= shownLanes) {
                continue;
            }
            Segmentation.Segment s = segments.get(i);
            int x = LEFT + (int) Math.round(s.base() * (double) plotW / end);
            int w = Math.max(2, (int) Math.round(s.limit() * (double) plotW / end));
            Rectangle r = new Rectangle(x, TOP + lane * LANE + 4, w, LANE - 14);
            Color fill = Theme.series(s.id());
            g.setColor(lane > 0 ? Theme.mix(fill, Theme.c(Theme.Role.DANGER), 0.35) : fill);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            String text = fit(g, "S" + s.id(), w);
            if (text != null) {
                g.setFont(Theme.SMALL_BOLD);
                g.setColor(Theme.textOn(fill));
                centered(g, text, x + w / 2, r.y + r.height / 2 + 5);
                g.setFont(Theme.SMALL);
            }
            tip(r, "<b>Segment " + s.id() + "</b><br>base " + s.base() + " · limit " + s.limit()
                    + "<br>occupies " + s.base() + " – " + ((long) s.base() + s.limit() - 1)
                    + (lane > 0 ? "<br><font color='red'>overlaps another segment</font>" : ""));
        }

        // valid references as markers under the lanes
        int markerY = lanesBottom + 4;
        int count = 0;
        for (Segmentation.Translation t : translations) {
            if (!t.valid() || count++ >= MARKER_LIMIT) {
                continue;
            }
            int x = LEFT + (int) Math.round(t.physical() * (double) plotW / end);
            Polygon tri = new Polygon(new int[]{x, x - 5, x + 5}, new int[]{markerY, markerY + 9, markerY + 9}, 3);
            g.setColor(Theme.c(Theme.Role.SUCCESS));
            g.fill(tri);
            tip(new Rectangle(x - 6, markerY - 2, 12, 14), "<b>(" + t.reference().segment() + ", " + t.reference().offset()
                    + ")</b> → " + t.physical());
        }

        // axis
        int axisY = markerY + 22;
        g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
        g.drawLine(LEFT, axisY, width - RIGHT, axisY);
        double step = niceStep(end, Math.max(2, plotW / 90));
        g.setFont(Theme.SMALL);
        for (double v = 0; v <= end; v += step) {
            int x = LEFT + (int) Math.round(v * plotW / end);
            g.setColor(Theme.c(Theme.Role.BORDER_STRONG));
            g.drawLine(x, axisY, x, axisY + 4);
            g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
            centered(g, Ui.grouped((long) v), x, axisY + 17);
        }
        int ly = axisY + 40;
        int lx = legend(g, LEFT, ly, Theme.series(0), "Segment (base → base + limit)", false);
        lx = legend(g, lx, ly, Theme.c(Theme.Role.SUCCESS), "Valid translated address", false);
        if (laneCount > 1) {
            legend(g, lx, ly, Theme.mix(Theme.series(0), Theme.c(Theme.Role.DANGER), 0.35), "Overlapping segment", false);
        }
    }
}
