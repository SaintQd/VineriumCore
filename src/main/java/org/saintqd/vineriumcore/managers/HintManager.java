package org.saintqd.vineriumcore.managers;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.PlayTime.PlayTimeManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class HintManager {

    List<String> hints = new ArrayList<>();
    private BukkitTask starterHintTask = null;

    public void loadHints(VineriumCore plugin) {
        hints.clear();
        File hintsFile = new File(plugin.getDataFolder().getPath() + File.separator + "Hints.yml");
        try {
            if (!hintsFile.exists() && hintsFile.createNewFile()) {
                return;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,"Hints: couldn't load hints from file "+hintsFile+"!");
        }
        YamlConfiguration hintsFileYaml = YamlConfiguration.loadConfiguration(hintsFile);
        hints.addAll(hintsFileYaml.getStringList("Hints"));
    }

    public List<String> getHints() {
        return hints;
    }

    public void sendHint(Audience audience) {
        sendHint(audience, ThreadLocalRandom.current().nextInt(0,hints.size()));
    }

    public void sendHint(Audience audience, int index) {
        String hint = hints.get(index);
        String hintPrefix = VineriumLib.inst().getLangManager().getLangLines(
                VineriumCore.inst()).get("hintPrefix").replace("{1}",Integer.toString(index));
        String finalHint = hintPrefix + hint;
        audience.sendMessage(MiniMessage.miniMessage().deserialize(finalHint));
    }

    public void sendStarterHint(Audience audience) {
        sendStarterHint(audience, ThreadLocalRandom.current().nextInt(0,hints.size()));
    }

    public void sendStarterHint(Audience audience, int index) {
        if (VineriumCore.inst().isCMIEnabled()) {
            long maxPlaytime = VineriumCore.inst().getConfig().getLong("StarterHints.MaxPlaytime",18000000);
            audience = audience.filterAudience(testedAudience -> {
                if (testedAudience instanceof Player player && player.permissionValue("vineriumcore.disablehints") != TriState.TRUE) {
                    CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
                    return user.getTotalPlayTime() < maxPlaytime;
                }
                else return false;
            });
        }
        sendHint(audience, index);
    }

    public void setupStarterHintTask(Plugin plugin) {
        if (starterHintTask != null) {
            starterHintTask.cancel();
            starterHintTask = null;
        }
        long timer = VineriumCore.inst().getConfig().getLong("StarterHints.Timer",-1);
        if (timer < 0)
            return;
        starterHintTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> sendStarterHint(Audience.audience(Bukkit.getOnlinePlayers())),timer,timer);
    }

}
