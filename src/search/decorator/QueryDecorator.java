package search.decorator;

public abstract class QueryDecorator implements QueryBuilder {

    protected final QueryBuilder wrapped;

    public QueryDecorator(QueryBuilder wrapped) {
        this.wrapped = wrapped;
    }
}