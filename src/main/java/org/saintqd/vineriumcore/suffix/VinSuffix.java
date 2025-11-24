package org.saintqd.vineriumcore.suffix;

import net.kyori.adventure.audience.Audience;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.List;

public class VinSuffix {

    private final String name;
    private final String displayName;
    private final String itemModel;
    private final String permission;
    private final List<String> desc;
    private final String symbol;

    public VinSuffix(String name, ConfigurationSection suffixConfig) {
        this.name = name;
        this.displayName = suffixConfig.getString("Display",name);
        this.itemModel = suffixConfig.getString("Model",null);
        this.desc = suffixConfig.getStringList("Desc");
        this.permission = suffixConfig.getString("Permission","vineriumcore.suffix."+name.toLowerCase());
        this.symbol = suffixConfig.getString("Symbol","+");
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getItemModel() {
        return itemModel;
    }

    public List<String> getDesc() {
        return desc;
    }

    public String getPermission() {
        return permission;
    }

    public String getSymbol() {
        return symbol;
    }

    public void changeSuffix(Audience audience, Player player) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) {
            audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixAreNotSupported"));
            return;
        }
        vaultManager.getChatProvider().setPlayerSuffix(null,player, symbol);
        audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixAppliedMessage",symbol));
    }

    public static void clearSuffix(Audience audience, Player player) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) {
            audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixAreNotSupported"));
            return;
        }
        vaultManager.getChatProvider().setPlayerSuffix(null,player, null);
        audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixRemovedMessage"));
    }
}
