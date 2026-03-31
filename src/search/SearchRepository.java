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
                   f.last_modified, f.tags, f.preview
            FROM files f
            JOIN files_fts fts ON f.id = fts.rowid
            WHERE files_fts MATCH ?
            GROUP BY f.name, f.size
            ORDER BY rank
            LIMIT ?;
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, processedQuery);
            stmt.setInt(2, maxResults);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(new SearchResult(
                        rs.getString("path"),
                        rs.getString("name"),
                        rs.getString("extension"),
                        rs.getLong("size"),
                        rs.getLong("last_modified"),
                        rs.getString("tags"),
                        rs.getString("preview")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return results;
    }
}