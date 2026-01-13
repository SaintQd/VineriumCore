package org.saintqd.vineriumcore.managers;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private static final NamespacedKey LOCK_KEY = new NamespacedKey(VineriumCore.inst(),"item_lock");

    public static NamespacedKey getLockKey() {
        return LOCK_KEY;
    }

    private final Set<String> maceDenierCustomNames = new HashSet<>();
    private final Set<String> itemLockMaterials = new HashSet<>();
    private final InjectedVillagerTrades injectedVillagerTrades = new InjectedVillagerTrades();

    public void checkConfigs() {
        File suffixFile = new File(VineriumCore.inst().getDataFolder().getPath() + File.separator + "Suffixes.yml");
        File parent = suffixFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            VinUtils.sendDebugMessage(0,"<red>Couldn't create suffix config!");
            return;
        }
        YamlConfiguration suffixYaml = YamlConfiguration.loadConfiguration(suffixFile);
        suffixYaml.addDefault("PlaceholderTemplate","%oraxen_{0}%");
        suffixYaml.addDefault("HideWithoutPermission",true);
        suffixYaml.addDefault("MenuTitle","Suffixes");
        suffixYaml.addDefault("MenuPageSize",36);
        suffixYaml.addDefault("MenuModels.CloseButton","minecraft:paper");
        suffixYaml.addDefault("MenuModels.PrevPageButton","minecraft:arrow");
        suffixYaml.addDefault("MenuModels.NextPageButton","minecraft:arrow");
        if (!suffixYaml.contains("Suffixes")) {
            suffixYaml.addDefault("Suffixes.Test.Display", "<gray>Тестовый суффикс");
            suffixYaml.addDefault("Suffixes.Test.Desc", List.of("<white>Описание тестового суффикса.", "<white>Может быть многострочным."));
            suffixYaml.addDefault("Suffixes.Test.Model", "test");
            suffixYaml.addDefault("Suffixes.Test.Permission", "vineriumcore.suffix.test");
            suffixYaml.addDefault("Suffixes.Test.Symbol", "+");
        }
        suffixYaml.options().copyDefaults(true);
        try {
            suffixYaml.save(suffixFile);
        } catch (IOException e) {
            VinUtils.sendDebugMessage(0,"<red>Couldn't save suffix config to "+ suffixFile +"!");
        }
    }

    public void loadParams(Plugin plugin) {
        maceDenierCustomNames.clear();
        itemLockMaterials.clear();
        if (plugin.getConfig().getBoolean("Tweaks.MaceDenier.Enabled"))
            maceDenierCustomNames.addAll(plugin.getConfig().getStringList("Tweaks.MaceDenier.CustomNames"));
        if (plugin.getConfig().getBoolean("Tweaks.ItemLock.Enabled"))
            itemLockMaterials.addAll(plugin.getConfig().getStringList("Tweaks.ItemLock.Materials"));
    }

    public Set<String> getMaceDenierCustomNames() {
        return maceDenierCustomNames;
    }

    public Set<String> getItemLockMaterials() {
        return itemLockMaterials;
    }

    public static class InjectedVillagerTrades {
        private final HashMap<Key,List<MerchantRecipe>> recipes = new HashMap<>();

        public void updateParams() {
            recipes.clear();
            if (!VineriumCore.inst().getConfig().contains("Tweaks.VillagerOptimizer.InjectTradesOnOptimize")) return;
            for (@Subst("minecraft:paper") String professionName : VineriumCore.inst().getConfig().getConfigurationSection("Tweaks.VillagerOptimizer.InjectTradesOnOptimize").getKeys(false)) {
                Key professionKey = Key.key(professionName);
                List<MerchantRecipe> professionRecipes = new ArrayList<>();
                List<String> tradeList = VineriumCore.inst().getConfig().getStringList("Tweaks.VillagerOptimizer.InjectTradesOnOptimize."+professionName);
                for (String tradeInfo : tradeList) {
                    String[] tradeData = tradeInfo.split(",");
                    String[] tradeFirstItemData = tradeData[0].split(":");
                    ItemStack firstItem = ItemStack.of(Material.valueOf(tradeFirstItemData[0]),Integer.parseInt(tradeFirstItemData[1]));
                    String[] tradeSecondItemData = tradeData[1].split(":");
                    ItemStack secondItem = ItemStack.of(Material.valueOf(tradeSecondItemData[0]),Integer.parseInt(tradeSecondItemData[1]));
                    String[] tradeResultItemData = tradeData[2].split(":");
                    ItemStack resultItem = ItemStack.of(Material.valueOf(tradeResultItemData[0]),Integer.parseInt(tradeResultItemData[1]));

                    MerchantRecipe injectedRecipe = new MerchantRecipe(resultItem,0,7,true);
                    if (firstItem.getType() != Material.AIR)
                        injectedRecipe.addIngredient(firstItem);
                    if (secondItem.getType() != Material.AIR)
                        injectedRecipe.addIngredient(secondItem);
                    professionRecipes.add(injectedRecipe);
                }
                recipes.put(professionKey,professionRecipes);
            }
        }

        public HashMap<Key, List<MerchantRecipe>> getRecipes() {
            return recipes;
        }
    }

    public InjectedVillagerTrades getInjectedVillagerTrades() {
        return injectedVillagerTrades;
    }
}
