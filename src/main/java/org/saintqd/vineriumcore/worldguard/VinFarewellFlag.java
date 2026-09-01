package org.saintqd.vineriumcore.worldguard;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import net.kyori.adventure.util.TriState;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

public class VinFarewellFlag extends Handler {

    public static final VinFarewellFlag.Factory FACTORY = new VinFarewellFlag.Factory();
    public static class Factory extends Handler.Factory<VinFarewellFlag> {
        @Override
        public VinFarewellFlag create(Session session) {
            return new VinFarewellFlag(session);
        }
    }

    private Set<String> lastMessageStack = Collections.emptySet();
    private Set<String> lastActionBarStack = Collections.emptySet();
    private Set<String> lastTitleStack = Collections.emptySet();

    public VinFarewellFlag(Session session) {
        super(session);
    }

    private Set<String> getMessages(LocalPlayer player, ApplicableRegionSet set) {
        return Sets.newLinkedHashSet(set.queryAllValues(player, Flags.VIN_FAREWELL_MESSAGE));
    }

    @Override
    public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set) {
        lastMessageStack = getMessages(player, set);
        lastActionBarStack = getMessages(player, set);
        lastTitleStack = getMessages(player, set);
    }

    @Override
    public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
                                   Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {

        Player bukkitPlayer = BukkitAdapter.adapt(player);
        if (bukkitPlayer.permissionValue("vineriumcore.regionmessage.none") == TriState.TRUE)
            return true;

        if (bukkitPlayer.permissionValue("vineriumcore.regionmessage.actionbar") == TriState.TRUE) {
            lastActionBarStack = collectAndSend(player, toSet,
                    lastActionBarStack, VinMessagingUtil::sendStringToActionBar);
        }
        else if (bukkitPlayer.permissionValue("vineriumcore.regionmessage.chat") == TriState.TRUE) {
            lastMessageStack = collectAndSend(player, toSet,
                    lastMessageStack, VinMessagingUtil::sendStringToChat);
        }
        else
            lastTitleStack = collectAndSend(player, toSet,
                    lastTitleStack, VinMessagingUtil::sendStringToTitle);

        return true;
    }

    private Set<String> collectAndSend(LocalPlayer player, ApplicableRegionSet toSet,
                                       Set<String> stack, BiConsumer<LocalPlayer, String> msgFunc) {
        Set<String> messages = getMessages(player, toSet);

        if (!messages.isEmpty()) {
            // Due to flag priorities, we have to collect the lower
            // priority flag values separately
            for (ProtectedRegion region : toSet) {
                String message = region.getFlag(Flags.VIN_FAREWELL_MESSAGE);
                if (message != null) {
                    messages.add(message);
                }
            }
        }

        for (String message : stack) {
            if (!messages.contains(message)) {
                msgFunc.accept(player, message);
                break;
            }
        }
        return messages;
    }


}
