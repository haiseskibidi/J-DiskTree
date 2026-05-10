# Project State: J-DiskTree

## Phase 9: Extreme High-Performance Engine Redesign - [COMPLETED]
- [x] **Parallel I/O Engine:** Replaced single-threaded `Files.walkFileTree` with a custom `ForkJoinPool` architecture implementing "Directory-Granular Parallelism". 
- [x] **Memory & CPU Balance:** Tasks are spawned per-directory. Files are parsed using `Files.walkFileTree` with `maxDepth=1` to get file attributes (sizes) for free from the OS (especially on Windows), avoiding a massive number of slow, independent `stat` syscalls.
- [x] **Thread-Safe Reporting:** Implemented `LongAdder` instead of `AtomicLong` to completely eliminate cache-line contention when thousands of threads process millions of files. UI updates are locally batched (modulo 1000 files) per thread to avoid locking the CPU.

## Phase 10: Treemap Layout Refinement & Architecture Overhaul - [COMPLETED]
- [x] **God File Elimination:** Completely refactored `Main.kt` (~300 lines) into modular Compose files (`App.kt`, `TreemapCanvas.kt`, `Toolbar.kt`, etc.).
- [x] **Algorithm Extraction:** Split `TreemapService.java` by moving the pure math into `SquarifiedAlgorithm.java`.
- [x] **Physical Recursion Clipping:** The root cause of all slivers was that the squarified algorithm sometimes assigns extremely thin bounds (e.g., 1000x2 pixels) to directories. Now, if `w < 3.0` or `h < 3.0`, the engine stops recursing and instead renders the entire directory as a single solid "compressed folder" block, permanently eliminating overlapping micro-lines.

## Phase 11: Synchronized File Tree View & UI Polish - [COMPLETED]
- **File Tree Component**: Implemented a highly optimized `FileTreeView` using `LazyColumn` and dynamic tree flattening to handle millions of files without memory issues.
- **Synchronization**: Added bi-directional sync. Clicking a rectangle in the Treemap automatically highlights the node in the tree and auto-scrolls to the top-level parent folder ("Root + 1" level) to provide macro-context.
- **UI Polish**: Enforced Material Dark Theme globally across the application, resolving white-on-white text issues. Added a vertical scrollbar to the tree view for easy navigation.

## Summary
J-DiskTree has undergone a core architectural refactor to support massive file systems. By combining native NIO scanning with aggressive visual pruning and strict thread-safe state management, the application now delivers professional-grade performance. The UI and layout layers have been fully modularized, adhering to SOLID principles. The rendering engine now uses physical canvas constraints to intelligently group files, providing a pristine, glitch-free visualization alongside a synchronized, dark-themed hierarchical file tree.
