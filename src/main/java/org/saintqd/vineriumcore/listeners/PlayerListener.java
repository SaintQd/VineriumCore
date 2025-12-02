package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIVanish;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import kotlin.Pair;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.potion.PotionEffectType;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;

        String suffixSymbol = vaultManager.getChatProvider().getPlayerSuffix(event.getPlayer()).replace(" ","");
        String suffixName = VineriumCore.inst().getSuffixManager().getSuffixSymbolsToNames().get(suffixSymbol);
        if (suffixName != null) {
            VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
            if (!event.getPlayer().hasPermission(suffix.getPermission())) {
                vaultManager.getChatProvider().setPlayerSuffix(event.getPlayer(), null);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "suffixNoPermissionRemoved"));
            }
        }

        if (!VineriumCore.inst().getConfig().getBoolean("Messages.Enabled"))
            return;
        event.joinMessage(null);
        String joinMessage = null;
        String joinMessageFormat = null;
        if (event.getPlayer().hasPlayedBefore()) {
            if (event.getPlayer().permissionValue("vineriumcore.hidejoinmessage") == TriState.TRUE)
                return;
            // Фикс скрытия сообщений входа/выхода для системы ваниша в CMI
            if (VineriumCore.inst().isCMIEnabled()) {
                CMIVanish vanish = CMI.getInstance().getVanishManager().getVanish(event.getPlayer().getUniqueId());
                if (vanish != null && vanish.getState(VanishAction.isVanished).is() && !vanish.getState(VanishAction.informOnJoin).is())
                    return;
            }
            List<String> possibleJoinMessage = event.getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                    .filter(permission -> permission.startsWith("meta.join-message.")).toList();
            if (!possibleJoinMessage.isEmpty()
                    && event.getPlayer().hasPermission("vineriumcore.joinmessage")
                    && VineriumCore.inst().getConfig().getBoolean("Messages.Join.Enabled")) {
                String joinMessagePermission = possibleJoinMessage.getFirst();
                joinMessage = joinMessagePermission.replace("meta.join-message.", "").replace("\"", "");
                joinMessage = Pattern.compile("╝+(.)?").matcher(joinMessage).replaceAll(mr -> mr.group(1).toUpperCase());
                joinMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Join.Format", "<white>>> <gray>[message]");
            }
            else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultJoin.Enabled")) {
                joinMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultJoin.Format", null);
                joinMessageFormat = joinMessage;
            }
        }
        else if (VineriumCore.inst().getConfig().getBoolean("Messages.FirstJoin.Enabled")) {
            joinMessage = VineriumCore.inst().getConfig().getString("Messages.FirstJoin.Format", null);
            joinMessageFormat = joinMessage;
        }
        if (joinMessage == null || joinMessage.isEmpty())
            return;
        joinMessageFormat = joinMessageFormat.replace("[message]", joinMessage).replace("[dot]",".");
        joinMessageFormat = joinMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
        joinMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(event.getPlayer(), joinMessageFormat)
                : joinMessageFormat;
        event.joinMessage(VinUtils.parseString(joinMessageFormat));

        if (event.getPlayer().permissionValue("vineriumcore.pvpenabled") == TriState.TRUE) {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            playerManager.getPvpModePlayers().add(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Messages.Enabled"))
            return;
        event.quitMessage(null);
        if (event.getPlayer().permissionValue("vineriumcore.hideleavemessage") == TriState.TRUE)
            return;
        // Фикс скрытия сообщений входа/выхода для системы ваниша в CMI
        if (VineriumCore.inst().isCMIEnabled()) {
            CMIVanish vanish = CMI.getInstance().getVanishManager().getVanish(event.getPlayer().getUniqueId());
            if (vanish != null && vanish.getState(VanishAction.isVanished).is() && !vanish.getState(VanishAction.informOnLeave).is())
                return;
        }
        List<String> possibleLeaveMessage = event.getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("meta.leave-message.")).toList();
        String leaveMessage = null;
        String leaveMessageFormat = null;
        if (!possibleLeaveMessage.isEmpty()
                && event.getPlayer().hasPermission("vineriumcore.leavemessage")
                && VineriumCore.inst().getConfig().getBoolean("Messages.Leave.Enabled")) {
            String leaveMessagePermission = possibleLeaveMessage.getFirst();
            leaveMessage = leaveMessagePermission.replace("meta.leave-message.", "").replace("\"", "");
            leaveMessage = Pattern.compile("╝+(.)?").matcher(leaveMessage).replaceAll(mr -> mr.group(1).toUpperCase());
            leaveMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Leave.Format", "<white<<< <gray>[message]");
        }
        else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultLeave.Enabled")) {
            leaveMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultLeave.Format", null);
            leaveMessageFormat = leaveMessage;
        }
        if (leaveMessage == null || leaveMessage.isEmpty())
            return;
        leaveMessageFormat = leaveMessageFormat.replace("[message]", leaveMessage).replace("[dot]",".");
        leaveMessageFormat = leaveMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
        leaveMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(event.getPlayer(), leaveMessageFormat)
                : leaveMessageFormat;
        event.quitMessage(VinUtils.parseString(leaveMessageFormat));

        PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
        if (playerManager.getPvpModePlayers().contains(event.getPlayer())) {
            vaultManager.getPermissionProvider().playerAdd(null,event.getPlayer(),"vineriumcore.pvpenabled");
            playerManager.getPvpModePlayers().remove(event.getPlayer());
        }
        else {
            vaultManager.getPermissionProvider().playerRemove(null,event.getPlayer(),"vineriumcore.pvpenabled");
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof VinGUIHolder)) return;
        for (int slot : event.getRawSlots())
            if (slot <= event.getInventory().getSize() - 1)
                event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player interactedPlayer) {
            if (!VineriumCore.inst().getConfig().getBoolean("Messages.RightClickNickname.Enabled")) return;
            if (!(interactedPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
            && VineriumCore.inst().getConfig().getBoolean("Messages.RightClickNickname.HideInvisible"))
            || VineriumCore.inst().getConfig().getStringList("Messages.RightClickNickname.AlwaysShowGamemodes")
                    .contains(event.getPlayer().getGameMode().name())
            || event.getPlayer().hasPermission("vineriumcore.rightclicknickname.alwaysshow")) {
                String messageFormat = VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName());
                messageFormat = (VineriumCore.inst().getPlaceholders() != null)
                        ? PlaceholderAPI.setPlaceholders(interactedPlayer, messageFormat)
                        : messageFormat;
                event.getPlayer().sendActionBar(VinUtils.parseString(messageFormat));
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return; // Если урон уже был отменён (К примеру флагами WorldGuard), не обрабатываем

        Entity damagerEntity = event.getDamager();

        // Молния это CraftItem, следовательно Entity, но не LivingEntity, так что её не обрабатываем
        if (event.getCause().equals(EntityDamageEvent.DamageCause.LIGHTNING)) {
            return;
        }

        if (event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            Projectile projectile = (Projectile) event.getDamager();
            // Если источник выстрела - не сущность, не совершаем проверки
            if (!(projectile.getShooter() instanceof Entity)) {
                return;
            }
            damagerEntity = (Entity) projectile.getShooter();
        }

        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(damagerEntity instanceof LivingEntity)) return;


        if (event.getEntity() instanceof Player entityPlayer && damagerEntity instanceof Player damagerPlayer) {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            if (!playerManager.isPvpModeEnabled())
                return;
            if (damagerPlayer.hasPermission("vineriumcore.admin"))
                return;
            if (!playerManager.getPvpModePlayers().contains(entityPlayer)) {
                event.setCancelled(true);
                HashMap<Player, Pair<String,Long>> timers = playerManager.getTimers().getOrDefault("entityNotEnabledPvp",new HashMap<>());
                Pair<String,Long> damagerVariable = timers.getOrDefault(damagerPlayer,new Pair<>(null,0L));
                if (damagerVariable.getSecond() < VinUtils.getCurrentTick()) {
                    damagerVariable = new Pair<>(null,VinUtils.getCurrentTick() +
                            VineriumCore.inst().getConfig().getLong("TimersCooldown.entityNotEnabledPvp",200L));
                    timers.put(damagerPlayer,damagerVariable);
                    playerManager.getTimers().put("entityNotEnabledPvp",timers);
                    damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "entityNotEnabledPvp", entityPlayer.getName()));
                }
                return;
            }
            if (!playerManager.getPvpModePlayers().contains(damagerPlayer)) {
                event.setCancelled(true);
                HashMap<Player, Pair<String,Long>> timers = playerManager.getTimers().getOrDefault("damagerNotEnabledPvp",new HashMap<>());
                Pair<String,Long> damagerVariable = timers.getOrDefault(damagerPlayer,new Pair<>(null,0L));
                if (damagerVariable.getSecond() < VinUtils.getCurrentTick()) {
                    damagerVariable = new Pair<>(null,VinUtils.getCurrentTick() +
                            VineriumCore.inst().getConfig().getLong("TimersCooldown.damagerNotEnabledPvp",200L));
                    timers.put(damagerPlayer,damagerVariable);
                    playerManager.getTimers().put("damagerNotEnabledPvp",timers);
                    damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "damagerNotEnabledPvp", entityPlayer.getName()));
                }
                return;
            }
        }
    }

    // По какой-то причине у игрока во всех режимах кроме креатива скорость полёта устанавливается на 0
    //  фиксим это с помощью штуки ниже
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        if ((event.getPlayer().getGameMode() == GameMode.ADVENTURE || event.getPlayer().getGameMode() == GameMode.SURVIVAL)
        && event.getNewGameMode() == GameMode.SPECTATOR)
            event.getPlayer().setFlySpeed(0.1f);
    }
}
