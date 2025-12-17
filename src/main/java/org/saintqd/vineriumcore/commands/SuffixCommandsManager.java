package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.gui.SuffixGUI;
import org.saintqd.vineriumcore.managers.SuffixManager;
import org.saintqd.vineriumcore.suffix.CommunitySuffix;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.utils.VinUtils;

public class SuffixCommandsManager {

    private static final LiteralArgumentBuilder<CommandSourceStack> suffixCommands = Commands.literal("suffix")
            .executes(commandContext -> {
                openSuffixMenuCommand(commandContext.getSource().getSender(),null);
                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("menu")
                    .executes(ctx -> {
                        openSuffixMenuCommand(ctx.getSource().getSender(),null);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("player", ArgumentTypes.player())
                            .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                            .executes(ctx -> {
                                openSuffixMenuCommand(ctx.getSource().getSender(),ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                    )
            )
            .then(Commands.literal("set")
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                    .executes(ctx -> {
                        setSuffixCommand(ctx.getSource().getSender(),null,null);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("suffix", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                String partName = builder.getRemaining();
                                VineriumCore.inst().getSuffixManager().getSuffixes().forEach((suffixName, suffix) -> {
                                    if (suffixName.toLowerCase().startsWith(partName.toLowerCase()))
                                        builder.suggest(suffixName);
                                });
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                setSuffixCommand(ctx.getSource().getSender(),ctx.getArgument("suffix", String.class),null);
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .executes(ctx -> {
                                        setSuffixCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("suffix", String.class),
                                                ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            )
            .then(Commands.literal("clear")
                    .executes(ctx -> {
                        clearSuffixCommand(ctx.getSource().getSender(),null);
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                    .then(Commands.argument("player", ArgumentTypes.player())
                            .executes(ctx -> {
                                clearSuffixCommand(ctx.getSource().getSender(),
                                        ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst());
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                    )
            )
            .then(Commands.literal("adduser")
                    .then(Commands.argument("suffix", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                String partName = builder.getRemaining();
                                VineriumCore.inst().getSuffixManager().getCommunitySuffixes().forEach((suffixName, suffix) -> {
                                    if (suffixName.toLowerCase().startsWith(partName.toLowerCase()))
                                        builder.suggest(suffixName);
                                });
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .suggests((ctx,builder) -> {
                                        String partName = builder.getRemaining();
                                         Bukkit.getOnlinePlayers().forEach((player) -> {
                                             if (player.getName().startsWith(partName.toLowerCase()))
                                                 builder.suggest(player.getName());
                                        });
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        communitySuffixAddUserCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("suffix", String.class),
                                                ctx.getArgument("player", String.class));
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            )
            .then(Commands.literal("removeuser")
                    .then(Commands.argument("suffix", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                String partName = builder.getRemaining();
                                VineriumCore.inst().getSuffixManager().getCommunitySuffixes().forEach((suffixName, suffix) -> {
                                    if (suffixName.toLowerCase().startsWith(partName.toLowerCase()))
                                        builder.suggest(suffixName);
                                });
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .suggests((ctx,builder) -> {
                                        String partName = builder.getRemaining();
                                         Bukkit.getOnlinePlayers().forEach((player) -> {
                                             if (player.getName().startsWith(partName.toLowerCase()))
                                                 builder.suggest(player.getName());
                                        });
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        communitySuffixRemoveUserCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("suffix", String.class),
                                                ctx.getArgument("player", String.class));
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            )
            .then(Commands.literal("list")
                    .then(Commands.argument("suffix", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                String partName = builder.getRemaining();
                                VineriumCore.inst().getSuffixManager().getCommunitySuffixes().forEach((suffixName, suffix) -> {
                                    if (suffixName.toLowerCase().startsWith(partName.toLowerCase()))
                                        builder.suggest(suffixName);
                                });
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                communitySuffixListCommand(ctx.getSource().getSender(),
                                        ctx.getArgument("suffix", String.class));
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                    )
            )
            .then(Commands.literal("setlimit")
                    .requires(predicate -> predicate.getSender().hasPermission("vineriumcore.admin"))
                    .then(Commands.argument("suffix", StringArgumentType.word())
                            .suggests((ctx,builder) -> {
                                String partName = builder.getRemaining();
                                VineriumCore.inst().getSuffixManager().getCommunitySuffixes().forEach((suffixName, suffix) -> {
                                    if (suffixName.toLowerCase().startsWith(partName.toLowerCase()))
                                        builder.suggest(suffixName);
                                });
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("limit", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        communitySuffixSetLimitCommand(ctx.getSource().getSender(),
                                                ctx.getLastChild().getArgument("suffix", String.class),
                                                ctx.getArgument("limit", Integer.class));
                                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            )
            ;

    public static LiteralArgumentBuilder<CommandSourceStack> getSuffixCommands() {
        return suffixCommands;
    }

    private static void openSuffixMenuCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;

        SuffixGUI suffixGUI = new SuffixGUI(player);
        suffixGUI.setMainMenu(1);
        suffixGUI.openInventory();
        if (sender != player)
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixMenuOpenedForPlayer", player.getName()));
    }

    private static void setSuffixCommand(CommandSender sender, String suffixName, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;
        if (suffixName == null) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixCommandUsage"));
            return;
        }
        VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
        if (suffix == null) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixDoesntExist",suffixName));
            return;
        }
        suffix.changeSuffix(sender,player);
    }

    private static void clearSuffixCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;
        VinSuffix.clearSuffix(sender,player);
    }

    private static void communitySuffixListCommand(CommandSender sender, String suffixName) {

        SuffixManager suffixManager = VineriumCore.inst().getSuffixManager();
        if (!suffixManager.getCommunitySuffixes().containsKey(suffixName)) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixDoesntExist",suffixName));
            return;
        }
        VinSuffix suffix = suffixManager.getSuffixes().get(suffixName);
        CommunitySuffix communitySuffix = suffixManager.getCommunitySuffixes().get(suffixName);
        if (!sender.hasPermission(communitySuffix.getPermission())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixNoPermission"));
            return;
        }
        int index = 1;
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixListMessage",
                suffix.getSymbol(),suffixName,Integer.toString(communitySuffix.getUsers().size()),Integer.toString(communitySuffix.getSuffixLimit())));
        for (String username : communitySuffix.getUsers()) {
            Player onlinePlayer = Bukkit.getPlayer(username);
            if (onlinePlayer != null)
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),
                        "communitySuffixListOnlineMessageFormat",Integer.toString(index),username));
            else
                sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),
                    "communitySuffixListMessageFormat",Integer.toString(index),username));
            index++;
        }
    }

    private static void communitySuffixAddUserCommand(CommandSender sender, String suffixName, String playerName) {

        SuffixManager suffixManager = VineriumCore.inst().getSuffixManager();
        if (!suffixManager.getCommunitySuffixes().containsKey(suffixName)) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixDoesntExist",suffixName));
            return;
        }
        VinSuffix suffix = suffixManager.getSuffixes().get(suffixName);
        CommunitySuffix communitySuffix = suffixManager.getCommunitySuffixes().get(suffixName);
        if (!sender.hasPermission(communitySuffix.getPermission())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixNoPermission"));
            return;
        }
        if (!communitySuffix.isPossibleToAdd()) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixOverLimit"));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixPlayerNotFound",playerName));
            return;
        }
        communitySuffix.getUsers().add(playerName);
        if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerAdd(null,offlinePlayer,suffix.getPermission());
        if (offlinePlayer.isOnline()) {
            offlinePlayer.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixAddMessage",suffix.getSymbol()));
        }
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixAddedSuccessfully",playerName,suffix.getSymbol()));
    }

    private static void communitySuffixRemoveUserCommand(CommandSender sender, String suffixName, String playerName) {

        SuffixManager suffixManager = VineriumCore.inst().getSuffixManager();
        if (!suffixManager.getCommunitySuffixes().containsKey(suffixName)) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixDoesntExist",suffixName));
            return;
        }
        VinSuffix suffix = suffixManager.getSuffixes().get(suffixName);
        CommunitySuffix communitySuffix = suffixManager.getCommunitySuffixes().get(suffixName);
        if (!sender.hasPermission(communitySuffix.getPermission())) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixNoPermission"));
            return;
        }
        if (!communitySuffix.getUsers().contains(playerName)) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixCommunityPlayerNotFound",playerName));
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        communitySuffix.getUsers().remove(playerName);
        if (VineriumLib.inst().getVaultManager() != null && VineriumLib.inst().getVaultManager().getPermissionProvider() != null)
            VineriumLib.inst().getVaultManager().getPermissionProvider().playerRemove(null,offlinePlayer,suffix.getPermission());
        if (offlinePlayer.isOnline()) {
            VineriumCore.inst().getSuffixManager().checkSuffixPermission(offlinePlayer.getPlayer());
            offlinePlayer.getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixRemoveMessage",suffix.getSymbol()));
        }
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixRemovedSuccessfully",playerName,suffix.getSymbol()));
    }

    private static void communitySuffixSetLimitCommand(CommandSender sender, String suffixName, int limit) {

        SuffixManager suffixManager = VineriumCore.inst().getSuffixManager();
        if (!suffixManager.getCommunitySuffixes().containsKey(suffixName)) {
            sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixDoesntExist",suffixName));
            return;
        }
        VinSuffix suffix = suffixManager.getSuffixes().get(suffixName);
        CommunitySuffix communitySuffix = suffixManager.getCommunitySuffixes().get(suffixName);
        communitySuffix.setSuffixLimit(limit);
        sender.sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"communitySuffixSetLimitSuccessfully",suffix.getSymbol(),Integer.toString(limit)));
    }
}
