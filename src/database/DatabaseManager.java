package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:C:/Users/arian/Documents/an3_utcn/sem2/sd/SoftwareDesign_Project/Search_Engine/data/search_engine.db";
    private Connection connection;

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        System.out.println("Connected to SQLite.");
    }

    public void initializeSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS files (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                path      TEXT NOT NULL UNIQUE,
                name      TEXT NOT NULL,
                extension TEXT,
                size      INTEGER,
                last_modified INTEGER,
                content   TEXT,
                preview   TEXT
            );
        """;
        String fts = """
            CREATE VIRTUAL TABLE IF NOT EXISTS files_fts
            USING fts5(name, content, content='files', content_rowid='id');
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute(fts);
        }
    }

    public void close() throws SQLException {
        if (connection != null) connection.close();
    }

    public Connection getConnection() {
        return connection;
    }
}
