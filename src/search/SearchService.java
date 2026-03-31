package search;

import java.util.List;

public class SearchService implements SearchEngine {

    private final QueryProcessor processor;
    private final SearchRepository repository;

    public SearchService(SearchRepository repository) {
        this.processor  = new QueryProcessor();
        this.repository = repository;
    }

    @Override
    public List<SearchResult> search(String rawQuery) {
        String processed = processor.process(rawQuery);
        if (processed.isBlank()) return List.of();
        return repository.search(processed);
    }
}