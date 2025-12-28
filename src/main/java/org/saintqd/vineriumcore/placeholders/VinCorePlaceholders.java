package org.saintqd.vineriumcore.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumlib.VineriumLib;

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
        },
        VIP_UNTIL {
            @Override
            public String placeholderResult(VineriumCore plugin, Player player) {
                if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null && VineriumCore.inst().getLuckPermsManager() != null) {
                    return VineriumCore.inst().getLuckPermsManager().getVipUntil(player);
                }
                else return "";
            }
        },
        SUFFIX {
            @Override
            public String placeholderResult(VineriumCore plugin, Player player) {
                if (VineriumCore.inst().getLuckPermsManager() != null)
                    return VineriumCore.inst().getLuckPermsManager().getSuffix(player);
                else return "";
            }
        },
        SUFFIX_PARSED {
            @Override
            public String placeholderResult(VineriumCore plugin, Player player) {
                return PlaceholderAPI.setPlaceholders(player,PlaceholderAPI.setPlaceholders(player,"%luckperms_suffix%"));
            }
        };

        public abstract String placeholderResult(VineriumCore plugin, Player player);
    }
}
