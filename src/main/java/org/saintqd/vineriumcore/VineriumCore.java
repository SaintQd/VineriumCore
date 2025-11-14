package org.saintqd.vineriumcore;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.saintqd.vineriumcore.commands.VinCommandsManager;
import org.saintqd.vineriumcore.listeners.PlayerListener;
import org.saintqd.vineriumcore.managers.ConfigManager;
import org.saintqd.vineriumcore.managers.SuffixManager;
import org.saintqd.vineriumcore.placeholders.VinCorePlaceholders;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class VineriumCore extends JavaPlugin {

    private static VineriumCore plugin;
    private ConfigManager configManager;
    private VinCorePlaceholders placeholders;
    private SuffixManager suffixManager;

    public static VineriumCore inst() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        setupDefaultConfig();
        this.configManager = new ConfigManager();
        this.suffixManager = new SuffixManager();

        this.configManager.checkConfigs();

        loadData();

        VinCommandsManager.setupCommands(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // Подключаем плейсхолдеры
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new VinCorePlaceholders(this);
            placeholders.register();
        } else {
            placeholders = null;
            VinUtils.sendDebugMessage(0,"<yellow>Could not find PlaceholderAPI! Placeholders won't be registered.");
        }
    }

    @Override
    public void onDisable() {
        VinUtils.updateJarFile(this,this.getFile());
    }

    public void loadData() {
        reloadConfig();

        String selectedLang = getConfig().getString("Language");
        if (selectedLang != null) {
            File langFile = new File(plugin.getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml");
            if (!langFile.exists() && langFile.mkdirs()) {
                InputStream langStream = VineriumCore.class.getResourceAsStream("/lang/"+selectedLang+".yml");
                if (langStream != null) {
                    try {
                        Files.copy(langStream, Path.of(getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml"), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            HashMap<String,String> langLines = VineriumLib.inst().getLangManager().loadLanguageFile(this,"lang" + File.separator + selectedLang + ".yml");
            VineriumLib.inst().getLangManager().registerLangLines(this,langLines);
        }

        long startTime = System.currentTimeMillis();
        long prevTime = startTime;

        suffixManager.loadSuffixes(this);
        long time = System.currentTimeMillis();
        getLogger().info("Loaded " + suffixManager.getSuffixes().size() + " suffixes. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();
    }

    public String getMainDirectory() {
        return getDataFolder().getPath() + File.separator;
    }

    private void setupDefaultConfig() {

        FileConfiguration config = this.getConfig();

        config.addDefault("Language","ru_ru");

        config.options().copyDefaults(true);
        this.saveConfig();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SuffixManager getSuffixManager() {
        return suffixManager;
    }

    public VinCorePlaceholders getPlaceholders() {
        return placeholders;
    }
}
