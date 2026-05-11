package com.jdisktree.scanner;

import com.jdisktree.domain.DiffNode;
import com.jdisktree.domain.DiffStatus;
import com.jdisktree.domain.FileNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class SnapshotService {
    private static final ForkJoinPool pool = ForkJoinPool.commonPool();

    public static DiffNode compare(FileNode base, FileNode current) {
        return pool.invoke(new CompareTask(base, current));
    }

    private static class CompareTask extends RecursiveTask<DiffNode> {
        private final FileNode base;
        private final FileNode current;

        CompareTask(FileNode base, FileNode current) {
            this.base = base;
            this.current = current;
        }

        @Override
        protected DiffNode compute() {
            if (base == null && current == null) return null;
            if (base == null) return DiffNode.added(current);
            if (current == null) return DiffNode.removed(base);

            DiffStatus status = DiffStatus.UNCHANGED;
            long delta = current.size() - base.size();
            if (delta != 0) {
                status = DiffStatus.MODIFIED;
            }

            List<DiffNode> children = new ArrayList<>();
            Map<String, FileNode> baseChildren = new HashMap<>();
            for (FileNode child : base.children()) {
                baseChildren.put(child.name(), child);
            }

            Map<String, FileNode> currentChildren = new HashMap<>();
            for (FileNode child : current.children()) {
                currentChildren.put(child.name(), child);
            }

            List<CompareTask> subTasks = new ArrayList<>();

            // Process current (added or modified)
            for (FileNode curChild : current.children()) {
                FileNode baseChild = baseChildren.get(curChild.name());
                subTasks.add(new CompareTask(baseChild, curChild));
            }

            // Process removed (in base but not in current)
            for (FileNode baseChild : base.children()) {
                if (!currentChildren.containsKey(baseChild.name())) {
                    subTasks.add(new CompareTask(baseChild, null));
                }
            }

            for (CompareTask task : subTasks) {
                task.fork();
            }

            for (CompareTask task : subTasks) {
                DiffNode result = task.join();
                if (result != null) {
                    children.add(result);
                }
            }

            return new DiffNode(
                current.name(),
                current.absolutePath(),
                current.size(),
                delta,
                status,
                current.isDirectory(),
                children
            );
        }
    }
}
