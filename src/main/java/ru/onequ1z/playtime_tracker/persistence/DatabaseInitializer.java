package ru.onequ1z.playtime_tracker.persistence;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Создаёт и мигрирует таблицы БД при старте мода.
 * <p>
 * Гарантирует наличие {@code player_playtime} и {@code player_sessions},
 * а также применяет инкрементальные миграции схемы.
 */
public class DatabaseInitializer {
    private final ConnectionManager connectionManager;

    public DatabaseInitializer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initialize() {
        createPlayerPlaytimeTable();
        createPlayerSessionsTable();
        migratePlayerSessionsTable();
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
                last_seen_at TIMESTAMPTZ NOT NULL,
                logout_time TIMESTAMPTZ,
                duration_seconds BIGINT
            )
            """;

        executeUpdate(sql);
        PlayTimeTrackerMod.LOGGER.info("Ensured table player_sessions exists");
    }

    private void migratePlayerSessionsTable() {
        executeUpdate("ALTER TABLE player_sessions ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ");
        executeUpdate("""
            UPDATE player_sessions
            SET last_seen_at = login_time
            WHERE last_seen_at IS NULL
            """);
        executeUpdate("""
            ALTER TABLE player_sessions
            ALTER COLUMN last_seen_at SET NOT NULL
            """);
        executeUpdate("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_player_sessions_one_open_per_player
            ON player_sessions (player_uuid)
            WHERE logout_time IS NULL
            """);
        PlayTimeTrackerMod.LOGGER.info("Applied player_sessions migrations");
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
