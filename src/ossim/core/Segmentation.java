package ossim.core;

import ossim.util.InputException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Segmentation: segment table, limit checking and (segment, offset) translation. */
public final class Segmentation {

    public record Segment(int id, int base, int limit) { }

    public record Reference(int segment, int offset) { }

    public record Translation(Reference reference, Segment segment, Long physical, String status) {
        public boolean valid() {
            return physical != null;
        }
    }

    private Segmentation() { }

    public static List<Translation> translate(List<Segment> table, List<Reference> references) {
        Map<Integer, Segment> byId = indexById(table);
        List<Translation> out = new ArrayList<>(references.size());
        for (Reference ref : references) {
            Segment seg = byId.get(ref.segment());
            if (seg == null) {
                out.add(new Translation(ref, null, null, "ERROR: segment " + ref.segment() + " is not defined"));
            } else if (ref.offset() >= seg.limit()) {
                out.add(new Translation(ref, seg, null, "ERROR: offset " + ref.offset() + " >= limit " + seg.limit() + " (addressing trap)"));
            } else {
                out.add(new Translation(ref, seg, (long) seg.base() + ref.offset(), "OK"));
            }
        }
        return out;
    }

    /** Describes segments whose physical ranges overlap (allowed, but worth warning about). */
    public static List<String> overlapWarnings(List<Segment> table) {
        List<Segment> sorted = new ArrayList<>(table);
        sorted.sort(Comparator.comparingInt(Segment::base));
        List<String> warnings = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            Segment a = sorted.get(i - 1);
            Segment b = sorted.get(i);
            if ((long) a.base() + a.limit() > b.base()) {
                warnings.add("Segments " + a.id() + " and " + b.id() + " overlap in physical memory.");
            }
        }
        return warnings;
    }

    private static Map<Integer, Segment> indexById(List<Segment> table) {
        if (table.isEmpty()) {
            throw new InputException("Define at least one segment.");
        }
        Map<Integer, Segment> byId = new HashMap<>();
        for (Segment s : table) {
            if (s.limit() < 1) {
                throw new InputException("Segment " + s.id() + ": limit must be at least 1.");
            }
            if (byId.put(s.id(), s) != null) {
                throw new InputException("Segment " + s.id() + " is defined more than once.");
            }
        }
        return byId;
    }
}
