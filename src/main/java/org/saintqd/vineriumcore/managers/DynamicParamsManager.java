package org.saintqd.vineriumcore.managers;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class DynamicParamsManager {

    private final HashMap<SpawnCategory, WorldCaps> spawnCategoryCaps = new HashMap<>();
    private final WorldCaps viewDistanceCaps = new WorldCaps();
    private final WorldCaps simulationDistanceCaps = new WorldCaps();

    private static class WorldCaps {
        private final HashMap<World, TreeMap<Integer,Integer>> worldCaps = new HashMap<>();

        public HashMap<World, TreeMap<Integer,Integer>> getWorldCaps() {
            return worldCaps;
        }
    }

    public void loadParams(final Plugin plugin) {
        this.spawnCategoryCaps.clear();
        this.viewDistanceCaps.getWorldCaps().clear();
        this.simulationDistanceCaps.getWorldCaps().clear();
        if (plugin.getConfig().getBoolean("DynamicMobCaps.Enabled") && plugin.getConfig().contains("DynamicMobCaps.Categories")) {
            for (String spawnCategoryName : plugin.getConfig().getConfigurationSection("DynamicMobCaps.Categories").getKeys(false)) {
                Optional<SpawnCategory> spawnCategory = Enums.getIfPresent(SpawnCategory.class, spawnCategoryName);
                if (!spawnCategory.isPresent()) continue;
                WorldCaps selectedCategoryCaps = new WorldCaps();
                if (plugin.getConfig().contains("DynamicMobCaps.Categories." + spawnCategoryName + ".ALL")) {
                    for (World world : Bukkit.getWorlds()) {
                        TreeMap<Integer, Integer> playerMobCaps = new TreeMap<>();
                        for (String playerAmount : plugin.getConfig().getConfigurationSection(
                                "DynamicMobCaps.Categories." + spawnCategoryName + ".ALL").getKeys(false)) {
                            int amount = Integer.parseInt(playerAmount);
                            playerMobCaps.put(amount, plugin.getConfig().getInt(
                                    "DynamicMobCaps.Categories." + spawnCategoryName + ".ALL." + playerAmount));
                        }
                        selectedCategoryCaps.getWorldCaps().put(world, playerMobCaps);
                    }
                }
                for (String worldName : plugin.getConfig().getConfigurationSection("DynamicMobCaps.Categories." + spawnCategoryName).getKeys(false)) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;
                    TreeMap<Integer, Integer> playerMobCaps = new TreeMap<>();
                    for (String playerAmount : plugin.getConfig().getConfigurationSection(
                            "DynamicMobCaps.Categories." + spawnCategoryName + "." + worldName).getKeys(false)) {
                        int amount = Integer.parseInt(playerAmount);
                        playerMobCaps.put(amount, plugin.getConfig().getInt(
                                "DynamicMobCaps.Categories." + spawnCategoryName + "." + worldName + "." + playerAmount));
                    }
                    selectedCategoryCaps.getWorldCaps().put(world, playerMobCaps);
                }
                spawnCategoryCaps.put(spawnCategory.get(), selectedCategoryCaps);
            }
        }
        if (plugin.getConfig().getBoolean("DynamicDistance.Enabled")) {
            WorldCaps selectedViewDistanceCaps = new WorldCaps();
            if (plugin.getConfig().contains("DynamicDistance.View.ALL")) {
                for (World world : Bukkit.getWorlds()) {
                    TreeMap<Integer, Integer> playerCaps = new TreeMap<>();
                    for (String playerAmount : plugin.getConfig().getConfigurationSection(
                            "DynamicDistance.View.ALL").getKeys(false)) {
                        int amount = Integer.parseInt(playerAmount);
                        playerCaps.put(amount, plugin.getConfig().getInt(
                                "DynamicDistance.View.ALL." + playerAmount));
                    }
                    selectedViewDistanceCaps.getWorldCaps().put(world, playerCaps);
                }
            }
            for (String worldName : plugin.getConfig().getConfigurationSection("DynamicDistance.View").getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                TreeMap<Integer, Integer> playerCaps = new TreeMap<>();
                for (String playerAmount : plugin.getConfig().getConfigurationSection(
                        "DynamicDistance.View." + worldName).getKeys(false)) {
                    int amount = Integer.parseInt(playerAmount);
                    playerCaps.put(amount, plugin.getConfig().getInt(
                            "DynamicDistance.View." + worldName + "." + playerAmount));
                }
                selectedViewDistanceCaps.getWorldCaps().put(world, playerCaps);
            }

            WorldCaps selectedSimulationDistanceCaps = new WorldCaps();
            if (plugin.getConfig().contains("DynamicDistance.Simulation.ALL")) {
                for (World world : Bukkit.getWorlds()) {
                    TreeMap<Integer, Integer> playerCaps = new TreeMap<>();
                    for (String playerAmount : plugin.getConfig().getConfigurationSection(
                            "DynamicDistance.Simulation.ALL").getKeys(false)) {
                        int amount = Integer.parseInt(playerAmount);
                        playerCaps.put(amount, plugin.getConfig().getInt(
                                "DynamicDistance.Simulation.ALL." + playerAmount));
                    }
                    selectedSimulationDistanceCaps.getWorldCaps().put(world, playerCaps);
                }
            }
            for (String worldName : plugin.getConfig().getConfigurationSection("DynamicDistance.Simulation").getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                TreeMap<Integer, Integer> playerCaps = new TreeMap<>();
                for (String playerAmount : plugin.getConfig().getConfigurationSection(
                        "DynamicDistance.Simulation." + worldName).getKeys(false)) {
                    int amount = Integer.parseInt(playerAmount);
                    playerCaps.put(amount, plugin.getConfig().getInt(
                            "DynamicDistance.Simulation." + worldName + "." + playerAmount));
                }
                selectedSimulationDistanceCaps.getWorldCaps().put(world, playerCaps);
            }
            viewDistanceCaps.getWorldCaps().putAll(selectedViewDistanceCaps.getWorldCaps());
            simulationDistanceCaps.getWorldCaps().putAll(selectedSimulationDistanceCaps.getWorldCaps());
        }
    }

    public void updateWorldCaps(final int newPlayerAmount) {
        if (!spawnCategoryCaps.isEmpty()) {
            for (SpawnCategory spawnCategory : spawnCategoryCaps.keySet()) {
                WorldCaps selectedCategoryCaps = spawnCategoryCaps.get(spawnCategory);
                for (World world : selectedCategoryCaps.getWorldCaps().keySet()) {
                    TreeMap<Integer, Integer> playerMobCaps = selectedCategoryCaps.getWorldCaps().get(world);
                    Map.Entry<Integer, Integer> entry = playerMobCaps.floorEntry(newPlayerAmount);
                    if (entry != null) {
                        int selectedPlayerCap = entry.getValue();
                        world.setSpawnLimit(spawnCategory, selectedPlayerCap);
                    }
                }
            }
        }
        if (!viewDistanceCaps.getWorldCaps().isEmpty()) {
            for (World world : viewDistanceCaps.getWorldCaps().keySet()) {
                TreeMap<Integer, Integer> playerCaps = viewDistanceCaps.getWorldCaps().get(world);
                Map.Entry<Integer, Integer> entry = playerCaps.floorEntry(newPlayerAmount);
                if (entry != null) {
                    int selectedDistanceCap = entry.getValue();
                    world.setViewDistance(selectedDistanceCap);
                }
            }
        }
        if (!simulationDistanceCaps.getWorldCaps().isEmpty()) {
            for (World world : simulationDistanceCaps.getWorldCaps().keySet()) {
                TreeMap<Integer, Integer> playerCaps = simulationDistanceCaps.getWorldCaps().get(world);
                Map.Entry<Integer, Integer> entry = playerCaps.floorEntry(newPlayerAmount);
                if (entry != null) {
                    int selectedDistanceCap = entry.getValue();
                    world.setViewDistance(selectedDistanceCap);
                }
            }
        }
    }
}
