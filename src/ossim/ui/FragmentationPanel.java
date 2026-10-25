package ossim.ui;

import ossim.core.Fragmentation;
import ossim.core.Fragmentation.Mode;
import ossim.core.Fragmentation.Result;
import ossim.core.Fragmentation.Strategy;
import ossim.util.InputException;
import ossim.util.Parse;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 6: memory allocation and fragmentation. */
final class FragmentationPanel extends ModulePanel {
    private final InputTable blocks = new InputTable("Memory blocks / holes", "Size");
    private final InputTable processes = new InputTable("Processes", "Process", "Size");
    private final JComboBox<Strategy> strategy = new JComboBox<>(Strategy.values());
    private final JComboBox<Mode> mode = new JComboBox<>(Mode.values());

    FragmentationPanel() {
        super("<b>Fragmentation</b> - enter any number of memory blocks and processes. <i>Fixed partitions</i> show internal "
                + "fragmentation (unused space inside an allocated block); <i>dynamic partitions</i> split blocks and expose "
                + "external fragmentation (enough free memory in total, but no single hole large enough).");
        JPanel main = new JPanel(new GridLayout(2, 1, 4, 4));
        main.add(blocks);
        main.add(processes);
        arrange(main,
                Ui.labeled("Placement strategy:", strategy),
                Ui.labeled("Partitioning:", mode),
                Ui.flow(action("Load Sample", this::loadSample), action("Load Large (200 / 500)", this::loadLarge),
                        action("Run", this::run)));
        loadSample();
    }

    private void loadSample() {
        List<String[]> sizes = new ArrayList<>();
        for (int s : new int[]{100, 500, 200, 300, 600}) {
            sizes.add(new String[]{String.valueOf(s)});
        }
        blocks.setRows(sizes);
        processes.setRows(List.of(new String[]{"P1", "212"}, new String[]{"P2", "417"},
                new String[]{"P3", "112"}, new String[]{"P4", "426"}));
    }

    private void loadLarge() {
        Random rnd = new Random();
        List<String[]> sizes = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            sizes.add(new String[]{String.valueOf(50 + rnd.nextInt(950))});
        }
        List<String[]> procs = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            procs.add(new String[]{"P" + i, String.valueOf(10 + rnd.nextInt(400))});
        }
        blocks.setRows(sizes);
        processes.setRows(procs);
    }

    private void run() {
        List<Integer> sizes = new ArrayList<>();
        List<String[]> blockRows = blocks.rows();
        for (int i = 0; i < blockRows.size(); i++) {
            sizes.add(Parse.integer(blockRows.get(i)[0], "Block row " + (i + 1) + " size", 1, Ui.MAX_VALUE));
        }
        List<Fragmentation.Request> requests = new ArrayList<>();
        List<String[]> processRows = processes.rows();
        for (int i = 0; i < processRows.size(); i++) {
            String row = "Process row " + (i + 1) + " ";
            if (processRows.get(i)[0].isEmpty()) {
                throw new InputException(row + "name is blank.");
            }
            requests.add(new Fragmentation.Request(processRows.get(i)[0],
                    Parse.integer(processRows.get(i)[1], row + "size", 1, Ui.MAX_VALUE)));
        }
        Strategy selected = (Strategy) strategy.getSelectedItem();
        Mode chosenMode = (Mode) mode.getSelectedItem();
        Result result = Fragmentation.run(selected, chosenMode, sizes, requests);

        output.reset();
        output.addTable("Allocation", new String[]{"Process", "Size", "Block", "Internal Frag.", "Status"}, allocationRows(result));
        output.addTable("Memory Map", new String[]{"Block", "Size", "Processes", "Used", "Free / Wasted"}, mapRows(result));
        output.addTable("Strategy Comparison", new String[]{"Strategy", "Placed", "Not placed", "Internal", "Total free",
                "Largest hole", "External failures"}, comparisonRows(sizes, requests, chosenMode));
        output.setSummary(summary(result));
    }

    private static List<Object[]> allocationRows(Result result) {
        List<Object[]> rows = new ArrayList<>();
        for (Fragmentation.Allocation a : result.allocations()) {
            rows.add(new Object[]{a.request().id(), a.request().size(), a.block() < 0 ? "-" : "B" + (a.block() + 1),
                    a.internalWaste(), a.note()});
        }
        return rows;
    }

    private static List<Object[]> mapRows(Result result) {
        List<Object[]> rows = new ArrayList<>();
        for (Fragmentation.BlockState b : result.blocks()) {
            rows.add(new Object[]{"B" + (b.index() + 1), b.size(), b.residents().isEmpty() ? "(empty)" : String.join(", ", b.residents()),
                    b.used(), b.free()});
        }
        return rows;
    }

    private static List<Object[]> comparisonRows(List<Integer> sizes, List<Fragmentation.Request> requests, Mode mode) {
        List<Object[]> rows = new ArrayList<>();
        for (Strategy s : Strategy.values()) {
            Result r = Fragmentation.run(s, mode, sizes, requests);
            long placed = r.allocations().stream().filter(a -> a.block() >= 0).count();
            rows.add(new Object[]{s.toString(), placed, r.allocations().size() - placed, r.internalFragmentation(),
                    r.totalFree(), r.largestHole(), r.externalFailures()});
        }
        return rows;
    }

    private static String summary(Result r) {
        long placed = r.allocations().stream().filter(a -> a.block() >= 0).count();
        String verdict = r.mode() == Mode.FIXED
                ? "Internal fragmentation (sum of unused space inside allocated blocks): " + r.internalFragmentation()
                : "Internal fragmentation: 0 (blocks are split exactly)";
        String external = r.externalFailures() > 0
                ? "EXTERNAL FRAGMENTATION detected: " + r.externalFailures() + " process(es) failed although total free memory ("
                + r.totalFree() + ") was large enough; the largest single hole is " + r.largestHole() + "."
                : "No external fragmentation failure.";
        return r.strategy() + " / " + r.mode() + "\n"
                + "Placed: " + placed + " of " + r.allocations().size() + "\n"
                + verdict + "\n"
                + "Total free memory: " + r.totalFree() + "   Largest hole: " + r.largestHole() + "\n"
                + external;
    }
}
