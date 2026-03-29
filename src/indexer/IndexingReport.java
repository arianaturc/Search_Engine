package indexer;

import java.time.Duration;
import java.time.Instant;

public class IndexingReport {

    private final Instant startTime = Instant.now();
    private int indexed  = 0;
    private int skipped  = 0;
    private int failed   = 0;

    public void recordIndexed()  {
        indexed++;
    }
    public void recordSkipped()  {
        skipped++;
    }
    public void recordFailed()   {
        failed++;
    }

    public String generate(String format) {
        Duration duration = Duration.between(startTime, Instant.now());
        long seconds = duration.getSeconds();

        if (format.equals("json")) {
            return String.format("""
                {
                  "indexed": %d,
                  "skipped": %d,
                  "failed":  %d,
                  "duration": "%ds"
                }
                """, indexed, skipped, failed, seconds);
        }

        return String.format("""
                ════════════════════════════════════════
                           INDEXING REPORT
                ════════════════════════════════════════
                  Indexed:  %d files
                  Skipped:  %d files
                  Failed:   %d files
                  Duration: %d seconds
                ════════════════════════════════════════
                """, indexed, skipped, failed, seconds);
    }
}