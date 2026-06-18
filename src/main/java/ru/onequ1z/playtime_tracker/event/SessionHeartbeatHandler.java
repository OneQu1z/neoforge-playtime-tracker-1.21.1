package ru.onequ1z.playtime_tracker.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.onequ1z.playtime_tracker.service.HeartbeatScheduler;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;

public class SessionHeartbeatHandler {
    private final PlayTimeService playTimeService;
    private final HeartbeatScheduler heartbeatScheduler;

    public SessionHeartbeatHandler(PlayTimeService playTimeService, HeartbeatScheduler heartbeatScheduler) {
        this.playTimeService = playTimeService;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @SubscribeEvent
    public void handleServerTick(ServerTickEvent.Post event) {
        if (heartbeatScheduler.onServerTick()) {
            playTimeService.flushHeartbeats();
        }
    }
}
