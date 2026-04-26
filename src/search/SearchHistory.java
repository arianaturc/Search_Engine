package search;

import java.util.*;
import java.util.stream.Collectors;

public class SearchHistory implements SearchObserver{
    private final List<HistoryEntry> history = new ArrayList<>();
    private final Map<String, Integer> fileAccessCounts = new LinkedHashMap<>();
    private static final int MAX_HISTORY = 500;

    public record HistoryEntry(String query, long timestamp) {}

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

    public Map<String, Integer> getFileAccessCounts() {
        return Collections.unmodifiableMap(fileAccessCounts);
    }

    public int getTotalSearches() {
        return history.size();
    }

    public void clear() {
        history.clear();
        fileAccessCounts.clear();
    }
}
