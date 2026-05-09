# Project State: J-DiskTree

## Phase 1: Foundation (Domain & I/O Engine) - [COMPLETED]
- [x] **Domain Models:** Implemented immutable `FileNode` record.
- [x] **Progress Tracking:** Implemented `ScanProgress` record and `ScanProgressListener` interface.
- [x] **Disk Scanner:** Implemented `DiskScannerService` using `ForkJoinPool`.
- [x] **Verification:** Successful manual test with correct size aggregation.

## Phase 2: Treemap Algorithm - [COMPLETED]
- [x] **Geometry Models:** Implemented `TreeMapRect` record.
- [x] **Treemap Service:** Implemented Squarified Treemap algorithm in `TreemapService`.
- [x] **Verification:** Successful manual test (`TreemapManualTest`) with area and coordinate validation.

## Phase 3: State Management (ViewModel) - [COMPLETED]
- [x] **UI State:** Implemented `ScanStatus` enum and `UiState` record.
- [x] **ViewModel:** Implemented `ScanViewModel` for asynchronous orchestration.
    - Asynchronous scanning and layout calculation using `CompletableFuture`.
    - Throttled progress updates (~30 FPS) for UI performance.
    - Thread-safe state updates using a reactive observer pattern.
- [x] **Verification:** Successful manual test (`ViewModelManualTest`) confirming state transitions (IDLE -> SCANNING -> CALCULATING -> COMPLETED).

## Next Steps
- **Phase 4: Compose UI Rendering:** Canvas-based rendering of the file tree.
