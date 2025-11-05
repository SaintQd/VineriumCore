package org.saintqd.vineriumcore.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.managers.VaultManager;
import org.saintqd.vineriumlib.utils.VinUtils;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        VaultManager vaultManager = VineriumLib.inst().getVaultManager();
        if (vaultManager == null || vaultManager.getChatProvider() == null) return;

        String suffixSymbol = vaultManager.getChatProvider().getPlayerSuffix(event.getPlayer()).replace(" ","");
        String suffixName = VineriumCore.inst().getSuffixManager().getSuffixSymbolsToNames().get(suffixSymbol);
        if (suffixName == null) return;
        VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
        if (!event.getPlayer().hasPermission(suffix.getPermission())) {
            vaultManager.getChatProvider().setPlayerSuffix(event.getPlayer(),null);
            event.getPlayer().sendMessage(VinUtils.parseString("<gold>Ваш суффикс был убран, поскольку у вас больше нет прав на его использование."));
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof VinGUIHolder)) return;
        for (int slot : event.getRawSlots())
            if (slot <= event.getInventory().getSize() - 1)
                event.setCancelled(true);
    }
}
