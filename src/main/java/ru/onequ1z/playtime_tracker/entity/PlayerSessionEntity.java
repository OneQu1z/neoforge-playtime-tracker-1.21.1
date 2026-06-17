package ru.onequ1z.playtime_tracker.entity;

import java.time.Instant;
import java.util.UUID;


public class PlayerSessionEntity {
    private UUID playerUuid;
    private Instant loginTime;
    private Instant logoutTime;
    private long durationSeconds;

    public PlayerSessionEntity() {
    }

    public PlayerSessionEntity(UUID playerUuid, Instant loginTime, Instant logoutTime, long durationSeconds) {
        this.playerUuid = playerUuid;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.durationSeconds = durationSeconds;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }

    public Instant getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(Instant logoutTime) {
        this.logoutTime = logoutTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
