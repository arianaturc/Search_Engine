package indexer;

import config.Config;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryCrawler implements Crawler {

    private final List<String> ignoredExtensions;
    private final Config config;
    private int skippedCount = 0;

    public DirectoryCrawler(Config config) {
        this.config = config;
        this.ignoredExtensions = config.getIgnoredExtensions();
    }

    @Override
    public List<Path> crawl(String rootPath) throws IOException {
        List<Path> discoveredFiles = new ArrayList<>();

        Files.walkFileTree(Paths.get(rootPath), new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                if (config.getIgnoredDirs().contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                if (Files.isSymbolicLink(dir)) {
                    try {
                        Path realPath = dir.toRealPath();
                        if (realPath.startsWith(dir)) {
                            System.err.println("Symlink loop detected, skipping: " + dir);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    } catch (IOException e) {
                        System.err.println("Could not resolve symlink: " + dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                String ext = FileUtils.getExtension(file.getFileName().toString());
                if (!ignoredExtensions.contains(ext.toLowerCase())) {
                    discoveredFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Skipping (access denied or broken): " + file);
                skippedCount++;
                return FileVisitResult.CONTINUE;
            }
        });

        return discoveredFiles;
    }

    @Override
    public int getSkippedCount() {
        return skippedCount;
    }
}