package search;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchRepository {
    private final Connection connection;
    private final int maxResults;

    public SearchRepository(Connection connection, int maxResults) {
        this.connection = connection;
        this.maxResults = maxResults;
    }

    public List<SearchResult> search(String processedQuery) {
        List<SearchResult> results = new ArrayList<>();

        if (processedQuery == null || processedQuery.isBlank()) {
            return results;
        }

        String sql = """
            SELECT f.path, f.name, f.extension, f.size,
                   f.last_modified, f.tags, f.preview, f.path_score, f.content
            FROM files f
            JOIN files_fts fts ON f.id = fts.rowid
            WHERE files_fts MATCH ?
            GROUP BY f.name, f.size
            ORDER BY f.path_score DESC, rank
            LIMIT ?;
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, processedQuery);
            stmt.setInt(2, maxResults);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapResult(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return results;
    }


    public List<SearchResult> search(QueryProcessor.ParsedQuery parsedQuery, QueryProcessor processor) {
        List<SearchResult> results = new ArrayList<>();

        if (parsedQuery.isEmpty()) {
            return results;
        }

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        boolean needsFts = !parsedQuery.getGeneralTerms().isEmpty()
                || !parsedQuery.getContentTerms().isEmpty();

        sql.append("SELECT f.path, f.name, f.extension, f.size, ");
        sql.append("f.last_modified, f.tags, f.preview, f.path_score, f.content ");
        sql.append("FROM files f ");

        if (needsFts) {
            sql.append("JOIN files_fts fts ON f.id = fts.rowid ");
        }

        sql.append("WHERE 1=1 ");

        if (!parsedQuery.getGeneralTerms().isEmpty()) {
            String ftsQuery = processor.buildFtsQuery(parsedQuery.getGeneralTerms());
            sql.append("AND files_fts MATCH ? ");
            params.add(ftsQuery);
        }

        if (!parsedQuery.getContentTerms().isEmpty()) {
            for (String term : parsedQuery.getContentTerms()) {
                sql.append("AND files_fts MATCH ? ");
                params.add("content:" + term + "*");
            }
        }

        if (!parsedQuery.getPathTerms().isEmpty()) {
            for (String term : parsedQuery.getPathTerms()) {
                sql.append("AND f.path LIKE ? ");
                params.add("%" + term + "%");
            }
        }

        if (!parsedQuery.getExtTerms().isEmpty()) {
            for (String term : parsedQuery.getExtTerms()) {
                String ext = term.startsWith(".") ? term : "." + term;
                sql.append("AND LOWER(f.extension) = ? ");
                params.add(ext.toLowerCase());
            }
        }

        if (!parsedQuery.getTagTerms().isEmpty()) {
            for (String term : parsedQuery.getTagTerms()) {
                sql.append("AND LOWER(f.tags) = ? ");
                params.add(term.toLowerCase());
            }
        }

        sql.append("GROUP BY f.name, f.size ");
        sql.append("ORDER BY f.path_score DESC");
        if (needsFts) {
            sql.append(", rank");
        }
        sql.append(" LIMIT ?;");
        params.add(maxResults);

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String s) {
                    stmt.setString(i + 1, s);
                } else if (param instanceof Integer n) {
                    stmt.setInt(i + 1, n);
                }
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapResult(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    private SearchResult mapResult(ResultSet rs) throws SQLException {
        return new SearchResult(
                rs.getString("path"),
                rs.getString("name"),
                rs.getString("extension"),
                rs.getLong("size"),
                rs.getLong("last_modified"),
                rs.getString("tags"),
                rs.getString("preview"),
                rs.getDouble("path_score"),
                rs.getString("content") != null ? rs.getString("content") : ""
        );
    }
}