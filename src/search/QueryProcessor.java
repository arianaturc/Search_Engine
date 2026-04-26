package search;

import java.util.ArrayList;
import java.util.List;

public class QueryProcessor {

    public static class ParsedQuery {
        private final List<String> generalTerms = new ArrayList<>();
        private final List<String> pathTerms = new ArrayList<>();
        private final List<String> contentTerms = new ArrayList<>();
        private final List<String> extTerms = new ArrayList<>();
        private final List<String> tagTerms = new ArrayList<>();

        public List<String> getGeneralTerms()  { return generalTerms; }
        public List<String> getPathTerms()     { return pathTerms; }
        public List<String> getContentTerms()  { return contentTerms; }
        public List<String> getExtTerms()      { return extTerms; }
        public List<String> getTagTerms()      { return tagTerms; }

        public boolean isEmpty() {
            return generalTerms.isEmpty() && pathTerms.isEmpty()
                    && contentTerms.isEmpty() && extTerms.isEmpty()
                    && tagTerms.isEmpty();
        }
    }

    public ParsedQuery parse(String rawQuery) {
        ParsedQuery parsed = new ParsedQuery();

        if (rawQuery == null || rawQuery.isBlank()) {
            return parsed;
        }

        String cleaned = rawQuery.strip().replaceAll("\\s+", " ");
        String[] tokens = cleaned.split(" ");

        for (String token : tokens) {
            if (token.isBlank())
                continue;

            int colonIndex = token.indexOf(':');
            if (colonIndex > 0 && colonIndex < token.length() - 1) {
                String qualifier = token.substring(0, colonIndex).toLowerCase();
                String value = token.substring(colonIndex + 1);

                switch (qualifier) {
                    case "path"    -> parsed.pathTerms.add(value);
                    case "content" -> parsed.contentTerms.add(value);
                    case "ext"     -> parsed.extTerms.add(value);
                    case "tag"     -> parsed.tagTerms.add(value);
                    default        -> parsed.generalTerms.add(token);
                }
            } else {
                parsed.generalTerms.add(token);
            }
        }

        return parsed;
    }

    public String buildFtsQuery(List<String> terms) {
        if (terms.isEmpty())
            return "";

        if (terms.size() == 1) {
            return terms.get(0) + "*";
        }

        StringBuilder ftsQuery = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            ftsQuery.append(terms.get(i));
            if (i < terms.size() - 1) {
                ftsQuery.append(" AND ");
            }
        }
        return ftsQuery.toString();
    }


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
