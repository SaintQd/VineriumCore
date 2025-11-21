package org.saintqd.vineriumcore.listeners;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;
import net.kyori.adventure.util.TriState;

public class DiscordSRVListener {

    @Subscribe
    public void onDiscordMessageReceive(DiscordGuildMessagePreBroadcastEvent event) {
        event.getRecipients().removeIf(sender -> sender.permissionValue("vineriumcore.discord.toggleenabled") == TriState.TRUE);
    }
}
