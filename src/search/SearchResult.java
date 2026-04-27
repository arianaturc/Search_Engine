package search;

public record SearchResult(
        String path,
        String name,
        String extension,
        long   size,
        long   lastModified,
        String tags,
        String preview,
        double pathScore,
        String content,
        String snippet,
        double positionScore
) {

    public SearchResult(
            String path, String name, String extension,
            long size, long lastModified,
            String tags, String preview, double pathScore
    ) {
        this(path, name, extension, size, lastModified, tags, preview, pathScore,
                "", "", 0.0);
    }

    public SearchResult(
            String path, String name, String extension,
            long size, long lastModified,
            String tags, String preview, double pathScore,
            String content
    ) {
        this(path, name, extension, size, lastModified, tags, preview, pathScore,
                content, "", 0.0);
    }

    public SearchResult withSnippet(String snippet, double positionScore) {
        return new SearchResult(
                path, name, extension, size, lastModified, tags, preview, pathScore,
                content, snippet, positionScore
        );
    }


}