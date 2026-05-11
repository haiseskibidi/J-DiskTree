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

## Phase 12: Release & Distribution - [COMPLETED]
- [x] **Distribution Config:** Updated `build.gradle.kts` to enable automatic Desktop and Start Menu shortcuts during installation.
- [x] **Stability:** Fixed invalid `upgradeUuid` which caused MSI bundler failures. 
- [x] **Documentation:** Created professional `README.md` and tagged version `v1.0.0`.
- [x] **Final Build:** Confirmed successful generation of `J-DiskTree-1.0.0.msi`.

## Phase 13: UI Performance & Resizing Optimization - [COMPLETED]
- [x] **State Isolation:** Implemented `@Stable LayoutWeights` class to isolate panel resizing state from the main `App` component, preventing full-screen recompositions on every mouse move.
- [x] **Stability Wrappers:** Introduced `@Immutable StableFileTree` and `StableTreemapData` to wrap Java records. This fixed the "unstable argument" issue in Compose, allowing heavy components like `FileTreeView` to properly skip recomposition.
- [x] **Lag Elimination:** Verified that the File Tree no longer lags during dynamic scaling, as the expensive tree-flattening logic is now only triggered by actual data changes, not UI layout updates.

## Phase 14: Settings & Configuration - [COMPLETED]
- [x] **Advanced Compact UI:** Designed a professional `SettingsDialog.kt` using a compact `600x450 dp` fixed size. Switched from long vertical lists to an adaptive **Grid/Chip layout** (`LazyVerticalGrid`), increasing data density by 3x.
- [x] **Dynamic Color System:** Implemented real-time color updates. Treemap now invalidates its internal `ImageBitmap` cache immediately when colors are saved, allowing users to see changes without re-scanning.
- [x] **Wildcard Scan Exclusions:** Updated `DiskScannerService` to support smart patterns like `*.mp4`, `.idea`, or `node_modules`. Added a circular toggle switch (`CustomCheckbox`) and deletion for every exclusion entry.
- [x] **Dark-Themed Color Picker:** Integrated **FlatLaf** library to provide a beautiful, theme-aware native `JColorChooser` with HSV as the default tab.
- [x] **Single Source of Truth:** Migrated all preferences (Exclusions & Colors) to structured JSON storage (`settings.json`, `colors.json`) via **Gson**, ensuring a robust configuration layer.

## Phase 15: Interactive Selection & UX Mastery (Release v1.3.0) - [COMPLETED]
- [x] **Intelligent Search Engine:** Implemented real-time "X-ray" search. Matches are highlighted on the Treemap while non-matches are dimmed. Added background rendering with a 60ms debounce to ensure perfectly fluid typing.
- [x] **Multi-Selection Logic:** Implemented a robust `Set<String>` based selection system in the core state with Shift/Ctrl support.
- [x] **Smart Synchronization:** Tree view automatically expands and centers any item selected on the Treemap. Clicking empty space in either view now clears the selection.
- [x] **Liquid Smooth Resize:** Eliminated all resize lag by switching to a **GPU-accelerated fixed-size bitmap buffer (1000x1000)**. 
- [x] **UX Polishing:** Implemented "Select on Right-Click", `Ctrl+F` shortcut support, and professional white border highlights.
- [x] **v1.3.0 Milestone:** Finalized localization and distribution packaging for the major 1.3.0 release.
- [x] **Export Engine:** Implemented background-safe CSV (flat list) and JSON (hierarchical) report generation. CSV includes UTF-8 BOM for perfect Windows/Excel compatibility.

## Summary
J-DiskTree is now a production-ready, high-performance disk space analyzer. It features a world-class parallel scanning engine, an intelligent real-time search system with background rendering, and a robust reporting system. The application supports advanced multi-selection, bi-directional synchronization, and liquid-smooth GPU-accelerated graphics. Fully packaged for Windows with persistent user settings, CSV/JSON export capabilities, and comprehensive localization support.

