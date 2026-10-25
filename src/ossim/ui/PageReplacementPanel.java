package ossim.ui;

import ossim.core.PageReplacement;
import ossim.core.PageReplacement.Algorithm;
import ossim.core.PageReplacement.Result;
import ossim.util.Parse;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 4: page replacement. */
final class PageReplacementPanel extends ModulePanel {
    private final JTextArea references = Ui.textArea(8, "");
    private final JTextField frames = new JTextField("3", 5);
    private final JComboBox<Algorithm> algorithm = new JComboBox<>(Algorithm.values());

    PageReplacementPanel() {
        super("<b>Page Replacement</b> - enter a reference string of any length and the number of frames. FIFO, LRU, Optimal "
                + "and Second Chance are supported; the trace shows every frame change, hit and fault, and all algorithms "
                + "are compared on each run.");
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createTitledBorder("Reference string (numbers separated by commas or spaces)"));
        box.add(Ui.scroll(references), BorderLayout.CENTER);
        arrange(box,
                Ui.flow(new JLabel("Frames:"), frames, new JLabel("Algorithm:"), algorithm),
                Ui.flow(action("Load Sample", this::loadSample), action("Load Large (5000)", this::loadLarge),
                        action("Run", this::run)));
        loadSample();
    }

    private void loadSample() {
        references.setText("7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1");
        frames.setText("3");
    }

    private void loadLarge() {
        Random rnd = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            text.append(rnd.nextInt(50)).append(i < 4999 ? ", " : "");
        }
        references.setText(text.toString());
        frames.setText("8");
    }

    private void run() {
        List<Integer> refs = Parse.integerList(references.getText(), "Reference string", 0, Ui.MAX_VALUE);
        int frameCount = Parse.integer(frames.getText(), "Number of frames", 1);
        Algorithm selected = (Algorithm) algorithm.getSelectedItem();
        Result result = PageReplacement.run(selected, refs, frameCount);

        output.reset();
        output.addTable("Frame Trace (" + selected + ")", traceColumns(frameCount), traceRows(result));
        output.addTable("Comparison", new String[]{"Algorithm", "Hits", "Faults", "Hit %", "Fault %"}, comparison(refs, frameCount));
        output.setSummary("Algorithm: " + selected + "   Frames: " + frameCount + "   References: " + refs.size() + "\n"
                + "Page hits  : " + result.hits() + " (" + Ui.two(result.hitRatio()) + " %)\n"
                + "Page faults: " + result.faults() + " (" + Ui.two(result.faultRatio()) + " %)\n"
                + "Optimal gives the minimum possible faults; FIFO may suffer Belady's anomaly (more frames, more faults).");
    }

    private static String[] traceColumns(int frameCount) {
        String[] columns = new String[frameCount + 4];
        columns[0] = "Step";
        columns[1] = "Page";
        for (int f = 0; f < frameCount; f++) {
            columns[f + 2] = "F" + (f + 1);
        }
        columns[frameCount + 2] = "Result";
        columns[frameCount + 3] = "Evicted";
        return columns;
    }

    private static List<Object[]> traceRows(Result result) {
        List<Object[]> rows = new ArrayList<>();
        for (PageReplacement.Step s : result.steps()) {
            int f = s.frames().size();
            Object[] row = new Object[f + 4];
            row[0] = s.index() + 1;
            row[1] = s.page();
            for (int i = 0; i < f; i++) {
                row[i + 2] = s.frames().get(i) == null ? "-" : s.frames().get(i);
            }
            row[f + 2] = s.hit() ? "HIT" : "FAULT";
            row[f + 3] = s.evicted() == null ? "-" : s.evicted();
            rows.add(row);
        }
        return rows;
    }

    private static List<Object[]> comparison(List<Integer> refs, int frameCount) {
        List<Object[]> rows = new ArrayList<>();
        for (Algorithm a : Algorithm.values()) {
            Result r = PageReplacement.run(a, refs, frameCount);
            rows.add(new Object[]{a.toString(), r.hits(), r.faults(), Ui.two(r.hitRatio()), Ui.two(r.faultRatio())});
        }
        return rows;
    }
}
