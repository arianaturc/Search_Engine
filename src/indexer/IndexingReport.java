package indexer;

import java.time.Duration;
import java.time.Instant;

public class IndexingReport {

    private final Instant startTime = Instant.now();
    private int indexed = 0;
    private int skipped = 0;
    private int failed = 0;
    private int unchanged = 0;
    private int removed = 0;

    public void recordIndexed() {
        indexed++;
    }
    public void recordSkipped() {
        skipped++;
    }
    public void recordFailed() {
        failed++;
    }
    public void recordUnchanged() {
        unchanged++;
    }
    public void recordRemoved() {
        removed++;
    }

    public String generate(String format) {
        Duration duration = Duration.between(startTime, Instant.now());
        long seconds = duration.getSeconds();

        if (format.equals("json")) {
            return String.format("""
                {
                  "indexed":   %d,
                  "unchanged": %d,
                  "skipped":   %d,
                  "removed":   %d,
                  "failed":    %d,
                  "duration":  "%ds"
                }
                """, indexed, unchanged, skipped, removed, failed, seconds);
        }

        return String.format("""
                ════════════════════════════════════════
                           INDEXING REPORT
                ════════════════════════════════════════
                  Indexed:    %d files (new/modified)
                  Unchanged:  %d files (skipped)
                  Skipped:    %d files (unreadable)
                  Removed:    %d files (deleted from disk)
                  Failed:     %d files
                  Duration:   %d seconds
                ════════════════════════════════════════
                """, indexed, unchanged, skipped, removed, failed, seconds);
    }
}