package indexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public class Extractor implements FileExtractor {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".txt", ".java", ".py", ".js", ".ts", ".html", ".css",
            ".xml", ".json", ".md", ".csv", ".yaml", ".yml", ".c",
            ".cpp", ".h", ".sh", ".bat", ".log"
    );

    private static final Charset[] FALLBACK_CHARSETS = {
            StandardCharsets.UTF_8,
            Charset.forName("Windows-1250"),
            Charset.forName("Windows-1252"),
            Charset.forName("ISO-8859-2")
    };

    @Override
    public FileRecord extract(Path file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

            String name = file.getFileName().toString();
            String path = file.toAbsolutePath().toString();
            String extension = FileUtils.getExtension(name);
            long size = attrs.size();
            long lastMod = attrs.lastModifiedTime().toMillis();
            long createdAt = attrs.creationTime().toMillis();
            boolean isHidden = Files.isHidden(file);
            boolean isReadable = Files.isReadable(file);

            String mimeType = probeMimeType(file);
            String tags = extractTags(extension, mimeType);

            String[] contentAndPreview = extractContentAndPreview(file, extension, path);

            return new FileRecord(
                    path, name, extension,
                    size, lastMod, createdAt,
                    isHidden, isReadable,
                    mimeType, tags,
                    contentAndPreview[0], contentAndPreview[1]
            );

        } catch (IOException e) {
            System.err.println("Could not extract metadata: " + file);
            return null;
        }
    }

    private String probeMimeType(Path file) {
        try {
            String probed = Files.probeContentType(file);
            return (probed != null) ? probed : "";
        } catch (IOException e) {
            System.err.println("Could not probe mime type: " + file);
            return "";
        }
    }

    private String extractPreview(String content) {
        String[] lines = content.split("\n");
        StringBuilder preview = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (!line.isBlank() && count < 3) {
                preview.append(line.strip()).append("\n");
                count++;
            }
        }
        return preview.toString().strip();
    }

    private String extractTags(String extension, String mimeType) {
        return switch (extension.toLowerCase()) {
            case ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h", ".sh", ".bat" -> "code";
            case ".txt", ".md", ".log"                                           -> "text";
            case ".json", ".xml", ".yaml", ".yml"                                -> "config";
            case ".html", ".css"                                                 -> "web";
            case ".csv"                                                          -> "data";
            default -> mimeType.contains("text") ? "text" : "binary";
        };
    }

    private String[] extractContentAndPreview(Path file, String extension, String path) {
        if (!TEXT_EXTENSIONS.contains(extension.toLowerCase())) {
            return new String[]{"", ""};
        }

        String content = tryReadWithFallback(file);

        if (content == null) {
            System.err.println("Skipping unreadable file: " + path);
            return new String[]{"", ""};
        }

        long nonPrintable = content.chars()
                .filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')
                .count();
        if (nonPrintable > 100) {
            System.err.println("Skipping binary content (non-printable chars): " + path);
            return new String[]{"", ""};
        }

        return new String[]{content, extractPreview(content)};
    }

    private String tryReadWithFallback(Path file) {
        for (Charset charset : FALLBACK_CHARSETS) {
            try {
                return Files.readString(file, charset);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}