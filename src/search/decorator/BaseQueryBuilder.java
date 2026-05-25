package search.decorator;

public class BaseQueryBuilder implements QueryBuilder {

    @Override
    public String build(String query) {
        if (query == null) return "";
        return query.strip();
    }
}