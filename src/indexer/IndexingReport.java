package indexer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexingReport {

    private final Instant startTime = Instant.now();
    private final AtomicInteger indexed   = new AtomicInteger(0);
    private final AtomicInteger skipped   = new AtomicInteger(0);
    private final AtomicInteger failed    = new AtomicInteger(0);
    private final AtomicInteger unchanged = new AtomicInteger(0);
    private final AtomicInteger removed   = new AtomicInteger(0);

    public void recordIndexed() { indexed.incrementAndGet(); }
    public void recordSkipped() { skipped.incrementAndGet(); }
    public void recordFailed() { failed.incrementAndGet(); }
    public void recordUnchanged() { unchanged.incrementAndGet(); }
    public void recordRemoved() { removed.incrementAndGet(); }

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
                """, indexed.get(), unchanged.get(), skipped.get(),
                    removed.get(), failed.get(), seconds);
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
                """, indexed.get(), unchanged.get(), skipped.get(),
                removed.get(), failed.get(), seconds);
    }
}