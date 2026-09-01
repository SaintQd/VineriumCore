package org.saintqd.vineriumcore.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.commands.CommandUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.saintqd.vineriumcore.VineriumCore;

public class VinMessagingUtil {

    public static void sendStringToChat(LocalPlayer player, String message) {
        String effective = CommandUtils.replaceColorMacros(message);
        effective = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, effective);
        BukkitAdapter.adapt(player).sendRichMessage(effective.replace("\n","<newline>"));
    }

    public static void sendStringToTitle(LocalPlayer player, String message) {
        String[] parts = message.replaceAll("\\\\n", "\n").split("\\n", 2);
        String title = CommandUtils.replaceColorMacros(parts[0]);
        title = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, title);
        if (parts.length > 1) {
            String subtitle = CommandUtils.replaceColorMacros(parts[1]);
            BukkitAdapter.adapt(player).showTitle(Title.title(
                    MiniMessage.miniMessage().deserialize(title),
                    MiniMessage.miniMessage().deserialize(subtitle),
                    Title.Times.times(Ticks.duration(10), Ticks.duration(30), Ticks.duration(20))
            ));
        } else {
            BukkitAdapter.adapt(player).showTitle(Title.title(
                    MiniMessage.miniMessage().deserialize(title),
                    Component.empty(),
                    Title.Times.times(Ticks.duration(10), Ticks.duration(30), Ticks.duration(20))
            ));
        }
    }

    public static void sendStringToActionBar(LocalPlayer player, String message) {
        String[] parts = message.replaceAll("\\\\n", "\n").split("\\n", 2);
        String finalMessage = parts[0];
        if (parts.length > 1) {
            String delimiter = VineriumCore.inst().getConfig().getString("WorldGuardFlags.VinMessage.Delimiter","<reset> | ");
            finalMessage = finalMessage + delimiter + parts[1];
        }
        String actionBarMessage = CommandUtils.replaceColorMacros(finalMessage);
        actionBarMessage = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, actionBarMessage);
        BukkitAdapter.adapt(player).sendActionBar(MiniMessage.miniMessage().deserialize(actionBarMessage));
    }
}
