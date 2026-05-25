package widget;

import search.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeAnalyzerWidget implements Widget {

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h", ".sh", ".bat"
    );

    private static final double ACTIVATION_THRESHOLD = 0.5;
    private static final int MIN_CODE_FILES = 3;

    @Override
    public String getName() {
        return "Code Summary";
    }

    @Override
    public String getDescription() {
        return "Summarize code files by language and size";
    }

    @Override
    public boolean shouldActivate(List<SearchResult> results, String query) {
        if (results.isEmpty()) return false;

        long codeCount = countCodeFiles(results);

        if (query != null && query.toLowerCase().contains("tag:code")) {
            return codeCount >= 1;
        }

        double ratio = (double) codeCount / results.size();
        return codeCount >= MIN_CODE_FILES && ratio >= ACTIVATION_THRESHOLD;
    }

    @Override
    public String execute(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║         CODE SUMMARY                 ║\n");
        sb.append("╚══════════════════════════════════════╝\n");

        Map<String, Integer> langCount = new HashMap<>();
        Map<String, Long> langSize = new HashMap<>();
        long totalSize = 0;
        int totalFiles = 0;

        for (SearchResult r : results) {
            if (CODE_EXTENSIONS.contains(r.extension().toLowerCase())) {
                String lang = extensionToLanguage(r.extension());
                langCount.merge(lang, 1, Integer::sum);
                langSize.merge(lang, r.size(), Long::sum);
                totalSize += r.size();
                totalFiles++;
            }
        }

        sb.append("  Languages:\n");
        langCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    String lang = e.getKey();
                    int count = e.getValue();
                    long size = langSize.getOrDefault(lang, 0L);
                    sb.append(String.format("    %-12s %3d files  (%s)\n", lang, count, formatSize(size)));
                });

        sb.append("\n");
        sb.append("  Total: ").append(totalFiles).append(" code files, ").append(formatSize(totalSize)).append("\n");

        return sb.toString();
    }

    private long countCodeFiles(List<SearchResult> results) {
        return results.stream()
                .filter(r -> CODE_EXTENSIONS.contains(r.extension().toLowerCase()))
                .count();
    }

    private String extensionToLanguage(String ext) {
        return switch (ext.toLowerCase()) {
            case ".java"       -> "Java";
            case ".py"         -> "Python";
            case ".js"         -> "JavaScript";
            case ".c"          -> "C";
            case ".cpp"        -> "C++";
            case ".h"          -> "C/C++ Header";
            case ".sh", ".bat" -> "Shell";
            default            -> ext;
        };
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return bytes / 1024 + " KB";

        return bytes / (1024 * 1024) + " MB";
    }
}