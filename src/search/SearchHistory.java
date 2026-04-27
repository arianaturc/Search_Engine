package search;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SearchHistory implements SearchObserver, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<HistoryEntry> history = new ArrayList<>();
    private final Map<String, Integer> fileAccessCounts = new LinkedHashMap<>();
    private static final int MAX_HISTORY = 500;

    private static final String DEFAULT_PATH = "data/search_history.txt";

    public record HistoryEntry(String query, long timestamp) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    @Override
    public void onSearch(String query, List<SearchResult> results) {
        if (query == null || query.isBlank()) return;

        history.add(new HistoryEntry(query.strip(), System.currentTimeMillis()));

        for (SearchResult result : results) {
            fileAccessCounts.merge(result.path(), 1, Integer::sum);
        }

        if (history.size() > MAX_HISTORY) {
            history.subList(0, history.size() - MAX_HISTORY).clear();
        }
    }

    public List<String> suggest(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return getRecentQueries(10);
        }

        String lowerPrefix = prefix.strip().toLowerCase();

        Map<String, Long> frequencyMap = history.stream()
                .map(e -> e.query().toLowerCase())
                .filter(q -> q.startsWith(lowerPrefix))
                .collect(Collectors.groupingBy(q -> q, Collectors.counting()));

        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<String> getRecentQueries(int limit) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (int i = history.size() - 1; i >= 0 && seen.size() < limit; i--) {
            seen.add(history.get(i).query());
        }
        return new ArrayList<>(seen);
    }

    public int getAccessCount(String filePath) {
        return fileAccessCounts.getOrDefault(filePath, 0);
    }

    public void save() {
        save(DEFAULT_PATH);
    }

    public void save(String filePath) {
        try {
            Path parent = Path.of(filePath).getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
                out.writeObject(history);
                out.writeObject(fileAccessCounts);
            }
            System.out.println("Search history saved (" + history.size() + " entries).");
        } catch (IOException e) {
            System.err.println("Could not save search history: " + e.getMessage());
        }
    }

    public static SearchHistory load() {
        return load(DEFAULT_PATH);
    }

    public static SearchHistory load(String filePath) {
        SearchHistory loaded = new SearchHistory();

        if (!Files.exists(Path.of(filePath))) {
            System.out.println("No saved search history found.");
            return loaded;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            List<HistoryEntry> savedHistory = (List<HistoryEntry>) in.readObject();
            Map<String, Integer> savedCounts = (Map<String, Integer>) in.readObject();

            loaded.history.addAll(savedHistory);
            loaded.fileAccessCounts.putAll(savedCounts);

            System.out.println("Search history loaded (" + loaded.history.size() + " entries).");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load search history: " + e.getMessage());
        }

        return loaded;
    }

}
