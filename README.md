# J-DiskTree

High-performance disk space analyzer and visualizer built with Java 21 and Compose Multiplatform. A modern, cross-platform alternative to WinDirStat/QDirStat.

## Key Features

- **Extreme Parallel Scanning**: Utilizes `ForkJoinPool` and `java.nio.file` to saturate NVMe queues for maximum scanning speed.
- **Interactive Treemap**: High-performance Canvas-based visualization of disk usage with real-time hover and selection.
- **Cycle Detection**: Industrial-grade recursive loop protection using Canonical Path resolution (handles Windows Junctions and Node.js deep links).
- **Extension Statistics**: Automatic grouping by file type with interactive highlighting on the Treemap.
- **File Management**: Integrated file operations (Move to Trash, Permanent Delete, Open in Explorer, Properties).
- **Multi-language Support**: Full support for English and Russian.
- **Persistent Preferences**: Remembers your theme, language, and layout settings.

## Getting Started

### Prerequisites
- JDK 21 or higher

### Running from source
```powershell
./gradlew run
```

### Building a distribution
```powershell
./gradlew packageDistributionForCurrentOS
```
*Packages will be available in `build/compose/binaries`.*

## Tech Stack
- **Language**: Kotlin & Java 21
- **UI Framework**: Compose Multiplatform (Desktop)
- **Engine**: Java NIO (File Systems), ForkJoinPool (Parallelism)
- **Persistence**: Standard Java Properties

## License
MIT
