package org.saintqd.vineriumcore.listeners;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerListener implements Listener {

    private final Map<Villager.Profession, Material> professionMaterialMap;
    private final HashMap<Entity,Long> lastRestock;

    public VillagerListener() {
        this.lastRestock = new HashMap<>();

        Map<Villager.Profession, Material> map = new ConcurrentHashMap<>();

        map.put(Villager.Profession.ARMORER, Material.BLAST_FURNACE);
        map.put(Villager.Profession.BUTCHER, Material.SMOKER);
        map.put(Villager.Profession.CARTOGRAPHER, Material.CARTOGRAPHY_TABLE);
        map.put(Villager.Profession.CLERIC, Material.BREWING_STAND);
        map.put(Villager.Profession.FARMER, Material.COMPOSTER);
        map.put(Villager.Profession.FISHERMAN, Material.BARREL);
        map.put(Villager.Profession.FLETCHER, Material.FLETCHING_TABLE);
        map.put(Villager.Profession.LEATHERWORKER, Material.CAULDRON);
        map.put(Villager.Profession.LIBRARIAN, Material.LECTERN);
        map.put(Villager.Profession.MASON, Material.STONECUTTER);
        map.put(Villager.Profession.SHEPHERD, Material.LOOM);
        map.put(Villager.Profession.TOOLSMITH, Material.SMITHING_TABLE);
        map.put(Villager.Profession.WEAPONSMITH, Material.GRINDSTONE);

        professionMaterialMap = Collections.unmodifiableMap(map);
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!VineriumCore.inst().getConfig().getBoolean("VillagerOptimizer.Enabled",true)) return;

        Material interactMaterial = Material.valueOf(VineriumCore.inst().getConfig()
                .getString("VillagerOptimizer.InteractMaterial",Material.SHEARS.name()).toUpperCase());
        @Subst("block.chain.hit") String interactSound = VineriumCore.inst().getConfig()
                .getString("VillagerOptimizer.InteractSound", null);

        if (villager.isAware()) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == interactMaterial) {
                villager.setAware(false);
                if (interactSound != null)
                    villager.getWorld().playSound(villager.getLocation(),interactSound,SoundCategory.NEUTRAL,1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"villagerOptimizerInteractOff"));

                event.setCancelled(true);
                return;
            }
        }
        else {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == interactMaterial) {
                villager.setAware(true);
                if (interactSound != null)
                    villager.getWorld().playSound(villager.getLocation(),interactSound,SoundCategory.NEUTRAL, 1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"villagerOptimizerInteractOn"));

                event.setCancelled(true);
                return;
            }
            else {
                if (!isJobSiteNearby(villager)) {
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villagerOptimizerTooFarFromStation"));
                    event.setCancelled(true);
                    return;
                }
                refreshTrades(villager, event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onVillagerInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;
        if (!VineriumCore.inst().getConfig().getBoolean("VillagerOptimizer.Enabled", true)) return;
        if (villager.isAware() && VineriumCore.inst().getConfig()
                .getBoolean("VillagerOptimizer.DisableUnoptimizedTrades",true)) {
            event.getWhoClicked().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villagerOptimizerInteractHint"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerTradeSelect(TradeSelectEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;
        if (!VineriumCore.inst().getConfig().getBoolean("VillagerOptimizer.Enabled", true)) return;
        if (villager.isAware() && VineriumCore.inst().getConfig()
                .getBoolean("VillagerOptimizer.DisableUnoptimizedTrades",true)) {
            event.getWhoClicked().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villagerOptimizerInteractHint"));
            event.setCancelled(true);
        }
    }

    private void refreshTrades(Villager villager, Player player) {

        long lastRestock = this.lastRestock.getOrDefault(villager,0L);
        long restockInterval = VineriumCore.inst().getConfig().getLong("VillagerOptimizer.RestockInterval",1000L);

        if (VinUtils.getCurrentTick() < lastRestock + restockInterval) return;

        this.lastRestock.put(villager,VinUtils.getCurrentTick());

        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        for (MerchantRecipe recipe : recipes) {
            recipe.setUses(0);
        }
        villager.setRecipes(recipes);
        villager.setRestocksToday(villager.getRestocksToday() + 1);
        // Tell the villager to update pricing of their trades
        villager.updateDemand();

        villager.getWorld().playSound(villager.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_CELEBRATE,SoundCategory.NEUTRAL,1f,1f);

        int currentLevel = villager.getVillagerLevel();

        if (currentLevel == 5) {
            return;
        }

        int expectedLevel = getVillagerLevel(villager);

        if (currentLevel < expectedLevel) {
            // We can just set the villager level to the expected level
            int increaseAmount = Math.max(0, expectedLevel - currentLevel);
            villager.increaseLevel(increaseAmount);
            PotionEffect regenEffect = new PotionEffect(PotionEffectType.REGENERATION, 200, 0, false);
            villager.addPotionEffect(regenEffect);
        }
    }

    private boolean isJobSiteNearby(Villager villager) {
        Material jobSite = professionMaterialMap.get(villager.getProfession());

        if (jobSite == null || jobSite == Material.AIR) {
            return false;
        }

        Location location = villager.getLocation();
        int[] yOffsets = {0, 1}; // feet and body levels
        for (int yOffset : yOffsets) {
            int checkY = location.getBlockY() + yOffset;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    int checkX = location.getBlockX() + x;
                    int checkZ = location.getBlockZ() + z;

                    if (villager.getWorld().getBlockAt(checkX, checkY, checkZ).getType() == jobSite) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getVillagerLevel(Villager villager) {
        int villagerExperience = villager.getVillagerExperience();
        if (villagerExperience >= 250) {
            return 5;
        }
        if (villagerExperience >= 150) {
            return 4;
        }
        if (villagerExperience >= 70) {
            return 3;
        }
        if (villagerExperience >= 10) {
            return 2;
        }
        return 1;
    }
}
