package ru.onequ1z.playtime_tracker.repository;

import ru.onequ1z.playtime_tracker.entity.PlayerSessionEntity;
import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class PlayerSessionRepository {
    private static final String INSERT_SQL = """
            INSERT INTO player_sessions (player_uuid, login_time, logout_time, duration_seconds)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SUM_DURATION_SINCE_SQL = """
            SELECT COALESCE(SUM(
                GREATEST(0, EXTRACT(EPOCH FROM (
                    LEAST(logout_time, NOW()) - GREATEST(login_time, ?)
                ))::bigint)
            ), 0)
            FROM player_sessions
            WHERE player_uuid = ?
              AND logout_time > ?
              AND login_time < NOW()
            """;

    private final ConnectionManager connectionManager;

    public PlayerSessionRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void save(PlayerSessionEntity session) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)
        ) {
            statement.setObject(1, session.getPlayerUuid());
            statement.setTimestamp(2, Timestamp.from(session.getLoginTime()));
            statement.setTimestamp(3, Timestamp.from(session.getLogoutTime()));
            statement.setLong(4, session.getDurationSeconds());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save player session", e);
        }
    }

    public long sumDurationSince(UUID playerUuid, Instant since) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SUM_DURATION_SINCE_SQL)
        ) {
            statement.setTimestamp(1, Timestamp.from(since));
            statement.setObject(2, playerUuid);
            statement.setTimestamp(3, Timestamp.from(since));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum session duration", e);
        }
    }
}
