package org.saintqd.vineriumcore;

import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
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

@Getter
public class VineriumCore extends JavaPlugin {

    @Getter(AccessLevel.NONE)
    private static VineriumCore plugin;
    private ConfigManager configManager;
    private VinCorePlaceholders placeholders;
    private SuffixManager suffixManager;
    private PlayerManager playerManager;
    private DynamicParamsManager dynamicParamsManager;
    private HintManager hintManager;
    private OreManager oreManager;
    private BukkitTask dynamicParamsTask = null;

    // Совместимость с другими плагинами
    private boolean CMIEnabled = false;
    private boolean liteBansEnabled = false;
    private LuckPermsManager luckPermsManager = null;
    private boolean nexoEnabled = false;
    private boolean mythicMobsEnabled = false;
    private boolean hmcCosmeticsEnabled = false;

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
        this.dynamicParamsManager = new DynamicParamsManager();
        this.hintManager = new HintManager();
        this.oreManager = new OreManager();

        this.configManager.checkConfigs();

        // Подключаем плейсхолдеры
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new VinCorePlaceholders(this);
            placeholders.registerPlaceholders();
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

        Plugin nexo = Bukkit.getPluginManager().getPlugin("Nexo");
        if (nexo != null && nexo.isEnabled()) {
            this.nexoEnabled = true;
            VinUtils.sendDebugMessage(0,"Nexo found, compatibility features enabled.");
        }

        Plugin hmcCosmetics = Bukkit.getPluginManager().getPlugin("HMCCosmetics");
        if (hmcCosmetics != null && hmcCosmetics.isEnabled()) {
            this.hmcCosmeticsEnabled = true;
            VinUtils.sendDebugMessage(0,"HMCCosmetics found, compatibility features enabled.");
        }

        Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythicMobs != null && mythicMobs.isEnabled()) {
            this.mythicMobsEnabled = true;
            VinUtils.sendDebugMessage(0,"MythicMobs found, compatibility features enabled.");
            MythicMobsListener listener = new MythicMobsListener();
            listener.registerConditions();
            listener.registerMechanics();
            getServer().getPluginManager().registerEvents(listener, this);
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
        dynamicParamsManager.loadParams(this);
        if (dynamicParamsTask != null)
            dynamicParamsTask.cancel();
        dynamicParamsTask = getServer().getScheduler().runTaskTimer(this,
                () -> dynamicParamsManager.updateWorldCaps(Bukkit.getOnlinePlayers().size()),
                1L,
                getConfig().getLong("DynamicParamsUpdateTime",12000L));

        configManager.loadParams(this);
        configManager.getInjectedVillagerTrades().updateParams();

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

        ItemSkinManager.INSTANCE.loadItemSkins(this);
        time = System.currentTimeMillis();
        getLogger().info("Loaded " + ItemSkinManager.INSTANCE.getItemSkins().size() + " item skins. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        DecorationManager.Companion.getInstance().loadParams(this);
        time = System.currentTimeMillis();
        getLogger().info("Loaded " + DecorationManager.Companion.getInstance().getDecorationElements().size() + " decoration elements. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        TradeManager.Companion.getInstance().loadParams(this);
        time = System.currentTimeMillis();
        getLogger().info("Loaded " + TradeManager.Companion.getInstance().getTradeSets().size() + " custom trades. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        MailbookManager.INSTANCE.loadParams(this);
        time = System.currentTimeMillis();
        if (!MailbookManager.INSTANCE.getUnreadMailbooks().isEmpty())
            getLogger().info("Loaded " + MailbookManager.INSTANCE.getUnreadMailbooks().size() + " players with mailbooks. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        CalendarEventsManager.Companion.getInstance().loadTimedEvents(this);
        time = System.currentTimeMillis();
        if (!CalendarEventsManager.Companion.getInstance().getEvents().isEmpty())
            getLogger().info("Loaded " + CalendarEventsManager.Companion.getInstance().getEvents().size() + " calendar events. ("+(time-prevTime)+" ms)");
        prevTime = System.currentTimeMillis();

        VineriumLib.inst().getCustomGUIManager().unregisterGuis(this);
        VineriumLib.inst().getCustomGUIManager().registerGuis(this);

        oreManager.updateData(this);
        oreManager.loadData(this);
    }

    public void saveData() {
        oreManager.updateData(this);
        VinUtils.sendDebugMessage(0,"Saving community suffixes data...");
        suffixManager.saveCommunitySuffixes();
        VinUtils.sendDebugMessage(0,"Saved "+suffixManager.getCommunitySuffixes().size()+" community suffixes.");

        if (!MailbookManager.INSTANCE.getUnreadMailbooks().isEmpty()) {
            VinUtils.sendDebugMessage(0, "Saving mailbooks data...");
            MailbookManager.INSTANCE.saveMailbooks(this);
            VinUtils.sendDebugMessage(0, "Saved " + MailbookManager.INSTANCE.getUnreadMailbooks().size() + " players with mailbooks.");
        }
    }
}
