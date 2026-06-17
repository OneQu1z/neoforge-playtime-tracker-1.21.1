package ru.onequ1z.playtime_tracker.persistence;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseInitializer {
    private final ConnectionManager connectionManager;

    public DatabaseInitializer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initialize() {
        createPlayerPlaytimeTable();
        createPlayerSessionsTable();
    }

    private void createPlayerPlaytimeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_playtime (
                player_uuid UUID PRIMARY KEY,
                total_playtime_seconds BIGINT NOT NULL,
                weekly_playtime_seconds BIGINT NOT NULL
            )
            """;

        executeUpdate(sql);
        PlayTimeTrackerMod.LOGGER.info("Ensured table player_playtime exists");

    }

    private void createPlayerSessionsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_sessions (
                id BIGSERIAL PRIMARY KEY,
                player_uuid UUID NOT NULL,
                login_time TIMESTAMPTZ NOT NULL,
                logout_time TIMESTAMPTZ NOT NULL,
                duration_seconds BIGINT NOT NULL
            )
            """;

        executeUpdate(sql);
        PlayTimeTrackerMod.LOGGER.info("Ensured table player_sessions exists");
    }

    private void executeUpdate(String sql) {
        try (
                Connection connection = connectionManager.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to execute SQL statement",
                    e
            );
        }
    }
}
