package ru.onequ1z.playtime_tracker.service;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.entity.PlayerSessionEntity;
import ru.onequ1z.playtime_tracker.repository.PlayerPlaytimeRepository;
import ru.onequ1z.playtime_tracker.repository.PlayerSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayTimeService {
    private static final int WEEKLY_PERIOD_DAYS = 7;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final PlayerSessionRepository sessionRepository;
    private final PlayerPlaytimeRepository playtimeRepository;
    private final ExecutorService dbExecutor;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final Map<UUID, Instant> activeSessions = new ConcurrentHashMap<>();

    public PlayTimeService(
            PlayerSessionRepository sessionRepository,
            PlayerPlaytimeRepository playtimeRepository,
            ExecutorService dbExecutor
    ) {
        this.sessionRepository = sessionRepository;
        this.playtimeRepository = playtimeRepository;
        this.dbExecutor = dbExecutor;
    }

    public void playerJoined(UUID uuid, String nickname) {
        Instant loginTime = Instant.now();
        Instant previous = activeSessions.putIfAbsent(uuid, loginTime);
        if (previous != null) {
            PlayTimeTrackerMod.LOGGER.warn("Player {} already in active session", uuid);
            return;
        }
        PlayTimeTrackerMod.LOGGER.info("Started session for player {}", nickname);
    }

    public void playerLeft(UUID uuid) {
        Instant loginTime = activeSessions.remove(uuid);
        if (loginTime == null) {
            PlayTimeTrackerMod.LOGGER.warn("No active session found for player {}", uuid);
            return;
        }

        Instant logoutTime = Instant.now();
        long durationSeconds = Duration.between(loginTime, logoutTime).getSeconds();
        logSession(uuid, durationSeconds);

        enqueuePersist(new PlayerSessionEntity(uuid, loginTime, logoutTime, durationSeconds));
    }

    public void shutdown() {
        shuttingDown.set(true);

        List<PlayerSessionEntity> remainingSessions = drainActiveSessions();
        for (PlayerSessionEntity session : remainingSessions) {
            persistSession(session);
        }

        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                PlayTimeTrackerMod.LOGGER.warn("Database executor did not finish in time, forcing shutdown");
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private List<PlayerSessionEntity> drainActiveSessions() {
        Instant shutdownTime = Instant.now();
        List<PlayerSessionEntity> sessions = new ArrayList<>();

        for (Map.Entry<UUID, Instant> entry : activeSessions.entrySet()) {
            UUID uuid = entry.getKey();
            Instant loginTime = entry.getValue();
            long durationSeconds = Duration.between(loginTime, shutdownTime).getSeconds();
            sessions.add(new PlayerSessionEntity(uuid, loginTime, shutdownTime, durationSeconds));
            PlayTimeTrackerMod.LOGGER.info(
                    "Closing active session for player {} on server shutdown, duration = {}s",
                    uuid,
                    durationSeconds
            );
        }

        activeSessions.clear();
        return sessions;
    }

    private void enqueuePersist(PlayerSessionEntity session) {
        if (shuttingDown.get()) {
            persistSession(session);
            return;
        }
        try {
            dbExecutor.submit(() -> persistSession(session));
        } catch (RejectedExecutionException e) {
            persistSession(session);
        }
    }

    private void persistSession(PlayerSessionEntity session) {
        try {
            sessionRepository.save(session);
            playtimeRepository.incrementAfterSession(
                    session.getPlayerUuid(),
                    session.getDurationSeconds()
            );

            Instant weekStart = Instant.now().minus(WEEKLY_PERIOD_DAYS, ChronoUnit.DAYS);
            long weeklySeconds = sessionRepository.sumDurationSince(
                    session.getPlayerUuid(),
                    weekStart
            );
            playtimeRepository.updateWeeklyPlaytime(session.getPlayerUuid(), weeklySeconds);
        } catch (RuntimeException e) {
            PlayTimeTrackerMod.LOGGER.error(
                    "Failed to persist session for player {}",
                    session.getPlayerUuid(),
                    e
            );
        }
    }

    private void logSession(UUID uuid, long durationSeconds) {
        Duration duration = Duration.ofSeconds(durationSeconds);
        String formattedDuration = String.format(
                "%dh %dm %ds",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart()
        );
        PlayTimeTrackerMod.LOGGER.info(
                "Player {} left, session duration = {}",
                uuid,
                formattedDuration
        );
    }
}
