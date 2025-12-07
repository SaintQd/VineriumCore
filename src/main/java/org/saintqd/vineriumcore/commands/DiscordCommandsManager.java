package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.util.TriState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

public class DiscordCommandsManager {

    private static final LiteralArgumentBuilder<CommandSourceStack> discordCommands = Commands.literal("discord")
            .requires(ctx -> VineriumLib.inst().getDiscordSRVManager() != null)
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
}
