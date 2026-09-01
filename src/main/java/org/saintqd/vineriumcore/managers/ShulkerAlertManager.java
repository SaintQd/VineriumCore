package org.saintqd.vineriumcore.managers;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;

import java.util.*;

public class ShulkerAlertManager {

    public static final ShulkerAlertManager INSTANCE = new ShulkerAlertManager();
    public static final NamespacedKey SHULKER_UUID_KEY = new NamespacedKey(VineriumCore.inst(), "shulker_uuid");
    private BukkitTask shulkerAlertTask = null;
    @Getter
    private final HashMap<String,Long> worldShulkerUuids = new HashMap<>();
    @Getter
    private Sound alertSound;

    public void loadData(Plugin plugin) {
        alertSound = null;
        String[] soundData = plugin.getConfig().getString("ShulkerDupeDetector.Sound","entity.player.levelup,2").split(",");
        @Subst("block.chain.hit") String soundName = soundData[0];
        float pitch = soundData.length > 1 ? Float.parseFloat(soundData[1]) : 1.0f;
        Key soundKey = Key.key(soundName);
        alertSound = Sound.sound(soundKey, Sound.Source.PLAYER,1.0f,pitch);

        int taskTimer = plugin.getConfig().getInt("ShulkerDupeDetector.CheckPeriod",6000);
        if (shulkerAlertTask != null) {
            shulkerAlertTask.cancel();
        }
        shulkerAlertTask = Bukkit.getScheduler().runTaskTimer(plugin,() -> {
            Bukkit.getOnlinePlayers().stream().filter(player -> !player.hasPermission(
                    "vineriumcore.shulkerdupealerts.bypass")).forEach(player -> {
                Set<String> foundUuids = new HashSet<>();
                boolean dupeCheck = false;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        if (item.getType().name().contains(Material.SHULKER_BOX.name())) {
                            if (item.getPersistentDataContainer().has(ShulkerAlertManager.SHULKER_UUID_KEY)) {
                                String shulkerUuid = item.getPersistentDataContainer().get(ShulkerAlertManager.SHULKER_UUID_KEY, PersistentDataType.STRING);
                                if (foundUuids.contains(shulkerUuid)) {
                                    dupeCheck = true;
                                } else
                                    foundUuids.add(shulkerUuid);
                            }
                            else {
                                UUID uuid = UUID.randomUUID();
                                item.editPersistentDataContainer(pdc ->
                                        pdc.set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, uuid.toString())
                                );
                            }
                        }
                    }
                }
                if (dupeCheck) {
                    showAlert(player);
                }
            });
        },taskTimer,taskTimer);
    }

    public void showAlert(Player player) {
        List<Player> alertedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                .filter(possiblePlayer -> possiblePlayer.hasPermission("vineriumcore.shulkerdupealerts.show")).toList());

        String hoverCommand = VineriumCore.inst().getConfig().getString("ShulkerDupeDetector.HoverCommand", "tp {1}")
                .replace("{1}", player.getName());
        String hoverText = "<click:run_command:\"/" + hoverCommand + "\"><hover:show_text:'" + VineriumLib.inst().getLangManager().getLangLines()
                .get(Key.key("vineriumcore:ore_alert_hover_tooltip")) + "'>" + VineriumLib.inst().getLangManager().getLangLines().get(Key.key("vineriumcore:ore_alert_hover")) + "</hover></click>";
        Component smallAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "shulker_dupe_alert",
                player.getName(), hoverText);
        for (Player alertedPlayer : alertedPlayers) {
            if (alertedPlayer.permissionValue("vineriumcore.shulkerdupealerts.disable") != TriState.TRUE) {
                alertedPlayer.sendMessage(smallAlertComponent);
                if (alertedPlayer.permissionValue("vineriumcore.shulkerdupealerts.disablesound") != TriState.TRUE) {
                    alertedPlayer.playSound(alertSound, alertedPlayer);
                }
            }
        }
        Location playerLoc = player.getLocation();
        for (String command : VineriumCore.inst().getConfig().getStringList("ShulkerDupeDetector.CommandsOnDetect")) {
            String parsedCommand = command.replace("%player_name%", player.getName());
            parsedCommand = parsedCommand.replace("%location%",playerLoc.getWorld().getName()+","+playerLoc.x()+","+playerLoc.y()+","+playerLoc.z());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }

    public void showAlert(Location location) {
        List<Player> alertedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                .filter(possiblePlayer -> possiblePlayer.hasPermission("vineriumcore.shulkerdupealerts.show")).toList());

        String hoverCommand = VineriumCore.inst().getConfig().getString("ShulkerDupeDetector.HoverCommand", "tp {1}")
                .replace("{1}", location.toString());
        String hoverText = "<click:run_command:\"/" + hoverCommand + "\"><hover:show_text:'" + VineriumLib.inst().getLangManager().getLangLines()
                .get(Key.key("vineriumcore:ore_alert_hover_tooltip")) + "'>" + VineriumLib.inst().getLangManager().getLangLines().get(Key.key("vineriumcore:ore_alert_hover")) + "</hover></click>";
        Component smallAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "shulker_dupe_alert",
                location.toString(), hoverText);
        for (Player alertedPlayer : alertedPlayers) {
            if (alertedPlayer.permissionValue("vineriumcore.shulkerdupealerts.disable") != TriState.TRUE) {
                alertedPlayer.sendMessage(smallAlertComponent);
                if (alertedPlayer.permissionValue("vineriumcore.shulkerdupealerts.disablesound") != TriState.TRUE) {
                    alertedPlayer.playSound(alertSound, alertedPlayer);
                }
            }
        }
        Location playerLoc = location;
        for (String command : VineriumCore.inst().getConfig().getStringList("ShulkerDupeDetector.CommandsOnDetect")) {
            String parsedCommand = command.replace("%player_name%", location.getWorld().getName()+","+location.getX()+","+location.getY()+","+location.getZ());
            parsedCommand = parsedCommand.replace("%location%",playerLoc.getWorld().getName()+","+playerLoc.x()+","+playerLoc.y()+","+playerLoc.z());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }
}
