package indexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;


public class TextProcessingStrategy implements FileProcessingStrategy {

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
    public boolean supports(String extension) {
        return TEXT_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public ProcessingResult process(Path file, String extension) {
        String content = tryReadWithFallback(file);

        if (content == null) {
            System.err.println("Skipping unreadable file: " + file);
            return new ProcessingResult("", "");
        }

        long nonPrintable = content.chars()
                .filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')
                .count();
        if (nonPrintable > 100) {
            System.err.println("Skipping binary content (non-printable chars): " + file);
            return new ProcessingResult("", "");
        }

        String preview = extractPreview(content);
        return new ProcessingResult(content, preview);
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