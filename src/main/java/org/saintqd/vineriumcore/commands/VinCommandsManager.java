package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
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
                                        VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"notEnoughArguments"));
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
                            .then(SuffixCommandsManager.getSuffixCommands())
                            .then(DiscordCommandsManager.getDiscordCommands())
                            .build(),
                    "Основная команда."
            );

        });
    }

    private static void reloadCommand(CommandSender sender) {
        VineriumCore.inst().loadData();
        if (sender instanceof Player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"reloadMessage"));
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
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"joinMessageRemoved"));
            return;
        }
        if (!message.contains("*") && !message.contains(player.getName())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"joinMessageHint"));
            return;
        }
        // При использовании Alias CMI знак % заменяется на %. , ломая плейсхолдеры
        // Исправляем это фиксом ниже
        message = message.replace("%.","%");

        String joinMessage = message.replace(player.getName(),"*").replace(".","[dot]");
        joinMessage = PlainTextComponentSerializer.plainText().serialize(VinUtils.parseString(joinMessage));

        int maxLength = VineriumCore.inst().getConfig().getInt("Messages.MaxLength",100);
        if (joinMessage.length() > maxLength) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"customMessageTooLong",Integer.toString(maxLength)));
        }

        joinMessage = joinMessage.replaceAll("(\\p{Lu})", "╝$1");
        joinMessage = joinMessage.toLowerCase();

        if (joinMessagePermission != null)
            vaultManager.getPermissionProvider().playerRemove(null,player,joinMessagePermission);
        vaultManager.getPermissionProvider().playerAdd(null,player,"meta.join-message."
                + joinMessage);

        if (sender == player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"joinMessageApplied"));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"joinMessageApplied",player.getName()));
        String joinMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Join.Format","<white>>> <gray>[message]");
        joinMessageFormat = joinMessageFormat.replace("[message]",joinMessage)
                .replace("[dot]",".");
        joinMessageFormat = Pattern.compile("╝+(.)?").matcher(joinMessageFormat).replaceAll(mr -> mr.group(1).toUpperCase());
        joinMessageFormat = joinMessageFormat.replace("*",VineriumCore.inst().getConfig().getString("Messages.NicknameFormat",player.getName()));
        joinMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player,joinMessageFormat)
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
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leaveMessageRemoved"));
            return;
        }
        if (!message.contains("*") && !message.contains(player.getName())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leaveMessageHint"));
            return;
        }
        message = message.replace("%.","%");

        String leaveMessage = message.replace(player.getName(),"*").replace(".","[dot]");
        leaveMessage = PlainTextComponentSerializer.plainText().serialize(VinUtils.parseString(leaveMessage));

        int maxLength = VineriumCore.inst().getConfig().getInt("Messages.MaxLength",100);
        if (leaveMessage.length() > maxLength) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"customMessageTooLong",Integer.toString(maxLength)));
        }

        leaveMessage = leaveMessage.replaceAll("(\\p{Lu})", "╝$1");
        leaveMessage = leaveMessage.toLowerCase();

        if (leaveMessagePermission != null)
            vaultManager.getPermissionProvider().playerRemove(null,player,leaveMessagePermission);
        vaultManager.getPermissionProvider().playerAdd(null,player,"meta.leave-message."
                + leaveMessage);

        if (sender == player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leaveMessageApplied"));
        else
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"leaveMessageApplied",player.getName()));
        String leaveMessageFormat = VineriumCore.inst().getConfig().getString("Messages.Leave.Format","<white<<< <gray>[message]");
        leaveMessageFormat = leaveMessageFormat.replace("[message]",leaveMessage)
                .replace("[dot]",".");
        leaveMessageFormat = Pattern.compile("╝+(.)?").matcher(leaveMessageFormat).replaceAll(mr -> mr.group(1).toUpperCase());
        leaveMessageFormat = leaveMessageFormat.replace("*",VineriumCore.inst().getConfig().getString("Messages.NicknameFormat",player.getName()));
        leaveMessageFormat = (VineriumCore.inst().getPlaceholders() != null)
                ? PlaceholderAPI.setPlaceholders(player,leaveMessageFormat)
                : leaveMessageFormat;
        sender.sendMessage(VinUtils.parseString(leaveMessageFormat));
    }
}
