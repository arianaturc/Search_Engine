package database;

import indexer.FileRecord;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class FileRepository {

    private final DatabaseManager db;

    public FileRepository(DatabaseManager db) {
        this.db = db;
    }

    public void initializeSchema() throws SQLException {
        String createFiles = """
            CREATE TABLE IF NOT EXISTS files (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                path          TEXT NOT NULL UNIQUE,
                name          TEXT NOT NULL,
                extension     TEXT,
                size          INTEGER,
                last_modified INTEGER,
                created_at    INTEGER,
                is_hidden     INTEGER,
                is_readable   INTEGER,
                mime_type     TEXT,
                tags          TEXT,
                content       TEXT,
                preview       TEXT
            );
        """;

        String createFts = """
            CREATE VIRTUAL TABLE IF NOT EXISTS files_fts
            USING fts5(name, content, content='files', content_rowid='id');
        """;

        String createInsertTrigger = """
            CREATE TRIGGER IF NOT EXISTS files_ai
            AFTER INSERT ON files BEGIN
                INSERT INTO files_fts(rowid, name, content)
                VALUES (new.id, new.name, new.content);
            END;
        """;

        String createUpdateTrigger = """
            CREATE TRIGGER IF NOT EXISTS files_au
            AFTER UPDATE ON files BEGIN
                INSERT INTO files_fts(files_fts, rowid, name, content)
                VALUES ('delete', old.id, old.name, old.content);
                INSERT INTO files_fts(rowid, name, content)
                VALUES (new.id, new.name, new.content);
            END;
        """;

        String createDeleteTrigger = """
            CREATE TRIGGER IF NOT EXISTS files_ad
            AFTER DELETE ON files BEGIN
                INSERT INTO files_fts(files_fts, rowid, name, content)
                VALUES ('delete', old.id, old.name, old.content);
            END;
        """;

        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute(createFiles);
            stmt.execute(createFts);
            stmt.execute(createInsertTrigger);
            stmt.execute(createUpdateTrigger);
            stmt.execute(createDeleteTrigger);
        }
    }

    public void insertOrUpdate(FileRecord record) throws SQLException {
        db.ensureConnected();
        String sql = """
            INSERT INTO files
                (path, name, extension, size, last_modified, created_at,
                 is_hidden, is_readable, mime_type, tags, content, preview)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(path) DO UPDATE SET
                name          = excluded.name,
                extension     = excluded.extension,
                size          = excluded.size,
                last_modified = excluded.last_modified,
                is_hidden     = excluded.is_hidden,
                is_readable   = excluded.is_readable,
                mime_type     = excluded.mime_type,
                tags          = excluded.tags,
                content       = excluded.content,
                preview       = excluded.preview;
        """;

        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1,  record.path());
            stmt.setString(2,  record.name());
            stmt.setString(3,  record.extension());
            stmt.setLong  (4,  record.size());
            stmt.setLong  (5,  record.lastModified());
            stmt.setLong  (6,  record.createdAt());
            stmt.setInt   (7,  record.isHidden() ? 1 : 0);
            stmt.setInt   (8,  record.isReadable() ? 1 : 0);
            stmt.setString(9,  record.mimeType());
            stmt.setString(10, record.tags());
            stmt.setString(11, record.content());
            stmt.setString(12, record.preview());
            stmt.executeUpdate();
        }
    }


    public long getLastModified(String path) throws SQLException {
        db.ensureConnected();
        String sql = "SELECT last_modified FROM files WHERE path = ?;";

        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, path);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_modified");
            }
        }
        return -1;
    }


    public Set<String> getAllIndexedPaths() throws SQLException {
        db.ensureConnected();
        Set<String> paths = new HashSet<>();
        String sql = "SELECT path FROM files;";

        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                paths.add(rs.getString("path"));
            }
        }
        return paths;
    }


    public void removeByPath(String path) throws SQLException {
        db.ensureConnected();
        String sql = "DELETE FROM files WHERE path = ?;";

        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.executeUpdate();
        }
    }

    public void clearDatabase() throws SQLException {
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute("DELETE FROM files;");
            stmt.execute("DELETE FROM files_fts;");
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='files';");
            System.out.println("Database cleared.");
        }
    }
}