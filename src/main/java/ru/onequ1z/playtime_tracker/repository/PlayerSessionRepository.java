package ru.onequ1z.playtime_tracker.repository;

import ru.onequ1z.playtime_tracker.entity.PlayerSessionEntity;
import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerSessionRepository {
    private static final String SUM_DURATION_SINCE_SQL = """
            SELECT COALESCE(SUM(duration_seconds), 0)
            FROM player_sessions
            WHERE player_uuid = ?
              AND logout_time IS NOT NULL
              AND duration_seconds IS NOT NULL
              AND logout_time > ?
              AND login_time < NOW()
            """;

    private static final String INSERT_OPEN_SESSION_SQL = """
            INSERT INTO player_sessions (player_uuid, login_time, last_seen_at)
            VALUES (?, ?, ?)
            """;

    private static final String CLOSE_SESSION_SQL = """
            UPDATE player_sessions
            SET logout_time = ?, duration_seconds = ?
            WHERE id = ? AND logout_time IS NULL
            """;

    private static final String UPDATE_LAST_SEEN_SQL = """
            UPDATE player_sessions
            SET last_seen_at = ?
            WHERE id = ? AND logout_time IS NULL
            """;

    private static final String FIND_OPEN_SESSIONS_SQL = """
            SELECT id, player_uuid, login_time, last_seen_at
            FROM player_sessions
            WHERE logout_time IS NULL
            """;

    private static final String FIND_OPEN_SESSION_BY_PLAYER_SQL = """
            SELECT id, player_uuid, login_time, last_seen_at
            FROM player_sessions
            WHERE player_uuid = ? AND logout_time IS NULL
            ORDER BY login_time DESC
            LIMIT 1
            """;

    private final ConnectionManager connectionManager;

    public PlayerSessionRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public long sumDurationSince(UUID playerUuid, Instant since) {
        try (Connection connection = connectionManager.getConnection()) {
            return sumDurationSince(connection, playerUuid, since);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum session duration", e);
        }
    }

    public long sumDurationSince(Connection connection, UUID playerUuid, Instant since) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SUM_DURATION_SINCE_SQL)) {
            statement.setObject(1, playerUuid);
            statement.setTimestamp(2, Timestamp.from(since));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            }
        }
    }

    public Long createOpenSession(UUID playerUuid, Instant loginTime) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        INSERT_OPEN_SESSION_SQL,
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setObject(1, playerUuid);
            statement.setTimestamp(2, Timestamp.from(loginTime));
            statement.setTimestamp(3, Timestamp.from(loginTime));
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create open session", e);
        }
    }

    public boolean closeSession(long sessionId, Instant logoutTime, long durationSeconds) {
        try (Connection connection = connectionManager.getConnection()) {
            return closeSession(connection, sessionId, logoutTime, durationSeconds);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close player session", e);
        }
    }

    public boolean closeSession(
            Connection connection,
            long sessionId,
            Instant logoutTime,
            long durationSeconds
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(CLOSE_SESSION_SQL)) {
            statement.setTimestamp(1, Timestamp.from(logoutTime));
            statement.setLong(2, durationSeconds);
            statement.setLong(3, sessionId);
            return statement.executeUpdate() > 0;
        }
    }

    public void updateLastSeen(long sessionId, Instant seenAt) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_LAST_SEEN_SQL)
        ) {
            statement.setTimestamp(1, Timestamp.from(seenAt));
            statement.setLong(2, sessionId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update last seen for session", e);
        }
    }

    public List<PlayerSessionEntity> findOpenSessions() {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_OPEN_SESSIONS_SQL);
                ResultSet resultSet = statement.executeQuery()
        ) {
            List<PlayerSessionEntity> sessions = new ArrayList<>();
            while (resultSet.next()) {
                sessions.add(mapOpenSession(resultSet));
            }
            return sessions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find open sessions", e);
        }
    }

    public Optional<PlayerSessionEntity> findOpenSessionByPlayer(UUID playerUuid) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_OPEN_SESSION_BY_PLAYER_SQL)
        ) {
            statement.setObject(1, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapOpenSession(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find open session for player", e);
        }
    }

    public Optional<Long> findOpenSessionIdByPlayer(UUID playerUuid) {
        return findOpenSessionByPlayer(playerUuid).map(PlayerSessionEntity::getId);
    }

    private PlayerSessionEntity mapOpenSession(ResultSet resultSet) throws SQLException {
        PlayerSessionEntity entity = new PlayerSessionEntity();
        entity.setId(resultSet.getLong("id"));
        entity.setPlayerUuid((UUID) resultSet.getObject("player_uuid"));
        entity.setLoginTime(resultSet.getTimestamp("login_time").toInstant());
        entity.setLastSeenAt(resultSet.getTimestamp("last_seen_at").toInstant());
        return entity;
    }
}
