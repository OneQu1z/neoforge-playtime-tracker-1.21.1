package ru.onequ1z.playtime_tracker.service;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.entity.PlayerSessionEntity;
import ru.onequ1z.playtime_tracker.repository.PlayerSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SessionRecoveryService {
    private final PlayerSessionRepository sessionRepository;
    private final SessionFinalizer sessionFinalizer;

    public SessionRecoveryService(
            PlayerSessionRepository sessionRepository,
            SessionFinalizer sessionFinalizer
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionFinalizer = sessionFinalizer;
    }

    public void recoverOrphanedSessions() {
        List<PlayerSessionEntity> openSessions = sessionRepository.findOpenSessions();
        if (openSessions.isEmpty()) {
            PlayTimeTrackerMod.LOGGER.info("No orphaned sessions to recover");
            return;
        }

        PlayTimeTrackerMod.LOGGER.info("Recovering {} orphaned session(s)", openSessions.size());
        for (PlayerSessionEntity session : openSessions) {
            recoverSession(session);
        }
    }

    public void closeOrphanForPlayer(UUID playerUuid) {
        sessionRepository.findOpenSessionByPlayer(playerUuid).ifPresent(this::recoverSession);
    }

    private void recoverSession(PlayerSessionEntity session) {
        Instant logoutTime = session.getLastSeenAt();
        long durationSeconds = Duration.between(session.getLoginTime(), logoutTime).getSeconds();

        if (sessionFinalizer.finalizeSession(
                session.getId(),
                session.getPlayerUuid(),
                logoutTime,
                durationSeconds
        )) {
            PlayTimeTrackerMod.LOGGER.info(
                    "Recovered orphaned session {} for player {}, duration = {}s",
                    session.getId(),
                    session.getPlayerUuid(),
                    durationSeconds
            );
        }
    }
}
