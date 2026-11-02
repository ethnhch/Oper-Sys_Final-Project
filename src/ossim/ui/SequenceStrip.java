package ossim.ui;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Banker's verdict at a glance: a large SAFE / UNSAFE banner, the safe sequence as chips joined by arrows,
 * and (when unsafe) the processes that could never finish.
 */
final class SequenceStrip extends Chart {
    private static final int CHIP_H = 30;

    private final boolean safe;
    private final List<Integer> sequence;
    private final List<Integer> blocked;
    private final String headline;

    SequenceStrip(boolean safe, List<Integer> sequence, List<Integer> blocked, String headline) {
        super(380, 160 + (Math.max(sequence.size(), blocked.size()) / 12) * (CHIP_H + 14));
        this.safe = safe;
        this.sequence = sequence;
        this.blocked = blocked;
        this.headline = headline;
    }

    @Override
    protected void draw(Graphics2D g, int width, int height) {
        // verdict banner
        Rectangle banner = new Rectangle(16, 14, width - 32, 54);
        g.setColor(safe ? Theme.c(Theme.Role.SUCCESS_SOFT) : Theme.c(Theme.Role.DANGER_SOFT));
        g.fillRoundRect(banner.x, banner.y, banner.width, banner.height, 14, 14);
        Icons.paint(g, safe ? Icons.Kind.SHIELD : Icons.Kind.ALERT, banner.x + 16, banner.y + 15, 24,
                safe ? Theme.c(Theme.Role.SUCCESS) : Theme.c(Theme.Role.DANGER));
        g.setFont(Theme.H2);
        g.setColor(safe ? Theme.c(Theme.Role.SUCCESS) : Theme.c(Theme.Role.DANGER));
        g.drawString(safe ? "SYSTEM IS SAFE" : "SYSTEM IS NOT SAFE", banner.x + 52, banner.y + 24);
        g.setFont(Theme.SMALL);
        g.setColor(Theme.c(Theme.Role.TEXT_MUTED));
        g.drawString(headline, banner.x + 52, banner.y + 42);

        int y = banner.y + banner.height + 30;
        g.setFont(Theme.SMALL_BOLD);
        label(g, safe ? "SAFE SEQUENCE" : "PROCESSES THAT CAN FINISH", 16, y, Theme.Role.TEXT_MUTED);
        y = chips(g, sequence, y + 12, width, true);
        if (!blocked.isEmpty()) {
            y += 22;
            g.setFont(Theme.SMALL_BOLD);
            label(g, "BLOCKED (Need > Work forever)", 16, y, Theme.Role.DANGER);
            chips(g, blocked, y + 12, width, false);
        }
    }

    private int chips(Graphics2D g, List<Integer> ids, int top, int width, boolean finished) {
        int x = 16;
        int y = top;
        g.setFont(Theme.BODY_BOLD);
        FontMetrics fm = g.getFontMetrics();
        if (ids.isEmpty()) {
            g.setFont(Theme.SMALL);
            label(g, "none", x, y + 20, Theme.Role.TEXT_FAINT);
            return y + CHIP_H;
        }
        for (int i = 0; i < ids.size(); i++) {
            String text = "P" + ids.get(i);
            int w = fm.stringWidth(text) + 26;
            if (x + w + 30 > width) {
                x = 16;
                y += CHIP_H + 12;
            }
            Rectangle chip = new Rectangle(x, y, w, CHIP_H);
            g.setColor(finished ? Theme.seriesFor(text) : Theme.c(Theme.Role.DANGER_SOFT));
            g.fillRoundRect(chip.x, chip.y, chip.width, chip.height, CHIP_H, CHIP_H);
            g.setColor(finished ? Theme.textOn(Theme.seriesFor(text)) : Theme.c(Theme.Role.DANGER));
            g.setFont(Theme.BODY_BOLD);
            g.drawString(text, x + 13, y + 20);
            tip(chip, finished ? "<b>" + text + "</b> finishes " + ordinal(i + 1) + "<br>Need ≤ Work, then Work += Allocation"
                    : "<b>" + text + "</b> cannot finish: its Need exceeds the available Work");
            x += w;
            if (finished && i < ids.size() - 1) {
                Icons.paint(g, Icons.Kind.ARROW_RIGHT, x + 4, y + 7, 16, Theme.c(Theme.Role.TEXT_FAINT));
                x += 24;
            } else {
                x += 10;
            }
        }
        return y + CHIP_H;
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }
}
