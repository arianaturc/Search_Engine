package indexer;

/// metadata about an indexed file
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
        String  preview,
        double pathScore
) {
    public FileRecord( String path, String name, String extension, long size,
                       long lastModified, long createdAt, boolean isHidden,
                       boolean isReadable, String mimeType, String tags,
                       String content, String preview
    ) {

        this(path, name, extension, size, lastModified, createdAt,
                isHidden, isReadable, mimeType, tags, content, preview, 0.0);
    }

}