package search.decorator;

public class SanitizationDecorator extends QueryDecorator {

    public SanitizationDecorator(QueryBuilder wrapped) {
        super(wrapped);
    }

    @Override
    public String build(String query) {
        String result = wrapped.build(query);
        if (result.isBlank()) return result;

        String[] tokens = result.split("\\s+");
        StringBuilder sanitized = new StringBuilder();

        for (String token : tokens) {
            if (sanitized.length() > 0) sanitized.append(" ");

            int colonIndex = token.indexOf(':');
            if (colonIndex > 0 && colonIndex < token.length() - 1) {
                String qualifier = token.substring(0, colonIndex).toLowerCase();
                if (isKnownQualifier(qualifier)) {
                    sanitized.append(token);
                    continue;
                }
            }

            String cleaned = token.replaceAll("[.:\\-*^()\"{}\\[\\]~@#$%&+=|<>]", " ").strip();
            cleaned = cleaned.replaceAll("\\s+", " ");
            if (!cleaned.isBlank()) {
                sanitized.append(cleaned);
            }
        }

        return sanitized.toString().strip();
    }

    private boolean isKnownQualifier(String qualifier) {
        return switch (qualifier) {
            case "path", "content", "ext", "tag", "color" -> true;
            default -> false;
        };
    }
}