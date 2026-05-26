package indexer;

import config.Config;
import database.FileRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/// coordinated the entire indexing pipeline, checks if the discovered files have been changed,
/// extracts changed or new files, and updates/insert them into the database while unchanged files are skipped
public class IndexingService {

    private final Config config;
    private final FileRepository fileRepository;

    private static final int NUM_READERS = Runtime.getRuntime().availableProcessors();
    private static final FileRecord STOP_SIGNAL = new FileRecord(
            "", "", "", 0, 0, 0, false, false, "", "", "", ""
    );

    public IndexingService(Config config, FileRepository fileRepository) {
        this.config = config;
        this.fileRepository = fileRepository;
    }


    public String runIndex() {
        Crawler crawler = new DirectoryCrawler(config);
        FileExtractor extractor = new Extractor();
        IndexingReport report = new IndexingReport();
        PathScorer scorer = new PathScorer();

        List<Path> files;
        try {
            files = crawler.crawl(config.getRootDirectory());
        } catch (Exception e) {
            return "Crawl failed: " + e.getMessage();
        }

        Set<String> currentPaths = new HashSet<>();

        BlockingQueue<FileRecord> queue = new LinkedBlockingQueue<>(100);

        Thread writerThread = new Thread(() -> writerLoop(queue, report), "IndexWriter");
        writerThread.start();

        ExecutorService readerPool = Executors.newFixedThreadPool(NUM_READERS);

        for (Path file : files) {
            readerPool.submit(() -> readerTask(file, queue, report, currentPaths));
        }

        readerPool.shutdown();
        try {
            readerPool.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Reader pool interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        try {
            queue.put(STOP_SIGNAL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        cleanUpDeletedFiles(currentPaths, report);

        for (int i = 0; i < crawler.getSkippedCount(); i++) {
            report.recordSkipped();
        }

        return report.generate(config.getReportFormat());
    }

    private void readerTask(Path file, BlockingQueue<FileRecord> queue,
                            IndexingReport report, Set<String> currentPaths) {
        String absolutePath = file.toAbsolutePath().toString();
        currentPaths.add(absolutePath);

        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            long currentLastModified = attrs.lastModifiedTime().toMillis();
            long storedLastModified = fileRepository.getLastModified(absolutePath);

            if (storedLastModified == currentLastModified) {
                report.recordUnchanged();
                return;
            }
        } catch (Exception e) {
            System.err.println("Could not check file status: " + file + " (" + e.getMessage() + ")");
        }

        FileExtractor extractor = new Extractor();
        PathScorer scorer = new PathScorer();

        FileRecord record = extractor.extract(file);
        if (record != null) {
            double pathScore = scorer.score(record);
            FileRecord scoredRecord = new FileRecord(
                    record.path(), record.name(), record.extension(),
                    record.size(), record.lastModified(), record.createdAt(),
                    record.isHidden(), record.isReadable(),
                    record.mimeType(), record.tags(),
                    record.content(), record.preview(),
                    pathScore,
                    record.dominantColor()
            );

            try {
                queue.put(scoredRecord);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            report.recordSkipped();
        }
    }

    private void writerLoop(BlockingQueue<FileRecord> queue, IndexingReport report) {
        while (true) {
            try {
                FileRecord record = queue.take();

                if (record == STOP_SIGNAL) {
                    break;
                }

                try {
                    fileRepository.insertOrUpdate(record);
                    report.recordIndexed();
                } catch (Exception e) {
                    System.err.println("Failed to insert: " + record.path() + " (" + e.getMessage() + ")");
                    report.recordFailed();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void cleanUpDeletedFiles(Set<String> currentPaths, IndexingReport report) {
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
    }
}