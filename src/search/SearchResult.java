package search;

public record SearchResult(
        String path,
        String name,
        String extension,
        long   size,
        long   lastModified,
        String tags,
        String preview,
        double pathScore
) {
    public SearchResult(
            String path, String name, String extension,
            long size, long lastModified,
            String tags, String preview
    ) {
        this(path, name, extension, size, lastModified, tags, preview, 0.0);
    }
}