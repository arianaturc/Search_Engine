package indexer;

public record FileRecord(
        String  path,
        String  name,
        String  extension,
        long    size,
        long    lastModified,
        long    createdAt,
        boolean isHidden,
        boolean isReadable,
        String  mimeType,
        String  tags,
        String  content,
        String  preview
) {}