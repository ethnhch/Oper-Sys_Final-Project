package ossim.ui;

import ossim.core.BankersAlgorithm;
import ossim.util.InputException;
import ossim.util.Parse;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Module 2: deadlock avoidance with the Banker's Algorithm. */
final class BankerPanel extends ModulePanel {
    private final JTextField processCount = field("Number of processes", "5", 4);
    private final JTextField resourceCount = field("Number of resource types", "3", 4);
    private final JTextField requestProcess = field("Request process number", "1", 4);
    private final JTextField requestVector = field("Request vector", "1,0,2", 9);
    private final JPanel tables = Ui.clear(null);
    private MatrixTable allocation;
    private MatrixTable max;
    private MatrixTable available;

    BankerPanel() {
        super("Deadlock · Banker's Algorithm", Icons.Kind.LOCK, "Processes",
                "Avoid deadlock by granting resources only when the system stays safe. Set the number of processes and "
                + "resource types, press Build Tables, then fill in Allocation, Max and Available. Run computes "
                + "Need = Max - Allocation and searches for a safe sequence; Check Request tests one new request.");
        tables.setLayout(new BoxLayout(tables, BoxLayout.Y_AXIS));
        var scroll = Ui.scroll(tables);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        JPanel sizeRow = Ui.row(Ui.form("Processes (n)", processCount), Ui.form("Resource types (m)", resourceCount),
                Ui.form(" ", action("Build Tables", Icons.Kind.GRID, this::buildFromFields)));
        JPanel requestRow = Ui.row(Ui.form("Request by process #", requestProcess), Ui.form("Request vector", requestVector),
                Ui.form(" ", action("Check Request", Icons.Kind.SHIELD, this::checkRequest)));
        JPanel main = Ui.clear(new java.awt.BorderLayout(0, 6));
        main.add(sizeRow, java.awt.BorderLayout.NORTH);
        main.add(scroll, java.awt.BorderLayout.CENTER);
        arrange(main, requestRow);
        actions(this::loadSample, "Load Large (200 × 10)", this::loadLarge, this::run);
        loadSample();
    }

    private void buildFromFields() {
        build(Parse.integer(processCount.getText(), "Number of processes", 1),
                Parse.integer(resourceCount.getText(), "Number of resource types", 1));
    }

    private void build(int n, int m) {
        processCount.setText(String.valueOf(n));
        resourceCount.setText(String.valueOf(m));
        allocation = new MatrixTable("Allocation", n, m, "P");
        max = new MatrixTable("Max", n, m, "P");
        available = new MatrixTable("Available", 1, m, "Avail");
        tables.removeAll();
        tables.add(allocation);
        tables.add(max);
        tables.add(available);
        tables.revalidate();
        tables.repaint();
    }

    private void loadSample() {
        build(5, 3);
        allocation.setValues(new int[][]{{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}});
        max.setValues(new int[][]{{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}});
        available.setValues(new int[][]{{3, 3, 2}});
    }

    private void loadLarge() {
        int n = 200;
        int m = 10;
        Random rnd = new Random();
        int[][] maxValues = new int[n][m];
        int[][] allocValues = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                maxValues[i][j] = rnd.nextInt(10);
                allocValues[i][j] = rnd.nextInt(maxValues[i][j] + 1) / 2;
            }
        }
        int[][] avail = new int[1][m];
        for (int j = 0; j < m; j++) {
            avail[0][j] = 20 + rnd.nextInt(30);
        }
        build(n, m);
        allocation.setValues(allocValues);
        max.setValues(maxValues);
        available.setValues(avail);
    }

    private void requireTables() {
        if (allocation == null) {
            throw new InputException("Press Build Tables first.");
        }
    }

    private void run() {
        requireTables();
        BankersAlgorithm.Result r = BankersAlgorithm.analyze(allocation.values(), max.values(), available.values()[0]);
        show(r, "");
    }

    private void checkRequest() {
        requireTables();
        int[][] alloc = allocation.values();
        int[] avail = available.values()[0];
        int process = Parse.integer(requestProcess.getText(), "Request process number", 0, alloc.length - 1);
        List<Integer> vector = Parse.integerList(requestVector.getText(), "Request vector", 0, Ui.MAX_VALUE);
        if (vector.size() != avail.length) {
            throw new InputException("Request vector needs exactly " + avail.length + " values (one per resource type).");
        }
        BankersAlgorithm.RequestResult rr = BankersAlgorithm.request(alloc, max.values(), avail, process,
                vector.stream().mapToInt(Integer::intValue).toArray());
        show(rr.stateAfter(), "Request by P" + process + " " + vector + ": " + rr.reason() + "\n"
                + (rr.granted() ? "State shown below is AFTER granting the request.\n" : "State shown below is the state evaluated.\n"));
    }

    private void show(BankersAlgorithm.Result r, String prefix) {
        int m = r.need().length == 0 ? 0 : r.need()[0].length;
        String[] columns = new String[m + 1];
        columns[0] = "Process";
        for (int j = 0; j < m; j++) {
            columns[j + 1] = "Need R" + j;
        }
        List<Object[]> needRows = new ArrayList<>();
        for (int i = 0; i < r.need().length; i++) {
            Object[] row = new Object[m + 1];
            row[0] = "P" + i;
            for (int j = 0; j < m; j++) {
                row[j + 1] = r.need()[i][j];
            }
            needRows.add(row);
        }
        List<Object[]> traceRows = new ArrayList<>();
        r.trace().forEach(line -> traceRows.add(new Object[]{line}));

        List<Integer> blocked = new ArrayList<>();
        for (int i = 0; i < r.need().length; i++) {
            if (!r.safeSequence().contains(i)) {
                blocked.add(i);
            }
        }
        output.reset();
        output.addStat("Verdict", r.safe() ? "SAFE" : "UNSAFE", r.safe() ? Theme.Tone.SUCCESS : Theme.Tone.DANGER,
                r.safe() ? "deadlock-free" : "deadlock possible");
        output.addStat("Processes", String.valueOf(r.need().length), Theme.Tone.NEUTRAL, m + " resource types");
        output.addStat("Can finish", r.safeSequence().size() + " / " + r.need().length,
                r.safe() ? Theme.Tone.SUCCESS : Theme.Tone.WARNING, "in safety check");
        output.addStat("Blocked", String.valueOf(blocked.size()), blocked.isEmpty() ? Theme.Tone.NEUTRAL : Theme.Tone.DANGER,
                "Need > Work");
        String headline = prefix.isEmpty() ? "Need = Max - Allocation · a process finishes when Need ≤ Work, then Work += Allocation"
                : prefix.lines().findFirst().orElse("");
        output.addComponent("Safe Sequence", new SequenceStrip(r.safe(), r.safeSequence(), blocked, headline));
        output.addTable("Need = Max - Allocation", columns, needRows);
        output.addTable("Safety Trace", new String[]{"Step"}, traceRows);
        List<String> sequence = new ArrayList<>();
        r.safeSequence().forEach(i -> sequence.add("P" + i));
        output.setSummary(prefix
                + (r.safe() ? "SYSTEM IS SAFE\nSafe sequence: <" + Ui.preview(sequence, 40, ", ") + ">"
                : "SYSTEM IS NOT SAFE - deadlock is possible (see Safety Trace for blocked processes).")
                + "\nNeed = Max - Allocation; a process can finish when Need <= Work, then Work += its Allocation.");
    }
}
