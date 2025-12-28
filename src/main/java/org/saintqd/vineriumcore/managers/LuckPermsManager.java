package org.saintqd.vineriumcore.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LuckPermsManager {

    private final LuckPerms luckPerms;

    public LuckPermsManager() {
        this.luckPerms = Bukkit.getServicesManager().getRegistration(LuckPerms.class).getProvider();
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public String getVipUntil(Player player) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String vipGroupName = VineriumCore.inst().getConfig().getString("Compatibility.LuckPerms.VipGroupName","");
        if (vipGroupName.isEmpty())
            return "-";
        for (Node node : user.getDistinctNodes()) {
            if (node.getKey().equals("group."+vipGroupName)) {
                if (node.getExpiry() == null)
                    return "∞";
                else {
                    ZonedDateTime zonedDateTime = node.getExpiry().atZone(ZoneId.of("Europe/Moscow"));
                    return zonedDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }

            }
        }
        return "-";
    }

    public void copyPermissions(OfflinePlayer oldPlayer, OfflinePlayer newPlayer) {
        luckPerms.getUserManager().loadUser(oldPlayer.getUniqueId()).thenAcceptAsync(user -> {
            luckPerms.getUserManager().modifyUser(newPlayer.getUniqueId(), newUser -> {
               for (Node node : user.getNodes())
                   newUser.data().add(node);
            });
        });
    }

    public String getSuffix(Player player) {
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        String suffixValue = user.getCachedData().getMetaData().getMetaValue("suffix-symbol");
        return suffixValue != null ? suffixValue : "";
    }
}
