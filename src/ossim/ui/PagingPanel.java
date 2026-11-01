package ossim.ui;

import ossim.core.Paging;
import ossim.util.InputException;
import ossim.util.Parse;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Module 3: paging. */
final class PagingPanel extends ModulePanel {
    private static final String[] COLUMNS = {"Logical", "Page (p)", "Offset (d)", "Frame", "Physical", "Calculation", "Status"};

    private final JTextField pageSize = field("Page size", "1024", 6);
    private final JTextField frames = field("Number of frames", "8", 6);
    private final InputTable pageTable = table(new InputTable("Page table (page → frame)", "Page", "Frame"), "Page table row ");
    private final JTextArea addresses = Ui.textArea(4, "");

    PagingPanel() {
        super("Paging", Icons.Kind.GRID, "Memory",
                "Map fixed-size logical pages onto physical frames. Define the page size, the number of frames and the "
                + "page table, then list logical addresses. Each address is split into page and offset and translated as "
                + "physical = frame × page size + offset; an unmapped page is reported as a page fault.");
        JPanel main = Ui.clear(new java.awt.GridLayout(2, 1, 0, 12));
        main.add(pageTable);
        main.add(listInput("Logical addresses", "Any count, separated by commas or spaces", addresses, "Logical address"));
        arrange(main, Ui.row(Ui.form("Page size (bytes)", pageSize), Ui.form("Physical frames", frames)));
        actions(this::loadSample, "Load Large (1000 pages)", this::loadLarge, this::run);
        loadSample();
    }

    private void loadSample() {
        pageSize.setText("1024");
        frames.setText("8");
        pageTable.setRows(List.of(new String[]{"0", "5"}, new String[]{"1", "2"}, new String[]{"2", "7"}, new String[]{"3", "0"}));
        addresses.setText("100, 1500, 3000, 4000, 5000, 2048, 3071");
    }

    private void loadLarge() {
        int pages = 1000;
        pageSize.setText("4096");
        frames.setText(String.valueOf(pages));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            order.add(i);
        }
        Collections.shuffle(order);
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            rows.add(new String[]{String.valueOf(i), String.valueOf(order.get(i))});
        }
        pageTable.setRows(rows);
        Random rnd = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            text.append(rnd.nextInt((int) (pages * 4096L * 1.05))).append(i < 999 ? ", " : "");
        }
        addresses.setText(text.toString());
    }

    private void run() {
        int size = Parse.integer(pageSize.getText(), "Page size", 1, Ui.MAX_VALUE);
        int frameCount = Parse.integer(frames.getText(), "Number of frames", 1);
        List<Paging.Entry> entries = new ArrayList<>();
        List<String[]> rows = pageTable.rows();
        for (int i = 0; i < rows.size(); i++) {
            String row = "Page table row " + (i + 1) + " ";
            entries.add(new Paging.Entry(Parse.integer(rows.get(i)[0], row + "page", 0),
                    Parse.integer(rows.get(i)[1], row + "frame", 0)));
        }
        List<Long> logical = new ArrayList<>();
        Parse.integerList(addresses.getText(), "Logical address", 0, Integer.MAX_VALUE).forEach(a -> logical.add((long) a));
        if (logical.isEmpty()) {
            throw new InputException("Enter at least one logical address.");
        }
        List<Paging.Translation> result = Paging.translate(size, frameCount, entries, logical);

        List<Object[]> table = new ArrayList<>();
        long faults = 0;
        List<Long> faultPages = new ArrayList<>();
        for (Paging.Translation t : result) {
            faults += t.physical() == null ? 1 : 0;
            if (t.physical() == null) {
                faultPages.add(t.page());
            }
            table.add(new Object[]{t.logical(), t.page(), t.offset(), t.frame() == null ? "-" : t.frame(),
                    t.physical() == null ? "-" : t.physical(), t.calculation(), t.status()});
        }
        output.reset();
        output.addStat("Addresses", Ui.grouped(result.size()), Theme.Tone.NEUTRAL, "logical references");
        output.addStat("Translated", Ui.grouped(result.size() - faults), Theme.Tone.SUCCESS, "valid mappings");
        output.addStat("Page faults", Ui.grouped(faults), faults > 0 ? Theme.Tone.DANGER : Theme.Tone.NEUTRAL,
                "page not in table");
        output.addStat("Physical memory", Ui.grouped((long) size * frameCount), Theme.Tone.ACCENT,
                frameCount + " frames × " + size);
        output.addTable("Address Translation", COLUMNS, table);
        output.addComponent("Page → Frame Map", new PageMap(entries, frameCount, size, faultPages));
        output.setSummary("Page size: " + size + "   Frames: " + frameCount + "   Physical memory: " + (long) size * frameCount + "\n"
                + "Page table entries: " + entries.size() + "\n"
                + "Addresses: " + result.size() + "   Translated: " + (result.size() - faults) + "   Page faults: " + faults + "\n"
                + "page = address / page size ; offset = address % page size ; physical = frame x page size + offset");
    }
}
