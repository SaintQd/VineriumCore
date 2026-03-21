package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Containers.CMIVanish;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.event.player.CartographyItemEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.*;
import org.saintqd.vineriumcore.worldguard.Flags;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerListener implements Listener {

    private final HashMap<AnvilInventory, Integer> realMaxRepairCosts = new HashMap<>();

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (!VineriumCore.inst().getConfig().getBoolean("PlayerLimit.Enabled"))
            return;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(event.getUniqueId());
        if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null) {
            if (VineriumLib.inst().getVaultManager().getPermissionProvider().playerHas(null,offlinePlayer,"vineriumcore.bypasslimit")) {
                return;
            }
        }
        if (Bukkit.getOnlinePlayers().size() + 1 > VineriumCore.inst().getConfig().getInt("PlayerLimit.Limit",100)) {
            event.kickMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"kick_server_is_full"));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_FULL);
        }
    }

    @EventHandler
    public void onPlayerJoinMessage(final PlayerJoinEvent event) {
        VineriumCore.inst().getSuffixManager().checkSuffixPermission(event.getPlayer());

        int hintIndex = VineriumCore.inst().getConfig().getInt("StarterHints.Join",-2);
        if (hintIndex > -2 && event.getPlayer().permissionValue("vineriumcore.disablehints") != TriState.TRUE) {
            long maxPlaytime = VineriumCore.inst().getConfig().getLong("StarterHints.MaxPlaytime",18000000);
            boolean playTimeCheck = true;
            CMIUser user = CMI.getInstance().getPlayerManager().getUser(event.getPlayer());
            if (user.getTotalPlayTime() > maxPlaytime)
                playTimeCheck = false;
            if (playTimeCheck) {
                if (hintIndex == -1 || hintIndex >= VineriumCore.inst().getHintManager().getHints().size())
                    VineriumCore.inst().getHintManager().sendStarterHint(Audience.audience(event.getPlayer()));
                else
                    VineriumCore.inst().getHintManager().sendStarterHint(Audience.audience(event.getPlayer()), hintIndex);
            }
        }
        MailbookManager mailbookManager = MailbookManager.INSTANCE;
        if (mailbookManager.getUnreadMailbooks().containsKey(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"mailbook_receive_message"));
        }

        if (CalendarEventsManager.Companion.getInstance().getCalendar().containsKey(LocalDate.now().getDayOfYear())) {
            List<String> currentEventsList = CalendarEventsManager.Companion.getInstance().getCalendar().get(LocalDate.now().getDayOfYear());
            event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"calendar_event_message_format"));
            for (String eventName : currentEventsList) {
                CalendarEventsManager.VinCalendarEvent timedEvent = CalendarEventsManager.Companion.getInstance().getEvents().get(eventName);
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().
                        parseLangString(VineriumCore.inst(),"calendar_event_message_list_format",timedEvent.getDisplayName()));
            }
            event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"calendar_event_message_hint_format"));
        }

        if (event.getPlayer().permissionValue("vineriumcore.pvpenabled") == TriState.TRUE) {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            playerManager.getPvpModePlayers().add(event.getPlayer());
        }

        if (VineriumCore.inst().getConfig().getBoolean("Messages.Enabled")) {
            event.joinMessage(null);
            String joinMessage = null;
            String joinMessageFormat = null;
            if (event.getPlayer().hasPlayedBefore()) {
                if (event.getPlayer().permissionValue("vineriumcore.hidejoinmessage") == TriState.TRUE)
                    return;
                // Фикс скрытия сообщений входа/выхода для системы ваниша в CMI
                if (VineriumCore.inst().isCMIEnabled()) {
                    CMIVanish vanish = CMI.getInstance().getVanishManager().getVanish(event.getPlayer().getUniqueId());
                    if (vanish != null && vanish.getState(VanishAction.isVanished).is() && !vanish.getState(VanishAction.informOnJoin).is())
                        return;
                }
                List<String> possibleJoinMessage = event.getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                        .filter(permission -> permission.startsWith("meta.join-message.")).toList();
                if (!possibleJoinMessage.isEmpty()
                        && event.getPlayer().hasPermission("vineriumcore.joinmessage")
                        && VineriumCore.inst().getConfig().getBoolean("Messages.Join.Enabled")) {
                    String joinMessagePermission = possibleJoinMessage.getFirst();
                    joinMessage = joinMessagePermission.replace("meta.join-message.", "").replace("\"", "");
                    joinMessage = Pattern.compile("╝+(.)?").matcher(joinMessage).replaceAll(mr -> mr.group(1).toUpperCase());
                    joinMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Join.Format", "<white>>> <gray>[message]");
                } else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultJoin.Enabled")) {
                    joinMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultJoin.Format", null);
                    joinMessageFormat = joinMessage;
                }
            } else if (VineriumCore.inst().getConfig().getBoolean("Messages.FirstJoin.Enabled")) {
                joinMessage = VineriumCore.inst().getConfig().getString("Messages.FirstJoin.Format", null);
                joinMessageFormat = joinMessage;
            }
            if (joinMessage == null || joinMessage.isEmpty())
                return;
            joinMessageFormat = joinMessageFormat.replace("[message]", joinMessage).replace("[dot]", ".");
            joinMessageFormat = joinMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
            // Обрабатываем плейсхолдеры дважды, т.к. после первой обработки могут остаться вложенные плейсхолдеры
            joinMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                    ? PlaceholderAPI.setPlaceholders(event.getPlayer(), PlaceholderAPI.setPlaceholders(event.getPlayer(), joinMessageFormat))
                    : joinMessageFormat;
            event.joinMessage(VinUtils.parseString(joinMessageFormat));
        }
    }

    @EventHandler
    public void onPlayerQuitMessage(PlayerQuitEvent event) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;

        PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
        if (playerManager.getPvpModePlayers().contains(event.getPlayer())) {
            vaultManager.getPermissionProvider().playerAdd(null,event.getPlayer(),"vineriumcore.pvpenabled");
            playerManager.getPvpModePlayers().remove(event.getPlayer());
        }
        else {
            vaultManager.getPermissionProvider().playerRemove(null,event.getPlayer(),"vineriumcore.pvpenabled");
        }

        if (VineriumCore.inst().getConfig().getBoolean("Messages.Enabled")) {
            event.quitMessage(null);
            if (event.getPlayer().permissionValue("vineriumcore.hideleavemessage") == TriState.TRUE)
                return;
            // Фикс скрытия сообщений входа/выхода для системы ваниша в CMI
            if (VineriumCore.inst().isCMIEnabled()) {
                CMIVanish vanish = CMI.getInstance().getVanishManager().getVanish(event.getPlayer().getUniqueId());
                if (vanish != null && vanish.getState(VanishAction.isVanished).is() && !vanish.getState(VanishAction.informOnLeave).is())
                    return;
            }
            List<String> possibleLeaveMessage = event.getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                    .filter(permission -> permission.startsWith("meta.leave-message.")).toList();
            String leaveMessage = null;
            String leaveMessageFormat = null;
            if (!possibleLeaveMessage.isEmpty()
                    && event.getPlayer().hasPermission("vineriumcore.leavemessage")
                    && VineriumCore.inst().getConfig().getBoolean("Messages.Leave.Enabled")) {
                String leaveMessagePermission = possibleLeaveMessage.getFirst();
                leaveMessage = leaveMessagePermission.replace("meta.leave-message.", "").replace("\"", "");
                leaveMessage = Pattern.compile("╝+(.)?").matcher(leaveMessage).replaceAll(mr -> mr.group(1).toUpperCase());
                leaveMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Leave.Format", "<white<<< <gray>[message]");
            } else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultLeave.Enabled")) {
                leaveMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultLeave.Format", null);
                leaveMessageFormat = leaveMessage;
            }
            if (leaveMessage == null || leaveMessage.isEmpty())
                return;
            leaveMessageFormat = leaveMessageFormat.replace("[message]", leaveMessage).replace("[dot]", ".");
            leaveMessageFormat = leaveMessageFormat.replace("*", VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName()));
            leaveMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                    ? PlaceholderAPI.setPlaceholders(event.getPlayer(), PlaceholderAPI.setPlaceholders(event.getPlayer(), leaveMessageFormat))
                    : leaveMessageFormat;
            event.quitMessage(VinUtils.parseString(leaveMessageFormat));
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof VinGUIHolder)) return;
        for (int slot : event.getRawSlots())
            if (slot <= event.getInventory().getSize() - 1)
                event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player interactedPlayer) {
            if (!VineriumCore.inst().getConfig().getBoolean("Messages.RightClickNickname.Enabled")) return;
            if (VineriumCore.inst().getConfig().getBoolean("Messages.RightClickNickname.SneakingAllowed")
                    && event.getPlayer().isSneaking()) return;
            if (!(interactedPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
            && VineriumCore.inst().getConfig().getBoolean("Messages.RightClickNickname.HideInvisible"))
            || VineriumCore.inst().getConfig().getStringList("Messages.RightClickNickname.AlwaysShowGamemodes")
                    .contains(event.getPlayer().getGameMode().name())
            || event.getPlayer().hasPermission("vineriumcore.rightclicknickname.alwaysshow")) {
                String messageFormat = VineriumCore.inst().getConfig().getString("Messages.NicknameFormat", event.getPlayer().getName());
                messageFormat = (VineriumCore.inst().getPlaceholders() != null)
                        ? PlaceholderAPI.setPlaceholders(interactedPlayer, PlaceholderAPI.setPlaceholders(interactedPlayer,messageFormat))
                        : messageFormat;
                event.getPlayer().sendActionBar(VinUtils.parseString(messageFormat));
            }
        }
        if (event.getRightClicked() instanceof Breedable breedable) {
            if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.AgeLock.Enabled",true)) return;
            if (breedable.getAgeLock() || breedable.isAdult()) return;
            ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
            if (handItem.getType() !=
                    Material.valueOf(VineriumCore.inst().getConfig().getString("Tweaks.AgeLock.Item",Material.POISONOUS_POTATO.name()))) return;
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.AgeLock.RemoveItem",true) && event.getPlayer().getGameMode() != GameMode.CREATIVE)
                handItem.setAmount(handItem.getAmount() - 1);
            breedable.setAgeLock(true);
            breedable.addPotionEffect(new PotionEffect(PotionEffectType.POISON,40,1,false,true));
        }
    }

    @EventHandler
    public void onPlayerBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() != null
                && (event.getClickedBlock().getType() == Material.CHIPPED_ANVIL || event.getClickedBlock().getType() == Material.DAMAGED_ANVIL)
                && VineriumCore.inst().getConfig().getBoolean("Tweaks.AnvilRepair.Enabled",true)) {
            Material repairMaterial = Material.valueOf(VineriumCore.inst().getConfig().getString("Tweaks.AnvilRepair.RepairMaterial","IRON_BLOCK"));
            @Subst("block.chain.hit") String repairSound = VineriumCore.inst().getConfig()
                    .getString("Tweaks.AnvilRepair.RepairSound", null);
            if (!event.getPlayer().hasPermission("vineriumcore.anvilrepair")) return;
            ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
            if (mainHandItem.getType() != repairMaterial) return;
            if (event.getClickedBlock().getType() == Material.DAMAGED_ANVIL) {
                event.getClickedBlock().setType(Material.CHIPPED_ANVIL);
            } else {
                event.getClickedBlock().setType(Material.ANVIL);
            }
            if (repairSound != null)
                event.getClickedBlock().getWorld().playSound(event.getClickedBlock().getLocation(),repairSound,SoundCategory.BLOCKS,1f,1f);
            mainHandItem.setAmount(mainHandItem.getAmount() - 1);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return; // Если урон уже был отменён (К примеру флагами WorldGuard), не обрабатываем

        Entity damagerEntity = event.getDamager();

        // Молния это CraftItem, следовательно Entity, но не LivingEntity, так что её не обрабатываем
        if (event.getCause().equals(EntityDamageEvent.DamageCause.LIGHTNING)) {
            return;
        }

        if (event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            Projectile projectile = (Projectile) event.getDamager();
            // Если источник выстрела - не сущность, не совершаем проверки
            if (!(projectile.getShooter() instanceof Entity)) {
                return;
            }
            damagerEntity = (Entity) projectile.getShooter();
        }

        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(damagerEntity instanceof LivingEntity)) return;


        if (event.getEntity() instanceof Player entityPlayer && damagerEntity instanceof Player damagerPlayer) {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            if (!playerManager.isPvpModeEnabled())
                return;
            if (damagerPlayer.hasPermission("vineriumcore.admin"))
                return;
            if (!playerManager.getPvpModePlayers().contains(entityPlayer)) {
                event.setCancelled(true);
                HashMap<Player, ImmutablePair<String,Long>> timers = playerManager.getTimers().getOrDefault("entity_not_enabled_pvp",new HashMap<>());
                ImmutablePair<String,Long> damagerVariable = timers.getOrDefault(damagerPlayer,new ImmutablePair<>(null,0L));
                if (damagerVariable.getRight() < VinUtils.getCurrentTick()) {
                    damagerVariable = new ImmutablePair<>(null,VinUtils.getCurrentTick() +
                            VineriumCore.inst().getConfig().getLong("TimersCooldown.entity_not_enabled_pvp",200L));
                    timers.put(damagerPlayer,damagerVariable);
                    playerManager.getTimers().put("entity_not_enabled_pvp",timers);
                    damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "entity_not_enabled_pvp", entityPlayer.getName()));
                }
                return;
            }
            if (!playerManager.getPvpModePlayers().contains(damagerPlayer)) {
                event.setCancelled(true);
                HashMap<Player, ImmutablePair<String,Long>> timers = playerManager.getTimers().getOrDefault("damager_not_enabled_pvp",new HashMap<>());
                ImmutablePair<String,Long> damagerVariable = timers.getOrDefault(damagerPlayer,new ImmutablePair<>(null,0L));
                if (damagerVariable.getRight() < VinUtils.getCurrentTick()) {
                    damagerVariable = new ImmutablePair<>(null,VinUtils.getCurrentTick() +
                            VineriumCore.inst().getConfig().getLong("TimersCooldown.damager_not_enabled_pvp",200L));
                    timers.put(damagerPlayer,damagerVariable);
                    playerManager.getTimers().put("damager_not_enabled_pvp",timers);
                    damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "damager_not_enabled_pvp", entityPlayer.getName()));
                }
                return;
            }
        }
    }

    // По какой-то причине у игрока во всех режимах кроме креатива скорость полёта устанавливается на 0
    //  фиксим это с помощью штуки ниже
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        if ((event.getPlayer().getGameMode() == GameMode.ADVENTURE || event.getPlayer().getGameMode() == GameMode.SURVIVAL)
        && event.getNewGameMode() == GameMode.SPECTATOR)
            event.getPlayer().setFlySpeed(0.1f);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void anvilEvent(PrepareAnvilEvent event) {
        String renameText = event.getView().getRenameText();
        if (renameText != null && !renameText.isEmpty()
                && VineriumCore.inst().getConfig().getBoolean("CommandFilter.Enabled") && !event.getView().getPlayer().hasPermission("vineriumcore.commandfilter.bypass")) {
            for (String regex : VineriumCore.inst().getConfig().getStringList("CommandFilter.Regex")) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(renameText);
                if (matcher.find()) {
                    event.setResult(null);
                    return;
                }
            }
        }
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.RepairCostFix.Enabled")) return;
        if (event.getView().getMaximumRepairCost() != Integer.MAX_VALUE)
            realMaxRepairCosts.put(event.getInventory(),event.getView().getMaximumRepairCost());

        if (event.getView().getRepairCost() < realMaxRepairCosts.getOrDefault(event.getInventory(),0))
        {
            Integer repairCost = realMaxRepairCosts.remove(event.getInventory());
            if (repairCost != null)
                event.getView().setMaximumRepairCost(repairCost);
            return;
        }

        event.getView().setMaximumRepairCost(Integer.MAX_VALUE);
        event.getView().setRepairCost(
                VineriumCore.inst().getConfig().getBoolean("Tweaks.RepairCostFix.Force",false)
                ? 39
                : realMaxRepairCosts.getOrDefault(event.getInventory(),2) - 1);
    }

    @EventHandler
    public void onPlayerGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.GLIDE)) {
                player.setGliding(false);
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    player.playSound(player,Sound.ENTITY_ITEM_BREAK,SoundCategory.PLAYERS,1f,1f);
                    chestplate.setData(DataComponentTypes.DAMAGE,chestplate.getData(DataComponentTypes.MAX_DAMAGE));
                }
                event.setCancelled(true);
            }
        }
    }

    private final HashMap<UUID,Set<Long>> deathCounter = new HashMap<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.HideDeathMessages.Enabled",true)) {
            long period = VineriumCore.inst().getConfig().getLong("Tweaks.HideDeathMessages.Period",12000);
            int minDeaths = VineriumCore.inst().getConfig().getInt("Tweaks.HideDeathMessages.MinDeaths",3);
            Set<Long> deathCounterPerPlayer = deathCounter.getOrDefault(event.getEntity().getUniqueId(),new HashSet<>());
            deathCounterPerPlayer.removeIf( time -> time + period < VinUtils.getCurrentTick());
            deathCounterPerPlayer.add(VinUtils.getCurrentTick());
            if (deathCounterPerPlayer.size() > minDeaths)
                event.setShowDeathMessages(false);
            deathCounter.put(event.getEntity().getUniqueId(),deathCounterPerPlayer);
        }

        if (!event.getShowDeathMessages()) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Messages.Death.Enabled",true)) return;

        Component originalDeathMessage = event.deathMessage();
        if (originalDeathMessage == null) return;
        String serializedMessage = MiniMessage.miniMessage().serialize(originalDeathMessage);
        String prefix = VineriumCore.inst().getConfig().getString("Messages.Death.Format","");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (serializedMessage.contains(">"+onlinePlayer.getName()+"\":")) {
                String playerName = VineriumCore.inst().getConfig().getString("Messages.Death.PlayerName","*")
                        .replace("*",onlinePlayer.getName());
                playerName = VineriumLib.inst().isPlaceholderAPIEnabled()
                        ? PlaceholderAPI.setPlaceholders(onlinePlayer,PlaceholderAPI.setPlaceholders(onlinePlayer,playerName))
                        : playerName;
                serializedMessage = serializedMessage.replace(">"+onlinePlayer.getName()+"\":",">"+playerName+"\":");
            }
        }
        serializedMessage = prefix.replace("*",serializedMessage);

        event.deathMessage(VinUtils.parseString(serializedMessage));
    }

    @EventHandler
    public void onAdvancementReceive(PlayerAdvancementDoneEvent event) {
        if (Boolean.FALSE.equals(event.getPlayer().getWorld().getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES))) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Messages.Advancement.Enabled",true)) return;

        List<String> showTypes = VineriumCore.inst().getConfig().getStringList("Messages.Advancement.ShowTypes");
        if (!VineriumCore.inst().getConfig().getStringList("Messages.Advancement.AlwaysShowNamespaces")
                .contains(event.getAdvancement().key().namespace())) {
            if (event.getAdvancement().getDisplay() != null &&
                    !showTypes.contains(event.getAdvancement().getDisplay().frame().name())) {
                event.message(null);
                return;
            }
        }

        Component originalMessage = event.message();
        if (originalMessage == null) return;
        String serializedMessage = MiniMessage.miniMessage().serialize(originalMessage);
        String prefix = VineriumCore.inst().getConfig().getString("Messages.Advancement.Format","");

        String playerName = VineriumCore.inst().getConfig().getString("Messages.Advancement.PlayerName","*")
                .replace("*",event.getPlayer().getName());
        playerName = VineriumLib.inst().isPlaceholderAPIEnabled()
                ? PlaceholderAPI.setPlaceholders(event.getPlayer(),PlaceholderAPI.setPlaceholders(event.getPlayer(),playerName))
                : playerName;
        serializedMessage = serializedMessage.replace(">"+event.getPlayer().getName()+"\":",">"+playerName+"\":");
        serializedMessage = prefix.replace("*",serializedMessage);
        event.message(VinUtils.parseString(serializedMessage));
    }

    /*@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinDynamic(PlayerJoinEvent event) {
        VineriumCore.inst().getDynamicParamsManager().updateWorldCaps(Bukkit.getOnlinePlayers().size());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitDynamic(PlayerQuitEvent event) {
        VineriumCore.inst().getDynamicParamsManager().updateWorldCaps(Bukkit.getOnlinePlayers().size() - 1);
    }*/

    @EventHandler
    public void onGrindstoneClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof GrindstoneInventory grindstoneInventory)) return;
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.GrindstoneDisenchant",true)) return;
        if (!event.getWhoClicked().hasPermission("vineriumcore.grindstonedisenchant")) return;
        ItemStack itemOnCursor = event.getWhoClicked().getItemOnCursor();
        if (event.getSlot() == 1 && itemOnCursor.getType() == Material.BOOK && grindstoneInventory.getLowerItem() == null) {
            grindstoneInventory.setLowerItem(itemOnCursor.clone());
            grindstoneInventory.getLowerItem().setAmount(1);
            event.getWhoClicked().getItemOnCursor().setAmount(itemOnCursor.getAmount() - 1);
            if (grindstoneInventory.getUpperItem() != null && grindstoneInventory.getResult() == null) {
                ItemStack bookItem = ItemStack.of(Material.ENCHANTED_BOOK);
                ItemStack upperItem = grindstoneInventory.getUpperItem();
                ItemEnchantments itemEnchantments = upperItem.getData(DataComponentTypes.ENCHANTMENTS);
                if (itemEnchantments.enchantments().isEmpty())
                    return;
                bookItem.setData(DataComponentTypes.STORED_ENCHANTMENTS,itemEnchantments);
                grindstoneInventory.setResult(bookItem);
            }
        }
    }

    @EventHandler
    public void onPlayerOreBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        if (!VineriumCore.inst().getConfig().getBoolean("OreAlerts.Enabled"))
            return;
        if (VineriumCore.inst().getOreManager().getThresholdMaterials().containsKey(event.getBlock().getType())) {
            if (event.getPlayer().permissionValue("vineriumcore.orealerts.bypass") == TriState.TRUE)
                return;
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE || event.getPlayer().getGameMode() == GameMode.SPECTATOR)
                return;
            Location blockLoc = event.getBlock().getLocation();
            if (VineriumCore.inst().getOreManager().getCheckedLocations().containsKey(blockLoc)
                || VineriumCore.inst().getOreManager().getPlacedOres().contains(blockLoc))
                return;
            VineriumCore.inst().getOreManager().getPlacedOres().remove(blockLoc);
            //String stringLoc = blockLoc.getWorld().getName()+","+blockLoc.x()+","+blockLoc.y()+","+blockLoc.z();
            //if (VineriumCore.inst().getOreManager().getCheckedLocations().containsKey(stringLoc))
            //    return;

            long currentTick = VinUtils.getCurrentTick();
            long timeToThreshold = VineriumCore.inst().getConfig().getLong("OreAlerts.TimeToThreshold",12000);
            OreManager.BlockCounter blockCounter = new OreManager.BlockCounter();
            Set<Location> blockLocations = blockCounter.getNearBlocks(event.getBlock().getLocation(),event.getBlock().getType());
            if (blockLocations.isEmpty())
                return;
            for (Location location : blockLocations)
                VineriumCore.inst().getOreManager().getCheckedLocations().put(location,currentTick);
            List<OreManager.OreData> oreData = VineriumCore.inst().getOreManager().getPlayerOreData().getOrDefault(event.getPlayer().getName(),new ArrayList<>());
            oreData.add(new OreManager.OreData(event.getBlock().getType(), blockLocations.size()));
            int currentMaterialAmount = 0;
            long oldestData = Long.MAX_VALUE;
            oreData.removeIf(data -> data.getTimestamp() + timeToThreshold < currentTick);
            for (OreManager.OreData data : oreData) {
                currentMaterialAmount += data.getAmount();
                if (data.getTimestamp() < oldestData)
                    oldestData = data.getTimestamp();
            }
            VineriumCore.inst().getOreManager().getPlayerOreData().put(event.getPlayer().getName(),oreData);
            List<Player> alertedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("vineriumcore.orealerts.show")).toList());
            String parsedOreName = VineriumCore.inst().getOreManager()
                    .getThresholdMaterials().get(event.getBlock().getType()).getRight() + "<lang:" + event.getBlock().getType().translationKey() + ">";
            String hoverCommand = VineriumCore.inst().getConfig().getString("OreAlerts.HoverCommand","tp {1}")
                    .replace("{1}",event.getPlayer().getName());
            String hoverText = "<click:run_command:\"/" + hoverCommand +"\"><hover:show_text:'" +VineriumLib.inst().getLangManager().getLangLines()
                    .get(Key.key("vineriumcore:ore_alert_hover_tooltip"))+ "'>" +VineriumLib.inst().getLangManager().getLangLines().get(Key.key("vineriumcore:ore_alert_hover"))+"</hover></click>";
            Component smallAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_small",
                    event.getPlayer().getName(),Integer.toString(blockLocations.size()),parsedOreName,hoverText);
            for (Player player : alertedPlayers) {
                if (player.permissionValue("vineriumcore.orealerts.disablesmall") != TriState.TRUE) {
                    player.sendMessage(smallAlertComponent);
                    if (player.permissionValue("vineriumcore.orealerts.disablesmallsound") != TriState.TRUE) {
                        player.playSound(VineriumCore.inst().getOreManager().getAlertSound(),player);
                    }
                }
            }
            if (currentMaterialAmount >= VineriumCore.inst().getOreManager().getThresholdMaterials().get(event.getBlock().getType()).getLeft()) {
                long timeRange = currentTick - oldestData;
                // Перевод в минуты. Всегда отображается минимум 1 минута
                timeRange = timeRange / 20 / 60;
                if (timeRange <= 0)
                    timeRange = 1;
                Component largeAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_large",
                        event.getPlayer().getName(),Integer.toString(currentMaterialAmount),parsedOreName,Long.toString(timeRange),hoverText);
                for (Player player : alertedPlayers) {
                    if (player.permissionValue("vineriumcore.orealerts.disablelarge") != TriState.TRUE) {
                        player.sendMessage(largeAlertComponent);
                        if (player.permissionValue("vineriumcore.orealerts.disablelargesound") != TriState.TRUE) {
                            player.playSound(VineriumCore.inst().getOreManager().getAlertSound(),player);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onOreBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        if (!VineriumCore.inst().getConfig().getBoolean("OreAlerts.Enabled"))
            return;
        if (VineriumCore.inst().getOreManager().getThresholdMaterials().containsKey(event.getBlock().getType())) {
            Location blockLoc = event.getBlock().getLocation();
            //String stringLoc = blockLoc.getWorld().getName()+","+blockLoc.x()+","+blockLoc.y()+","+blockLoc.z();
            VineriumCore.inst().getOreManager().getCheckedLocations().remove(blockLoc);
            VineriumCore.inst().getOreManager().getPlacedOres().add(blockLoc);
        }
    }

    @EventHandler
    public void onPlayerPortalTeleport(PlayerPortalEvent event) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(event.getPlayer());
        switch (event.getCause()) {
            case NETHER_PORTAL -> {
                if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.NETHER_PORTAL_TELEPORT))
                    event.setCancelled(true);
            }
            case END_PORTAL -> {
                if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.END_PORTAL_TELEPORT))
                    event.setCancelled(true);
            }
        }
        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.PortalRedirect.Enabled")) {
            if (VineriumCore.inst().getConfig().contains("Tweaks.PortalRedirect.Worlds." + event.getFrom().getWorld().getName())) {
                event.setCancelled(true);
                for (String command : VineriumCore.inst().getConfig().getStringList("Tweaks.PortalRedirect.Worlds." + event.getFrom().getWorld().getName() + ".Commands")) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", event.getPlayer().getName()));
                }
            }
        }
        if (VineriumCore.inst().getConfig().getBoolean("CustomPortals.Enabled") && event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            HashMap<Material,Integer> foundCornerBlocks = ConfigManager.PortalCornerBlocksFinder.findCornerBlocks(event.getFrom().toBlockLocation());
            for (Material foundBlockType : foundCornerBlocks.keySet()) {
                if (VineriumCore.inst().getConfig().contains("CustomPortals.List."+foundBlockType.name())) {
                    int requiredAmount = VineriumCore.inst().getConfig().getInt("CustomPortals.List."+foundBlockType.name()+".Amount",1);
                    if (foundCornerBlocks.get(foundBlockType) >= requiredAmount) {
                        event.setCancelled(true);
                        for (String command : VineriumCore.inst().getConfig().getStringList("CustomPortals.List."+foundBlockType.name()+".Commands")) {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player_name%", event.getPlayer().getName()));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityPortalTeleport(EntityPortalEvent event) {
        if (event.getEntity() instanceof Player)
            return;
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.EntityPortalBlock.Enabled"))
            return;
        if ((event.getPortalType() == PortalType.NETHER && VineriumCore.inst().getConfig().getBoolean("Tweaks.EntityPortalBlock.EnabledNether"))
            || (event.getPortalType() == PortalType.ENDER && VineriumCore.inst().getConfig().getBoolean("Tweaks.EntityPortalBlock.EnabledEnd"))) {
            event.setCancelled(true);
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.EntityPortalBlock.Remove"))
                event.getEntity().remove();
        }
    }

    @EventHandler
    public void onPlayerMaceDamage(EntityDamageByEntityEvent event) {
        if (!VineriumCore.inst().getConfig().getBoolean("Tweaks.MaceDenier.Enabled"))
            return;
        if (event.getDamager() instanceof Player player && player.getInventory().getItemInMainHand().getType() == Material.MACE) {
            Component entityCustomNameComponent = event.getEntity().customName();
            if (entityCustomNameComponent != null) {
                String name = PlainTextComponentSerializer.plainText().serialize(entityCustomNameComponent);
                if (VineriumCore.inst().getConfigManager().getMaceDenierCustomNames().contains(name))
                    event.setDamage(1);
            }
        }
    }

    @EventHandler
    public void onPlayerCraft(PrepareItemCraftEvent event) {
        ItemStack originalResultItem = event.getInventory().getResult();
        if (originalResultItem != null) {
            if (VineriumCore.inst().getConfigManager().getItemLockMaterials().contains(originalResultItem.getType().name())) {
                for (ItemStack materialItem : event.getInventory().getMatrix()) {
                    if (materialItem != null && materialItem.getPersistentDataContainer().has(ConfigManager.getLockKey())) {
                        event.getInventory().setResult(null);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCartographyTableCraft(CartographyItemEvent event) {
        if (event.getInventory().getResult() != null && VineriumCore.inst().getConfigManager()
                .getItemLockMaterials().contains(event.getInventory().getResult().getType().name())) {
            ItemStack mapItem = event.getInventory().getResult();
            if (mapItem.getPersistentDataContainer().has(ConfigManager.getLockKey()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandExecute(PlayerCommandPreprocessEvent event) {
        if (VineriumCore.inst().getConfig().getBoolean("CommandFilter.Enabled") && !event.getPlayer().hasPermission("vineriumcore.commandfilter.bypass")) {
            for (String regex : VineriumCore.inst().getConfig().getStringList("CommandFilter.Regex")) {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(event.getMessage());
                if (matcher.find()) {
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "command_filter_message"));
                    event.setCancelled(true);
                    return;
                }
            }
        }
        ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
        if (handItem.getType() != Material.AIR && handItem.getPersistentDataContainer().has(ConfigManager.getLockKey())) {
            List<String> blockedCommands = VineriumCore.inst().getConfig().getStringList("Tweaks.ItemLock.BlockedCommands");
            for (String command : blockedCommands) {
                if (event.getMessage().startsWith("/" + command)) {
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "lock_item_command_blocked"));
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamageItem(PlayerItemDamageEvent event) {
        if (event.getItem().getPersistentDataContainer().has(ItemSkinManager.ITEM_SKIN_KEY)) {
            String skinName = event.getItem().getPersistentDataContainer().get(ItemSkinManager.ITEM_SKIN_KEY, PersistentDataType.STRING);
            ItemSkinManager.ItemSkin itemSkin = ItemSkinManager.INSTANCE.getItemSkins().get(skinName);
            if (itemSkin != null) {
                if (!itemSkin.permission().isEmpty() && !event.getPlayer().hasPermission(itemSkin.permission())) {
                    event.getItem().editPersistentDataContainer(pdc -> pdc.remove(ItemSkinManager.ITEM_SKIN_KEY));
                    event.getItem().resetData(DataComponentTypes.ITEM_MODEL);
                    event.getItem().resetData(DataComponentTypes.EQUIPPABLE);
                    event.getItem().resetData(DataComponentTypes.CUSTOM_MODEL_DATA);
                    event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_no_permission"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemDrop(PlayerDropItemEvent event) {
        String originalType = event.getItemDrop().getItemStack().getType().name();
        if (VineriumCore.inst().getConfigManager().getCauldronTransforms().containsKey(originalType)) {
            Item droppedItem = event.getItemDrop();
            Material requiredMaterial = VineriumCore.inst().getConfig().getBoolean("Tweaks.CauldronTransform.RequireWater", true)
                    ? Material.WATER_CAULDRON
                    : Material.CAULDRON;
            Material newMaterial = Material.valueOf(VineriumCore.inst().getConfigManager().getCauldronTransforms().get(originalType));
            Bukkit.getScheduler().scheduleSyncDelayedTask(VineriumCore.inst(), () -> {
                if (droppedItem.isValid()) {
                    Location location = droppedItem.getLocation();
                    Location blockLocation = location.toBlockLocation();
                    Block block = blockLocation.getBlock();
                    if (block.getType() == requiredMaterial) {
                        ItemStack originalItemStack = droppedItem.getItemStack();
                        droppedItem.setCanPlayerPickup(true);
                        droppedItem.setItemStack(ItemStack.of(newMaterial,originalItemStack.getAmount()));
                        location.getWorld().playSound(location,Sound.ENTITY_PLAYER_SPLASH,SoundCategory.BLOCKS,1f,2f);
                        location.getWorld().spawnParticle(Particle.BUBBLE_POP,location.add(0,1,0),20,0.3,0.2,0.3,0.0);
                        droppedItem.setVelocity(new Vector(0,0.45,0));
                    }
                }}, 20L);
        }
    }
}
