package widget;

import search.SearchResult;

import java.util.List;

public class LogAnalyzerWidget implements Widget {

    private static final double ACTIVATION_THRESHOLD = 0.4;
    private static final int MIN_LOGS = 2;

    @Override
    public String getName() {
        return "Analyze Logs";
    }

    @Override
    public String getDescription() {
        return "Analyze log files for patterns and recent activity";
    }

    @Override
    public boolean shouldActivate(List<SearchResult> results, String query) {
        if (results.isEmpty()) return false;

        long logCount = countLogs(results);

        if (query != null && query.toLowerCase().contains("log")) {
            return logCount >= 1;
        }

        double ratio = (double) logCount / results.size();
        return logCount >= MIN_LOGS && ratio >= ACTIVATION_THRESHOLD;
    }

    @Override
    public String execute(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║         LOG ANALYZER                 ║\n");
        sb.append("╚══════════════════════════════════════╝\n");

        long totalSize = 0;
        long newest = Long.MIN_VALUE;
        long oldest = Long.MAX_VALUE;
        int count = 0;

        for (SearchResult r : results) {
            if (r.extension().equalsIgnoreCase(".log")) {
                count++;
                totalSize += r.size();
                newest = Math.max(newest, r.lastModified());
                oldest = Math.min(oldest, r.lastModified());

                sb.append(String.format("  [%d] %s (%s)\n", count, r.name(), formatSize(r.size())));

                if (r.preview() != null && !r.preview().isBlank()) {
                    sb.append("       Latest entries:\n");
                    for (String line : r.preview().split("\n")) {
                        sb.append("         ").append(line).append("\n");
                    }
                }
            }
        }

        sb.append("\n");
        sb.append("  Summary:\n");
        sb.append("    Log files found: ").append(count).append("\n");
        sb.append("    Total size:      ").append(formatSize(totalSize)).append("\n");

        if (count > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            sb.append("    Oldest:          ").append(sdf.format(new java.util.Date(oldest))).append("\n");
            sb.append("    Newest:          ").append(sdf.format(new java.util.Date(newest))).append("\n");
        }

        return sb.toString();
    }

    private long countLogs(List<SearchResult> results) {
        return results.stream()
                .filter(r -> r.extension().equalsIgnoreCase(".log"))
                .count();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return bytes / 1024 + " KB";

        return bytes / (1024 * 1024) + " MB";
    }
}