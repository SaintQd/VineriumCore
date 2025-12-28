package org.saintqd.vineriumcore.suffix;

import me.clip.placeholderapi.PlaceholderAPI;
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
    private final String placeholder;

    public VinSuffix(String name, ConfigurationSection suffixConfig) {
        this.name = name;
        this.displayName = suffixConfig.getString("Display",name);
        this.itemModel = suffixConfig.getString("Model",null);
        this.desc = suffixConfig.getStringList("Desc");
        this.permission = suffixConfig.getString("Permission","vineriumcore.suffix."+name.toLowerCase());
        this.symbol = suffixConfig.getString("Symbol","+");
        this.placeholder = VineriumCore.inst().getSuffixManager().getPlaceholderTemplate().replace("{0}",name);
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

    public String getPlaceholder() {
        return placeholder;
    }

    public String getParsedPlaceholder() {
        return VineriumLib.inst().isPlaceholderAPIEnabled() ? PlaceholderAPI.setPlaceholders(null,placeholder) : placeholder;
    }
}
