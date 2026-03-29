package indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Crawler {
    List<Path> crawl(String rootPath) throws IOException;
    int getSkippedCount();
}