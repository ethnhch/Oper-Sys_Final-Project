# NEXUS // OS — 10-Minute Presentation Guide

## Demo setup (before presenting)

1. Open the project folder and run `java -jar MiniOS.jar` (JDK 17+ required).
2. Keep the Neon Matrix theme selected.
3. Prepare the sample dataset in each module with **Load Sample**. Avoid generating random large data during the live demo.
4. Keep the source code and this guide available for technical questions.

## Timed presentation

### 0:00–1:00 — Introduction

"Good day. Our project is NEXUS // OS, a Java Swing Operating System Simulator. It demonstrates eight operating-system topics through interactive inputs, real algorithm calculations, and visual or tabular outputs. The interface uses a Neon Matrix cyber theme, while the calculations are separated from the GUI so they can be tested independently."

### 1:00–3:00 — Interface and navigation

- Point out the eight modules in the sidebar.
- Explain the common workflow: enter or add data, select an algorithm where applicable, press Run, then inspect the results and summary.
- Show the cyber theme and mention that the input tables support adding/removing rows; the sample and large-data buttons are demonstrations, not fixed input limits.
- Explain that invalid values are reported to the user instead of being silently accepted.

### 3:00–7:00 — Live module tour

Keep each module demonstration brief; use the sample data and point to one meaningful result.

1. **CPU Scheduling:** run FCFS or Round Robin. Explain that waiting time is turnaround time minus burst time; use the Gantt chart to show execution order and idle periods.
2. **Banker's Algorithm:** show the Need matrix (`Max − Allocation`) and safe sequence. Explain that a process can finish when its remaining Need is no greater than the currently available Work vector.
3. **Paging:** translate one logical address. Show page number, offset, frame lookup, and physical address (`frame × page size + offset`).
4. **Page Replacement:** use FIFO and compare its page faults with LRU or Optimal. Explain that a hit means the page is already resident; a fault means it must be loaded.
5. **Segmentation:** show one valid and one invalid offset. Explain the limit check and physical address (`base + offset`).
6. **Fragmentation:** compare fixed and dynamic partition modes. Point out internal waste in fixed partitions and an external-fragmentation failure when total free space exists but no single hole is large enough.
7. **Disk Scheduling:** compare FCFS and SSTF or SCAN. Explain that total head movement is the sum of the distances between successive head positions; state whether circular return jumps are counted.
8. **Multiprogramming:** show the timeline, resident programs, waiting queue, memory use, CPU utilization, and degree of multiprogramming. Explain FCFS memory admission and Round Robin CPU sharing.

### 7:00–9:00 — Algorithm explanation and testing

- Explain that the core algorithms are in `src/ossim/core` and the Swing screens are in `src/ossim/ui`.
- Mention that the tests include textbook/known-result cases, invalid-input cases, boundary cases, and larger datasets.
- Run `./test.sh` (or `test.bat` on Windows) if time and environment permit. The test runner reports core checks and GUI smoke tests separately.
- Do not claim a result is correct solely because it appears on screen; point to the expected-value tests in `tests/ossim/SelfTest.java`.

### 9:00–10:00 — Conclusion

"NEXUS // OS combines an interactive cyber-themed interface with actual OS algorithm simulations. It accepts user-defined datasets, explains the results, validates invalid inputs, and includes source code, a runnable JAR, documentation, and tests. Thank you."

## Likely technical questions

**Q: What is the difference between SJF and SRTF?**  
A: SJF is non-preemptive: once a process starts, it keeps the CPU until it finishes. SRTF is preemptive: a newly arrived process can take the CPU if its remaining burst is shorter.

**Q: What does a safe state mean in Banker's Algorithm?**  
A: A state is safe if there is at least one order in which every process can obtain its remaining resources, finish, and release its allocation.

**Q: What is the difference between a page fault and a page hit?**  
A: A hit means the referenced page is already in a frame. A fault means it is not currently resident and must be loaded, possibly replacing another page.

**Q: Why can segmentation produce an addressing trap?**  
A: A logical offset must be smaller than the selected segment's limit. An offset at or beyond the limit is invalid.

**Q: What is internal versus external fragmentation?**  
A: Internal fragmentation is unused space inside an allocated fixed-size block. External fragmentation is free memory split into holes, where total free space may be sufficient but no individual hole is large enough.

**Q: Why can SSTF starve a request?**  
A: SSTF always services the request closest to the current head. A far-away request can be postponed repeatedly if nearer requests keep arriving. This simulator uses the entered request batch, so the demonstration is of the selection rule rather than continuous arrivals.

**Q: What do CPU and memory utilization mean in the multiprogramming module?**  
A: CPU utilization is the percentage of simulated elapsed time during which the CPU is busy. Memory utilization is the time-weighted average fraction of total memory occupied.

## Important scope note

The instructor's rubric says to implement all algorithms covered in class. The provided rubric names FCFS, SJF, Priority, and Round Robin as examples for CPU scheduling and does not enumerate the exact classroom list. NEXUS currently includes FCFS, SJF (non-preemptive), SRTF, non-preemptive and preemptive Priority, and Round Robin for CPU; and FCFS, SSTF, SCAN, C-SCAN, LOOK, and C-LOOK for disk scheduling. Compare these lists with the actual lecture coverage before the final submission.
