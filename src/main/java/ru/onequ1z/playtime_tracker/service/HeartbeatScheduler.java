package ru.onequ1z.playtime_tracker.service;

public class HeartbeatScheduler {
    public static final int HEARTBEAT_INTERVAL_TICKS = 5 * 60 * 20;

    private int tickCounter;

    public boolean onServerTick() {
        tickCounter++;
        if (tickCounter >= HEARTBEAT_INTERVAL_TICKS) {
            tickCounter = 0;
            return true;
        }
        return false;
    }
}
