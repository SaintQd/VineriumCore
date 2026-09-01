package org.saintqd.vineriumcore.managers;

import com.destroystokyo.paper.ParticleBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ColorableArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.events.HighPingDetectedEvent;
import org.saintqd.vineriumcore.worldguard.Flags;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.*;
import java.util.function.Consumer;

public class PlayerManager {

    private Boolean pvpModeEnabled = false;

    private Boolean coloredHelmetTeamsEnabled = false;
    @Getter
    private String pvpPlaceholder = "";
    @Getter
    private final HashSet<Player> pvpModePlayers = new HashSet<>();
    @Getter
    private final HashMap<UUID,Long> knockoutPlayers = new HashMap<>();

    @Getter
    private final HashMap<String, HashMap<Player, ImmutablePair<String,Long>>> timers = new HashMap<>();

    @Getter
    private final HashMap<UUID,BukkitRunnable> inspectedPlayers = new HashMap<>();

    private BukkitTask pingAlertCheckTask = null;
    private BukkitTask phantomsToggleTask = null;
    private BukkitTask glideCheckTask = null;

    public void loadParams(Plugin plugin) {
        pvpModeEnabled = plugin.getConfig().getBoolean("PvPMode.Enabled");
        coloredHelmetTeamsEnabled = plugin.getConfig().getBoolean("Tweaks.ColoredHelmetTeams.Enabled",true);
        pvpPlaceholder = plugin.getConfig().getString("PvPMode.Placeholder");

        if (pingAlertCheckTask != null) {
            pingAlertCheckTask.cancel();
            pingAlertCheckTask = null;
        }
        if (plugin.getConfig().getBoolean("PingAlert.Enabled") && pingAlertCheckTask == null) {
            Bukkit.getScheduler().runTaskTimer(plugin,() -> {
                int pingPerPlayer = 0;
                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
                int average = 0;
                if (!onlinePlayers.isEmpty()) {
                    for (Player player : onlinePlayers)
                        pingPerPlayer += player.getPing();
                    average = pingPerPlayer / Bukkit.getOnlinePlayers().size();
                }
                if (average > plugin.getConfig().getDouble("PingAlert.AlertThreshold")) {
                    plugin.getLogger().warning(VineriumLib.inst().getLangManager().getLangLines().get(Key.key(
                            "vineriumcore:ping_alert_message")).replace("{1}", Integer.toString(average)));
                    HighPingDetectedEvent event = new HighPingDetectedEvent(average);
                    event.callEvent();
                }
            },plugin.getConfig().getLong("PingAlert.Period"), 1200L);
        }
        if (phantomsToggleTask != null) {
            phantomsToggleTask.cancel();
            phantomsToggleTask = null;
        }
        if (plugin.getConfig().getBoolean("Tweaks.PhantomsToggle.Enabled") && phantomsToggleTask == null) {
            Bukkit.getScheduler().runTaskTimer(plugin,() -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.permissionValue("vineriumcore.phantomsdisabled") == TriState.TRUE)
                        onlinePlayer.setStatistic(Statistic.TIME_SINCE_REST,0);
                }
            },plugin.getConfig().getLong("Tweaks.PhantomsToggle.Period"), 6000L);
        }
        if (glideCheckTask != null) {
            glideCheckTask.cancel();
            glideCheckTask = null;
        }
        if (plugin.getConfig().getLong("WorldGuardFlags.Glide.CheckTask",40L) > 0 && glideCheckTask == null && VineriumCore.inst().isWorldGuardEnabled()) {
            Bukkit.getScheduler().runTaskTimer(plugin,() -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
                    com.sk89q.worldguard.LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(onlinePlayer);
                    if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.GLIDE)) {
                        ItemStack chestplate = onlinePlayer.getInventory().getChestplate();
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            NamespacedKey key = new NamespacedKey(VineriumCore.inst(),VineriumCore.inst().getConfig()
                                    .getString("WorldGuardFlags.Glide.AllowedComponentName","glide_allow"));
                            if (chestplate.getPersistentDataContainer().has(key)) {
                                return;
                            }
                        }
                        onlinePlayer.setGliding(false);
                        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                            onlinePlayer.getWorld().strikeLightningEffect(onlinePlayer.getLocation());
                            onlinePlayer.playSound(onlinePlayer,Sound.ENTITY_ITEM_BREAK,SoundCategory.PLAYERS,1f,1f);
                            chestplate.setData(DataComponentTypes.DAMAGE,chestplate.getData(DataComponentTypes.MAX_DAMAGE));
                        }
                    }
                }
            },plugin.getConfig().getLong("WorldGuardFlags.Glide.CheckTask"), 40L);
        }
    }

    public Boolean isPvpModeEnabled() {
        return pvpModeEnabled;
    }

    public boolean checkTeamPvP(Player firstPlayer, Player secondPlayer) {
        if (!coloredHelmetTeamsEnabled)
            return true;
        ItemStack firstPlayerHelmet = firstPlayer.getInventory().getHelmet();
        if (firstPlayerHelmet == null || !firstPlayerHelmet.hasItemMeta() || firstPlayerHelmet.getType() != Material.LEATHER_HELMET)
            return true;
        ColorableArmorMeta armorMeta = (ColorableArmorMeta) firstPlayerHelmet.getItemMeta();
        Color firstColor = armorMeta.getColor();
        if (firstColor == Bukkit.getServer().getItemFactory().getDefaultLeatherColor())
            return true;

        ItemStack secondPlayerHelmet = secondPlayer.getInventory().getHelmet();
        if (secondPlayerHelmet == null || !secondPlayerHelmet.hasItemMeta() || secondPlayerHelmet.getType() != Material.LEATHER_HELMET)
            return true;
        armorMeta = (ColorableArmorMeta) secondPlayerHelmet.getItemMeta();
        Color secondColor = armorMeta.getColor();
        if (secondColor == Bukkit.getServer().getItemFactory().getDefaultLeatherColor())
            return true;

        return !firstColor.equals(secondColor);
    }

    public void applyKnockout(Player player, Player killerPlayer) {
        UUID uuid = player.getUniqueId();
        knockoutPlayers.put(uuid, VinUtils.getCurrentTick());
        int period = VineriumCore.inst().getConfig().getInt("Tweaks.DeathKnockout.KnockoutPeriod",1200);
        long originalTimeInSeconds = period / 20;
        Location originalLocation = player.getLocation();
        player.showTitle(Title.title(
                VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_title"),
                VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_subtitle",Long.toString(originalTimeInSeconds)),
                Title.Times.times(Ticks.duration(10),Ticks.duration(30),Ticks.duration(20))
                ));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,period,0,false,false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,period,200,false,false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,period,0,false,false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,period,200,false,false));

        String nicknameFormat = VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", player.getName());
        String playerNicknameFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player, PlaceholderAPI.setPlaceholders(player,nicknameFormat))
                : nicknameFormat;
        String killerNicknameFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player, PlaceholderAPI.setPlaceholders(killerPlayer,nicknameFormat))
                : nicknameFormat;

        Audience audience = Audience.audience(player.getNearbyEntities(25,25,25).stream()
                .filter(entity -> entity instanceof Player).toList());
        player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_message",
                playerNicknameFormat, killerNicknameFormat, Long.toString(originalTimeInSeconds)));
        audience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_message",
                playerNicknameFormat, killerNicknameFormat, Long.toString(originalTimeInSeconds)));

        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.DUST);
        particleBuilder.color(Color.RED);
        particleBuilder.count(25);
        particleBuilder.offset(0.5,0.5,0.5);

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTick = VinUtils.getCurrentTick();
                long timeUntilRestore = knockoutPlayers.getOrDefault(uuid,VinUtils.getCurrentTick()) + period - currentTick;
                if (timeUntilRestore > 0 && knockoutPlayers.containsKey(uuid)) {
                    Player playerByUuid = Bukkit.getPlayer(uuid);
                    if (playerByUuid != null && playerByUuid.isValid()) {
                        long timeInSeconds = timeUntilRestore / 20;
                        playerByUuid.showTitle(Title.title(
                                VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_title"),
                                VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_subtitle", Long.toString(timeInSeconds)),
                                Title.Times.times(Ticks.duration(0), Ticks.duration(30), Ticks.duration(20))
                        ));
                        playerByUuid.setFallDistance(0.0f);
                        playerByUuid.teleport(originalLocation);
                        particleBuilder.location(playerByUuid.getLocation().add(0,0.5,0));
                        particleBuilder.receivers(25);
                        particleBuilder.spawn();
                    }
                }
                else {
                    knockoutPlayers.remove(uuid);
                    Player playerByUuid = Bukkit.getPlayer(uuid);
                    if (playerByUuid != null && playerByUuid.isValid()) {
                        playerByUuid.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                        playerByUuid.removePotionEffect(PotionEffectType.SLOWNESS);
                        playerByUuid.removePotionEffect(PotionEffectType.BLINDNESS);
                        playerByUuid.removePotionEffect(PotionEffectType.JUMP_BOOST);

                        Audience newAudience = Audience.audience(playerByUuid.getNearbyEntities(25,25,25).stream()
                                .filter(entity -> entity instanceof Player).toList());
                        playerByUuid.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_message_restore",
                                playerNicknameFormat,Long.toString(originalTimeInSeconds)));
                        newAudience.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "death_knockout_message_restore",
                                playerNicknameFormat,Long.toString(originalTimeInSeconds)));
                    }
                    this.cancel();
                }
            }
        };
        runnable.runTaskTimer(VineriumCore.inst(), 20, 20);
    }
}
