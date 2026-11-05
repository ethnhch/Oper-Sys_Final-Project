# Changelog

## NEXUS // OS — verification and presentation pass

- Added strict core validation for multiprogramming program IDs, arrival times, memory requirements, CPU bursts, and total simulated-time overflow; corrected admission timestamps to reflect the actual scheduling decision boundary.
- Added CPU scheduling time-range validation to prevent integer overflow in completion/Gantt timestamps.
- Hardened Banker's Algorithm against mismatched/null matrix dimensions and used a `long` Work accumulator so released allocations cannot overflow the safety calculation.
- Changed disk total head movement to `long` and updated the comparison display, preventing overflow when many long seeks are accumulated.
- Changed fragmentation totals to `long`, validated block/process definitions, and prevented fixed-partition allocation failures from being mislabeled as external fragmentation.
- Added validation for negative paging addresses/frame numbers, segmentation bases/offsets, page references, and missing disk-scan directions.
- Expanded automated tests to 124 core checks, including malformed Banker matrices, multiprogramming invalid inputs and admission timing, CPU/multiprogramming time overflow, Banker resource accumulation, fragmentation classification/totals, paging/segmentation boundaries, and large disk movement totals.
- Added `PRESENTATION_GUIDE.md` with a timed 10-minute demo, module talking points, formulas, and likely instructor questions.
- Updated README links and notes to describe the additional validation accurately.

## Initial NEXUS cyber refresh

- Applied the Neon Matrix cyber theme and NEXUS // OS branding across the Swing interface.
- Updated shared panels, tables, fields, navigation, charts, and theme components.
- Added a NEXUS CPU screenshot and refreshed README/build/test guidance.
