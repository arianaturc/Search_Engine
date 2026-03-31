import config.Config;
import database.DatabaseManager;
import database.FileRepository;
import indexer.IndexingService;
import search.SearchRepository;
import search.SearchService;
import ui.CLI;
import ui.GUI;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        Config config = new Config();
        parseArguments(args, config);
        validateRootDirectory(config);

        System.out.println("Root directory: " + config.getRootDirectory());
        System.out.println("Max results:    " + config.getMaxResults());
        System.out.println("Report format:  " + config.getReportFormat());

        // Database
        DatabaseManager db = new DatabaseManager();
        FileRepository fileRepository = new FileRepository(db);
        try {
            db.connect();
            fileRepository.initializeSchema();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        // Index
        IndexingService indexingService = new IndexingService(config, fileRepository);
        System.out.println(indexingService.runIndex());

        // Search
        SearchRepository searchRepo  = new SearchRepository(db.getConnection(), config.getMaxResults());
        SearchService    searchService = new SearchService(searchRepo);

        SwingUtilities.invokeLater(() -> new GUI(searchService, config, fileRepository, indexingService).start());

        Thread cliThread = new Thread(() -> new CLI(searchService).start());
        cliThread.setDaemon(true);
        cliThread.start();
    }

    private static void parseArguments(String[] args, Config config) {
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--root"   -> config.setRootDirectory(args[i + 1]);
                case "--ignore" -> config.setIgnoredExtensionsFromString(args[i + 1]);
                case "--limit"  -> {
                    try {
                        config.setMaxResults(Integer.parseInt(args[i + 1]));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid --limit value: " + args[i + 1] + ". Using default.");
                    }
                }
                case "--report" -> config.setReportFormat(args[i + 1]);
            }
        }
    }

    private static void validateRootDirectory(Config config) {
        Path rootPath = Paths.get(config.getRootDirectory());
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            System.err.println("Error: Root directory does not exist or is not a directory: " + rootPath);
            System.exit(1);
        }
        if (!Files.isReadable(rootPath)) {
            System.err.println("Error: Root directory is not readable: " + rootPath);
            System.exit(1);
        }
    }
}