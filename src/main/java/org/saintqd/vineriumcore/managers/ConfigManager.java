package org.saintqd.vineriumcore.managers;

import kotlin.Pair;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
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

    @Getter
    private final Set<String> maceDenierCustomNames = new HashSet<>();
    @Getter
    private final Set<String> itemLockMaterials = new HashSet<>();
    @Getter
    private final InjectedVillagerTrades injectedVillagerTrades = new InjectedVillagerTrades();
    @Getter
    private final Set<String> disabledDrops = new HashSet<>();
    @Getter
    private final HashMap<String,String> cauldronTransforms = new HashMap<>();
    private final HashMap<String,String> cosmeticPermissionsToNames = new HashMap<>();

    public HashMap<String,String> getCosmeticPermissionsToNames() {
        return cosmeticPermissionsToNames;
    }

    public void checkConfigs() {
        File suffixFile = new File(VineriumCore.inst().getDataFolder().getPath() + File.separator + "Suffixes.yml");
        File parent = suffixFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            VinUtils.sendDebugMessage(0,"<red>Couldn't create suffix config!");
            return;
        }
        YamlConfiguration suffixYaml = YamlConfiguration.loadConfiguration(suffixFile);
        suffixYaml.addDefault("PlaceholderTemplate","%nexo_{0}%");
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

    public void loadParams(VineriumCore plugin) {
        maceDenierCustomNames.clear();
        itemLockMaterials.clear();
        disabledDrops.clear();
        cauldronTransforms.clear();
        cosmeticPermissionsToNames.clear();
        if (plugin.getConfig().getBoolean("Tweaks.MaceDenier.Enabled"))
            maceDenierCustomNames.addAll(plugin.getConfig().getStringList("Tweaks.MaceDenier.CustomNames"));
        if (plugin.getConfig().getBoolean("Tweaks.ItemLock.Enabled"))
            itemLockMaterials.addAll(plugin.getConfig().getStringList("Tweaks.ItemLock.Materials"));
        if (plugin.getConfig().getBoolean("DisableDrops.Enabled"))
            disabledDrops.addAll(plugin.getConfig().getStringList("DisableDrops.Materials"));
        if (plugin.getConfig().getBoolean("Tweaks.CauldronTransform.Enabled") && plugin.getConfig().contains("Tweaks.CauldronTransform.TransformList")) {
            for (String originalType : plugin.getConfig().getConfigurationSection("Tweaks.CauldronTransform.TransformList").getKeys(false)) {
                cauldronTransforms.put(originalType,plugin.getConfig().getString("Tweaks.CauldronTransform.TransformList."+originalType));
            }
        }
        if (plugin.isHmcCosmeticsEnabled()) {
            for (com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic cosmetic : com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics.values()) {
                cosmeticPermissionsToNames.put(cosmetic.getPermission(),cosmetic.getId());
            }
        }
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

    public static class PortalCornerBlocksFinder {
        private final HashMap<Material,Integer> foundCornerBlocks = new HashMap<>();
        private boolean portalError = false;

        public static HashMap<Material,Integer> findCornerBlocks(Location startingLoc) {
            PortalCornerBlocksFinder customPortalFinder = new PortalCornerBlocksFinder();
            customPortalFinder.findBlocks(startingLoc);
            return customPortalFinder.foundCornerBlocks;
        }

        private PortalCornerBlocksFinder() {}

        private void findBlocks(Location startingLoc) {
            if (startingLoc.getBlock().getType() != Material.NETHER_PORTAL)
                return;
            boolean xAxis = findAxis(startingLoc);
            checkForPortalError(startingLoc, xAxis);
            if (!portalError) {
                if (xAxis) {
                    findBorderBlock(startingLoc, new Pair<>(1.0, 0.0));
                    findBorderBlock(startingLoc, new Pair<>(-1.0, 0.0));
                } else {
                    findBorderBlock(startingLoc, new Pair<>(0.0, 1.0));
                    findBorderBlock(startingLoc, new Pair<>(0.0, -1.0));
                }
            }
        }

        private boolean findAxis(Location startingLoc) {
            Location changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX() - 1, startingLoc.getY(), startingLoc.getZ());
            if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                return true;
            }
            changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX() + 1, startingLoc.getY(), startingLoc.getZ());
            if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                return true;
            }
            changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ() - 1);
            if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                return false;
            }
            changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ() + 1);
            if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                return false;
            }
            else {
                portalError = true;
                return false;
            }
        }

        private void checkForPortalError(Location startingLoc,boolean xAxis) {
            Location changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ());
            if (xAxis) {
                changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ() - 1);
                if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                    portalError = true;
                    return;
                }
                changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ() + 1);
                if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                    portalError = true;
                    return;
                }
            }
            else {
                changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX() - 1, startingLoc.getY(), startingLoc.getZ());
                if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                    portalError = true;
                    return;
                }
                changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX() + 1, startingLoc.getY(), startingLoc.getZ());
                if (changedLoc.getWorld().getBlockAt(changedLoc).getType() == Material.NETHER_PORTAL) {
                    portalError = true;
                    return;
                }
            }
        }

        private void findBorderBlock(Location startingLoc, Pair<Double,Double> shiftPair) {
            Material selectedBlockMaterial = Material.AIR;
            Location changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY(), startingLoc.getZ());
            while (selectedBlockMaterial != Material.OBSIDIAN && selectedBlockMaterial != Material.CRYING_OBSIDIAN) {
                changedLoc = new Location(changedLoc.getWorld(), changedLoc.getX()+shiftPair.getFirst(), changedLoc.getY(), changedLoc.getZ()+shiftPair.getSecond());
                selectedBlockMaterial = changedLoc.getWorld().getBlockAt(changedLoc).getType();
            }
            findCornerBlock(changedLoc,1.0);
            findCornerBlock(changedLoc,-1.0);
        }

        private void findCornerBlock(Location startingLoc, double shift) {
            if (startingLoc.getY() + shift >= 319 || startingLoc.getY() + shift <= -63)
                return;
            Location changedLoc = new Location(startingLoc.getWorld(), startingLoc.getX(), startingLoc.getY()+shift, startingLoc.getZ());
            Material selectedBlockMaterial = changedLoc.getWorld().getBlockAt(changedLoc).getType();
            if (selectedBlockMaterial == Material.OBSIDIAN || selectedBlockMaterial == Material.CRYING_OBSIDIAN) {
                findCornerBlock(changedLoc, shift);
            }
            else {
                foundCornerBlocks.put(selectedBlockMaterial,foundCornerBlocks.getOrDefault(selectedBlockMaterial,0) + 1);
            }
        }
    }
}
