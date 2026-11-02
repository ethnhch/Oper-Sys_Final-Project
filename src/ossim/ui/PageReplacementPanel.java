package ossim.ui;

import ossim.core.PageReplacement;
import ossim.core.PageReplacement.Algorithm;
import ossim.core.PageReplacement.Result;
import ossim.util.Parse;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 4: page replacement. */
final class PageReplacementPanel extends ModulePanel {
    private final JTextArea references = Ui.textArea(8, "");
    private final JTextField frames = field("Number of frames", "3", 5);
    private final JComboBox<Algorithm> algorithm = new JComboBox<>(Algorithm.values());

    PageReplacementPanel() {
        super("Page Replacement", Icons.Kind.SWAP, "Memory",
                "Choose which page leaves memory when a new page is needed and every frame is full. Enter a reference "
                + "string of any length and the number of frames. FIFO, LRU, Optimal and Second Chance (Clock) are "
                + "supported; the frame diagram shows every change, hit and fault, and all algorithms are compared.");
        arrange(listInput("Reference string", "Page numbers separated by commas or spaces", references, "Reference string"),
                Ui.row(Ui.form("Frames", frames), Ui.form("Algorithm", algorithm)));
        actions(this::loadSample, "Load Large (5000)", this::loadLarge, this::run);
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

        List<Object[]> comparison = comparison(refs, frameCount);
        List<String> names = new ArrayList<>();
        List<Double> faults = new ArrayList<>();
        for (Object[] row : comparison) {
            names.add((String) row[0]);
            faults.add(((Integer) row[2]).doubleValue());
        }
        output.reset();
        output.addStat("Page faults", Ui.grouped(result.faults()), Theme.Tone.DANGER, Ui.two(result.faultRatio()) + " % fault rate");
        output.addStat("Page hits", Ui.grouped(result.hits()), Theme.Tone.SUCCESS, Ui.two(result.hitRatio()) + " % hit rate");
        output.addStat("References", Ui.grouped(refs.size()), Theme.Tone.NEUTRAL, frameCount + " frames");
        output.addStat("Optimal faults", Ui.grouped(PageReplacement.run(Algorithm.OPTIMAL, refs, frameCount).faults()),
                Theme.Tone.ACCENT, "lower bound");
        output.addComponent("Frame Diagram", new FrameGrid(result, frameCount));
        output.addTable("Frame Trace (" + selected + ")", traceColumns(frameCount), traceRows(result));
        output.addChartAndTable("Compare Algorithms", new BarChart("Page faults", names, faults, true, selected.toString()),
                new String[]{"Algorithm", "Hits", "Faults", "Hit %", "Fault %"}, comparison);
        output.setSummary("Algorithm: " + selected + "   Frames: " + frameCount + "   References: " + refs.size() + "\n"
                + "Page hits  : " + result.hits() + " (" + Ui.two(result.hitRatio()) + " %)\n"
                + "Page faults: " + result.faults() + " (" + Ui.two(result.faultRatio()) + " %)\n"
                + "Optimal gives the minimum possible faults; FIFO may suffer Belady's anomaly (more frames, more faults)."
                + (refs.size() > FrameGrid.STEP_LIMIT ? "\nFrame diagram shows the first " + FrameGrid.STEP_LIMIT
                + " references; the Frame Trace table lists all " + refs.size() + "." : ""));
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
