package ru.onequ1z.playtime_tracker.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import ru.onequ1z.playtime_tracker.PlayTimeTrackerMod;
import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;

/**
 * Обработчик остановки сервера.
 * <p>
 * Сохраняет активные сессии, завершает фоновые задачи БД и закрывает пул соединений.
 */
public class ServerShutdownHandler {
    private final PlayTimeService playTimeService;
    private final ConnectionManager connectionManager;

    public ServerShutdownHandler(
            PlayTimeService playTimeService,
            ConnectionManager connectionManager
    ) {
        this.playTimeService = playTimeService;
        this.connectionManager = connectionManager;
    }

    @SubscribeEvent
    public void handleServerStopping(ServerStoppingEvent event) {
        PlayTimeTrackerMod.LOGGER.info("Saving playtime data before server shutdown");
        playTimeService.shutdown();
        connectionManager.close();
    }
}
