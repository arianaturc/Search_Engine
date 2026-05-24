package indexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

public class Extractor implements FileExtractor {

    private final List<FileProcessingStrategy> strategies;

    public Extractor() {
        this.strategies = List.of(
                new TextProcessingStrategy(),
                new ImageProcessingStrategy()
        );
    }

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

            FileProcessingStrategy.ProcessingResult processingResult = processFile(file, extension);

            return new FileRecord(
                    path, name, extension,
                    size, lastMod, createdAt,
                    isHidden, isReadable,
                    mimeType, tags,
                    processingResult.content(),
                    processingResult.preview(),
                    0.0,
                    processingResult.dominantColor()
            );

        } catch (IOException e) {
            System.err.println("Could not extract metadata: " + file);
            return null;
        }
    }

    private FileProcessingStrategy.ProcessingResult processFile(Path file, String extension) {
        for (FileProcessingStrategy strategy : strategies) {
            if (strategy.supports(extension)) {
                return strategy.process(file, extension);
            }
        }
        return new FileProcessingStrategy.ProcessingResult("", "", "");
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

    private String extractTags(String extension, String mimeType) {
        return switch (extension.toLowerCase()) {
            case ".java", ".py", ".js", ".ts", ".c", ".cpp", ".h", ".sh", ".bat" -> "code";
            case ".txt", ".md", ".log"                                           -> "text";
            case ".json", ".xml", ".yaml", ".yml"                                -> "config";
            case ".html", ".css"                                                 -> "web";
            case ".csv"                                                          -> "data";
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif" -> "image";
            default -> mimeType.contains("text") ? "text" : "binary";
        };
    }

}