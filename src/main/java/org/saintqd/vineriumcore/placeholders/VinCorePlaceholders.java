package org.saintqd.vineriumcore.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;

import java.util.HashMap;
import java.util.function.BiFunction;

public class VinCorePlaceholders extends PlaceholderExpansion {

    private final VineriumCore plugin;
    private final HashMap<String, BiFunction<VineriumCore,Player,String>> placeholders = new HashMap<>();

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
    public @NonNull String getAuthor(){
        return plugin.getPluginMeta().getAuthors().toString();
    }

    @Override
    public @NonNull String getIdentifier(){
        return "vineriumcore";
    }

    @Override
    public @NonNull String getVersion(){
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NonNull String identifier) {

        if(player == null) {
            return "";
        }

        return placeholders.get(identifier.toLowerCase()).apply(plugin,player);
    }

    public void registerPlaceholders() {
        placeholders.put("pvp_mode",(plugin,player) -> {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            if (playerManager.getPvpModePlayers().contains(player))
                return playerManager.getPvpPlaceholder();
            else return "";
        });
        placeholders.put("vip_until",(plugin,player) -> {
            if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null
                    && VineriumCore.inst().getLuckPermsManager() != null) {
                return VineriumCore.inst().getLuckPermsManager().getVipUntil(player);
            }
            else return "";
        });
        placeholders.put("suffix",(plugin,player) -> {
            if (VineriumCore.inst().getLuckPermsManager() != null)
                return VineriumCore.inst().getLuckPermsManager().getSuffix(player);
            else return "";
        });
        placeholders.put("suffix_parsed",(plugin,player) ->
                PlaceholderAPI.setPlaceholders(player,PlaceholderAPI.setPlaceholders(player,"%luckperms_suffix%")));
        placeholders.put("suffix_symbol_from_luckperms",(plugin,player) -> {
            String suffixName = VineriumCore.inst().getSuffixManager().getSuffixPlaceholdersToNames().getOrDefault(
                    PlaceholderAPI.setPlaceholders(player,"%luckperms_suffix%"),"");
            VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
            if (suffix != null) {
                return suffix.getSymbol();
            }
            else return "";
        });
        placeholders.put("confirmation_status",(plugin,player) -> {
            if (player.permissionValue("vineriumcore.status.confirmation") == TriState.TRUE) {
                return PlaceholderAPI.setPlaceholders(player,
                        PlaceholderAPI.setPlaceholders(player,VineriumCore.inst().getConfig().getString("ConfirmationStatus.Format","")));
            }
            else
                return "";
        });
    }
}
