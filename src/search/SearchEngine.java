package search;

import java.util.List;

public interface SearchEngine {
    List<SearchResult> search(String rawQuery);
}