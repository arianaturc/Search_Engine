package indexer;

import config.Config;
import database.FileRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IndexingService {

    private final Config config;
    private final FileRepository fileRepository;

    public IndexingService(Config config, FileRepository fileRepository) {
        this.config = config;
        this.fileRepository = fileRepository;
    }


    public String runIndex() {
        Crawler crawler = new DirectoryCrawler(config);
        FileExtractor extractor = new Extractor();
        IndexingReport report = new IndexingReport();

        List<Path> files;
        try {
            files = crawler.crawl(config.getRootDirectory());
        } catch (Exception e) {
            return "Crawl failed: " + e.getMessage();
        }

        Set<String> currentPaths = new HashSet<>();

        for (Path file : files) {
            String absolutePath = file.toAbsolutePath().toString();
            currentPaths.add(absolutePath);

            try {
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                long currentLastModified = attrs.lastModifiedTime().toMillis();
                long storedLastModified  = fileRepository.getLastModified(absolutePath);

                if (storedLastModified == currentLastModified) {
                    report.recordUnchanged();
                    continue;
                }
            } catch (Exception e) {
                System.err.println("Could not check file status: " + file + " (" + e.getMessage() + ")");
            }

            FileRecord record = extractor.extract(file);
            if (record != null) {
                try {
                    fileRepository.insertOrUpdate(record);
                    report.recordIndexed();
                } catch (Exception e) {
                    System.err.println("Failed to insert: " + file + " (" + e.getMessage() + ")");
                    report.recordFailed();
                }
            } else {
                report.recordSkipped();
            }
        }


        try {
            Set<String> indexedPaths = fileRepository.getAllIndexedPaths();
            for (String indexedPath : indexedPaths) {
                if (!currentPaths.contains(indexedPath)) {
                    fileRepository.removeByPath(indexedPath);
                    report.recordRemoved();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to clean up deleted files: " + e.getMessage());
        }

        for (int i = 0; i < crawler.getSkippedCount(); i++) {
            report.recordSkipped();
        }

        return report.generate(config.getReportFormat());
    }
}