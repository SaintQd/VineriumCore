package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.SchedulerUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.DiscordSRVManager;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

public class DiscordCommandsManager {

    private static final LiteralArgumentBuilder<CommandSourceStack> discordCommands = Commands.literal("discord")
            .requires(ctx -> VineriumCore.inst().getDiscordSRVManager() != null)
            .then(Commands.literal("toggle")
                    .requires(ctx -> VineriumLib.inst().getVaultManager() != null
                            && ctx.getSender().hasPermission("vineriumcore.discord.toggle"))
                    .executes(ctx -> {
                        toggleDiscordMessagesCommand(ctx.getSource().getSender(),null);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("player", ArgumentTypes.player())
                            .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                            .executes(ctx -> {
                                toggleDiscordMessagesCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                    )
            )
            .then(Commands.literal("broadcast")
                    .requires(ctx -> ctx.getSender().hasPermission("vineriumcore.discord.broadcast"))
                    .then(Commands.argument("channel", StringArgumentType.word())
                            .then(Commands.argument("messageFormatName", StringArgumentType.word())
                                    .then(Commands.argument("actorPlayerName", StringArgumentType.word())
                                            .suggests((ctx,builder) -> {
                                                Bukkit.getOnlinePlayers().forEach((player) -> {
                                                    builder.suggest(player.getName());
                                                });
                                                return builder.buildFuture();
                                            })
                                            .then(Commands.argument("args", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        broadcastEmbedMessage(ctx.getSource().getSender(),
                                                                ctx.getLastChild().getLastChild().getLastChild().getArgument("channel", String.class),
                                                                ctx.getLastChild().getLastChild().getArgument("messageFormatName", String.class),
                                                                ctx.getLastChild().getArgument("actorPlayerName", String.class),
                                                                ctx.getArgument("args", String.class)
                                                        );
                                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                                    })
                                            )
                                    )
                            )
                    )
            )
            ;

    public static LiteralArgumentBuilder<CommandSourceStack> getDiscordCommands() {
        return discordCommands;
    }

    private static void toggleDiscordMessagesCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;

        TriState permissionState = player.permissionValue("vineriumcore.discord.toggleenabled");
        if (permissionState == TriState.FALSE || permissionState == TriState.NOT_SET) {
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerAdd(null,player, "vineriumcore.discord.toggleenabled");
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"discordMessagesToggleOff"));
        }
        else {
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerRemove(null,player, "vineriumcore.discord.toggleenabled");
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"discordMessagesToggleOn"));
        }
    }

    private static void broadcastEmbedMessage(CommandSender sender, String channelName, String messageFormatName, String actorPlayerName, String args) {

        String[] argsSplit = args.split(";");
        OfflinePlayer actorPlayer = Bukkit.getOfflinePlayer(actorPlayerName);
        SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                DiscordSRVManager.runMessageAsync(channelName, actorPlayer,
                        VineriumCore.inst().getDiscordSRVManager().getMessageFormats().get(messageFormatName),argsSplit));
    }
}
