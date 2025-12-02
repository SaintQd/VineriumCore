package org.saintqd.vineriumcore.managers;

import kotlin.Pair;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;

public class PlayerManager {

    private Boolean pvpModeEnabled = false;
    private String pvpPlaceholder = "";
    private final HashSet<Player> pvpModePlayers = new HashSet<>();

    private final HashMap<String, HashMap<Player, Pair<String,Long>>> timers = new HashMap<>();

    public void loadParams(Plugin plugin) {
        pvpModeEnabled = plugin.getConfig().getBoolean("PvPMode.Enabled");
        pvpPlaceholder = plugin.getConfig().getString("PvPMode.Placeholder");
    }

    public HashSet<Player> getPvpModePlayers() {
        return pvpModePlayers;
    }

    public HashMap<String, HashMap<Player, Pair<String, Long>>> getTimers() {
        return timers;
    }

    public String getPvpPlaceholder() {
        return pvpPlaceholder;
    }

    public Boolean isPvpModeEnabled() {
        return pvpModeEnabled;
    }
}
