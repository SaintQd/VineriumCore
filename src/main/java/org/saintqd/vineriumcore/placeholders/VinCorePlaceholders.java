package org.saintqd.vineriumcore.placeholders;

import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class VinCorePlaceholders extends PlaceholderExpansion {

    private final VineriumCore plugin;
    private final HashMap<String, BiFunction<VineriumCore,OfflinePlayer,String>> placeholders = new HashMap<>();
    private final HashSet<String> offlineIdentifiers = new HashSet<>();
    @Getter
    private static final HashMap<String,HashMap<String,String>> offlinePlayerPlaceholders = new HashMap<>();

    public VinCorePlaceholders(VineriumCore plugin){
        this.plugin = plugin;
    }

    private HashMap<String,String> getOfflinePlayerPlaceholders(OfflinePlayer offlinePlayer) {
        if (offlinePlayerPlaceholders.containsKey(offlinePlayer.getName())) {
            return offlinePlayerPlaceholders.get(offlinePlayer.getName());
        }
        else {
            if (!VineriumCore.inst().isCMIEnabled())
                return null;
            com.Zrips.CMI.Containers.CMIUser user = com.Zrips.CMI.Containers.CMIUser.getUser(offlinePlayer.getUniqueId());
            if (user != null) {
                Player player = user.getPlayer(true);
                HashMap<String, String> placeholders = offlinePlayerPlaceholders.getOrDefault(offlinePlayer.getName(), new HashMap<>());
                placeholders.put("health", Double.toString(Math.round(player.getPlayer().getHealth())));
                placeholders.put("max_health", Double.toString(Math.round(player.getAttribute(Attribute.MAX_HEALTH).getValue())));

                List<Advancement> advancementList = new ArrayList<>();
                Bukkit.getServer().advancementIterator().forEachRemaining(advancementList::add);
                int advancementAmount = 0;
                for (Advancement advancement : advancementList) {
                    if (advancement.getKey().getKey().startsWith("recipes"))
                        continue;
                    if (player.getAdvancementProgress(advancement).isDone()) {
                        advancementAmount++;
                    }
                }
                placeholders.put("advancements", Integer.toString(advancementAmount));
                placeholders.put("kills", Integer.toString(player.getStatistic(Statistic.PLAYER_KILLS)));
                placeholders.put("deaths", Integer.toString(player.getStatistic(Statistic.DEATHS)));
                placeholders.put("hours_played", Long.toString(TimeUnit.SECONDS.toHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE)) / 24));

                offlinePlayerPlaceholders.put(offlinePlayer.getName(), placeholders);
                if (VineriumCore.inst().getLuckPermsManager() != null) {
                    CompletableFuture<String> completableFuture = VineriumCore.inst().getLuckPermsManager().getOfflineVipUntil(player.getUniqueId());
                    completableFuture.thenAccept(string -> {
                        placeholders.put("vip_until", string);
                        offlinePlayerPlaceholders.put(offlinePlayer.getName(), placeholders);
                    });
                }
                return placeholders;
            }
            else
                return null;
        }
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
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String identifier) {

        if (offlineIdentifiers.contains(identifier)) {
            if (!player.isOnline()) {
                return placeholders.get(identifier.toLowerCase()).apply(plugin,player);
            }
        }
        return player != null && player.isOnline() ? this.onPlaceholderRequest(player.getPlayer(), identifier) : this.onPlaceholderRequest(null, identifier);
    }

    @Override
    public String onPlaceholderRequest(Player player, @NonNull String identifier) {

        if(player == null) {
            return "";
        }
        else
            return placeholders.get(identifier.toLowerCase()).apply(plugin,player);
    }

    public void registerPlaceholders() {

        offlineIdentifiers.clear();
        placeholders.clear();

        offlineIdentifiers.add("health");
        offlineIdentifiers.add("max_health");
        offlineIdentifiers.add("advancements");
        offlineIdentifiers.add("kills");
        offlineIdentifiers.add("deaths");
        offlineIdentifiers.add("hours_played");
        offlineIdentifiers.add("vip_until");

        placeholders.put("pvp_mode",(plugin,player) -> {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            if (playerManager.getPvpModePlayers().contains(player.getPlayer()))
                return playerManager.getPvpPlaceholder();
            else return "";
        });
        placeholders.put("vip_until",(plugin,player) -> {
            if (player.isOnline()) {
                if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null
                        && VineriumCore.inst().getLuckPermsManager() != null) {
                    return VineriumCore.inst().getLuckPermsManager().getVipUntil(player.getPlayer());
                }
                else
                    return "";
            }
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("vip_until","-");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("suffix",(plugin,player) -> {
            if (VineriumCore.inst().getLuckPermsManager() != null)
                return VineriumCore.inst().getLuckPermsManager().getSuffix(player.getPlayer());
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
            String permission = VineriumCore.inst().getConfig().getString("ConfirmationStatus.Permission","group.rp");
            if (player.getPlayer().permissionValue(permission) == TriState.TRUE) {
                return PlaceholderAPI.setPlaceholders(player,
                        PlaceholderAPI.setPlaceholders(player,VineriumCore.inst().getConfig().getString("ConfirmationStatus.Format","")));
            }
            else
                return "";
        });
        placeholders.put("health",(plugin,player) -> {
            if (player.isOnline())
                return Double.toString(Math.round(player.getPlayer().getHealth()));
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("health","20.0");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("max_health",(plugin,player) -> {
            if (player.isOnline())
                return Double.toString(Math.round(player.getPlayer().getAttribute(Attribute.MAX_HEALTH).getValue()));
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("max_health","20.0");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("advancements",(plugin,player) -> {
            if (player.isOnline())
                return "0";
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("advancements","0");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("kills",(plugin,player) -> {
            if (player.isOnline())
                return Integer.toString(player.getStatistic(Statistic.PLAYER_KILLS));
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("kills","0");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("deaths",(plugin,player) -> {
            if (player.isOnline())
                return Integer.toString(player.getStatistic(Statistic.DEATHS));
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("deaths","0");
                }
                else {
                    return "";
                }
            }
        });
        placeholders.put("hours_played",(plugin,player) -> {
            if (player.isOnline())
                return Long.toString(TimeUnit.SECONDS.toHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE)) / 24);
            else {
                HashMap<String,String> placeholders = getOfflinePlayerPlaceholders(player);
                if (placeholders != null) {
                    return placeholders.getOrDefault("hours_played","0");
                }
                else {
                    return "";
                }
            }
        });
    }
}
