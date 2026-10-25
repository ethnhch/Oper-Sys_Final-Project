# Mini Operating System Simulator (Java / Swing)

A GUI-based simulator covering the eight required Operating System modules: **CPU Scheduling, Deadlock (Banker's Algorithm), Paging, Page Replacement, Segmentation, Fragmentation, Disk Scheduling and Multiprogramming**.

Every module accepts a **user-defined, unlimited number of items** (rows are added in the GUI, not hardcoded), validates input, and performs the real calculation. The GUI only collects input and displays results.

---

## 1. Quick Start

**Requirements:** JDK 17 or newer (developed and tested on JDK 21). No external libraries.

```bash
./build.sh              # compiles and creates MiniOS.jar   (Windows: build.bat)
java -jar MiniOS.jar    # starts the simulator
./test.sh               # runs both test suites
```

Every module has three buttons: **Load Sample** (small textbook dataset), **Load Large** (hundreds to thousands of items), and **Run**.

---

## 2. Project Structure

```
MiniOS/
├── src/ossim/
│   ├── Main.java                    entry point
│   ├── util/   InputException, Parse        validation helpers (messages shown to the user)
│   ├── core/   one class per module         PURE LOGIC, no Swing  (the algorithms)
│   │           CpuScheduler, BankersAlgorithm, Paging, PageReplacement,
│   │           Segmentation, Fragmentation, DiskScheduler, Multiprogramming
│   └── ui/     one panel per module         Swing only, no algorithms
│               MainFrame, ModulePanel (base), InputTable, MatrixTable,
│               OutputPane, GanttChart, HeadPathChart + 8 *Panel classes
├── tests/      SelfTest (algorithms), GuiSmokeTest (panels)
├── build.sh / build.bat / test.sh / manifest.txt
├── MiniOS.jar                       runnable package
└── README.md, TEST_RESULTS_*.txt
```

### Why it is "clean, not spaghetti"
- **Strict layering:** `core` never imports Swing; `ui` never contains an algorithm. Each algorithm can be tested without a GUI.
- **One class per module**, each with a single public entry point (`run`, `analyze`, `translate`).
- **Immutable `record`s** carry results (`Result`, `Row`, `Step`, `Move`…), so panels only read data.
- **One shared base class** (`ModulePanel`) gives every screen the same layout and the same error handling, so there is no duplicated UI code.
- **One validation path:** parsing throws `InputException`; `ModulePanel.action()` is the only place that catches it and shows a dialog.

---

## 3. GUI Guide

The left tab strip is the main menu (modules 1–8). Each screen has the same layout:

| Area | Content |
|---|---|
| Top | One-paragraph description of the module |
| Left | Input tables (**Add Row / Remove Selected / Clear**) or text areas, option selectors, and the **Load Sample / Load Large / Run** buttons |
| Right | Result tabs (tables, charts) and a text summary with totals and formulas |

**Dynamic input:** tables grow row by row; reference strings and request lists are free text of any length; the Banker's tables are generated from the process/resource counts you type. The only numeric limit is **1,000,000 per single value** (to keep sums inside `int`). There is **no limit on the number of items**.

**Error handling:** blank fields, non-numbers, negative values, out-of-range values and inconsistent data (duplicate IDs, Allocation > Max, request beyond the disk, frame out of range, …) show a message naming the row and field. They never crash the program.

---

## 4. Modules: Input → Processing → Output

### 4.1 CPU Scheduling — `CpuScheduler`
**Input:** any number of processes (PID, Arrival, Burst, Priority), algorithm, time quantum.
**Algorithms:** FCFS, SJF (non-preemptive), SRTF (preemptive SJF), Priority (non-preemptive), Priority (preemptive), Round Robin.

**Processing**
- Non-preemptive / preemptive algorithms share one engine: at each decision point choose the *ready* process with the smallest key (arrival for FCFS, burst for SJF, remaining time for SRTF, priority number for Priority). Ties → earlier arrival → input order. Preemptive modes re-decide at every arrival.
- Round Robin uses a FIFO queue; processes that arrive during a slice enter the queue **before** the preempted process.
- CPU idle gaps are shown as `idle` in the Gantt chart.

**Formulas:** `Turnaround = Completion − Arrival`, `Waiting = Turnaround − Burst`, `Response = First run − Arrival`, `CPU utilization = total burst ÷ (finish − first arrival)`.

**Output:** per-process table, Gantt chart drawn to scale, execution order, averages, and a **comparison tab of all algorithms** with the best average waiting time called out.

### 4.2 Deadlock — Banker's Algorithm — `BankersAlgorithm`
**Input:** number of processes *n* and resource types *m*; Allocation (n×m), Max (n×m), Available (m).
**Processing:**
1. Validate (no negatives, `Allocation ≤ Max`).
2. `Need = Max − Allocation`.
3. Safety algorithm: `Work = Available`; repeatedly scan processes in order; a process with `Need ≤ Work` can finish, so `Work += Allocation`. All finished → **safe**, and the finishing order is the safe sequence.
4. **Check Request:** grants `Request` only if `Request ≤ Need`, `Request ≤ Available`, and the state after tentative allocation is still safe.

**Output:** Need matrix, step-by-step safety trace (including which processes are blocked when unsafe), `SAFE/UNSAFE` verdict, safe sequence, request verdict.
*Note:* more than one safe sequence can exist; this program reports the first one found by in-order scanning.

### 4.3 Paging — `Paging`
**Input:** page size, number of physical frames, page table (page → frame), list of logical addresses.
**Processing:** `page = address ÷ size`, `offset = address mod size`, `physical = frame × size + offset`. Unmapped page → **page fault**. Rejects duplicate pages, a frame used twice, or a frame outside physical memory.
**Output:** table of page, offset, frame, physical address, the arithmetic shown as text, and status.

### 4.4 Page Replacement — `PageReplacement`
**Input:** reference string of any length, number of frames, algorithm.
**Algorithms:** FIFO, LRU, Optimal, Second Chance (Clock).
**Processing:** empty frames are filled left to right; otherwise the victim is
- FIFO: the oldest loaded page (circular pointer),
- LRU: the frame with the smallest last-used time,
- Optimal: the page whose next use is farthest in the future (a precomputed next-occurrence array makes this O(n)),
- Clock: sweep from the pointer, clearing reference bits until a page with bit 0 is found.

**Output:** step-by-step frame table (hit/fault and evicted page), hit/fault counts and ratios, and a comparison of all algorithms.

### 4.5 Segmentation — `Segmentation`
**Input:** any number of segments (number, base, limit) and logical addresses (segment, offset).
**Processing:** segment must exist, and `0 ≤ offset < limit`; then `physical = base + offset`. Otherwise an addressing error (undefined segment, or offset ≥ limit) is reported. Overlapping segments are flagged as a warning.
**Output:** translation table with base, limit, physical address and status; counts of valid and invalid references.

### 4.6 Fragmentation — `Fragmentation`
**Input:** any number of memory blocks and processes (name, size); strategy; partitioning mode.
**Strategies:** First Fit, Best Fit, Worst Fit, Next Fit.
**Modes**
- **Fixed partitions:** one process per block. `Internal fragmentation = block size − process size`.
- **Dynamic (split) partitions:** the block is split; the leftover remains a hole. When a process cannot be placed but `total free ≥ its size`, **external fragmentation** is reported.

**Output:** allocation table, memory map (processes per block, used, free/wasted), a comparison across all four strategies, and totals (internal, free, largest hole, external failures).

### 4.7 Disk Scheduling — `DiskScheduler`
**Input:** any number of cylinder requests, initial head, highest cylinder (disk range `0..max`), direction, algorithm.
**Algorithms:** FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK.
**Conventions (stated so results are reproducible):**
- SCAN / C-SCAN travel all the way to the disk edge; LOOK / C-LOOK turn at the last request.
- C-SCAN / C-LOOK return jumps are **counted by default** (textbook figures); a checkbox excludes them.
- SSTF ties → first request in input order.

**Output:** step-by-step movement table (from, to, distance, cumulative), head-movement chart (jumps drawn dashed red), total movement, average per request, and a comparison of all algorithms.

### 4.8 Multiprogramming — `Multiprogramming`
**Input:** total memory, time quantum, any number of programs (name, arrival, memory, CPU burst).
**Model:** a long-term queue admits programs **first-come first-served** while memory allows (a program that doesn't fit blocks the queue, preventing starvation of large jobs). Resident programs share the CPU by Round Robin. Memory is released when a program finishes, which admits waiting programs.
**Output:** system timeline (running program, resident programs, memory used and %, programs waiting for memory), per-program statistics (admission delay, turnaround, waiting), **CPU utilization, time-weighted memory utilization, maximum degree of multiprogramming, throughput**.

---

## 5. Sample Inputs and Outputs (produced by the program)

**CPU Scheduling** — P1(0,8,pri 3), P2(1,4,pri 1), P3(2,9,pri 4), P4(3,5,pri 2), quantum 4

| Algorithm | Avg waiting | Avg turnaround | Avg response |
|---|---|---|---|
| FCFS | 8.75 | 15.25 | 8.75 |
| SJF | 7.75 | 14.25 | 7.75 |
| SRTF | 6.50 | 13.00 | 4.25 |
| Priority | 7.75 | 14.25 | 7.75 |
| Priority (preemptive) | 6.50 | 13.00 | 4.25 |
| Round Robin | 11.75 | 18.25 | 4.50 |

SRTF Gantt: `P1[0-1] P2[1-5] P4[5-10] P1[10-17] P3[17-26]`

**Banker's** — 5 processes, 3 resources, Available (3,3,2) → `SAFE`, safe sequence `<P1, P3, P4, P0, P2>`; Need = `[7,4,3] [1,2,2] [6,0,0] [0,1,1] [4,3,1]`.

**Page Replacement** — `7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1`, 3 frames

| Algorithm | Hits | Faults |
|---|---|---|
| FIFO | 5 | 15 |
| LRU | 8 | 12 |
| Optimal | 11 | 9 |
| Second Chance | 6 | 14 |

**Disk Scheduling** — `98 183 37 122 14 124 65 67`, head 53, 0..199, direction up

| Algorithm | Total movement | Service order |
|---|---|---|
| FCFS | 640 | 98, 183, 37, 122, 14, 124, 65, 67 |
| SSTF | 236 | 65, 67, 37, 14, 98, 122, 124, 183 |
| SCAN | 331 | 65, 67, 98, 122, 124, 183, 37, 14 |
| C-SCAN | 382 | 65, 67, 98, 122, 124, 183, 14, 37 |
| LOOK | 299 | 65, 67, 98, 122, 124, 183, 37, 14 |
| C-LOOK | 322 | 65, 67, 98, 122, 124, 183, 14, 37 |

**Fragmentation** — blocks 100, 500, 200, 300, 600; processes 212, 417, 112, 426

| Mode / Strategy | Result |
|---|---|
| Fixed / Best Fit | all placed, internal fragmentation 433 |
| Fixed / First Fit | P4 unplaced (free memory 400 < 426, so no external fragmentation) |
| Dynamic / First Fit | P4 fails although 959 is free in total, largest hole 300: **external fragmentation** |
| Dynamic / Best Fit | all placed, no failure |

**Multiprogramming** — memory 100, quantum 4, J1(0,40,10) J2(1,30,8) J3(2,50,6) J4(3,20,5) J5(4,60,7): finish 36, CPU 100 %, memory 66.11 %, max degree 2, J3 admitted at t=16 (waited for memory).

**Segmentation** — table `0:(1400,1000) 2:(4300,400) 3:(3200,1100)`: `(2,53) → 4353`, `(3,852) → 4052`, `(0,1222) → ERROR offset ≥ limit`, `(9,0) → ERROR segment undefined`.

**Paging** — page size 1024, page 0 → frame 5: logical `100 → 5×1024+100 = 5220`; an unmapped page gives a page fault.

---

## 6. Testing and Verification Evidence

Run `./test.sh`. Results are stored in `TEST_RESULTS_core.txt` (103 checks) and `TEST_RESULTS_gui.txt` (24 scenarios).

**A. Algorithm tests (`SelfTest`)**
- **Known/manual results** from standard textbook problems: Banker's safe sequence, FIFO 15 / LRU 12 / OPT 9 faults, Belady's anomaly (FIFO 9 faults with 3 frames, 10 with 4), disk movement 640/236/331/382/299/322, segmentation and paging translations, first/best/worst/next-fit placements, CPU averages (FCFS 17, SJF 3, RR 17/3, SRTF 6.5, Priority 7.75).
- **Boundary cases:** offset = limit−1 vs limit, head on a request, one frame, more frames than pages, single process, CPU idle gap, last byte of a page.
- **Exceptional cases:** burst 0, duplicate IDs, quantum 0, Allocation > Max, frame out of range, request beyond disk, job larger than total memory, empty input.
- **Large datasets:** 5,000 processes, 300×8 Banker's system, 20,000 page references, 20,000 disk requests, 2,000 blocks × 2,000 processes, 1,000 multiprogrammed jobs, 20,000 address translations. Checks are invariants: executed time equals total burst, every request is served, OPT ≤ LRU and OPT ≤ FIFO, SSTF ≤ FCFS, utilization ≤ 100 %.

**B. GUI tests (`GuiSmokeTest`)** drive each real panel through its buttons: *Run* on sample data, *Load Large → Run*, and invalid input (cleared tables, text in numeric fields). Invalid input must reach the validation dialog, never an unexpected exception.

---

## 7. Mapping to the Rubric

| Rubric section (points) | Where it is satisfied |
|---|---|
| **A. Implementation (40)** | All 8 modules, with every algorithm listed in the rubric examples plus extras (SRTF, preemptive priority, Clock, Next/Worst Fit, LOOK/C-LOOK) |
| **B. GUI (10)** | Left-menu navigation, identical layout per module, tables/Gantt/head-path chart, validation messages, add/remove rows with no item cap |
| **C. Accuracy & testing (15)** | Section 6: normal, boundary, exceptional, small and large data; textbook-verified results; stability test |
| **D. Source & submission (10)** | Layered packages, a documentation comment on every class, `MiniOS.jar`, build scripts, this document, sample I/O |
| **E. Presentation (15)** | See section 8 |
| **F. OS concepts (10)** | See section 9 |

**Submission checklist:** source code ✔ · runnable JAR ✔ · dependencies (none) and run instructions ✔ · module/algorithm documentation ✔ · sample I/O ✔ · small + large test evidence ✔ · flexible input ✔ · validation without crashing ✔.

---

## 8. 10-Minute Demo Plan (matches the rubric schedule)

| Time | What to show |
|---|---|
| 1 min | Title, goal, Java/Swing, the eight modules, `core` vs `ui` separation |
| 2 min | Left menu, an input table (add/remove rows), result tabs, one invalid input producing a message |
| 4 min | Load Sample then Run in each module: CPU Gantt + comparison; Banker's safe sequence (then Check Request); paging translation with a fault; page replacement trace; segmentation error; fragmentation (Dynamic/First Fit shows external fragmentation); disk chart; multiprogramming utilization. Press **Load Large** once to prove unlimited input |
| 2 min | Explain one algorithm end to end (e.g., Banker's: Need → Work → sequence) and one challenge |
| 1 min | Questions and conclusion |

## 9. Likely Questions

- **Why does SRTF beat SJF on waiting time?** It is preemptive, so a newly arrived short job does not wait behind a long one; SRTF is optimal for average waiting time.
- **Safe vs unsafe state?** Safe means at least one ordering lets every process finish; unsafe does not guarantee deadlock but permits it.
- **Is the safe sequence unique?** No; ours is the first found by in-order scanning.
- **What is Belady's anomaly?** More frames can give more faults under FIFO (9 → 10 on `1 2 3 4 1 2 5 1 2 3 4 5`).
- **Why is Optimal not implementable in a real OS?** It needs future knowledge; it is the benchmark.
- **Internal vs external fragmentation?** Internal = wasted space inside an allocated block (fixed partitions, paging's last page); external = enough total free memory but no single hole large enough (variable partitions, segmentation).
- **How does segmentation differ from paging?** Segments are variable-size logical units with a limit check; pages are fixed-size, so there is no external fragmentation.
- **SCAN vs C-SCAN?** C-SCAN returns to the start without servicing, so waiting time is more uniform.

---

## 10. Design Decisions and Known Limits

- **Tie-breaking** is defined and consistent (arrival time, then input order), so results are reproducible and can be checked by hand.
- **Disk-edge and return-jump conventions** differ between textbooks; they are documented above and the jump is switchable.
- **Multiprogramming model:** CPU-bound programs only (no I/O waits); dispatch happens at quantum boundaries. It demonstrates memory sharing, admission control and utilization, but CPU utilization is naturally near 100 % when programs are always ready.
- **Value cap:** a single number must be ≤ 1,000,000 (not the item count). Very large bursts with a quantum of 1 produce very long Round Robin traces and are limited only by memory and time.
- **Algorithm complexity:** CPU scheduling is O(n²) in the number of processes (verified with 5,000); Optimal replacement is O(n·f) and the Banker safety check is O(n²·m); both are verified on the large datasets above.
- **Not included (out of the rubric's scope):** reading/writing input files and exporting results; the tables can be sorted by clicking column headers.
