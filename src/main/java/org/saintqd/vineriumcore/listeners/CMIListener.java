package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIBanRecords;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import com.Zrips.CMI.events.*;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.SchedulerUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.DiscordSRVManager;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CMIListener implements Listener {

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerJoinCheckSameIp(PlayerJoinEvent event) {
        if (!VineriumCore.inst().getConfig().getBoolean("SameIpCheck.Enabled")) return;
        if (event.getPlayer().hasPlayedBefore() && VineriumCore.inst().getConfig().getBoolean("SameIpCheck.OnlyOnFirstJoin")) return;
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
        Set<CMIUser> sameIpUsers = CMI.getInstance().getIpManager().getUsers(user.getLastIp());
        if (sameIpUsers != null && sameIpUsers.size() > 1) {
            Component parsedMessage = VineriumLib.inst().
                    getLangManager().parseLangString(VineriumCore.inst(),"checkSameIpMessage",user.getName());
            Audience staffAudience = Audience.audience(Bukkit.getOnlinePlayers()).filterAudience(audience ->
                            ((Permissible) audience).hasPermission("vineriumcore.checkip"));
            VineriumCore.inst().getServer().getConsoleSender().sendMessage(parsedMessage);
            staffAudience.sendMessage(parsedMessage);
            int index = 1;
            for (CMIUser sameIpUser : sameIpUsers) {
                String color = "<gray>";
                if (sameIpUser.isOnline())
                    color = "<green>";
                else if (sameIpUser.isBanned())
                    color = "<red>";
                // Фейковое предупреждение о null - в CMI-API все методы выдают null,
                //   в реальном CMI обработка работает нормально
                else if (CMIBanRecords.getBanEntryByIP(sameIpUser.getLastIp()) != null)
                    color = "<yellow>";
                parsedMessage = VineriumLib.inst().
                        getLangManager().parseLangString(VineriumCore.inst(),"checkSameIpMessageListFormat",
                                Integer.toString(index),color+sameIpUser.getName());
                VineriumCore.inst().getServer().getConsoleSender().sendMessage(parsedMessage);
                staffAudience.sendMessage(parsedMessage);
                index++;
            }
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onCMIKick(CMIPlayerKickEvent event) {
        if (!event.isCancelled() && VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", Bukkit.getOfflinePlayer(event.getBanned()),
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIKickMessage"),event.getBannedBy().getName(),event.getReason()));
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onCMIBan(CMIPlayerBanEvent event) {
        CMIUser bannedUser = CMI.getInstance().getPlayerManager().getUser(event.getBanned());
        long time = bannedUser.getBanEntry().getLeftTime() / 1000 + 1;
        String timeName = " сек";
        if (time > 60) {
            time = time / 60;
            timeName = " мин";
            if (time > 60) {
                time = time / 60;
                timeName = " ч";
                if (time > 24) {
                    time = time / 24;
                    timeName = " д";
                }
            }
        }
        String finalTime = bannedUser.getBanEntry().isPermanent() ? "∞" : time + timeName;
        if (VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", Bukkit.getOfflinePlayer(event.getBanned()),
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIBanMessage"),event.getBannedBy().getName(),event.getReason(),finalTime));
        }
    }

    @EventHandler
    public void onCMIUnban(CMIPlayerUnBanEvent event) {
        if (VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()),
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIUnbanMessage"),event.getBannedBy().getName()));
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onCMIIPBan(CMIIpBanEvent event) {
        long time = (Instant.now().getEpochSecond() - event.getUntil()) / 1000;
        String timeName = " сек";
        if (time > 60) {
            time = time / 60;
            timeName = " мин";
            if (time > 60) {
                time = time / 60;
                timeName = " ч";
                if (time > 24) {
                    time = time / 24;
                    timeName = " д";
                }
            }
        }
        String finalTime = event.getUntil() == -1 ? "∞" : time + timeName;
        if (VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", null,
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIBanMessage"),event.getBannedBy().getName(),event.getReason(),finalTime));
        }
    }

    @EventHandler
    public void onCMIUnban(CMIIpUnBanEvent event) {
        if (VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", null,
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIUnbanMessage"),event.getBannedBy().getName()));
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onCMIWarn(CMIPlayerWarnEvent event) {
        if (!event.isCancelled() && VineriumCore.inst().getDiscordSRVManager() != null) {
            SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRVManager.runMessageAsync("punishments", Bukkit.getOfflinePlayer(event.getUser().getUniqueId()),
                            VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get("CMIWarnMessage"),event.getWarning().getGivenBy(),event.getWarning().getReason()));
        }
    }

    // Фикс параметра FakeJoinLeave для системы ваниша в CMI
    @EventHandler
    public void onCMIVanish(CMIPlayerVanishEvent event) {
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
        if (user == null) return;
        if (user.getVanish().getState(VanishAction.fakeJoinLeave).is()) {

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

            user.getVanish().set(VanishAction.fakeJoinLeave,false);
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    user.getVanish().set(VanishAction.fakeJoinLeave,true);
                }
            }.runTaskLater(VineriumCore.inst(),1L);

            leaveMessageFormat = leaveMessageFormat.replace("[message]", leaveMessage).replace("[dot]",".");
            leaveMessageFormat = leaveMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
            leaveMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                    ? PlaceholderAPI.setPlaceholders(event.getPlayer(), leaveMessageFormat)
                    : leaveMessageFormat;

            Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(VinUtils.parseString(leaveMessageFormat));
        }
    }

    // Фикс параметра FakeJoinLeave для системы ваниша в CMI
    @EventHandler
    public void onCMIUnVanish(CMIPlayerUnVanishEvent event) {
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
        if (user == null) return;
        if (user.getVanish().getState(VanishAction.fakeJoinLeave).is()) {

            String joinMessage = null;
            String joinMessageFormat = null;
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
            if (joinMessage == null || joinMessage.isEmpty())
                return;

            user.getVanish().set(VanishAction.fakeJoinLeave,false);
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    user.getVanish().set(VanishAction.fakeJoinLeave,true);
                }
            }.runTaskLater(VineriumCore.inst(),1L);

            joinMessageFormat = joinMessageFormat.replace("[message]", joinMessage).replace("[dot]",".");
            joinMessageFormat = joinMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
            joinMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                    ? PlaceholderAPI.setPlaceholders(event.getPlayer(), joinMessageFormat)
                    : joinMessageFormat;
            Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(VinUtils.parseString(joinMessageFormat));
        }
    }
}
