package org.saintqd.vineriumcore.listeners;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Containers.CMIVanish;
import com.Zrips.CMI.Modules.Vanish.VanishAction;
import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import io.papermc.paper.block.TileStateInventoryHolder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import io.papermc.paper.event.player.CartographyItemEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.Audiences;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.TriState;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.gui.ArmorStandGUI;
import org.saintqd.vineriumcore.gui.ItemSkinGUI;
import org.saintqd.vineriumcore.managers.*;
import org.saintqd.vineriumcore.placeholders.VinCorePlaceholders;
import org.saintqd.vineriumcore.worldguard.Flags;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.managers.LangManager;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerListener implements Listener {

    private final NamespacedKey SAVED_HEALTH_KEY = new NamespacedKey(VineriumCore.inst(), "saved_health");

    private final HashMap<AnvilInventory, Integer> realMaxRepairCosts = new HashMap<>();
    private final HashMap<Location,String> tempShulkerUuidLocations = new HashMap<>();

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
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED
                && VineriumCore.inst().getConfig().getBoolean("CommandsAfterLastSeen.Enabled")
                && VineriumCore.inst().getConfig().contains("CommandsAfterLastSeen.Presets")) {
            long lastSeen = offlinePlayer.getLastSeen();
            if (lastSeen > 0) {
                long checkDelay = VineriumCore.inst().getConfig().getLong("CommandsAfterLastSeen.CheckDelay", 60L);
                Bukkit.getScheduler().runTaskLater(VineriumCore.inst(), () -> {
                    if (!offlinePlayer.isOnline() || offlinePlayer.getName() == null)
                        return;
                    for (String presetName : VineriumCore.inst().getConfig().getConfigurationSection("CommandsAfterLastSeen.Presets").getKeys(false)) {
                        ConfigurationSection presetConfig = VineriumCore.inst().getConfig().getConfigurationSection("CommandsAfterLastSeen.Presets." + presetName);
                        long maxLastSeenTime = presetConfig.getLong("Time", 604800000L);
                        if (lastSeen + maxLastSeenTime < System.currentTimeMillis()) {
                            for (String command : presetConfig.getStringList("Commands")) {
                                String parsedCommand = command.replace("%player_name%", offlinePlayer.getName());
                                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
                            }
                        }
                    }
                }, checkDelay);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinAccepted(final PlayerJoinEvent event) {
        if (VineriumCore.inst().getConfigManager().getAccountTransferNewToOldNicknames().containsKey(event.getPlayer().getName())) {
            String oldNickname = VineriumCore.inst().getConfigManager().getAccountTransferNewToOldNicknames().get(event.getPlayer().getName());
            Bukkit.getScheduler().runTaskLater(VineriumCore.inst(),() -> {
                event.getPlayer().kick(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_kick_second_message"));
                Bukkit.getScheduler().runTask(VineriumCore.inst(),() -> {
                    VineriumCore.inst().getConfigManager().performAccountTransfer(null,oldNickname,event.getPlayer().getName());
                    String oldPlayerName = VineriumCore.inst().getConfigManager().getAccountTransferNewToOldNicknames().remove(event.getPlayer().getName());
                    VineriumCore.inst().getConfigManager().getPendingAccountTransfers().remove(oldPlayerName);
                });
            },20L);
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        VinCorePlaceholders.getOfflinePlayerPlaceholders().remove(event.getPlayer().getName());
        VineriumCore.inst().getSuffixManager().checkSuffixPermission(event.getPlayer());

        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.HealthSave",true)) {
            Bukkit.getScheduler().runTaskLater(VineriumCore.inst(), () -> {
                double savedHealth = event.getPlayer().getPersistentDataContainer().getOrDefault(SAVED_HEALTH_KEY,
                        PersistentDataType.DOUBLE, event.getPlayer().getHealth());
                savedHealth = Math.min(savedHealth, event.getPlayer().getAttribute(Attribute.MAX_HEALTH).getValue());
                event.getPlayer().setHealth(savedHealth);
            }, 2);
        }

        int hintIndex = VineriumCore.inst().getConfig().getInt("StarterHints.Join",-2);
        if (hintIndex > -2 && event.getPlayer().permissionValue("vineriumcore.disablehints") != TriState.TRUE && VineriumCore.inst().isCMIEnabled()) {
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

        if (VineriumCore.inst().getConfig().getBoolean("Guide.Enabled",true) && VineriumCore.inst().getConfig().getBoolean("Guide.ShowMessage",true)) {
            int ticksPlayed = event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE);
            if (ticksPlayed < VineriumCore.inst().getConfig().getInt("Guide.MaxTimePlayedToShowMessage",72000)) {
                event.getPlayer().sendMessage(LangManager.INSTANCE.parseLangString(VineriumCore.inst(),"guide_possible_message"));
            }
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
                    for (String groupName : VineriumCore.inst().getConfig().getConfigurationSection("Messages.Join.FormatPerGroup").getKeys(false)) {
                        if (event.getPlayer().hasPermission("group."+groupName))
                            joinMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Join.FormatPerGroup."+groupName, "<white>>> <gray>[message]");
                    }
                } else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultJoin.Enabled")) {
                    joinMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultJoin.Format", null);
                    for (String groupName : VineriumCore.inst().getConfig().getConfigurationSection("Messages.DefaultJoin.FormatPerGroup").getKeys(false)) {
                        if (event.getPlayer().hasPermission("group."+groupName)) {
                            joinMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultJoin.FormatPerGroup."+groupName, null);
                        }
                    }
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;

        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.HealthSave",true)) {
            event.getPlayer().getPersistentDataContainer().set(SAVED_HEALTH_KEY, PersistentDataType.DOUBLE,
                    event.getPlayer().getHealth());
        }

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
                for (String groupName : VineriumCore.inst().getConfig().getConfigurationSection("Messages.Leave.FormatPerGroup").getKeys(false)) {
                    if (event.getPlayer().hasPermission("group."+groupName))
                        leaveMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Leave.FormatPerGroup."+groupName, "<white>>> <gray>[message]");
                }
            } else if (VineriumCore.inst().getConfig().getBoolean("Messages.DefaultLeave.Enabled")) {
                leaveMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultLeave.Format", null);
                for (String groupName : VineriumCore.inst().getConfig().getConfigurationSection("Messages.DefaultLeave.FormatPerGroup").getKeys(false)) {
                    if (event.getPlayer().hasPermission("group."+groupName))
                        leaveMessage = VineriumCore.inst().getConfig().getString("Messages.DefaultLeave.FormatPerGroup."+groupName, null);
                }
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
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (VineriumCore.inst().getPlayerManager().getKnockoutPlayers().containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null) {
                switch (event.getClickedBlock().getType()) {
                    case CHIPPED_ANVIL,DAMAGED_ANVIL -> {
                        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.AnvilRepair.Enabled", true)) {
                            Material repairMaterial = Material.valueOf(VineriumCore.inst().getConfig().getString("Tweaks.AnvilRepair.RepairMaterial", "IRON_BLOCK"));
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
                                event.getClickedBlock().getWorld().playSound(event.getClickedBlock().getLocation(), repairSound, SoundCategory.BLOCKS, 1f, 1f);
                            mainHandItem.setAmount(mainHandItem.getAmount() - 1);
                            return;
                        }
                    }
                    case WATER_CAULDRON -> {
                        ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
                        if (mainHandItem.getType() != Material.AIR) {
                            Location location = event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
                            CauldronRecipesManager.CauldronRecipe cauldronRecipe =
                                    CauldronRecipesManager.INSTANCE.checkRecipe(mainHandItem, location);
                            if (cauldronRecipe != null) {
                                event.setCancelled(true);
                                if (cauldronRecipe.getRemoveCombineItem())
                                    event.getPlayer().getInventory().getItemInMainHand().setAmount(
                                            event.getPlayer().getInventory().getItemInMainHand().getAmount() - 1);
                            }
                            return;
                        }
                    }
                    case PODZOL -> {
                        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.RemovePodzol.Enabled", true)) {
                            ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
                            Optional<Material> possibleRequiredHandMaterial = Enums.getIfPresent(Material.class,
                                    VineriumCore.inst().getConfig().getString("Tweaks.RemovePodzol.HandMaterial", "BRUSH"));
                            if (possibleRequiredHandMaterial.isPresent()) {
                                Material requiredHandMaterial = possibleRequiredHandMaterial.get();
                                if (mainHandItem.getType() == requiredHandMaterial) {
                                    int replaceRadius = VineriumCore.inst().getConfig().getInt("Tweaks.RemovePodzol.Radius", 1);
                                    Block clickedBlock = event.getClickedBlock();
                                    Location centerLoc = clickedBlock.getLocation();

                                    if (replaceRadius <= 0) {
                                        clickedBlock.setType(Material.GRASS_BLOCK);
                                    }
                                    else {
                                        World world = clickedBlock.getWorld();
                                        int centerX = centerLoc.getBlockX();
                                        int centerY = centerLoc.getBlockY();
                                        int centerZ = centerLoc.getBlockZ();

                                        for (int x = centerX - replaceRadius; x <= centerX + replaceRadius; x++) {
                                            for (int z = centerZ - replaceRadius; z <= centerZ + replaceRadius; z++) {
                                                Block possibleBlock = world.getBlockAt(new Location(world,x,centerY,z));
                                                if (possibleBlock.getType() == Material.PODZOL)
                                                    possibleBlock.setType(Material.GRASS_BLOCK);
                                            }
                                        }
                                    }
                                    mainHandItem.damage(1, event.getPlayer());

                                    if (VineriumCore.inst().getConfig().getBoolean("Tweaks.RemovePodzol.SpawnParticles", true)) {
                                        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.COMPOSTER);
                                        particleBuilder.count(25);
                                        particleBuilder.offset(0.5,0.5,0.5);

                                        particleBuilder.location(clickedBlock.getLocation().add(0,0.5,0));
                                        particleBuilder.receivers(25);
                                        particleBuilder.spawn();
                                    }
                                }
                            }
                        }
                    }
                }
                Tag<Material> logsTag = Tag.LOGS;
                if (logsTag != null) {
                    if (logsTag.isTagged(event.getClickedBlock().getType())) {
                        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.UnstripLogs.Enabled", true)) {
                            Optional<Material> possibleRequiredHandMaterial = Enums.getIfPresent(Material.class,
                                    VineriumCore.inst().getConfig().getString("Tweaks.UnstripLogs.HandMaterial", "BONE_MEAL"));
                            if (possibleRequiredHandMaterial.isPresent()) {
                                Material requiredHandMaterial = possibleRequiredHandMaterial.get();
                                int neededAmount = VineriumCore.inst().getConfig().getInt("Tweaks.UnstripLogs.Amount", 1);
                                ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
                                if (handItem.getType() == requiredHandMaterial && handItem.getAmount() >= neededAmount) {
                                    String originalMaterialName = event.getClickedBlock().getType().name();
                                    if (originalMaterialName.contains("STRIPPED")) {
                                        String changedMaterialName = originalMaterialName.replace("STRIPPED_","");
                                        Optional<Material> possibleMaterial = Enums.getIfPresent(Material.class, changedMaterialName);
                                        if (possibleMaterial.isPresent()) {
                                            Material material = possibleMaterial.get();
                                            Block block = event.getClickedBlock();
                                            Orientable orientable = (Orientable) block.getBlockData();
                                            Axis axis = orientable.getAxis();

                                            block.setType(material);
                                            orientable = (Orientable) block.getBlockData();
                                            orientable.setAxis(axis);
                                            block.setBlockData(orientable);

                                            if (neededAmount > 0 && event.getPlayer().getGameMode() != GameMode.CREATIVE)
                                                handItem.setAmount(handItem.getAmount() - 1);

                                            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.UnstripLogs.SpawnParticles", true)) {
                                                ParticleBuilder particleBuilder = new ParticleBuilder(Particle.COMPOSTER);
                                                particleBuilder.count(25);
                                                particleBuilder.offset(0.5,0.5,0.5);

                                                particleBuilder.location(event.getClickedBlock().getLocation().add(0,0.5,0));
                                                particleBuilder.receivers(25);
                                                particleBuilder.spawn();
                                            }

                                            String possibleSoundData = VineriumCore.inst().getConfig().getString(
                                                    "Tweaks.UnstripLogs.Sound","item.axe.strip,2");
                                            if (!possibleSoundData.isEmpty()) {
                                                String[] soundData = VineriumCore.inst().getConfig().getString(
                                                        "Tweaks.UnstripLogs.Sound","item.axe.strip,2").split(",");
                                                @Subst("block.chain.hit") String soundName = soundData[0];
                                                float pitch = soundData.length > 1 ? Float.parseFloat(soundData[1]) : 1.0f;
                                                Key soundKey = Key.key(soundName);
                                                net.kyori.adventure.sound.Sound unstripSound = net.kyori.adventure.sound.Sound.sound(
                                                        soundKey, net.kyori.adventure.sound.Sound.Source.BLOCK,1.0f,pitch);

                                                Audience.audience(event.getClickedBlock().getLocation().getNearbyPlayers(25)).playSound(unstripSound);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (VineriumCore.inst().getConfig().getBoolean("ArmorStandInteractions.Enabled", true)) {
                ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
                String showNearItemMaterialName = VineriumCore.inst().getConfig().getString("ArmorStandInteractions.ShowNearItem", Material.ECHO_SHARD.name());
                int radius = VineriumCore.inst().getConfig().getInt("ArmorStandInteractions.ShowNearRadius", 10);
                int delay = VineriumCore.inst().getConfig().getInt("ArmorStandInteractions.ShowNearGlowTime", 200);
                if (mainHandItem.getType().name().equals(showNearItemMaterialName)) {
                    if (!event.getPlayer().hasPermission("vineriumcore.armorstandinteractions.shownear") &&
                            !event.getPlayer().hasPermission("vineriumcore.admin")) {
                        event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "no_permission"));
                        return;
                    }
                    HashSet<ArmorStand> armorStands = new HashSet<>();
                    for (ArmorStand armorStand : event.getPlayer().getWorld().getNearbyEntitiesByType(ArmorStand.class,
                            event.getPlayer().getLocation(), radius, radius, radius)) {
                        if (!armorStand.isGlowing())
                            armorStands.add(armorStand);
                    }
                    if (!armorStands.isEmpty())
                        event.setCancelled(true);
                    for (ArmorStand armorStand : armorStands) {
                        armorStand.setGlowing(true);
                    }
                    Bukkit.getScheduler().runTaskLater(VineriumCore.inst(), () -> {
                        for (ArmorStand armorStand : armorStands) {
                            if (armorStand.isValid())
                                armorStand.setGlowing(false);
                        }
                    }, delay);
                }
            }
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

        if (VineriumCore.inst().getPlayerManager().getKnockoutPlayers().containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
        if (VineriumCore.inst().getPlayerManager().getKnockoutPlayers().containsKey(damagerEntity.getUniqueId())) {
            event.setCancelled(true);
        }

        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!(damagerEntity instanceof LivingEntity)) return;

        if (event.getEntity() instanceof Player entityPlayer && damagerEntity instanceof Player damagerPlayer) {
            PlayerManager playerManager = VineriumCore.inst().getPlayerManager();
            if (!playerManager.checkTeamPvP(damagerPlayer,entityPlayer)) {
                event.setCancelled(true);
                return;
            }
            if (playerManager.isPvpModeEnabled()) {
                if (damagerPlayer.hasPermission("vineriumcore.admin"))
                    return;
                if (!playerManager.getPvpModePlayers().contains(entityPlayer)) {
                    event.setCancelled(true);
                    HashMap<Player, ImmutablePair<String, Long>> timers = playerManager.getTimers().getOrDefault("entity_not_enabled_pvp", new HashMap<>());
                    ImmutablePair<String, Long> damagerVariable = timers.getOrDefault(damagerPlayer, new ImmutablePair<>(null, 0L));
                    if (damagerVariable.getRight() < VinUtils.getCurrentTick()) {
                        damagerVariable = new ImmutablePair<>(null, VinUtils.getCurrentTick() +
                                VineriumCore.inst().getConfig().getLong("TimersCooldown.entity_not_enabled_pvp", 200L));
                        timers.put(damagerPlayer, damagerVariable);
                        playerManager.getTimers().put("entity_not_enabled_pvp", timers);
                        damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "entity_not_enabled_pvp", entityPlayer.getName()));
                    }
                    return;
                }
                if (!playerManager.getPvpModePlayers().contains(damagerPlayer)) {
                    event.setCancelled(true);
                    HashMap<Player, ImmutablePair<String, Long>> timers = playerManager.getTimers().getOrDefault("damager_not_enabled_pvp", new HashMap<>());
                    ImmutablePair<String, Long> damagerVariable = timers.getOrDefault(damagerPlayer, new ImmutablePair<>(null, 0L));
                    if (damagerVariable.getRight() < VinUtils.getCurrentTick()) {
                        damagerVariable = new ImmutablePair<>(null, VinUtils.getCurrentTick() +
                                VineriumCore.inst().getConfig().getLong("TimersCooldown.damager_not_enabled_pvp", 200L));
                        timers.put(damagerPlayer, damagerVariable);
                        playerManager.getTimers().put("damager_not_enabled_pvp", timers);
                        damagerPlayer.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "damager_not_enabled_pvp", entityPlayer.getName()));
                    }
                    return;
                }
            }
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.DeathKnockout.Enabled", true)) {
                if (event.getFinalDamage() >= entityPlayer.getHealth() && entityPlayer.permissionValue("vineriumcore.deathknockout") != TriState.TRUE) {
                    if (!VineriumCore.inst().getPlayerManager().getKnockoutPlayers().containsKey(event.getEntity().getUniqueId())) {
                        if (entityPlayer != damagerPlayer) {
                            VaultManager vaultManager = VineriumLib.inst().getVaultManager();
                            if (vaultManager == null)
                                return;
                            event.setDamage(0.001);
                            VineriumCore.inst().getPlayerManager().applyKnockout(entityPlayer, damagerPlayer);
                        }
                    }
                }
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
        if (event.getEntity() instanceof Player player && VineriumCore.inst().isWorldGuardEnabled()) {
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player);
            if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, Flags.GLIDE)) {
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                    NamespacedKey key = new NamespacedKey(VineriumCore.inst(),VineriumCore.inst().getConfig()
                            .getString("WorldGuardFlags.Glide.AllowedComponentName","glide_allow"));
                    if (chestplate.getPersistentDataContainer().has(key)) {
                        return;
                    }
                }
                player.setGliding(false);
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

        if (VineriumCore.inst().getConfig().getBoolean("Tweaks.HeadDrops",true)) {
            if (event.getPlayer().getKiller() != null && event.getPlayer().permissionValue("vineriumcore.headdrop") == TriState.TRUE) {
                Player killer = event.getPlayer().getKiller();
                ItemStack headItem = ItemStack.of(Material.PLAYER_HEAD);
                headItem.setData(DataComponentTypes.LORE, ItemLore.lore()
                        .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "head_lore", killer.getName()))
                        .build());
                headItem.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(event.getPlayer().getPlayerProfile()));
                event.getDrops().add(headItem);
            }
        }

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
    public void onPlayerBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        if (VineriumCore.inst().getConfig().getBoolean("OreAlerts.Enabled")) {
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
                long timeToThreshold = VineriumCore.inst().getConfig().getLong("OreAlerts.TimeToThreshold", 12000);
                OreManager.BlockCounter blockCounter = new OreManager.BlockCounter();
                Set<Location> blockLocations = blockCounter.getNearBlocks(event.getBlock().getLocation(), event.getBlock().getType());
                if (blockLocations.isEmpty())
                    return;
                for (Location location : blockLocations)
                    VineriumCore.inst().getOreManager().getCheckedLocations().put(location, currentTick);
                List<OreManager.OreData> oreData = VineriumCore.inst().getOreManager().getPlayerOreData().getOrDefault(event.getPlayer().getName(), new ArrayList<>());
                oreData.add(new OreManager.OreData(event.getBlock().getType(), blockLocations.size()));
                int currentMaterialAmount = 0;
                long oldestData = Long.MAX_VALUE;
                oreData.removeIf(data -> data.getTimestamp() + timeToThreshold < currentTick);
                for (OreManager.OreData data : oreData) {
                    currentMaterialAmount += data.getAmount();
                    if (data.getTimestamp() < oldestData)
                        oldestData = data.getTimestamp();
                }
                VineriumCore.inst().getOreManager().getPlayerOreData().put(event.getPlayer().getName(), oreData);
                List<Player> alertedPlayers = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .filter(player -> player.hasPermission("vineriumcore.orealerts.show")).toList());
                String parsedOreName = VineriumCore.inst().getOreManager()
                        .getThresholdMaterials().get(event.getBlock().getType()).getRight() + "<lang:" + event.getBlock().getType().translationKey() + ">";
                String hoverCommand = VineriumCore.inst().getConfig().getString("OreAlerts.HoverCommand", "tp {1}")
                        .replace("{1}", event.getPlayer().getName());
                String hoverText = "<click:run_command:\"/" + hoverCommand + "\"><hover:show_text:'" + VineriumLib.inst().getLangManager().getLangLines()
                        .get(Key.key("vineriumcore:ore_alert_hover_tooltip")) + "'>" + VineriumLib.inst().getLangManager().getLangLines().get(Key.key("vineriumcore:ore_alert_hover")) + "</hover></click>";
                Component smallAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "ore_alert_small",
                        event.getPlayer().getName(), Integer.toString(blockLocations.size()), parsedOreName, hoverText);
                for (Player player : alertedPlayers) {
                    if (player.permissionValue("vineriumcore.orealerts.disablesmall") != TriState.TRUE) {
                        player.sendMessage(smallAlertComponent);
                        if (player.permissionValue("vineriumcore.orealerts.disablesmallsound") != TriState.TRUE) {
                            player.playSound(VineriumCore.inst().getOreManager().getAlertSound(), player);
                        }
                    }
                }
                if (currentMaterialAmount >= VineriumCore.inst().getOreManager().getThresholdMaterials().get(event.getBlock().getType()).getLeft()) {
                    long timeRange = currentTick - oldestData;
                    // Перевод в минуты. Всегда отображается минимум 1 минута
                    timeRange = timeRange / 20 / 60;
                    if (timeRange <= 0)
                        timeRange = 1;
                    Component largeAlertComponent = VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "ore_alert_large",
                            event.getPlayer().getName(), Integer.toString(currentMaterialAmount), parsedOreName, Long.toString(timeRange), hoverText);
                    for (Player player : alertedPlayers) {
                        if (player.permissionValue("vineriumcore.orealerts.disablelarge") != TriState.TRUE) {
                            player.sendMessage(largeAlertComponent);
                            if (player.permissionValue("vineriumcore.orealerts.disablelargesound") != TriState.TRUE) {
                                player.playSound(VineriumCore.inst().getOreManager().getAlertSound(), player);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        if (VineriumCore.inst().getConfig().getBoolean("OreAlerts.Enabled")) {
            if (VineriumCore.inst().getOreManager().getThresholdMaterials().containsKey(event.getBlock().getType())) {
                Location blockLoc = event.getBlock().getLocation();
                VineriumCore.inst().getOreManager().getCheckedLocations().remove(blockLoc);
                VineriumCore.inst().getOreManager().getPlacedOres().add(blockLoc);
            }
        }
        if (VineriumCore.inst().getConfig().getBoolean("ShulkerDupeDetector.Enabled")) {
            if (event.getBlock().getType().name().contains(Material.SHULKER_BOX.name()) && event.getBlock().getState() instanceof TileStateInventoryHolder state) {
                ItemStack shulkerItem = event.getItemInHand();
                if (shulkerItem.getPersistentDataContainer().has(ShulkerAlertManager.SHULKER_UUID_KEY)) {
                    String shulkerUuidString = shulkerItem.getPersistentDataContainer().get(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING);
                    state.getPersistentDataContainer().set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, shulkerUuidString);
                    state.update();
                    long keepUuidTime = VineriumCore.inst().getConfig().getLong("ShulkerDupeDetector.KeepUuidTime",12000L);
                    if (!event.getPlayer().hasPermission("vineriumcore.shulkerdupealerts.bypass")) {
                        if (ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().containsKey(shulkerUuidString)) {
                            long time = ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().get(shulkerUuidString);
                            if (time + keepUuidTime > VinUtils.getCurrentTick()) {
                                ShulkerAlertManager.INSTANCE.showAlert(event.getPlayer());
                            } else {
                                ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().remove(shulkerUuidString);
                            }
                        }
                        ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().put(shulkerUuidString, VinUtils.getCurrentTick());
                    }
                }
                else {
                    UUID uuid = UUID.randomUUID();
                    state.getPersistentDataContainer().set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, uuid.toString());
                    state.update();
                }
            }
        }
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        if (VineriumCore.inst().getConfig().getBoolean("ShulkerDupeDetector.Enabled")) {
            if (event.getBlockState().getType().name().contains(Material.SHULKER_BOX.name()) && event.getBlockState() instanceof TileStateInventoryHolder state) {
                if (!event.getItems().isEmpty()) {
                    if (state.getPersistentDataContainer().has(ShulkerAlertManager.SHULKER_UUID_KEY)) {
                        String shulkerUuidString = state.getPersistentDataContainer().get(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING);
                        event.getItems().getFirst().getItemStack().editPersistentDataContainer(pdc ->
                                pdc.set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, shulkerUuidString)
                        );
                        ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().remove(shulkerUuidString);
                    }
                    else {
                        UUID uuid = UUID.randomUUID();
                        event.getItems().getFirst().getItemStack().editPersistentDataContainer(pdc ->
                                pdc.set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, uuid.toString())
                        );
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        if (VineriumCore.inst().getConfig().getBoolean("ShulkerDupeDetector.Enabled") && !event.getDrops().isEmpty()) {
            ItemStack droppedItem = event.getDrops().getFirst();
            if (event.getBlock().getType().name().contains(Material.SHULKER_BOX.name()) && event.getBlock().getState() instanceof TileStateInventoryHolder state) {
                String shulkerUuidString = state.getPersistentDataContainer().getOrDefault(ShulkerAlertManager.SHULKER_UUID_KEY,
                        PersistentDataType.STRING,UUID.randomUUID().toString());
                if (tempShulkerUuidLocations.containsKey(event.getBlock().getLocation())) {
                    shulkerUuidString = tempShulkerUuidLocations.get(event.getBlock().getLocation());
                }
                String finalShulkerUuidString = shulkerUuidString;
                droppedItem.editPersistentDataContainer(pdc ->
                        pdc.set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, finalShulkerUuidString)
                );
                ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().remove(shulkerUuidString);
            }
        }
    }

    @EventHandler
    public void onDispenserDispense(BlockDispenseEvent event) {
        if (VineriumCore.inst().getConfig().getBoolean("ShulkerDupeDetector.Enabled")
                && event.getBlock().getBlockData() instanceof Directional directional) {
            if (event.getItem().getType().name().contains(Material.SHULKER_BOX.name())) {
                // Если перед раздатчиком уже стоит шалкер - игнорируем проверки
                Block currentBlockInFront = event.getBlock().getRelative(directional.getFacing());
                if (currentBlockInFront.getType().name().contains(Material.SHULKER_BOX.name())
                        && currentBlockInFront.getState() instanceof TileStateInventoryHolder currentBlockState) {
                    return;
                }
                String shulkerUuidString = event.getItem().getPersistentDataContainer().getOrDefault(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING,UUID.randomUUID().toString());
                Block possibleBlock = event.getBlock().getRelative(directional.getFacing());
                tempShulkerUuidLocations.put(possibleBlock.getLocation(), shulkerUuidString);
                Bukkit.getScheduler().runTask(VineriumCore.inst(), () -> {
                    Block resultBlock = event.getBlock().getRelative(directional.getFacing());
                    if (resultBlock.getType().name().contains(Material.SHULKER_BOX.name())
                            && resultBlock.getState() instanceof TileStateInventoryHolder state) {
                        state.getPersistentDataContainer().set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, shulkerUuidString);
                        state.update();
                        long keepUuidTime = VineriumCore.inst().getConfig().getLong("ShulkerDupeDetector.KeepUuidTime",12000L);
                        if (ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().containsKey(shulkerUuidString)) {
                            long time = ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().get(shulkerUuidString);
                            if (time + keepUuidTime > VinUtils.getCurrentTick()) {
                                ShulkerAlertManager.INSTANCE.showAlert(resultBlock.getLocation());
                            } else {
                                ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().remove(shulkerUuidString);
                            }
                        }
                        ShulkerAlertManager.INSTANCE.getWorldShulkerUuids().put(shulkerUuidString, VinUtils.getCurrentTick());
                    }
                    tempShulkerUuidLocations.remove(resultBlock.getLocation());
                });
            }
        }
    }

    @EventHandler
    public void onPlayerPortalTeleport(PlayerPortalEvent event) {
        if (VineriumCore.inst().isWorldGuardEnabled()) {
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(event.getPlayer());
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
            /*
            if (VineriumCore.inst().getConfig().getBoolean("ShulkerDupeDetector.Enabled",true)
                    && originalResultItem.getType().name().contains(Material.SHULKER_BOX.name())) {
                if (!originalResultItem.getPersistentDataContainer().has(ShulkerAlertManager.SHULKER_UUID_KEY)) {
                    UUID uuid = UUID.randomUUID();
                    originalResultItem.editPersistentDataContainer(pdc -> pdc.set(ShulkerAlertManager.SHULKER_UUID_KEY,PersistentDataType.STRING, uuid.toString()));
                }
            }*/
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
        if (handItem.getType() != Material.AIR) {
            if (handItem.getPersistentDataContainer().has(ConfigManager.getLockKey())) {
                List<String> blockedCommands = VineriumCore.inst().getConfig().getStringList("Tweaks.ItemLock.BlockedCommands");
                for (String command : blockedCommands) {
                    if (event.getMessage().startsWith("/" + command)) {
                        event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "lock_item_command_blocked"));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            if (handItem.getPersistentDataContainer().has(ConfigManager.getSignKey())) {
                List<String> blockedCommands = VineriumCore.inst().getConfig().getStringList("Tweaks.ItemSign.BlockedCommands");
                for (String command : blockedCommands) {
                    if (event.getMessage().startsWith("/" + command)) {
                        event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "sign_item_command_blocked"));
                        event.setCancelled(true);
                        return;
                    }
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

    @EventHandler
    public void onVaultOpen(VaultChangeStateEvent event) {
        if (event.getPlayer() != null) {
            if (VineriumCore.inst().getConfig().getBoolean("Tweaks.VaultReset.Enabled",true)) {
                int cooldown = VineriumCore.inst().getConfig().getInt("Tweaks.VaultReset.Cooldown",600);
                if (event.getBlock().getState() instanceof Vault vaultBlockState && event.getNewState() == org.bukkit.block.data.type.Vault.State.UNLOCKING) {
                    Collection<UUID> rewardedPlayers = vaultBlockState.getRewardedPlayers();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(VineriumCore.inst(), () -> {
                        rewardedPlayers.forEach(vaultBlockState::removeRewardedPlayer);
                        vaultBlockState.update();
                        }, cooldown);
                }
            }
        }
    }

    @EventHandler
    public void onInteractWithArmorStand(PlayerInteractAtEntityEvent event) {
        if (!VineriumCore.inst().getConfig().getBoolean("ArmorStandInteractions.Enabled",true))
            return;
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) {
            return;
        }
        ItemStack handItem = event.getPlayer().getEquipment().getItemInMainHand();
        Material handItemType = handItem.getType();
        if (VineriumCore.inst().isWorldGuardEnabled()) {
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(event.getPlayer());
            if (!container.createQuery().testState(localPlayer.getLocation(),localPlayer, com.sk89q.worldguard.protection.flags.Flags.BUILD)) {
                event.setCancelled(true);
                return;
            }
        }
        if (VineriumCore.inst().getConfig().getString("ArmorStandInteractions.ChangeNameItem",Material.NAME_TAG.name()).equals(handItemType.name())) {
            event.setCancelled(true);
            if (!event.getPlayer().hasPermission("vineriumcore.armorstandinteractions.setname") &&
                    !event.getPlayer().hasPermission("vineriumcore.admin")) {
                event.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "no_permission"));
                return;
            }
            if (handItem.hasData(DataComponentTypes.CUSTOM_NAME)) {
                Component name = handItem.getData(DataComponentTypes.CUSTOM_NAME);
                armorStand.customName(name);
                armorStand.setCustomNameVisible(true);
            }
            else {
                armorStand.customName(null);
                armorStand.setCustomNameVisible(false);
            }
        }
        else if (VineriumCore.inst().getConfig().getString("ArmorStandInteractions.OpenMenuItem",Material.IRON_INGOT.name()).equals(handItemType.name())) {
            event.setCancelled(true);
            ArmorStandGUI armorStandGUI = new ArmorStandGUI(event.getPlayer());
            armorStandGUI.setArmorStandMenu(armorStand);
            if (armorStandGUI.getInventory() != null)
                event.getPlayer().openInventory(armorStandGUI.getInventory());
        }
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (VineriumCore.inst().getPlayerManager().getKnockoutPlayers().containsKey(event.getPlayer().getUniqueId())) {
            if (event.getItem().getType() == Material.MILK_BUCKET) {
                event.setCancelled(true);
            }
        }
    }
}
