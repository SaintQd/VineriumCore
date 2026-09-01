package org.saintqd.vineriumcore.listeners;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.*;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumcore.worldguard.Flags;
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

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(event.getPlayer());
        if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.VILLAGER_TRADE)) {
            event.setCancelled(true);
            return;
        }

            if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.VillagerOptimizer.Enabled",true)) return;
        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.VillagerManualBreed.Enabled",true)) {
            ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
            if (VineriumCore.inst().getConfig().contains("Tweaks.VillagerManualBreed.Items."+handItem.getType())) {

                if (!villager.isAdult()) return;

                PlayerManager playerManager = VineriumCore.inst().getPlayerManager();

                HashMap<Player, ImmutablePair<String,Long>> timers = playerManager.getTimers()
                        .getOrDefault("villager_manual_breed_cooldown",new HashMap<>());
                ImmutablePair<String,Long> timerVariable = timers.getOrDefault(event.getPlayer(),new ImmutablePair<>(null,0L));

                if (timerVariable.getRight() > VinUtils.getCurrentTick()) {
                    long remainingTime = (timerVariable.getRight() - VinUtils.getCurrentTick()) / 20;
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"villager_manual_breed_cooldown",Long.toString(remainingTime)));
                    return;
                }

                event.setCancelled(true);
                int amount = handItem.getAmount();
                int requiredAmount = VineriumCore.inst().getConfig().getInt("Tweaks.VillagerManualBreed.Items."+handItem.getType());
                if (amount >= requiredAmount) {
                    villager.getLocation().getWorld().spawn(villager.getLocation(),
                            Villager.class, CreatureSpawnEvent.SpawnReason.BREEDING, Ageable::setBaby);
                    villager.getWorld().spawnParticle(Particle.HEART, villager.getLocation().clone().add(0.0, 1.0, 0.0), 10, 0.25, 0.5, 0.25, 0.0);
                    villager.getWorld().playSound(villager.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_CELEBRATE,SoundCategory.NEUTRAL,1f,1f);
                    handItem.setAmount(handItem.getAmount() - requiredAmount);

                    timerVariable = new ImmutablePair<>(null,VinUtils.getCurrentTick() + VineriumCore.inst().getConfig()
                            .getLong("Tweaks.VillagerManualBreed.Cooldown",1200L));
                    timers.put(event.getPlayer(),timerVariable);
                    playerManager.getTimers().put("villager_manual_breed_cooldown",timers);
                }
                else {
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),
                            "villager_manual_breed_no_items",Integer.toString(requiredAmount),"<lang:"+handItem.getType().translationKey()+">"));
                }

                return;
            }
        }

        Material interactMaterial = Material.valueOf(VineriumCore.inst().getConfig()
                .getString("Tweaks.VillagerOptimizer.InteractMaterial",Material.SHEARS.name()).toUpperCase());
        @Subst("block.chain.hit") String interactSound = VineriumCore.inst().getConfig()
                .getString("Tweaks.VillagerOptimizer.InteractSound", null);

        if (villager.isAware()) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == interactMaterial) {

                Key professionKey = villager.getProfession().key();
                if (VineriumCore.inst().getConfigManager().getInjectedVillagerTrades().getRecipes().containsKey(professionKey)) {
                    List<MerchantRecipe> recipesToAdd = new ArrayList<>(VineriumCore.inst().getConfigManager().getInjectedVillagerTrades().getRecipes().get(professionKey));
                    recipesToAdd.removeIf(testedRecipe -> {
                        for (MerchantRecipe villagerRecipe : villager.getRecipes()) {
                            if (villagerRecipe.getResult().getType() == testedRecipe.getResult().getType())
                                return true;
                        }
                        return false;
                    });
                    List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
                    recipes.addAll(recipesToAdd);
                    villager.setRecipes(recipes);
                }

                villager.setAware(false);
                if (interactSound != null)
                    villager.getWorld().playSound(villager.getLocation(),interactSound,SoundCategory.NEUTRAL,1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"villager_optimizer_interact_off"));

                event.setCancelled(true);
                return;
            }
        }
        else {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == interactMaterial) {
                villager.setAware(true);
                if (interactSound != null)
                    villager.getWorld().playSound(villager.getLocation(),interactSound,SoundCategory.NEUTRAL, 1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"villager_optimizer_interact_on"));

                event.setCancelled(true);
                return;
            }
            else {
                if (!isJobSiteNearby(villager)) {
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villager_optimizer_too_far_from_station"));
                    event.setCancelled(true);
                    return;
                }
                refreshTrades(villager);
            }
        }
    }

    @EventHandler
    public void onVillagerInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.VillagerOptimizer.Enabled", true)) return;
        if (villager.isAware() && VineriumCore.inst().getConfig()
                .getBoolean("Tweaks.VillagerOptimizer.DisableUnoptimizedTrades",true)) {
            String interactMaterialString = VineriumCore.inst().getConfig().getString("Tweaks.VillagerOptimizer.InteractMaterial","STONE");
            Material interactMaterial = Material.valueOf(interactMaterialString);
            String translatableKey = "<lang:" + interactMaterial.translationKey() + ">";
            event.getWhoClicked().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villager_optimizer_interact_hint",translatableKey));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVillagerTradeSelect(TradeSelectEvent event) {
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (!(event.getInventory().getHolder() instanceof Villager villager)) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.VillagerOptimizer.Enabled", true)) return;
        if (villager.isAware() && VineriumCore.inst().getConfig()
                .getBoolean("Tweaks.VillagerOptimizer.DisableUnoptimizedTrades",true)) {
            String interactMaterialString = VineriumCore.inst().getConfig().getString("Tweaks.VillagerOptimizer.InteractMaterial","STONE");
            Material interactMaterial = Material.valueOf(interactMaterialString);
            String translatableKey = "<lang:" + interactMaterial.translationKey() + ">";
            event.getWhoClicked().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "villager_optimizer_interact_hint",translatableKey));
            event.setCancelled(true);
        }
    }

    private void refreshTrades(Villager villager) {

        long lastRestock = this.lastRestock.getOrDefault(villager,0L);
        long restockInterval = VineriumCore.inst().getConfig().getLong("Tweaks.VillagerOptimizer.RestockInterval",1000L);

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
