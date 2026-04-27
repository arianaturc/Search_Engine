package search;

import java.util.Comparator;
import java.util.List;

public class HistoryBoostedRanking implements RankingStrategy {
    private final SearchHistory searchHistory;

    public HistoryBoostedRanking(SearchHistory searchHistory) {
        this.searchHistory = searchHistory;
    }

    @Override
    public String getName() {
        return "History Boosted";
    }

    @Override
    public List<SearchResult> rank(List<SearchResult> results, String query) {
        results.sort(Comparator.comparingDouble((SearchResult r) -> {
            double score = r.pathScore();
            score += r.positionScore();

            int accessCount = searchHistory.getAccessCount(r.path());
            score += accessCount * 5.0;

            if (query != null && !query.isBlank()) {
                String nameLower = r.name().toLowerCase();
                String[] terms = query.toLowerCase().split("\\s+");
                for (String term : terms) {
                    if (term.contains(":")) {
                        term = term.substring(term.indexOf(':') + 1);
                    }
                    if (nameLower.contains(term)) {
                        score += 20.0;
                    }
                }
            }

            return score;
        }).reversed());

        return results;
    }
}
