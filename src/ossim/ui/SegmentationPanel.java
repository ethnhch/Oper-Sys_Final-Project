package ossim.ui;

import ossim.core.Segmentation;
import ossim.util.Parse;

import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 5: segmentation. */
final class SegmentationPanel extends ModulePanel {
    private static final String[] COLUMNS = {"Segment", "Offset", "Base", "Limit", "Physical", "Status"};

    private final InputTable segments = new InputTable("Segment Table", "Segment", "Base", "Limit");
    private final InputTable addresses = new InputTable("Logical addresses (segment, offset)", "Segment", "Offset");

    SegmentationPanel() {
        super("<b>Segmentation</b> - define any number of segments with a base address and a limit, then list logical addresses "
                + "as (segment, offset). A translation is valid only when the segment exists and offset &lt; limit; "
                + "otherwise an addressing error is reported. Physical = base + offset.");
        JPanel main = new JPanel(new GridLayout(2, 1, 4, 4));
        main.add(segments);
        main.add(addresses);
        arrange(main, Ui.flow(action("Load Sample", this::loadSample),
                action("Load Large (500 segments)", this::loadLarge), action("Run", this::run)));
        loadSample();
    }

    private void loadSample() {
        segments.setRows(List.of(new String[]{"0", "1400", "1000"}, new String[]{"1", "6300", "400"},
                new String[]{"2", "4300", "400"}, new String[]{"3", "3200", "1100"}, new String[]{"4", "4700", "1000"}));
        addresses.setRows(List.of(new String[]{"2", "53"}, new String[]{"3", "852"}, new String[]{"0", "1222"},
                new String[]{"4", "500"}, new String[]{"7", "0"}, new String[]{"1", "399"}, new String[]{"1", "400"}));
    }

    private void loadLarge() {
        Random rnd = new Random();
        List<String[]> table = new ArrayList<>();
        int base = 0;
        for (int i = 0; i < 500; i++) {
            int limit = 50 + rnd.nextInt(450);
            table.add(new String[]{String.valueOf(i), String.valueOf(base), String.valueOf(limit)});
            base += limit;
        }
        List<String[]> refs = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            refs.add(new String[]{String.valueOf(rnd.nextInt(520)), String.valueOf(rnd.nextInt(520))});
        }
        segments.setRows(table);
        addresses.setRows(refs);
    }

    private void run() {
        List<Segmentation.Segment> table = new ArrayList<>();
        List<String[]> segmentRows = segments.rows();
        for (int i = 0; i < segmentRows.size(); i++) {
            String row = "Segment row " + (i + 1) + " ";
            table.add(new Segmentation.Segment(Parse.integer(segmentRows.get(i)[0], row + "number", 0),
                    Parse.integer(segmentRows.get(i)[1], row + "base", 0),
                    Parse.integer(segmentRows.get(i)[2], row + "limit", 1)));
        }
        List<Segmentation.Reference> refs = new ArrayList<>();
        List<String[]> addressRows = addresses.rows();
        for (int i = 0; i < addressRows.size(); i++) {
            String row = "Address row " + (i + 1) + " ";
            refs.add(new Segmentation.Reference(Parse.integer(addressRows.get(i)[0], row + "segment", 0),
                    Parse.integer(addressRows.get(i)[1], row + "offset", 0)));
        }
        List<Segmentation.Translation> result = Segmentation.translate(table, refs);

        List<Object[]> rows = new ArrayList<>();
        int valid = 0;
        for (Segmentation.Translation t : result) {
            valid += t.valid() ? 1 : 0;
            rows.add(new Object[]{t.reference().segment(), t.reference().offset(),
                    t.segment() == null ? "-" : t.segment().base(), t.segment() == null ? "-" : t.segment().limit(),
                    t.valid() ? t.physical() : "-", t.status()});
        }
        List<String> warnings = Segmentation.overlapWarnings(table);
        output.reset();
        output.addTable("Address Translation", COLUMNS, rows);
        output.setSummary("Segments: " + table.size() + "   Addresses: " + result.size()
                + "   Valid: " + valid + "   Errors: " + (result.size() - valid) + "\n"
                + "Rule: valid if the segment exists and 0 <= offset < limit; physical = base + offset.\n"
                + (warnings.isEmpty() ? "No overlapping segments." : "Warning: " + Ui.preview(warnings, 5, " ")));
    }
}
