package org.saintqd.vineriumcore.managers;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OreManager {

    private final HashMap<String, List<OreData>> playerOreData = new HashMap<>();
    private final HashMap<Location,Long> checkedLocations = new HashMap<>();
    private final HashSet<Location> placedOres = new HashSet<>();
    private final HashMap<Material, Pair<Integer,String>> thresholdMaterials = new HashMap<>();
    private Sound alertSound;

    public void loadData(Plugin plugin) {
        thresholdMaterials.clear();
        alertSound = null;
        if (!plugin.getConfig().contains("OreAlerts.Materials"))
            return;
        for (String materialName : plugin.getConfig().getConfigurationSection("OreAlerts.Materials").getKeys(false)) {
            Optional<Material> possibleMaterial = Enums.getIfPresent(Material.class,materialName.toUpperCase());
            if (!possibleMaterial.isPresent()) {
                VinUtils.sendDebugMessage(0,"<yellow>OreAlerts: Could not load material "+materialName+"!");
            }
            else {
                int amount = plugin.getConfig().getInt("OreAlerts.Materials."+materialName+".Threshold");
                String color = plugin.getConfig().getString("OreAlerts.Materials."+materialName+".Color");
                thresholdMaterials.put(possibleMaterial.get(),Pair.of(amount,color));
            }
        }
        String[] soundData = plugin.getConfig().getString("OreAlerts.Sound","entity.player.levelup,2").split(",");
        @Subst("block.chain.hit") String soundName = soundData[0];
        float pitch = soundData.length > 1 ? Float.parseFloat(soundData[1]) : 1.0f;
        Key soundKey = Key.key(soundName);
        alertSound = Sound.sound(soundKey, Sound.Source.PLAYER,1.0f,pitch);
    }

    public void updateData(Plugin plugin) {
        if (!plugin.getConfig().getBoolean("OreAlerts.Enabled"))
            return;
        placedOres.clear();
        long currentTick = VinUtils.getCurrentTick();
        long timeToThreshold = plugin.getConfig().getLong("OreAlerts.TimeToThreshold",1200);
        checkedLocations.values().removeIf(time -> time + timeToThreshold < currentTick);
        for (String playerName : playerOreData.keySet()) {
            List<OreData> oreData = playerOreData.get(playerName);
            oreData.removeIf(data -> data.getTimestamp() + timeToThreshold < currentTick);
            playerOreData.put(playerName,oreData);
        }
    }

    public static class OreData {

        private final long timestamp;
        private final Material type;
        private final int amount;

        public OreData(Material type, int amount) {
            this.timestamp = VinUtils.getCurrentTick();
            this.type = type;
            this.amount = amount;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Material getType() {
            return type;
        }

        public int getAmount() {
            return amount;
        }
    }

    public HashMap<String, List<OreData>> getPlayerOreData() {
        return playerOreData;
    }

    public HashMap<Location,Long> getCheckedLocations() {
        return checkedLocations;
    }

    public HashMap<Material, Pair<Integer,String>> getThresholdMaterials() {
        return thresholdMaterials;
    }

    public Sound getAlertSound() {
        return alertSound;
    }

    public HashSet<Location> getPlacedOres() {
        return placedOres;
    }

    public static class BlockCounter {

        private final Set<Location> nearBlocks = new HashSet<>();
        private final Set<Location> alreadyCheckedBlocks = new HashSet<>();

        public Set<Location> getNearBlocks(Location location, Material material) {
            if (alreadyCheckedBlocks.contains(location) || VineriumCore.inst().getOreManager().getCheckedLocations().containsKey(location))
                return nearBlocks;
            if (location.getWorld().getBlockAt(location).getType() == material) {
                nearBlocks.add(location);
                alreadyCheckedBlocks.add(location);
                for (int x = location.blockX() - 1; x <= location.blockX() + 1; x++) {
                    for (int y = location.blockY() - 1; y <= location.blockY() + 1; y++) {
                        for (int z = location.blockZ() - 1; z <= location.blockZ() + 1; z++) {
                            if (nearBlocks.size() < 500) {
                                Location newLocation = new Location(location.getWorld(),x,y,z);
                                getNearBlocks(newLocation, material);
                            } else {
                                // Calculating too many blocks, print an error
                                // and force to stop the counter
                                return new HashSet<>();
                            }

                            if (nearBlocks.isEmpty())
                                return new HashSet<>();
                        }
                    }
                }
            }
            return nearBlocks;
        }
    }
}
