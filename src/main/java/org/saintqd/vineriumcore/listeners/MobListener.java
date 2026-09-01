package org.saintqd.vineriumcore.listeners;

import net.kyori.adventure.util.TriState;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.saintqd.vineriumcore.VineriumCore;

public class MobListener implements Listener {

    @EventHandler
    public void onEntityTargetLivingEntity(@NotNull EntityTargetLivingEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PHANTOM) return;
        final LivingEntity target = event.getTarget();
        if (target instanceof Player player && player.permissionValue("vineriumcore.phantomsdisabled") == TriState.TRUE) {
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.PhantomsToggle.Enabled",true))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        // Phantom attacking Player
        if (event.getDamager().getType() == EntityType.PHANTOM && event.getEntity() instanceof Player player
                && player.permissionValue("vineriumcore.phantomsdisabled") == TriState.TRUE
                && VineriumCore.inst().getConfig().getBoolean("Tweaks.PhantomsToggle.Enabled",true)) {
            event.setCancelled(true);
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.PhantomsToggle.RemovePhantomOnDamage",true))
                event.getDamager().remove();
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        // Reset statistic
        final Player player = event.getPlayer();
        if (player.permissionValue("vineriumcore.phantomsdisabled") == TriState.TRUE
                && VineriumCore.inst().getConfig().getBoolean("Tweaks.PhantomsToggle.Enabled",true))
            player.setStatistic(Statistic.TIME_SINCE_REST,0);
    }
}
