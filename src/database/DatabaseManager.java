package database;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/search_engine.db";
    private Connection connection;

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");
        }
        System.out.println("Connected to SQLite.");
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void ensureConnected() throws SQLException {
        if (!isConnected()) {
            System.err.println("Connection lost, reconnecting...");
            connect();
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}