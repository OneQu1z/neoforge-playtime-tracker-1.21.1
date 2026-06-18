package ru.onequ1z.playtime_tracker.service;

import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;
import ru.onequ1z.playtime_tracker.repository.PlayerPlaytimeRepository;
import ru.onequ1z.playtime_tracker.repository.PlayerSessionRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class SessionFinalizer {
    private static final int WEEKLY_PERIOD_DAYS = 7;

    private final ConnectionManager connectionManager;
    private final PlayerSessionRepository sessionRepository;
    private final PlayerPlaytimeRepository playtimeRepository;

    public SessionFinalizer(
            ConnectionManager connectionManager,
            PlayerSessionRepository sessionRepository,
            PlayerPlaytimeRepository playtimeRepository
    ) {
        this.connectionManager = connectionManager;
        this.sessionRepository = sessionRepository;
        this.playtimeRepository = playtimeRepository;
    }

    public boolean finalizeSession(
            long sessionId,
            UUID playerUuid,
            Instant logoutTime,
            long durationSeconds
    ) {
        try (Connection connection = connectionManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!sessionRepository.closeSession(connection, sessionId, logoutTime, durationSeconds)) {
                    connection.rollback();
                    return false;
                }

                playtimeRepository.incrementAfterSession(connection, playerUuid, durationSeconds);

                Instant weekStart = Instant.now().minus(WEEKLY_PERIOD_DAYS, ChronoUnit.DAYS);
                long weeklySeconds = sessionRepository.sumDurationSince(connection, playerUuid, weekStart);
                playtimeRepository.updateWeeklyPlaytime(connection, playerUuid, weeklySeconds);

                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException("Failed to finalize session " + sessionId, e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }
}
