package indexer;

import java.util.Set;

public class PathScorer {
    private static final Set<String> HIGH_PRIORITY_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h",
            ".md", ".txt", ".pdf", ".docx", ".xlsx", ".pptx",
            ".html", ".css", ".json", ".xml", ".yaml", ".yml"
    );

    private static final Set<String> MEDIUM_PRIORITY_EXTENSIONS = Set.of(
            ".csv", ".log", ".sh", ".bat", ".sql", ".properties",
            ".gradle", ".toml", ".ini", ".cfg"
    );

    private static final Set<String> IMPORTANT_DIRS = Set.of(
            "Documents", "Desktop", "Downloads", "Projects",
            "src", "main", "lib", "app", "core"
    );

    public double score(FileRecord record) {
        double score = 0.0;

        score += scorePathLength(record.path());
        score += scoreExtension(record.extension());
        score += scoreRecency(record.lastModified());
        score += scoreFileSize(record.size());
        score += scoreDirectoryImportance(record.path());
        score += scoreVisibility(record.isHidden());

        return score;
    }

    private double scorePathLength(String path) {
        long depth = path.chars().filter(c -> c == '/' || c == '\\').count();
        return Math.max(0, 20.0 - depth * 1.5);
    }

    private double scoreExtension(String extension) {
        if (extension == null || extension.isEmpty())
            return 0;
        String ext = extension.toLowerCase();

        if (HIGH_PRIORITY_EXTENSIONS.contains(ext))
            return 15.0;
        if (MEDIUM_PRIORITY_EXTENSIONS.contains(ext))
            return 10.0;

        return 3.0;
    }

    private double scoreRecency(long lastModifiedMillis) {
        long now = System.currentTimeMillis();
        long ageMillis = now - lastModifiedMillis;
        double ageDays = ageMillis / (1000.0 * 60 * 60 * 24);

        if (ageDays < 1)
            return 25.0;
        if (ageDays < 7)
            return 20.0;
        if (ageDays < 30)
            return 15.0;
        if (ageDays < 90)
            return 10.0;
        if (ageDays < 365)
            return 5.0;
        return 1.0;
    }

    private double scoreFileSize(long sizeBytes) {
        if (sizeBytes < 100)
            return 2.0;
        if (sizeBytes < 1024)
            return 5.0;
        if (sizeBytes < 100 * 1024)
            return 10.0;
        if (sizeBytes < 1024 * 1024)
            return 8.0;
        if (sizeBytes < 10 * 1024 * 1024)
            return 5.0;

        return 2.0;
    }

    private double scoreDirectoryImportance(String path) {
        double boost = 0.0;
        for (String dir : IMPORTANT_DIRS) {
            if (path.contains("/" + dir + "/") || path.contains("\\" + dir + "\\")) {
                boost += 5.0;
            }
        }
        return Math.min(boost, 15.0);
    }

    private double scoreVisibility(boolean isHidden) {
        return isHidden ? -5.0 : 0.0;
    }
}
