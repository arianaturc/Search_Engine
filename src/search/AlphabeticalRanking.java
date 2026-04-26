package search;

import java.util.Comparator;
import java.util.List;

public class AlphabeticalRanking implements RankingStrategy {
    @Override
    public String getName() {
        return "Alphabetical";
    }

    @Override
    public List<SearchResult> rank(List<SearchResult> results, String query) {
        results.sort(Comparator.comparing(
                (SearchResult r) -> r.name().toLowerCase()
        ));
        return results;
    }
}
