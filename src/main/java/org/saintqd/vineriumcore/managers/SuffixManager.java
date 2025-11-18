package org.saintqd.vineriumcore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

public class SuffixManager {

    private boolean hideWithoutPermission = true;
    private int menuPageSize = 36;
    private String menuTitle;
    private HashMap<String,String> menuModels;
    private HashMap<String, VinSuffix> suffixes = new HashMap<>();
    private HashMap<String,String> suffixSymbolsToNames = new HashMap<>();
    private HashMap<String,String> permissionsToSuffix = new HashMap<>();

    public void loadSuffixes(VineriumCore plugin) {
        suffixes = new HashMap<>();
        suffixSymbolsToNames = new HashMap<>();
        permissionsToSuffix = new HashMap<>();
        File suffixFile = new File(plugin.getMainDirectory() + "Suffixes.yml");
        if (!suffixFile.exists()) {
            plugin.getLogger().log(Level.WARNING,"Suffixes: Suffix file does not exist!");
            return;
        }
        YamlConfiguration suffixFileYaml = YamlConfiguration.loadConfiguration(suffixFile);
        this.hideWithoutPermission = suffixFileYaml.getBoolean("HideWithoutPermission");
        this.menuPageSize = suffixFileYaml.getInt("MenuPageSize",36);
        this.menuTitle = suffixFileYaml.getString("MenuTitle","Suffixes");
        this.menuModels = new HashMap<>();
        if (suffixFileYaml.contains("MenuModels")) {
            for (String modelName : suffixFileYaml.getConfigurationSection("MenuModels").getKeys(false))
                menuModels.put(modelName,suffixFileYaml.getString("MenuModels."+modelName));
        }
        ConfigurationSection suffixFileConfig = suffixFileYaml.getConfigurationSection("Suffixes");
        for (String suffixName : suffixFileConfig.getKeys(false)) {
            VinSuffix suffix = new VinSuffix(suffixName, suffixFileConfig.getConfigurationSection(suffixName));
            suffixSymbolsToNames.put(suffix.getSymbol(),suffixName);
            permissionsToSuffix.put(suffix.getPermission(),suffixName);
            suffixes.put(suffixName,suffix);
        }
    }

    public boolean isHideWithoutPermission() {
        return hideWithoutPermission;
    }

    public HashMap<String, VinSuffix> getSuffixes() {
        return suffixes;
    }

    public HashMap<String, String> getSuffixSymbolsToNames() {
        return suffixSymbolsToNames;
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
}
