package ru.onequ1z.playtime_tracker.service;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.repository.PlayerSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayTimeService {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final PlayerSessionRepository sessionRepository;
    private final SessionFinalizer sessionFinalizer;
    private final SessionRecoveryService sessionRecoveryService;
    private final ExecutorService dbExecutor;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    private record ActiveSession(Instant loginTime, Long sessionId) {
    }

    public PlayTimeService(
            PlayerSessionRepository sessionRepository,
            SessionFinalizer sessionFinalizer,
            SessionRecoveryService sessionRecoveryService,
            ExecutorService dbExecutor
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionFinalizer = sessionFinalizer;
        this.sessionRecoveryService = sessionRecoveryService;
        this.dbExecutor = dbExecutor;
    }

    public void playerJoined(UUID uuid, String nickname) {
        Instant loginTime = Instant.now();
        ActiveSession pendingSession = new ActiveSession(loginTime, null);
        ActiveSession previous = activeSessions.putIfAbsent(uuid, pendingSession);
        if (previous != null) {
            PlayTimeTrackerMod.LOGGER.warn("Player {} already in active session", uuid);
            return;
        }

        PlayTimeTrackerMod.LOGGER.info("Started session for player {}", nickname);
        enqueueOpenSession(uuid, loginTime, pendingSession);
    }

    private void enqueueOpenSession(UUID uuid, Instant loginTime, ActiveSession pendingSession) {
        Runnable task = () -> {
            try {
                sessionRecoveryService.closeOrphanForPlayer(uuid);

                Long sessionId = sessionRepository.createOpenSession(uuid, loginTime);
                if (sessionId == null) {
                    PlayTimeTrackerMod.LOGGER.error("Failed to create open session for player {}", uuid);
                    activeSessions.remove(uuid, pendingSession);
                    return;
                }
                activeSessions.computeIfPresent(uuid, (key, session) ->
                        session == pendingSession
                                ? new ActiveSession(loginTime, sessionId)
                                : session
                );
            } catch (RuntimeException e) {
                PlayTimeTrackerMod.LOGGER.error("Failed to create open session for player {}", uuid, e);
                activeSessions.remove(uuid, pendingSession);
            }
        };

        submitDbTask(task);
    }

    public void playerLeft(UUID uuid) {
        ActiveSession session = activeSessions.remove(uuid);
        if (session == null) {
            PlayTimeTrackerMod.LOGGER.warn("No active session found for player {}", uuid);
            return;
        }

        Instant logoutTime = Instant.now();
        long durationSeconds = Duration.between(session.loginTime(), logoutTime).getSeconds();
        logSession(uuid, durationSeconds);
        enqueueFinalize(uuid, session, logoutTime, durationSeconds);
    }

    public void shutdown() {
        shuttingDown.set(true);

        for (Map.Entry<UUID, ActiveSession> entry : activeSessions.entrySet()) {
            UUID uuid = entry.getKey();
            ActiveSession session = entry.getValue();
            Instant shutdownTime = Instant.now();
            long durationSeconds = Duration.between(session.loginTime(), shutdownTime).getSeconds();
            PlayTimeTrackerMod.LOGGER.info(
                    "Closing active session for player {} on server shutdown, duration = {}s",
                    uuid,
                    durationSeconds
            );
            finalizeActiveSession(uuid, session, shutdownTime, durationSeconds);
        }
        activeSessions.clear();

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

    public void flushHeartbeats() {
        Instant now = Instant.now();
        for (ActiveSession session : activeSessions.values()) {
            if (session.sessionId() != null) {
                enqueueHeartbeat(session.sessionId(), now);
            }
        }
    }

    public void recoverOnStartup() {
        runOnDbThreadAndWait(sessionRecoveryService::recoverOrphanedSessions);
    }

    public void runOnDbThreadAndWait(Runnable task) {
        try {
            dbExecutor.submit(task).get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Database task failed during startup recovery", e);
        }
    }

    private void enqueueFinalize(UUID uuid, ActiveSession session, Instant logoutTime, long durationSeconds) {
        submitDbTask(() -> finalizeActiveSession(uuid, session, logoutTime, durationSeconds));
    }

    private void finalizeActiveSession(
            UUID uuid,
            ActiveSession session,
            Instant logoutTime,
            long durationSeconds
    ) {
        try {
            Long sessionId = session.sessionId();
            if (sessionId == null) {
                sessionId = sessionRepository.findOpenSessionIdByPlayer(uuid).orElse(null);
            }
            if (sessionId == null) {
                PlayTimeTrackerMod.LOGGER.warn("No open session in database for player {}", uuid);
                return;
            }

            if (!sessionFinalizer.finalizeSession(sessionId, uuid, logoutTime, durationSeconds)) {
                PlayTimeTrackerMod.LOGGER.warn(
                        "Session {} for player {} was already closed",
                        sessionId,
                        uuid
                );
            }
        } catch (RuntimeException e) {
            PlayTimeTrackerMod.LOGGER.error("Failed to finalize session for player {}", uuid, e);
        }
    }

    private void enqueueHeartbeat(long sessionId, Instant seenAt) {
        submitDbTask(() -> {
            try {
                sessionRepository.updateLastSeen(sessionId, seenAt);
            } catch (RuntimeException e) {
                PlayTimeTrackerMod.LOGGER.error("Failed to update heartbeat for session {}", sessionId, e);
            }
        });
    }

    private void submitDbTask(Runnable task) {
        if (shuttingDown.get()) {
            task.run();
            return;
        }

        try {
            dbExecutor.submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
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
