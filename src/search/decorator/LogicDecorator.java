package search.decorator;

public class LogicDecorator extends QueryDecorator {

    public LogicDecorator(QueryBuilder wrapped) {
        super(wrapped);
    }

    @Override
    public String build(String query) {
        String result = wrapped.build(query);
        if (result.isBlank()) return result;

        String[] tokens = result.split("\\s+");
        StringBuilder wildcarded = new StringBuilder();
        boolean insideParens = false;

        for (String token : tokens) {
            if (wildcarded.length() > 0) wildcarded.append(" ");

            if (token.startsWith("(")) insideParens = true;
            if (token.endsWith(")")) {
                wildcarded.append(token);
                insideParens = false;
                continue;
            }

            if (insideParens) {
                wildcarded.append(token);
                continue;
            }

            if (token.contains(":")) {
                wildcarded.append(token);
                continue;
            }

            if (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR")) {
                wildcarded.append(token);
                continue;
            }

            if (token.endsWith("*")) {
                wildcarded.append(token);
                continue;
            }

            wildcarded.append(token).append("*");
        }

        return wildcarded.toString();
    }
}