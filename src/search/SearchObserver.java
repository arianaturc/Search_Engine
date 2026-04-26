package search;

import java.util.List;

public interface SearchObserver {
    void onSearch(String query, List<SearchResult> results);
}
