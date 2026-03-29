package indexer;

import java.nio.file.Path;

public interface FileExtractor {
    FileRecord extract(Path file);
}