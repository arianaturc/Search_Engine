package search.decorator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SynonymDecorator extends QueryDecorator {

    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();

    static {
        SYNONYM_MAP.put("img",    List.of("img", "image", "photo", "picture"));
        SYNONYM_MAP.put("image",  List.of("image", "img", "photo", "picture"));
        SYNONYM_MAP.put("photo",  List.of("photo", "image", "img", "picture"));
        SYNONYM_MAP.put("pic",    List.of("pic", "picture", "image", "photo"));
        SYNONYM_MAP.put("doc",    List.of("doc", "document", "file"));
        SYNONYM_MAP.put("docs",   List.of("docs", "documents", "files"));
        SYNONYM_MAP.put("config", List.of("config", "configuration", "settings"));
        SYNONYM_MAP.put("err",    List.of("err", "error", "exception"));
        SYNONYM_MAP.put("error",  List.of("error", "err", "exception", "failure"));
        SYNONYM_MAP.put("warn",   List.of("warn", "warning"));
        SYNONYM_MAP.put("info",   List.of("info", "information"));
        SYNONYM_MAP.put("dir",    List.of("dir", "directory", "folder"));
        SYNONYM_MAP.put("folder", List.of("folder", "directory", "dir"));
        SYNONYM_MAP.put("rm",     List.of("rm", "remove", "delete"));
        SYNONYM_MAP.put("del",    List.of("del", "delete", "remove"));
    }

    public SynonymDecorator(QueryBuilder wrapped) {
        super(wrapped);
    }

    @Override
    public String build(String query) {
        String result = wrapped.build(query);
        if (result.isBlank()) return result;

        String[] tokens = result.split("\\s+");
        StringBuilder expanded = new StringBuilder();

        for (String token : tokens) {
            if (expanded.length() > 0) expanded.append(" ");

            if (token.contains(":")) {
                expanded.append(token);
                continue;
            }

            if (token.equalsIgnoreCase("AND") || token.equalsIgnoreCase("OR")) {
                expanded.append(token);
                continue;
            }

            String lowerToken = token.toLowerCase();
            if (SYNONYM_MAP.containsKey(lowerToken)) {
                List<String> synonyms = SYNONYM_MAP.get(lowerToken);
                expanded.append("(");
                for (int i = 0; i < synonyms.size(); i++) {
                    expanded.append(synonyms.get(i));
                    if (i < synonyms.size() - 1) {
                        expanded.append(" OR ");
                    }
                }
                expanded.append(")");
            } else {
                expanded.append(token);
            }
        }

        return expanded.toString();
    }
}