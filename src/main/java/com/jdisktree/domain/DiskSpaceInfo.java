package com.jdisktree.domain;

/**
 * Immutable record representing disk space information.
 * 
 * @param totalSpace  Total capacity of the disk in bytes.
 * @param usableSpace Free usable space on the disk in bytes.
 * @param driveName   Name or mount point of the drive.
 */
public record DiskSpaceInfo(long totalSpace, long usableSpace, String driveName) {
}
