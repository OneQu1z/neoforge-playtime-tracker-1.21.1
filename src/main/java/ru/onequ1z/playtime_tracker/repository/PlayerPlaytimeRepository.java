package ru.onequ1z.playtime_tracker.repository;

import ru.onequ1z.playtime_tracker.entity.PlayerPlaytimeEntity;
import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с таблицей {@code player_playtime}.
 * <p>
 * Хранит и обновляет суммарное и недельное игровое время игроков.
 */
public class PlayerPlaytimeRepository {
    private static final String FIND_BY_UUID_SQL = """
            SELECT player_uuid, total_playtime_seconds, weekly_playtime_seconds
            FROM player_playtime
            WHERE player_uuid = ?
            """;

    private static final String FIND_ALL_UUIDS_SQL = """
            SELECT player_uuid FROM player_playtime
            """;

    private static final String ENSURE_EXISTS_SQL = """
            INSERT INTO player_playtime (player_uuid, total_playtime_seconds, weekly_playtime_seconds)
            VALUES (?, 0, 0)
            ON CONFLICT (player_uuid) DO NOTHING
            """;

    private static final String INCREMENT_AFTER_SESSION_SQL = """
            INSERT INTO player_playtime (player_uuid, total_playtime_seconds, weekly_playtime_seconds)
            VALUES (?, ?, 0)
            ON CONFLICT (player_uuid) DO UPDATE SET
                total_playtime_seconds = player_playtime.total_playtime_seconds + EXCLUDED.total_playtime_seconds
            """;

    private static final String UPDATE_WEEKLY_SQL = """
            UPDATE player_playtime
            SET weekly_playtime_seconds = ?
            WHERE player_uuid = ?
            """;

    private final ConnectionManager connectionManager;

    public PlayerPlaytimeRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Optional<PlayerPlaytimeEntity> findByUuid(UUID uuid) {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_BY_UUID_SQL)
        ) {
            statement.setObject(1, uuid);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new PlayerPlaytimeEntity(
                        (UUID) resultSet.getObject("player_uuid"),
                        resultSet.getLong("total_playtime_seconds"),
                        resultSet.getLong("weekly_playtime_seconds")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find player playtime", e);
        }
    }

    public List<UUID> findAllUuids() {
        try (
                Connection connection = connectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_ALL_UUIDS_SQL);
                ResultSet resultSet = statement.executeQuery()
        ) {
            List<UUID> uuids = new ArrayList<>();
            while (resultSet.next()) {
                uuids.add((UUID) resultSet.getObject("player_uuid"));
            }
            return uuids;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all player uuids", e);
        }
    }

    public void incrementAfterSession(UUID uuid, long sessionDurationSeconds) {
        try (Connection connection = connectionManager.getConnection()) {
            incrementAfterSession(connection, uuid, sessionDurationSeconds);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to increment player playtime", e);
        }
    }

    public void incrementAfterSession(Connection connection, UUID uuid, long sessionDurationSeconds)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INCREMENT_AFTER_SESSION_SQL)) {
            statement.setObject(1, uuid);
            statement.setLong(2, sessionDurationSeconds);
            statement.executeUpdate();
        }
    }

    public void updateWeeklyPlaytime(UUID uuid, long weeklyPlaytimeSeconds) {
        try (Connection connection = connectionManager.getConnection()) {
            updateWeeklyPlaytime(connection, uuid, weeklyPlaytimeSeconds);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update weekly playtime", e);
        }
    }

    public void updateWeeklyPlaytime(Connection connection, UUID uuid, long weeklyPlaytimeSeconds)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_WEEKLY_SQL)) {
            statement.setLong(1, weeklyPlaytimeSeconds);
            statement.setObject(2, uuid);
            statement.executeUpdate();
        }
    }
}
