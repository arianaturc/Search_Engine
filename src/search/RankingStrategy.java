package search;

import java.util.List;

public interface RankingStrategy {
    String getName();
    List<SearchResult> rank(List<SearchResult> results, String query);
}
