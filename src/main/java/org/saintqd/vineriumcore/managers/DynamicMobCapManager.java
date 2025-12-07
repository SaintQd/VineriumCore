package org.saintqd.vineriumcore.managers;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.plugin.Plugin;
import org.saintqd.vineriumcore.VineriumCore;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DynamicMobCapManager {

    private final HashMap<SpawnCategory,SpawnCategoryCaps> spawnCategoryCaps = new HashMap<>();

    private static class SpawnCategoryCaps {
        private final HashMap<World, TreeMap<Integer,Integer>> worldCaps = new HashMap<>();

        public HashMap<World, TreeMap<Integer,Integer>> getWorldCaps() {
            return worldCaps;
        }
    }

    public void loadParams(Plugin plugin) {
        this.spawnCategoryCaps.clear();
        if (!VineriumCore.inst().getConfig().getBoolean("DynamicMobCaps.Enabled")) return;
        if (!VineriumCore.inst().getConfig().contains("DynamicMobCaps.Categories")) return;
        for (String spawnCategoryName : VineriumCore.inst().getConfig().getConfigurationSection("DynamicMobCaps.Categories").getKeys(false)) {
            Optional<SpawnCategory> spawnCategory = Enums.getIfPresent(SpawnCategory.class,spawnCategoryName);
            if (!spawnCategory.isPresent()) continue;
            SpawnCategoryCaps selectedCategoryCaps = new SpawnCategoryCaps();
            if (VineriumCore.inst().getConfig().contains("DynamicMobCaps.Categories."+spawnCategoryName+".ALL")) {
                for (World world : Bukkit.getWorlds()) {
                    TreeMap<Integer,Integer> playerMobCaps = new TreeMap<>();
                    for (String playerAmount : VineriumCore.inst().getConfig().getConfigurationSection(
                            "DynamicMobCaps.Categories."+spawnCategoryName+".ALL").getKeys(false)) {
                        int amount = Integer.parseInt(playerAmount);
                        playerMobCaps.put(amount,VineriumCore.inst().getConfig().getInt(
                                "DynamicMobCaps.Categories."+spawnCategoryName+".ALL."+playerAmount));
                    }
                    selectedCategoryCaps.getWorldCaps().put(world,playerMobCaps);
                }
            }
            for (String worldName : VineriumCore.inst().getConfig().getConfigurationSection("DynamicMobCaps.Categories."+spawnCategoryName).getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                TreeMap<Integer,Integer> playerMobCaps = new TreeMap<>();
                for (String playerAmount : VineriumCore.inst().getConfig().getConfigurationSection(
                        "DynamicMobCaps.Categories."+spawnCategoryName+"."+worldName).getKeys(false)) {
                    int amount = Integer.parseInt(playerAmount);
                    playerMobCaps.put(amount,VineriumCore.inst().getConfig().getInt(
                            "DynamicMobCaps.Categories."+spawnCategoryName+"."+worldName+"."+playerAmount));
                }
                selectedCategoryCaps.getWorldCaps().put(world,playerMobCaps);
            }
            spawnCategoryCaps.put(spawnCategory.get(),selectedCategoryCaps);
        }
    }

    public void updateWorldCaps(int newPlayerAmount) {
        for (SpawnCategory spawnCategory : spawnCategoryCaps.keySet()) {
            SpawnCategoryCaps selectedCategoryCaps = spawnCategoryCaps.get(spawnCategory);
            for (World world : selectedCategoryCaps.getWorldCaps().keySet()) {
                TreeMap<Integer,Integer> playerMobCaps = selectedCategoryCaps.getWorldCaps().get(world);
                Map.Entry<Integer,Integer> entry = playerMobCaps.floorEntry(newPlayerAmount);
                if (entry != null) {
                    int selectedPlayerCap = entry.getValue();
                    world.setSpawnLimit(spawnCategory,selectedPlayerCap);
                }
            }
        }
    }
}
