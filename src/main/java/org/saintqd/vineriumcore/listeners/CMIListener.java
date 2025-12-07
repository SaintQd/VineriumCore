package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import com.Zrips.CMI.events.*;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.List;
import java.util.regex.Pattern;

public class CMIListener implements Listener {

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
