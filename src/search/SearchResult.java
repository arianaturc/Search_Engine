package search;

public record SearchResult(
        String path,
        String name,
        String extension,
        long   size,
        long   lastModified,
        String tags,
        String preview
) {}