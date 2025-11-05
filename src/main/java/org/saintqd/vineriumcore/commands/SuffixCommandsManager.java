package org.saintqd.vineriumcore.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.gui.SuffixGUI;
import org.saintqd.vineriumcore.suffix.VinSuffix;
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
                                String partName = builder.getInput().replace("/vin suffix set ","");
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
            ;

    public static LiteralArgumentBuilder<CommandSourceStack> getSuffixCommands() {
        return suffixCommands;
    }

    private static void openSuffixMenuCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;

        SuffixGUI suffixGUI = new SuffixGUI(player);
        suffixGUI.setMainMenu();
        suffixGUI.openInventory();
        if (sender != player)
            sender.sendMessage(VinUtils.parseString("<yellow>Меню суффиксов открыто для игрока <gold>"+player.getName()+"<yellow>."));
    }

    private static void setSuffixCommand(CommandSender sender, String suffixName, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;
        if (suffixName == null) {
            sender.sendMessage(VinUtils.parseString("<red>Использование: <gold>/vin suffix set [название_суффикса] <никнейм>"));
            return;
        }
        VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
        if (suffix == null) {
            sender.sendMessage(VinUtils.parseString("<red>Суффикс с названием <gold>"+ suffixName +" <red>не существует."));
            return;
        }
        suffix.changeSuffix(sender,player);
    }

    private static void clearSuffixCommand(CommandSender sender, Player player) {

        player = VinUtils.checkForPlayerPresent(sender,player);
        if (player == null) return;
        VinSuffix.clearSuffix(sender,player);
    }
}
