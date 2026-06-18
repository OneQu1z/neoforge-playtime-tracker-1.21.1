package ru.onequ1z.playtime_tracker.entity;

import java.time.Instant;
import java.util.UUID;


/**
 * Сущность одной игровой сессии игрока.
 * <p>
 * Соответствует строке таблицы {@code player_sessions}: от входа на сервер
 * до выхода или принудительного закрытия при сбое.
 */
public class PlayerSessionEntity {
    private Long id;
    private UUID playerUuid;
    private Instant loginTime;
    private Instant lastSeenAt;
    private Instant logoutTime;
    private Long durationSeconds;

    public PlayerSessionEntity() {
    }

    public PlayerSessionEntity(
            Long id,
            UUID playerUuid,
            Instant loginTime,
            Instant lastSeenAt,
            Instant logoutTime,
            Long durationSeconds
    ) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.loginTime = loginTime;
        this.lastSeenAt = lastSeenAt;
        this.logoutTime = logoutTime;
        this.durationSeconds = durationSeconds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
