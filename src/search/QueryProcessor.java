package search;

public class QueryProcessor {
    public String process(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }

        String cleaned = rawQuery.strip().replaceAll("\\s+", " ");


        String[] terms = cleaned.split(" ");
        if (terms.length == 1) {
            return terms[0] + "*";
        }

        StringBuilder ftsQuery = new StringBuilder();
        for (int i = 0; i < terms.length; i++) {
            ftsQuery.append(terms[i]);
            if (i < terms.length - 1) {
                ftsQuery.append(" AND ");
            }
        }
        return ftsQuery.toString();
    }
}
