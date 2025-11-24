package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIVanish;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffectType;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
}
