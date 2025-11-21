package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIBanRecords;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.*;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.SchedulerUtil;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permissible;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.DiscordSRVManager;
import org.saintqd.vineriumlib.VineriumLib;

import java.time.Instant;
import java.util.Set;

public class CMIListener implements Listener {

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        if (!VineriumCore.inst().getConfig().getBoolean("SameIpCheck.Enabled")) return;
        if (event.getPlayer().hasPlayedBefore() && VineriumCore.inst().getConfig().getBoolean("SameIpCheck.OnlyOnFirstJoin")) return;
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
        Set<CMIUser> sameIpUsers = CMI.getInstance().getIpManager().getUsers(user.getLastIp());
        if (sameIpUsers.size() > 1) {
            Audience staffAudience = Audience.audience(Bukkit.getOnlinePlayers()).filterAudience(audience ->
                            ((Permissible) audience).hasPermission("vineriumcore.checkip"));
            staffAudience.sendMessage(VineriumLib.inst().
                    getLangManager().parseLangString(VineriumCore.inst(),"checkSameIpMessage",user.getName()));
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
                staffAudience.sendMessage(VineriumLib.inst().
                        getLangManager().parseLangString(VineriumCore.inst(),"checkSameIpMessageListFormat",
                                Integer.toString(index),color+sameIpUser.getName()));
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
}
