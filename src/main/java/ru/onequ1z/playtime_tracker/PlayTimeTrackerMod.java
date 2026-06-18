package ru.onequ1z.playtime_tracker;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ru.onequ1z.playtime_tracker.event.PlayerConnectionHandler;
import ru.onequ1z.playtime_tracker.event.ServerShutdownHandler;
import ru.onequ1z.playtime_tracker.event.ServerStartupHandler;
import ru.onequ1z.playtime_tracker.event.SessionHeartbeatHandler;
import ru.onequ1z.playtime_tracker.persistence.ConnectionManager;
import ru.onequ1z.playtime_tracker.persistence.DatabaseConfigLoader;
import ru.onequ1z.playtime_tracker.persistence.DatabaseInitializer;
import ru.onequ1z.playtime_tracker.persistence.DatabaseProperties;
import ru.onequ1z.playtime_tracker.repository.PlayerPlaytimeRepository;
import ru.onequ1z.playtime_tracker.repository.PlayerSessionRepository;
import ru.onequ1z.playtime_tracker.service.HeartbeatScheduler;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;
import ru.onequ1z.playtime_tracker.service.SessionFinalizer;
import ru.onequ1z.playtime_tracker.service.SessionRecoveryService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(PlayTimeTrackerMod.MOD_ID)
public class PlayTimeTrackerMod {
    public static final String MOD_ID = "playtime_tracker";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final ConnectionManager connectionManager;

    public PlayTimeTrackerMod() {
        DatabaseProperties properties =
                new DatabaseConfigLoader().load();

        this.connectionManager =
                new ConnectionManager(properties);

        DatabaseInitializer initializer =
                new DatabaseInitializer(connectionManager);
        initializer.initialize();

        PlayerSessionRepository sessionRepository =
                new PlayerSessionRepository(connectionManager);
        PlayerPlaytimeRepository playtimeRepository =
                new PlayerPlaytimeRepository(connectionManager);

        SessionFinalizer sessionFinalizer = new SessionFinalizer(
                connectionManager,
                sessionRepository,
                playtimeRepository
        );

        SessionRecoveryService sessionRecoveryService = new SessionRecoveryService(
                sessionRepository,
                sessionFinalizer
        );

        ExecutorService dbExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "playtime-tracker-db");
            thread.setDaemon(true);
            return thread;
        });

        PlayTimeService playTimeService = new PlayTimeService(
                sessionRepository,
                sessionFinalizer,
                sessionRecoveryService,
                dbExecutor
        );

        HeartbeatScheduler heartbeatScheduler = new HeartbeatScheduler();

        PlayerConnectionHandler playerConnectionHandler =
                new PlayerConnectionHandler(playTimeService);

        ServerShutdownHandler serverShutdownHandler =
                new ServerShutdownHandler(playTimeService, connectionManager);

        ServerStartupHandler serverStartupHandler =
                new ServerStartupHandler(playTimeService);

        SessionHeartbeatHandler sessionHeartbeatHandler =
                new SessionHeartbeatHandler(playTimeService, heartbeatScheduler);

        NeoForge.EVENT_BUS.register(playerConnectionHandler);
        NeoForge.EVENT_BUS.register(serverShutdownHandler);
        NeoForge.EVENT_BUS.register(serverStartupHandler);
        NeoForge.EVENT_BUS.register(sessionHeartbeatHandler);

        LOGGER.info("Database initialized");
    }
}
