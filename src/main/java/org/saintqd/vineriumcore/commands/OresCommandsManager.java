package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

public class OresCommandsManager {

    private static final LiteralArgumentBuilder<CommandSourceStack> oresCommands = Commands.literal("ores")
            .then(Commands.literal("alerts")
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.orealerts.toggle"))
                    .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                builder.suggest("small");
                                builder.suggest("large");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                toggleAlertsCommand(ctx.getSource().getSender(),
                                        ctx.getArgument("type",String.class),
                                        null,
                                        null);
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("state", StringArgumentType.word())
                                    .suggests((ctx,builder) -> {
                                        builder.suggest("true");
                                        builder.suggest("false");
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        toggleAlertsCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("type",String.class),
                                                ctx.getArgument("state",String.class),
                                                null);
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ctx -> {
                                                toggleAlertsCommand(ctx.getSource().getSender(),
                                                        ctx.getLastChild().getLastChild().getArgument("type",String.class),
                                                        ctx.getLastChild().getArgument("state",String.class),
                                                        ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
            )
            .then(Commands.literal("sounds")
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.orealerts.togglesounds"))
                    .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                builder.suggest("small");
                                builder.suggest("large");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                toggleSoundsCommand(ctx.getSource().getSender(),
                                        ctx.getArgument("type",String.class),
                                        null,
                                        null);
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("state", StringArgumentType.word())
                                    .suggests((ctx,builder) -> {
                                        builder.suggest("true");
                                        builder.suggest("false");
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        toggleSoundsCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("type",String.class),
                                                ctx.getArgument("state",String.class),
                                                null);
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                                    .then(Commands.argument("player", ArgumentTypes.player())
                                            .executes(ctx -> {
                                                toggleSoundsCommand(ctx.getSource().getSender(),
                                                        ctx.getLastChild().getLastChild().getArgument("type",String.class),
                                                        ctx.getLastChild().getArgument("state",String.class),
                                                        ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                            })
                                    )
                            )
                    )
            )
            .then(Commands.literal("bypass")
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.orealerts.togglebypass"))
                    .executes(ctx -> {
                        toggleBypassCommand(ctx.getSource().getSender(),
                                null,
                                null);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("state", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                builder.suggest("true");
                                builder.suggest("false");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                toggleBypassCommand(ctx.getSource().getSender(),
                                        ctx.getArgument("state",String.class),
                                        null);
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .executes(ctx -> {
                                        toggleBypassCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("state",String.class),
                                                ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            );

    public static LiteralArgumentBuilder<CommandSourceStack> getOresCommands() {
        return oresCommands;
    }

    private static void toggleAlertsCommand(CommandSender sender, String type, String state, Player player) {

        player = VinUtils.checkForPlayerPresent(sender, player);
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();

        if (type.equals("small")) {
            boolean value = state != null ? Boolean.parseBoolean(state) : player.permissionValue("vineriumcore.orealerts.disablesmall") != TriState.TRUE;
            if (!value) {
                vaultManager.getPermissionProvider().playerRemove(null, player, "vineriumcore.orealerts.disablesmall");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_small_true"));
            }
            else {
                vaultManager.getPermissionProvider().playerAdd(null,player,"vineriumcore.orealerts.disablesmall");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_small_false"));
            }
        }

        if (type.equals("large")) {
            boolean value = state != null ? Boolean.parseBoolean(state) : player.permissionValue("vineriumcore.orealerts.disablelarge") != TriState.TRUE;
            if (!value) {
                vaultManager.getPermissionProvider().playerRemove(null, player, "vineriumcore.orealerts.disablelarge");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_large_true"));
            }
            else {
                vaultManager.getPermissionProvider().playerAdd(null,player,"vineriumcore.orealerts.disablelarge");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_large_false"));
            }
        }
    }

    private static void toggleSoundsCommand(CommandSender sender, String type, String state, Player player) {

        player = VinUtils.checkForPlayerPresent(sender, player);
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();

        if (type.equals("small")) {
            boolean value = state != null ? Boolean.parseBoolean(state) : player.permissionValue("vineriumcore.orealerts.disablesmallsound") != TriState.TRUE;
            if (!value) {
                vaultManager.getPermissionProvider().playerRemove(null, player, "vineriumcore.orealerts.disablesmallsound");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_small_sounds_true"));
            }
            else {
                vaultManager.getPermissionProvider().playerAdd(null,player,"vineriumcore.orealerts.disablesmallsound");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_small_sounds_false"));
            }
        }

        if (type.equals("large")) {
            boolean value = state != null ? Boolean.parseBoolean(state) : player.permissionValue("vineriumcore.orealerts.disablelargesound") != TriState.TRUE;
            if (!value) {
                vaultManager.getPermissionProvider().playerRemove(null, player, "vineriumcore.orealerts.disablelargesound");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_large_sounds_true"));
            }
            else {
                vaultManager.getPermissionProvider().playerAdd(null,player,"vineriumcore.orealerts.disablelargesound");
                player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_large_sounds_false"));
            }
        }
    }

    private static void toggleBypassCommand(CommandSender sender, String state, Player player) {

        player = VinUtils.checkForPlayerPresent(sender, player);
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();

        boolean value = state != null ? Boolean.parseBoolean(state) : player.permissionValue("vineriumcore.orealerts.bypass") != TriState.TRUE;
        if (!value) {
            vaultManager.getPermissionProvider().playerRemove(null, player, "vineriumcore.orealerts.bypass");
            player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_bypass_false"));
        }
        else {
            vaultManager.getPermissionProvider().playerAdd(null,player,"vineriumcore.orealerts.bypass");
            player.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"ore_alert_toggle_bypass_true"));
        }
    }
}
