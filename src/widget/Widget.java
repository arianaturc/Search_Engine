package widget;

import search.SearchResult;
import java.util.List;

public interface Widget {

    String getName();

    String getDescription();

    boolean shouldActivate(List<SearchResult> results, String query);

    String execute(List<SearchResult> results);
}