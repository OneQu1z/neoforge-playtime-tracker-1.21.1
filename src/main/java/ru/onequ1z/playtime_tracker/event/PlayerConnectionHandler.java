package ru.onequ1z.playtime_tracker.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;

import java.util.UUID;


/**
 * Обработчик событий входа и выхода игрока с сервера.
 * <p>
 * Передаёт UUID и никнейм в {@link ru.onequ1z.playtime_tracker.service.PlayTimeService}
 * для открытия и закрытия сессий.
 */
public class PlayerConnectionHandler {
    private final PlayTimeService playTimeService;
    public PlayerConnectionHandler(PlayTimeService playTimeService) {
        this.playTimeService = playTimeService;
    }

    @SubscribeEvent
    public void handlePlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String name = player.getGameProfile().getName();
        UUID uuid = player.getUUID();
        playTimeService.playerJoined(uuid, name);
    }

    @SubscribeEvent
    public void handlePlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        playTimeService.playerLeft(uuid);
    }

}
