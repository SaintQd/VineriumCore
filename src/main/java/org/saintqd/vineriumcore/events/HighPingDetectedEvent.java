package org.saintqd.vineriumcore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HighPingDetectedEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    private final int ping;

    public HighPingDetectedEvent(int ping) {
        this.ping = ping;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public int getPing() {
        return ping;
    }
}
