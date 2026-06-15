package ru.onequ1z.playtime_tracker.service;

import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.model.PlayerSessionData;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayTimeService {
    private final Map<UUID, PlayerSessionData> activeSessions = new HashMap<>();
    public void playerJoined(UUID uuid, String nickname) {
        if (activeSessions.containsKey(uuid)) {
            PlayTimeTrackerMod.LOGGER.warn("Player {} already in activeSession", uuid);
            return;
        }
        PlayTimeTrackerMod.LOGGER.info("Started session for player {}", nickname);
        activeSessions.put(
                uuid,
                new PlayerSessionData(nickname, Instant.now())

        );
    }
    public void playerLeft(UUID uuid){
        PlayerSessionData playerSessionData = activeSessions.get(uuid);

        if (playerSessionData != null) {
            Instant time = playerSessionData.getLoginTime();
            Instant now = Instant.now();

            Duration duration = Duration.between(time, now);
            long hours = duration.toHours();
            int minutes = duration.toMinutesPart();
            int seconds = duration.toSecondsPart();
            String formattedDuration = String.format("%dh %dm %ds", hours, minutes, seconds);
            PlayTimeTrackerMod.LOGGER.info("Player: {} left in server session, Duration = {}", playerSessionData.getNickname(), formattedDuration);
            activeSessions.remove(uuid);
        } else {
            PlayTimeTrackerMod.LOGGER.warn("No active session found for player {}", uuid);
        }

    }

}
