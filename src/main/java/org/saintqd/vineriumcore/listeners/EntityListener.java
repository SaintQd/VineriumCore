package org.saintqd.vineriumcore.listeners;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;

import java.util.*;

public class EntityListener implements Listener {

    private final NamespacedKey COPPER_GOLEM_LIMIT_KEY = new NamespacedKey(VineriumCore.inst(),"copper_golem_limit");

    @EventHandler
    public void onCopperGolemSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof CopperGolem copperGolem
                && event.getEntity().getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_COPPERGOLEM
                && VineriumCore.inst().getConfig().getBoolean("Tweaks.CopperGolem.Enabled",true)) {
            copperGolem.setAware(false);
            List<String> activationMaterials = VineriumCore.inst().getConfig().getStringList("Tweaks.CopperGolem.InteractMaterials");
            if (activationMaterials.isEmpty())
                return;
            List<Entity> nearbyEntities = event.getEntity().
                    getNearbyEntities(10,10,10);
            Material interactMaterial = Material.valueOf(activationMaterials.getFirst());
            String translatableKey = "<lang:" + interactMaterial.translationKey() + ">";
            nearbyEntities.removeIf(entity -> !(entity instanceof Player));
            Audience audience = Audience.audience(nearbyEntities);
            audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"copper_golem_interact_hint",translatableKey));
        }
    }

    @EventHandler
    public void onCopperGolemInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof CopperGolem copperGolem)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.CopperGolem.Enabled",true)) return;

        List<String> possibleInteractMaterials = VineriumCore.inst().getConfig().getStringList("Tweaks.CopperGolem.InteractMaterials");
        @Subst("block.chain.hit") String interactSound = VineriumCore.inst().getConfig()
                .getString("Tweaks.CopperGolem.InteractSound", null);

        if (copperGolem.isAware()) {
            if (possibleInteractMaterials.contains(event.getPlayer().getInventory().getItemInMainHand().getType().name())) {
                if (!event.getPlayer().hasPermission("vineriumcore.admin")) {
                    String unparsedUuids = event.getPlayer().getPersistentDataContainer().getOrDefault(COPPER_GOLEM_LIMIT_KEY, PersistentDataType.STRING, "");
                    List<String> uuids = !unparsedUuids.isEmpty() ? new ArrayList<>(Arrays.stream(unparsedUuids.split(",")).toList()) : new ArrayList<>();
                    String golemUuid = copperGolem.getUniqueId().toString();
                    if (!uuids.contains(golemUuid)) {
                        event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "copper_golem_not_owner"));
                        return;
                    }
                    uuids.remove(golemUuid);
                    event.getPlayer().getPersistentDataContainer().set(COPPER_GOLEM_LIMIT_KEY, PersistentDataType.STRING, String.join(",", uuids));
                }

                copperGolem.setAware(false);
                if (interactSound != null)
                    copperGolem.getWorld().playSound(copperGolem.getLocation(),interactSound, SoundCategory.NEUTRAL,1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"copper_golem_interact_off"));

                event.setCancelled(true);
                return;
            }
        }
        else {
            if (possibleInteractMaterials.contains(event.getPlayer().getInventory().getItemInMainHand().getType().name())) {
                if (!event.getPlayer().hasPermission("vineriumcore.admin")) {
                    String unparsedUuids = event.getPlayer().getPersistentDataContainer().getOrDefault(COPPER_GOLEM_LIMIT_KEY, PersistentDataType.STRING, "");
                    List<String> uuids = !unparsedUuids.isEmpty() ? new ArrayList<>(Arrays.stream(unparsedUuids.split(",")).toList()) : new ArrayList<>();
                    uuids.removeIf(uuid -> Bukkit.getEntity(UUID.fromString(uuid)) == null);
                    if (uuids.size() == VineriumCore.inst().getConfig()
                            .getInt("Tweaks.CopperGolem.MaxPerPlayer", 2)) {
                        event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "copper_golem_limit_reached"));
                        return;
                    }
                    uuids.add(copperGolem.getUniqueId().toString());
                    event.getPlayer().getPersistentDataContainer().set(COPPER_GOLEM_LIMIT_KEY, PersistentDataType.STRING, String.join(",", uuids));
                }

                copperGolem.setAware(true);
                if (interactSound != null)
                    copperGolem.getWorld().playSound(copperGolem.getLocation(),interactSound,SoundCategory.NEUTRAL, 1f,1f);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"copper_golem_interact_on"));

                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onCopperChestBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().name().contains("COPPER_CHEST")) {
            Collection<Entity> nearbyEntities = event.getBlock().getLocation().getNearbyEntities(58,58,58);
            nearbyEntities.removeIf(entity -> entity.getType() != EntityType.COPPER_GOLEM);
            SortedMap<Double,Entity> distanceMap = new TreeMap<>();
            nearbyEntities.forEach(entity -> distanceMap.put(entity.getLocation().distanceSquared(event.getBlock().getLocation()),entity));
            if (distanceMap.isEmpty())
                return;
            Entity firstEntity = distanceMap.firstEntry().getValue();
            firstEntity.getLocation().createExplosion(1.0f,false,false);
            if (firstEntity.isValid())
                firstEntity.remove();
        }
    }

    @EventHandler
    public void onDisabledBlockDestroy(BlockDestroyEvent event) {
        if (event.isCancelled())
            return;
        String blockType = event.getBlock().getType().name().toUpperCase();
        if (VineriumCore.inst().getConfigManager().getDisabledDrops().contains(blockType)) {
            event.setWillDrop(false);
        }
    }

    @EventHandler
    public void onDisabledBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        String blockType = event.getBlock().getType().name().toUpperCase();
        if (VineriumCore.inst().getConfigManager().getDisabledDrops().contains(blockType)) {
            event.setDropItems(false);
        }
    }

}
