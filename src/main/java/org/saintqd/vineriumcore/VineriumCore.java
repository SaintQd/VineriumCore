package org.saintqd.vineriumcore;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.saintqd.vineriumcore.commands.VinCommandsManager;
import org.saintqd.vineriumcore.listeners.*;
import org.saintqd.vineriumcore.managers.*;
import org.saintqd.vineriumcore.placeholders.VinCorePlaceholders;
import org.saintqd.vineriumcore.worldguard.Flags;
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
    private DynamicMobCapManager dynamicMobCapManager;
    private HintManager hintManager;

    // Совместимость с другими плагинами
    private boolean CMIEnabled = false;
    private boolean liteBansEnabled = false;
    private LuckPermsManager luckPermsManager = null;

    public static VineriumCore inst() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        Flags.registerFlags();
    }

    @Override
    public void onEnable() {
        Flags.registerHandlers();
        try {
            ResourceUtils.fetchAllResources(this,getFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.configManager = new ConfigManager();
        this.suffixManager = new SuffixManager();
        this.playerManager = new PlayerManager();
        this.dynamicMobCapManager = new DynamicMobCapManager();
        this.hintManager = new HintManager();

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

        Plugin liteBans = Bukkit.getPluginManager().getPlugin("LiteBans");
        if (liteBans != null && liteBans.isEnabled()) {
            liteBansEnabled = true;
            LiteBansListener.registerEvents();
            VinUtils.sendDebugMessage(0,"LiteBans found, compatibility features enabled.");
        }

        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPerms != null && luckPerms.isEnabled()) {
            this.luckPermsManager = new LuckPermsManager();
            VinUtils.sendDebugMessage(0,"LuckPerms found, compatibility features enabled.");
        }

        loadData();

        VinCommandsManager.setupCommands(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);

        //Создаем задачу регулярного сохранения данных раз в полчаса
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::saveData, 36000L, 36000L);
    }

    @Override
    public void onDisable() {
        VinUtils.updateJarFile(this,this.getFile());
        saveData();
    }

    public void loadData() {
        reloadConfig();

        String selectedLang = getConfig().getString("Language");
        HashMap<Key,String> langLines = VineriumLib.inst().getLangManager().loadLanguageFile(this,
                plugin.getDataFolder().getPath() + File.separator + "lang" + File.separator + selectedLang + ".yml");
        VineriumLib.inst().getLangManager().registerLangLines(langLines);

        playerManager.loadParams(this);
        dynamicMobCapManager.loadParams(this);

        long prevTime = System.currentTimeMillis();

        suffixManager.loadSuffixes(this);
        long time = System.currentTimeMillis();
        getLogger().info("Loaded " + suffixManager.getSuffixes().size() + " suffixes. ("+(time-prevTime)+" ms)");
        getLogger().info("Loaded " + suffixManager.getCommunitySuffixes().size() + " community suffixes. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        hintManager.loadHints(this);
        time = System.currentTimeMillis();
        getLogger().info("Loaded " + hintManager.getHints().size() + " hints. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        hintManager.setupStarterHintTask(this);

        VineriumLib.inst().getCustomGUIManager().unregisterGuis(this);
        VineriumLib.inst().getCustomGUIManager().registerGuis(this);
    }

    public void saveData() {
        VinUtils.sendDebugMessage(0,"Saving community suffixes data...");
        suffixManager.saveCommunitySuffixes();
        VinUtils.sendDebugMessage(0,"Saved "+suffixManager.getCommunitySuffixes().size()+" community suffixes.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HintManager getHintManager() {
        return hintManager;
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

    public boolean isLiteBansEnabled() {
        return liteBansEnabled;
    }

    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    public DynamicMobCapManager getDynamicMobCapManager() {
        return dynamicMobCapManager;
    }
}
