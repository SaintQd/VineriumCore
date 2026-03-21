package org.saintqd.vineriumcore.managers;

import net.kyori.adventure.audience.Audience;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.CommunitySuffix;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class SuffixManager {

    private boolean hideWithoutPermission = true;
    private int menuPageSize = 36;
    private String menuTitle;
    private HashMap<String,String> menuModels;
    private final HashMap<String, VinSuffix> suffixes = new HashMap<>();
    private final HashMap<String, CommunitySuffix> communitySuffixes = new HashMap<>();
    private final HashMap<String,String> suffixSymbolsToNames = new HashMap<>();
    private final HashMap<String,String> suffixPlaceholdersToNames = new HashMap<>();
    private final HashMap<String,String> permissionsToSuffix = new HashMap<>();
    private String placeholderTemplate = "{0}";

    public void loadSuffixes(VineriumCore plugin) {
        suffixes.clear();
        communitySuffixes.clear();
        suffixSymbolsToNames.clear();
        suffixPlaceholdersToNames.clear();
        permissionsToSuffix.clear();
        File suffixFile = new File(plugin.getDataFolder().getPath() + File.separator + "Suffixes.yml");
        if (!suffixFile.exists()) {
            plugin.getLogger().log(Level.WARNING,"Suffixes: Suffix file does not exist!");
            return;
        }
        YamlConfiguration suffixFileYaml = YamlConfiguration.loadConfiguration(suffixFile);
        this.hideWithoutPermission = suffixFileYaml.getBoolean("HideWithoutPermission");
        this.menuPageSize = suffixFileYaml.getInt("MenuPageSize",36);
        this.menuTitle = suffixFileYaml.getString("MenuTitle","Suffixes");
        this.menuModels = new HashMap<>();
        this.placeholderTemplate = suffixFileYaml.getString("PlaceholderTemplate","{0}");
        if (suffixFileYaml.contains("MenuModels")) {
            for (String modelName : suffixFileYaml.getConfigurationSection("MenuModels").getKeys(false))
                menuModels.put(modelName,suffixFileYaml.getString("MenuModels."+modelName));
        }
        ConfigurationSection suffixFileConfig = suffixFileYaml.getConfigurationSection("Suffixes");
        for (String suffixName : suffixFileConfig.getKeys(false)) {
            VinSuffix suffix = new VinSuffix(suffixName, suffixFileConfig.getConfigurationSection(suffixName));
            suffixSymbolsToNames.put(suffix.getSymbol(),suffixName);
            suffixPlaceholdersToNames.put(suffix.getPlaceholder(),suffixName);
            permissionsToSuffix.put(suffix.getPermission(),suffixName);
            suffixes.put(suffixName,suffix);
            if (suffixFileConfig.contains(suffixName+".CommunityPermission")) {
                String communitySuffixPermission = suffixFileConfig.getString(suffixName+".CommunityPermission");
                File communitySuffixFile = new File(plugin.getDataFolder().getPath() + File.separator + "CommunitySuffixes" + File.separator + suffixName + ".yml");
                ConfigurationSection communitySuffixConfig = communitySuffixFile.exists() ? YamlConfiguration.loadConfiguration(communitySuffixFile).getConfigurationSection(suffixName) : null;
                CommunitySuffix communitySuffix = new CommunitySuffix(suffixName,communitySuffixPermission,communitySuffixConfig);
                communitySuffixes.put(suffixName,communitySuffix);
            }
        }
    }

    public void saveCommunitySuffixes() {
        for (String suffixName : communitySuffixes.keySet()) {
            CommunitySuffix communitySuffix = communitySuffixes.get(suffixName);
            File communitySuffixFile = new File(VineriumCore.inst().getDataFolder().getPath() + File.separator + "CommunitySuffixes" + File.separator + suffixName + ".yml");
            try {
                File dir = communitySuffixFile.getParentFile();
                if (!dir.exists() && !dir.mkdirs()) {
                    VinUtils.sendDebugMessage(0,"<red>Couldn't save community suffix file to "+ communitySuffixFile +"!");
                    continue;
                }
                if (!communitySuffixFile.exists() && !communitySuffixFile.createNewFile()) {
                    VinUtils.sendDebugMessage(0,"<red>Couldn't save community suffix file to "+ communitySuffixFile +"!");
                    continue;
                }
                YamlConfiguration communitySuffixConfig = YamlConfiguration.loadConfiguration(communitySuffixFile);
                communitySuffixConfig.set(suffixName+".Limit",communitySuffix.getSuffixLimit());
                communitySuffixConfig.set(suffixName+".Users",communitySuffix.getUsers());
                communitySuffixConfig.save(communitySuffixFile);
            } catch (IOException e) {
                VinUtils.sendDebugMessage(0,"<red>Couldn't save community suffix file to "+ communitySuffixFile +"!");
            }
        }
    }

    public void checkSuffixPermission(Player player) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;

        String suffixPlaceholder = vaultManager.getChatProvider().getPlayerSuffix(player).strip();
        String suffixName = VineriumCore.inst().getSuffixManager().getSuffixPlaceholdersToNames().get(suffixPlaceholder);
        if (suffixName != null) {
            VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
            if (!player.hasPermission(suffix.getPermission())) {
                vaultManager.getChatProvider().setPlayerSuffix(player, null);
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "suffix_no_permission_removed"));
            }
        }
    }

    public void changeSuffix(Audience audience, Player player, VinSuffix suffix) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null || vaultManager.getPermissionProvider() == null) {
            audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_are_not_supported"));
            return;
        }
        vaultManager.getChatProvider().setPlayerSuffix(null,player, suffix.getPlaceholder());
        vaultManager.getPermissionProvider().playerAdd(null,player,"meta.suffix-symbol."
                + suffix.getSymbol());
        audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_applied_message", suffix.getParsedPlaceholder()));
    }

    public void clearSuffix(Audience audience, Player player) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null || vaultManager.getPermissionProvider() == null) {
            audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_are_not_supported"));
            return;
        }
        vaultManager.getChatProvider().setPlayerSuffix(null,player, null);
        List<String> possibleSuffix = player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("meta.suffix-symbol.")).toList();
        if (!possibleSuffix.isEmpty()) {
            for(String suffix : possibleSuffix)
                vaultManager.getPermissionProvider().playerRemove(null,player,suffix);
        }
        audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_removed_message"));
    }

    public boolean isHideWithoutPermission() {
        return hideWithoutPermission;
    }

    public HashMap<String, CommunitySuffix> getCommunitySuffixes() {
        return communitySuffixes;
    }

    public HashMap<String, VinSuffix> getSuffixes() {
        return suffixes;
    }

    public HashMap<String, String> getSuffixSymbolsToNames() {
        return suffixSymbolsToNames;
    }

    public HashMap<String, String> getSuffixPlaceholdersToNames() {
        return suffixPlaceholdersToNames;
    }

    public HashMap<String, String> getPermissionsToSuffix() {
        return permissionsToSuffix;
    }

    public int getMenuPageSize() {
        return menuPageSize;
    }

    public HashMap<String, String> getMenuModels() {
        return menuModels;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public String getPlaceholderTemplate() {
        return placeholderTemplate;
    }
}
