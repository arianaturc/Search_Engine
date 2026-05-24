package indexer;

import java.nio.file.Path;

public interface FileProcessingStrategy {
    boolean supports(String extension);

    ProcessingResult process(Path file, String extension);

    record ProcessingResult(
            String content,
            String preview,
            String dominantColor
    ) {
        public ProcessingResult(String content, String preview) {
            this(content, preview, "");
        }
    }

}
