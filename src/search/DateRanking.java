package search;

import java.util.Comparator;
import java.util.List;

public class DateRanking implements RankingStrategy {
    @Override
    public String getName() {
        return "Date Modified";
    }

    @Override
    public List<SearchResult> rank(List<SearchResult> results, String query) {
        results.sort(Comparator.comparingLong(
                SearchResult::lastModified
        ).reversed());
        return results;
    }
}
