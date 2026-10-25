package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Paging: page table lookup and logical-to-physical address translation. */
public final class Paging {

    public record Entry(int page, int frame) { }

    public record Translation(long logical, long page, long offset, Integer frame, Long physical,
                              String calculation, String status) { }

    private Paging() { }

    public static List<Translation> translate(int pageSize, int totalFrames, List<Entry> pageTable, List<Long> addresses) {
        if (pageSize < 1 || totalFrames < 1) {
            throw new InputException("Page size and number of frames must be at least 1.");
        }
        Map<Integer, Integer> map = new HashMap<>();
        Set<Integer> usedFrames = new HashSet<>();
        for (Entry e : pageTable) {
            if (e.frame() >= totalFrames) {
                throw new InputException("Page " + e.page() + " maps to frame " + e.frame()
                        + ", but physical memory only has frames 0.." + (totalFrames - 1) + ".");
            }
            if (map.put(e.page(), e.frame()) != null) {
                throw new InputException("Page " + e.page() + " appears more than once in the page table.");
            }
            if (!usedFrames.add(e.frame())) {
                throw new InputException("Frame " + e.frame() + " is assigned to more than one page.");
            }
        }
        List<Translation> out = new ArrayList<>(addresses.size());
        for (long address : addresses) {
            long page = address / pageSize;
            long offset = address % pageSize;
            String split = address + " = " + page + " x " + pageSize + " + " + offset;
            Integer frame = page <= Integer.MAX_VALUE ? map.get((int) page) : null;
            if (frame == null) {
                out.add(new Translation(address, page, offset, null, null, split, "PAGE FAULT: page " + page + " is not mapped"));
            } else {
                long physical = (long) frame * pageSize + offset;
                out.add(new Translation(address, page, offset, frame, physical,
                        split + " ; " + frame + " x " + pageSize + " + " + offset + " = " + physical, "OK"));
            }
        }
        return out;
    }
}
