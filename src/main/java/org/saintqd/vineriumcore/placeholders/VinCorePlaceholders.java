package org.saintqd.vineriumcore.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;

public class VinCorePlaceholders extends PlaceholderExpansion {

    private final VineriumCore plugin;

    public VinCorePlaceholders(VineriumCore plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor(){
        return plugin.getPluginMeta().getAuthors().toString();
    }

    @Override
    public String getIdentifier(){
        return "vineriumcore";
    }

    @Override
    public String getVersion(){
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }

        return Placeholder.valueOf(identifier.toUpperCase()).placeholderResult(plugin,player);
    }

    public enum Placeholder {

        PVP_MODE {
            @Override
            public String placeholderResult(VineriumCore plugin, Player player) {
                PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
                if (playerManager.getPvpModePlayers().contains(player))
                    return playerManager.getPvpPlaceholder();
                else return "";
            }
        };

        public abstract String placeholderResult(VineriumCore plugin, Player player);
    }
}
