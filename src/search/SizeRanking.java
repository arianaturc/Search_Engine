package search;

import java.util.Comparator;
import java.util.List;

public class SizeRanking implements RankingStrategy {
    @Override
    public String getName() {
        return "File Size";
    }

    @Override
    public List<SearchResult> rank(List<SearchResult> results, String query) {
        results.sort(Comparator.comparingLong(
                SearchResult::size
        ).reversed());
        return results;
    }
}
