package ru.onequ1z.playtime_tracker.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;

public class ServerStartupHandler {
    private final PlayTimeService playTimeService;

    public ServerStartupHandler(PlayTimeService playTimeService) {
        this.playTimeService = playTimeService;
    }

    @SubscribeEvent
    public void handleServerAboutToStart(ServerAboutToStartEvent event) {
        PlayTimeTrackerMod.LOGGER.info("Recovering orphaned playtime sessions before server start");
        playTimeService.recoverOnStartup();
    }
}
