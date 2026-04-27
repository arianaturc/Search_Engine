package search;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ResultFormatter implements Formatter{

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public String format(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "No results found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" result(s):\n");
        sb.append("─".repeat(60)).append("\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.name()).append("\n");
            sb.append("   Path:     ").append(r.path()).append("\n");
            sb.append("   Type:     ").append(r.tags())
                    .append(" (").append(r.extension()).append(")").append("\n");
            sb.append("   Size:     ").append(formatSize(r.size())).append("\n");
            sb.append("   Modified: ")
                    .append(DATE_FORMAT.format(new Date(r.lastModified()))).append("\n");
            sb.append("   Score:    ")
                    .append(String.format("Total Score: %.1f", r.pathScore() + r.positionScore()))
                    .append("\n");

            if (r.snippet() != null && !r.snippet().isBlank()) {
                sb.append("   Snippet:\n");
                sb.append(r.snippet());
            } else if (r.preview() != null && !r.preview().isBlank()) {
                sb.append("   Preview:\n");
                for (String line : r.preview().split("\n")) {
                    sb.append("      ").append(line).append("\n");
                }
            }

            sb.append("─".repeat(60)).append("\n");
        }

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return bytes / 1024 + " KB";
        }

        return bytes / (1024 * 1024) + " MB";
    }
}