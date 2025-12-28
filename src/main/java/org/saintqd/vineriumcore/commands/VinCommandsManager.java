package org.saintqd.vineriumcore.commands;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.PlayerManager;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class VinCommandsManager {

    public static void setupCommands(VineriumCore plugin) {
        LifecycleEventManager<@NotNull Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("vin")
                            .executes(commandContext -> {
                                commandContext.getSource().getSender().sendMessage(
                                        VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"not_enough_arguments"));
                                return Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.literal("reload")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                                    .executes(ctx -> {
                                        reloadCommand(ctx.getSource().getSender());
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("joinmessage")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.joinmessage")
                                            && VineriumLib.inst().getVaultManager() != null
                                            && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
                                    .executes(ctx -> {
                                        setJoinMessageCommand(ctx.getSource().getSender(),null,null);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("message", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                setJoinMessageCommand(ctx.getSource().getSender(),ctx.getArgument("message",String.class),null);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("leavemessage")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.leavemessage")
                                            && VineriumLib.inst().getVaultManager() != null
                                            && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
                                    .executes(ctx -> {
                                        setLeaveMessageCommand(ctx.getSource().getSender(),null,null);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("message", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                setLeaveMessageCommand(ctx.getSource().getSender(),ctx.getArgument("message",String.class),null);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("adminjoinmessage")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin")
                                            && VineriumLib.inst().getVaultManager() != null
                                            && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
                                    .then(Commands.argument("message", StringArgumentType.string())
                                            .then(Commands.argument("player", ArgumentTypes.player())
                                                    .executes(ctx -> {
                                                        setJoinMessageCommand(ctx.getSource().getSender(),
                                                                ctx.getLastChild().getArgument("message",String.class),
                                                                ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("adminleavemessage")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin")
                                            && VineriumLib.inst().getVaultManager() != null
                                            && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
                                    .then(Commands.argument("message", StringArgumentType.string())
                                            .then(Commands.argument("player", ArgumentTypes.player())
                                                    .executes(ctx -> {
                                                        setLeaveMessageCommand(ctx.getSource().getSender(),
                                                                ctx.getLastChild().getArgument("message",String.class),
                                                                ctx.getArgument("player",PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("pvptoggle")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.pvptoggle"))
                                    .executes(ctx -> {
                                        pvpToggleCommand(ctx.getSource().getSender(),null);
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                                            .executes(ctx -> {
                                                pvpToggleCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                            .then(Commands.literal("savedata")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                                    .executes(ctx -> {
                                        saveDataCommand(ctx.getSource().getSender());
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(Commands.literal("hint")
                                    .executes(ctx -> {
                                        sendHintCommand(ctx.getSource().getSender(),-1,null);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("index", IntegerArgumentType.integer())
                                            .executes(ctx -> {
                                                sendHintCommand(ctx.getSource().getSender(),ctx.getArgument("index", Integer.class),null);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.argument("player", ArgumentTypes.player())
                                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                                                    .executes(ctx -> {
                                                        sendHintCommand(
                                                                ctx.getSource().getSender(),
                                                                ctx.getLastChild().getArgument("index", Integer.class),
                                                                ctx.getArgument("player",PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("transferaccount")
                                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                                    .then(Commands.argument("old_player_name", StringArgumentType.string())
                                            .suggests((ctx,builder) -> {
                                                String partName = builder.getRemaining();
                                                Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                                                    if (onlinePlayer.getName().startsWith(partName))
                                                        builder.suggest(onlinePlayer.getName());
                                                });
                                                return builder.buildFuture();
                                            })
                                            .then(Commands.argument("new_player_name", StringArgumentType.string())
                                                    .suggests((ctx,builder) -> {
                                                        String partName = builder.getRemaining();
                                                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                                                            if (onlinePlayer.getName().startsWith(partName))
                                                                builder.suggest(onlinePlayer.getName());
                                                        });
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(ctx -> {
                                                        transferAccountCommand(ctx.getSource().getSender(),
                                                                ctx.getLastChild().getArgument("old_player_name", String.class),
                                                                ctx.getArgument("new_player_name", String.class));
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                            .then(SuffixCommandsManager.getSuffixCommands())
                            .build(),
                    "Основная команда."
            );

        });
    }

    private static void reloadCommand(CommandSender sender) {
        VineriumCore.inst().loadData();
        if (sender instanceof Player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"reload_message"));
    }

    private static void saveDataCommand(CommandSender sender) {
        VineriumCore.inst().saveData();
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"command_save_data"));
    }

    private static void setJoinMessageCommand(CommandSender sender, String message, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();

        String joinMessagePermission = null;
        List<String> possibleJoinMessage = player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("meta.join-message.")).toList();
        if (!possibleJoinMessage.isEmpty())
            joinMessagePermission = possibleJoinMessage.getFirst();

        if (message == null || message.isEmpty()) {
            if (joinMessagePermission != null)
                vaultManager.getPermissionProvider().playerRemove(null,player,joinMessagePermission);
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"join_message_removed"));
            return;
        }
        if (!message.contains("*") && !message.contains(player.getName())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"join_message_hint"));
            return;
        }
        // При использовании Alias CMI знак % заменяется на %. , ломая плейсхолдеры
        // Исправляем это фиксом ниже
        message = message.replace("%.","%");

        String joinMessage = message.replace(player.getName(),"*").replace(".","[dot]");
        joinMessage = PlainTextComponentSerializer.plainText().serialize(VinUtils.parseString(joinMessage));

        int maxLength = VineriumCore.inst().getConfig().getInt("Messages.MaxLength",100);
        if (joinMessage.length() > maxLength) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"custom_message_too_long",Integer.toString(maxLength)));
        }

        joinMessage = joinMessage.replaceAll("(\\p{Lu})", "╝$1");
        joinMessage = joinMessage.toLowerCase();

        if (joinMessagePermission != null)
            vaultManager.getPermissionProvider().playerRemove(null,player,joinMessagePermission);
        vaultManager.getPermissionProvider().playerAdd(null,player,"meta.join-message."
                + joinMessage);

        if (sender == player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"join_message_applied"));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"join_message_applied",player.getName()));
        String joinMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Join.Format","<white>>> <gray>[message]");
        joinMessageFormat = joinMessageFormat.replace("[message]",joinMessage)
                .replace("[dot]",".");
        joinMessageFormat = Pattern.compile("╝+(.)?").matcher(joinMessageFormat).replaceAll(mr -> mr.group(1).toUpperCase());
        joinMessageFormat = joinMessageFormat.replace("*",VineriumCore.inst().getConfig().getString("Messages.NicknameFormat",player.getName()));
        joinMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player, PlaceholderAPI.setPlaceholders(player,joinMessageFormat))
                : joinMessageFormat;
        sender.sendMessage(VinUtils.parseString(joinMessageFormat));
    }

    private static void setLeaveMessageCommand(CommandSender sender, String message, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();

        String leaveMessagePermission = null;
        List<String> possibleLeaveMessage = player.getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("meta.leave-message.")).toList();
        if (!possibleLeaveMessage.isEmpty())
            leaveMessagePermission = possibleLeaveMessage.getFirst();

        if (message == null || message.isEmpty()) {
            if (leaveMessagePermission != null)
                vaultManager.getPermissionProvider().playerRemove(null,player,leaveMessagePermission);
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leave_message_removed"));
            return;
        }
        if (!message.contains("*") && !message.contains(player.getName())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leave_message_hint"));
            return;
        }
        message = message.replace("%.","%");

        String leaveMessage = message.replace(player.getName(),"*").replace(".","[dot]");
        leaveMessage = PlainTextComponentSerializer.plainText().serialize(VinUtils.parseString(leaveMessage));

        int maxLength = VineriumCore.inst().getConfig().getInt("Messages.MaxLength",100);
        if (leaveMessage.length() > maxLength) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"custom_message_too_long",Integer.toString(maxLength)));
        }

        leaveMessage = leaveMessage.replaceAll("(\\p{Lu})", "╝$1");
        leaveMessage = leaveMessage.toLowerCase();

        if (leaveMessagePermission != null)
            vaultManager.getPermissionProvider().playerRemove(null,player,leaveMessagePermission);
        vaultManager.getPermissionProvider().playerAdd(null,player,"meta.leave-message."
                + leaveMessage);

        if (sender == player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leave_message_applied"));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leave_message_applied_admin",player.getName()));
        String leaveMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Leave.Format","<white<<< <gray>[message]");
        leaveMessageFormat = leaveMessageFormat.replace("[message]",leaveMessage)
                .replace("[dot]",".");
        leaveMessageFormat = Pattern.compile("╝+(.)?").matcher(leaveMessageFormat).replaceAll(mr -> mr.group(1).toUpperCase());
        leaveMessageFormat = leaveMessageFormat.replace("*",VineriumCore.inst().getConfig().getString("Messages.NicknameFormat",player.getName()));
        leaveMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player, PlaceholderAPI.setPlaceholders(player,leaveMessageFormat))
                : leaveMessageFormat;
        sender.sendMessage(VinUtils.parseString(leaveMessageFormat));
    }

    private static void pvpToggleCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender, player);
        PlayerManager playerManager = VineriumCore.inst().getPlayerManager();

        HashMap<Player, ImmutablePair<String,Long>> timers = playerManager.getTimers().getOrDefault("pvp_toggle_cooldown",new HashMap<>());
        ImmutablePair<String,Long> timerVariable = timers.getOrDefault(player,new ImmutablePair<>(null,0L));

        if (sender == player && !player.hasPermission("vineriumcore.admin")) {
            double minRadiusWithoutPlayers = VineriumCore.inst().getConfig().getDouble("PvPMode.MinRadiusWithoutPlayers",-1);
            if (minRadiusWithoutPlayers > 0 && player.getWorld().getNearbyPlayers(player.getLocation(), minRadiusWithoutPlayers).size() > 1) {
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_min_radius_without_players",Double.toString(minRadiusWithoutPlayers)));
                return;
            }
            if (timerVariable.getRight() > VinUtils.getCurrentTick()) {
                long remainingTime = (timerVariable.getRight() - VinUtils.getCurrentTick()) / 20;
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_toggle_cooldown",Long.toString(remainingTime)));
                return;
            }
        }
        if (playerManager.getPvpModePlayers().contains(player)) {
            playerManager.getPvpModePlayers().remove(player);
            if (sender != player)
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_mode_off_for_player", player.getName()));
            player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_mode_off"));
        }
        else {
            playerManager.getPvpModePlayers().add(player);
            if (sender != player)
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_mode_on_for_player", player.getName()));
            player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"pvp_mode_on"));
        }
        timerVariable = new ImmutablePair<>(null,VinUtils.getCurrentTick() + VineriumCore.inst().getConfig()
                .getLong("TimersCooldown.pvp_toggle_cooldown",6000L));
        timers.put(player,timerVariable);
        playerManager.getTimers().put("pvp_toggle_cooldown",timers);
    }

    private static void sendHintCommand(CommandSender sender, int hintIndex, Player player) {
        if (hintIndex < 0)
            hintIndex = ThreadLocalRandom.current().nextInt(0,VineriumCore.inst().getHintManager().getHints().size());
        if (hintIndex >= VineriumCore.inst().getHintManager().getHints().size()) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"hint_does_not_exist"));
            return;
        }
        String hint = VineriumCore.inst().getHintManager().getHints().get(hintIndex);
        String hintPrefix = VineriumLib.inst().getLangManager().getLangLines().getOrDefault(
                Key.key(VineriumCore.inst(),"hint_prefix"),"hint_prefix").replace("{1}",Integer.toString(hintIndex));
        String finalHint = hintPrefix + hint;
        if (player != null) {
            player.sendRichMessage(finalHint);
        }
        else {
            sender.sendRichMessage(finalHint);
        }
    }

    private static void transferAccountCommand(CommandSender sender, String oldPlayerName, String newPlayerName) {
        OfflinePlayer oldOfflinePlayer = Bukkit.getOfflinePlayer(oldPlayerName);
        OfflinePlayer newOfflinePlayer = Bukkit.getOfflinePlayer(newPlayerName);
        if (!oldOfflinePlayer.hasPlayedBefore() || !newOfflinePlayer.hasPlayedBefore()) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_not_played",
                    oldPlayerName,newPlayerName));
            return;
        }
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_started",
                oldOfflinePlayer.getName(),oldOfflinePlayer.getUniqueId().toString(),newOfflinePlayer.getName(),newOfflinePlayer.getUniqueId().toString()));
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.LuckPerms",true)) {
            VineriumCore.inst().getLuckPermsManager().copyPermissions(oldOfflinePlayer,newOfflinePlayer);
        }
        String playerDataPath = VineriumCore.inst().getConfig().getString("AccountTransfer.PlayerDataPath","world");
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.Advancements",true)) {
            File advancementFile = new File( VineriumCore.inst().getServer().getWorldContainer() + File.separator
                    + playerDataPath + File.separator + "advancements" + File.separator + oldOfflinePlayer.getUniqueId()+".json");
            if (advancementFile.exists()) {
                try {
                    Files.copy(advancementFile.toPath(), Path.of(VineriumCore.inst().getServer().getWorldContainer() + File.separator
                            + playerDataPath + File.separator + "advancements" + File.separator + newOfflinePlayer.getUniqueId()+".json"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_advancements_does_not_exist"));
        }
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.Stats",true)) {
            File statsFile = new File( VineriumCore.inst().getServer().getWorldContainer() + File.separator
                    + playerDataPath + File.separator + "stats" + File.separator + oldOfflinePlayer.getUniqueId()+".json");
            if (statsFile.exists()) {
                try {
                    Files.copy(statsFile.toPath(), Path.of(VineriumCore.inst().getServer().getWorldContainer() + File.separator
                            + playerDataPath + File.separator + "stats" + File.separator + newOfflinePlayer.getUniqueId()+".json"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_stats_does_not_exist"));
        }
        Player oldPlayer = null;
        Player newPlayer = null;
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.Inventory",true)) {
            if (oldOfflinePlayer.hasPlayedBefore() && newOfflinePlayer.hasPlayedBefore() && VineriumCore.inst().isCMIEnabled()) {
                CMIUser oldUser = CMIUser.getUser(oldOfflinePlayer.getUniqueId());
                CMIUser newUser = CMIUser.getUser(newOfflinePlayer.getUniqueId());
                // Ложная ошибка - в CMI API все методы выдают null
                oldPlayer = oldUser.getPlayer(true);
                newPlayer = newUser.getPlayer(true);
                if (oldPlayer != null && newPlayer != null) {
                    for (int slot = 0; slot <= 40; slot++) {
                        ItemStack itemStack = oldPlayer.getInventory().getItem(slot);
                        if (itemStack != null)
                            newPlayer.getInventory().setItem(slot, itemStack);
                    }
                }
            }
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_inventory_error"));
        }
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.EnderChest",true)) {
            if (oldOfflinePlayer.hasPlayedBefore() && newOfflinePlayer.hasPlayedBefore() && VineriumCore.inst().isCMIEnabled()) {
                if (oldPlayer == null || newPlayer == null) {
                    if (oldPlayer == null) {
                        CMIUser oldUser = CMIUser.getUser(oldOfflinePlayer.getUniqueId());
                        // Ложная ошибка - в CMI API все методы выдают null
                        oldPlayer = oldUser.getPlayer(true);
                    }
                    if (newPlayer == null) {
                        CMIUser newUser = CMIUser.getUser(newOfflinePlayer.getUniqueId());
                        // Ложная ошибка - в CMI API все методы выдают null
                        newPlayer = newUser.getPlayer(true);
                    }
                }
                if (oldPlayer != null && newPlayer != null) {
                    for (int slot = 0; slot <= 27; slot++) {
                        ItemStack itemStack = oldPlayer.getEnderChest().getItem(slot);
                        if (itemStack != null)
                            newPlayer.getEnderChest().setItem(slot,itemStack);
                    }
                }
            }
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_enderchest_error"));
        }
        if (newPlayer != null) {
            newPlayer.saveData();
        }
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.Money",true)) {
            VaultManager vaultManager = VineriumLib.inst().getVaultManager();
            if (vaultManager != null && vaultManager.getEconomyProvider() != null
                    && oldOfflinePlayer.hasPlayedBefore() && newOfflinePlayer.hasPlayedBefore()) {
                vaultManager.getEconomyProvider().depositPlayer(newOfflinePlayer,vaultManager.getEconomyProvider().getBalance(oldOfflinePlayer));
            }
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_money_error"));
        }
        if (VineriumCore.inst().getConfig().getBoolean("AccountTransfer.CMI",true) && VineriumCore.inst().isCMIEnabled()) {
            CMI.getInstance().getPlayerManager().switchPlayerData(oldOfflinePlayer.getUniqueId(),newOfflinePlayer.getUniqueId());
        }
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_completed"));
        List<String> commands = VineriumCore.inst().getConfig().getStringList("AccountTransfer.Commands");
        if (!commands.isEmpty()) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_commands_execute"));
            for (String command : commands) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),command
                        .replace("%old_player_name%",oldPlayerName).replace("%new_player_name%",newPlayerName));
            }
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"account_transfer_commands_completed"));
        }
    }
}
