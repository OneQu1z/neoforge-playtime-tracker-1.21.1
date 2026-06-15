package ru.onequ1z.playtime_tracker;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ru.onequ1z.playtime_tracker.event.PlayerConnectionHandler;
import ru.onequ1z.playtime_tracker.service.PlayTimeService;


@Mod(PlayTimeTrackerMod.MOD_ID)
public class PlayTimeTrackerMod {
    public static final String MOD_ID = "playtime_tracker";
    public static final Logger LOGGER = LogUtils.getLogger();


    public PlayTimeTrackerMod() {

        PlayTimeService playTimeService = new PlayTimeService();
        PlayerConnectionHandler playerConnectionHandler = new PlayerConnectionHandler(playTimeService);
        NeoForge.EVENT_BUS.register(playerConnectionHandler);
    }

}
