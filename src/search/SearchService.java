package search;

import java.util.ArrayList;
import java.util.List;

public class SearchService implements SearchEngine {

    private final QueryProcessor processor;
    private final SearchRepository repository;
    private final List<SearchObserver> observers = new ArrayList<>();
    private RankingStrategy rankingStrategy;

    public SearchService(SearchRepository repository) {
        this.processor  = new QueryProcessor();
        this.repository = repository;
        this.rankingStrategy = new RelevanceRanking();
    }

    @Override
    public List<SearchResult> search(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return List.of();

        QueryProcessor.ParsedQuery parsed = processor.parse(rawQuery);
        if (parsed.isEmpty()) return List.of();

        List<SearchResult> results;

        boolean hasQualifiers = !parsed.getPathTerms().isEmpty()
                || !parsed.getContentTerms().isEmpty()
                || !parsed.getExtTerms().isEmpty()
                || !parsed.getTagTerms().isEmpty();

        if (hasQualifiers) {
            results = repository.search(parsed, processor);
        } else {
            String ftsQuery = processor.buildFtsQuery(parsed.getGeneralTerms());
            if (ftsQuery.isBlank()) return List.of();
            results = repository.search(ftsQuery);
        }

        results = rankingStrategy.rank(results, rawQuery);

        notifyObservers(rawQuery, results);

        return results;
    }

    public void setRankingStrategy(RankingStrategy strategy) {
        this.rankingStrategy = strategy;
    }

    public RankingStrategy getRankingStrategy() {
        return rankingStrategy;
    }

    public void addObserver(SearchObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(SearchObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String query, List<SearchResult> results) {
        for (SearchObserver observer : observers) {
            observer.onSearch(query, results);
        }
    }
}