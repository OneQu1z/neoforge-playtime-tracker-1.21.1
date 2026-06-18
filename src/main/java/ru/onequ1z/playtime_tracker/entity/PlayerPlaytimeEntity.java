package ru.onequ1z.playtime_tracker.entity;

import java.util.UUID;


/**
 * Агрегированная статистика игрового времени игрока.
 * <p>
 * Соответствует строке таблицы {@code player_playtime} и хранит общее
 * и недельное время в секундах.
 */
public class PlayerPlaytimeEntity {
    private UUID playerUuid;
    private long totalPlaytimeSeconds;
    private long weeklyPlaytimeSeconds;

    public PlayerPlaytimeEntity() {
    }
    public PlayerPlaytimeEntity(UUID playerUuid, long totalPlaytimeSeconds, long weeklyPlaytimeSeconds) {
        this.playerUuid = playerUuid;
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
        this.weeklyPlaytimeSeconds = weeklyPlaytimeSeconds;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public long getTotalPlaytimeSeconds() {
        return totalPlaytimeSeconds;
    }

    public void setTotalPlaytimeSeconds(long totalPlaytimeSeconds) {
        this.totalPlaytimeSeconds = totalPlaytimeSeconds;
    }

    public long getWeeklyPlaytimeSeconds() {
        return weeklyPlaytimeSeconds;
    }

    public void setWeeklyPlaytimeSeconds(long weeklyPlaytimeSeconds) {
        this.weeklyPlaytimeSeconds = weeklyPlaytimeSeconds;
    }
}
