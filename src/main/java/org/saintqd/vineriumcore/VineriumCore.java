package org.saintqd.vineriumcore;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.saintqd.vineriumcore.commands.VinCommandsManager;
import org.saintqd.vineriumcore.listeners.CMIListener;
import org.saintqd.vineriumcore.listeners.DiscordSRVListener;
import org.saintqd.vineriumcore.listeners.PlayerListener;
import org.saintqd.vineriumcore.listeners.VillagerListener;
import org.saintqd.vineriumcore.managers.ConfigManager;
import org.saintqd.vineriumcore.managers.DiscordSRVManager;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumcore.managers.SuffixManager;
import org.saintqd.vineriumcore.placeholders.VinCorePlaceholders;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.ResourceUtils;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class VineriumCore extends JavaPlugin {

    private static VineriumCore plugin;
    private ConfigManager configManager;
    private VinCorePlaceholders placeholders;
    private SuffixManager suffixManager;
    private PlayerManager playerManager;

    // Совместимость с другими плагинами
    private boolean CMIEnabled = false;
    private DiscordSRVManager discordSRVManager = null;
    private DiscordSRVListener discordSRVListener = null;

    public static VineriumCore inst() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        try {
            ResourceUtils.fetchAllResources(this,getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.configManager = new ConfigManager();
        this.suffixManager = new SuffixManager();
        this.playerManager = new PlayerManager();

        this.configManager.checkConfigs();

        // Подключаем плейсхолдеры
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new VinCorePlaceholders(this);
            placeholders.register();
        } else {
            placeholders = null;
            VinUtils.sendDebugMessage(0,"<yellow>Could not find PlaceholderAPI! Placeholders won't be registered.");
        }

        Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
        if (cmi != null && cmi.isEnabled()) {
            CMIEnabled = true;
            getServer().getPluginManager().registerEvents(new CMIListener(), this);
            VinUtils.sendDebugMessage(0,"CMI found, compatibility features enabled.");
        }
        Plugin discordSRV = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSRV != null && discordSRV.isEnabled()) {
            discordSRVManager = new DiscordSRVManager();
            discordSRVListener = new DiscordSRVListener();
            DiscordSRV.api.subscribe(discordSRVListener);
            VinUtils.sendDebugMessage(0,"DiscordSRV found, compatibility features enabled.");
        }

        loadData();

        VinCommandsManager.setupCommands(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerListener(), this);
    }

    @Override
    public void onDisable() {
        if (discordSRVManager != null)
            DiscordSRV.api.unsubscribe(discordSRVListener);
        VinUtils.updateJarFile(this,this.getFile());
    }

    public void loadData() {
        reloadConfig();

        String selectedLang = getConfig().getString("Language");
        HashMap<String,String> langLines = VineriumLib.inst().getLangManager().loadLanguageFile(
                plugin.getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml");
        VineriumLib.inst().getLangManager().registerLangLines(this,langLines);

        playerManager.loadParams(this);

        if (discordSRVManager != null)
            discordSRVManager.loadDiscordData(this);

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

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SuffixManager getSuffixManager() {
        return suffixManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public VinCorePlaceholders getPlaceholders() {
        return placeholders;
    }

    public boolean isCMIEnabled() {
        return CMIEnabled;
    }

    public DiscordSRVManager getDiscordSRVManager() {
        return discordSRVManager;
    }
}
