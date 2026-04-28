package test;

import search.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ranking Strategy Tests")
class RankingStrategyTest {

    @Test
    @DisplayName("RelevanceRanking: higher combined score ranks first")
    void relevanceRanking() {
        RelevanceRanking ranking = new RelevanceRanking();

        SearchResult low = createResult("low.java", 10.0, 5.0);
        SearchResult high = createResult("high.java", 50.0, 20.0);

        List<SearchResult> results = new ArrayList<>(List.of(low, high));
        ranking.rank(results, "test");

        assertEquals("high.java", results.getFirst().name());
    }

    @Test
    @DisplayName("RelevanceRanking: filename containing query gets +20 boost")
    void filenameBoost() {
        RelevanceRanking ranking = new RelevanceRanking();

        SearchResult nameMatch = createResult("test_utils.java", 25.0, 10.0);
        SearchResult noMatch = createResult("other.java", 40.0, 10.0);

        List<SearchResult> results = new ArrayList<>(List.of(noMatch, nameMatch));
        ranking.rank(results, "test");

        assertEquals("test_utils.java", results.get(0).name());
    }

    @Test
    @DisplayName("AlphabeticalRanking: sorts A to Z case-insensitively")
    void alphabeticalRanking() {
        AlphabeticalRanking ranking = new AlphabeticalRanking();

        SearchResult c = createResult("AlphabeticalRanking.java", 10.0, 5.0);
        SearchResult a = createResult("BoostedRanking.java", 10.0, 5.0);
        SearchResult b = createResult("Class.java", 10.0, 5.0);

        List<SearchResult> results = new ArrayList<>(List.of(c, a, b));
        ranking.rank(results, "test");

        assertEquals("AlphabeticalRanking.java", results.get(0).name());
        assertEquals("BoostedRanking.java", results.get(1).name());
        assertEquals("Class.java", results.get(2).name());
    }

    @Test
    @DisplayName("DateRanking: most recently modified file ranks first")
    void dateRanking() {
        DateRanking ranking = new DateRanking();
        long now = System.currentTimeMillis();

        SearchResult old = createResultWithDate("old.java", now - 100_000);
        SearchResult recent = createResultWithDate("recent.java", now);

        List<SearchResult> results = new ArrayList<>(List.of(old, recent));
        ranking.rank(results, "test");

        assertEquals("recent.java", results.get(0).name());
    }

    @Test
    @DisplayName("SizeRanking: largest file ranks first")
    void sizeRanking() {
        SizeRanking ranking = new SizeRanking();

        SearchResult small = createResultWithSize("small.java", 100);
        SearchResult large = createResultWithSize("large.java", 100_000);

        List<SearchResult> results = new ArrayList<>(List.of(small, large));
        ranking.rank(results, "test");

        assertEquals("large.java", results.get(0).name());
    }

    @Test
    @DisplayName("HistoryBoostedRanking: frequently accessed files get boosted")
    void historyBoostedRanking() {
        SearchHistory history = new SearchHistory();
        SearchResult a = createResult("a.java", 50.0, 10.0);
        SearchResult b = createResult("b.java", 30.0, 10.0);

        for (int i = 0; i < 5; i++) {
            history.onSearch("test", List.of(b));
        }

        HistoryBoostedRanking ranking = new HistoryBoostedRanking(history);
        List<SearchResult> results = new ArrayList<>(List.of(a, b));
        ranking.rank(results, "test");

        assertEquals("b.java", results.get(0).name());
    }

    private SearchResult createResult(String name, double pathScore, double positionScore) {
        return new SearchResult("/home/" + name, name, ".java", 5000,
                System.currentTimeMillis(), "code", "preview", pathScore, "", "", positionScore);
    }

    private SearchResult createResultWithDate(String name, long lastModified) {
        return new SearchResult("/home/" + name, name, ".java", 5000,
                lastModified, "code", "preview", 0.0, "", "", 0.0);
    }

    private SearchResult createResultWithSize(String name, long size) {
        return new SearchResult("/home/" + name, name, ".java", size,
                System.currentTimeMillis(), "code", "preview", 0.0, "", "", 0.0);
    }
}