package org.saintqd.vineriumcore.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.gui.VinGUI;
import org.saintqd.vineriumlib.gui.VinGUIButton;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.HashMap;

public class SuffixGUI extends VinGUI {

    public SuffixGUI(Player player) {
        super(player);
    }

    public Inventory setMainMenu() {
        int index = 0;
        HashMap<Integer,ItemStack> suffixItems = new HashMap<>();
        for (VinSuffix suffix : VineriumCore.inst().getSuffixManager().getSuffixes().values()) {
            boolean hasPermission = getPlayer().hasPermission(suffix.getPermission());
            if (!hasPermission && VineriumCore.inst().getSuffixManager().isHideWithoutPermission()) continue;

            ItemStack suffixItem = ItemStack.of(Material.STONE);
            suffixItem.setData(DataComponentTypes.CUSTOM_NAME, VinUtils.parseString(suffix.getDisplayName()));
            if (suffix.getItemModel() != null)
                suffixItem.setData(DataComponentTypes.ITEM_MODEL, VinUtils.parseNamespace(suffix.getItemModel().toLowerCase()));

            ItemLore.Builder loreBuilder = ItemLore.lore();

            if (!suffix.getDesc().isEmpty()) {
                loreBuilder.addLine(Component.empty());
                loreBuilder.addLines(VinUtils.parseStringList(suffix.getDesc()));
            }
            if (hasPermission) {
                loreBuilder.addLine(Component.empty());
                loreBuilder.addLine(VinUtils.parseString("<yellow>Нажмите, чтобы выбрать суффикс..."));
                VinGUIButton button = new VinGUIButton().consumer(event -> suffix.changeSuffix(getPlayer(),getPlayer()));
                getButtons().put(index,button);
            }
            else {
                loreBuilder.addLine(VinUtils.parseString("<red>У вас нет прав для выбора данного суффикса!"));
            }

            suffixItem.setData(DataComponentTypes.LORE, loreBuilder.build());
            suffixItems.put(index,suffixItem);
            index++;
        }
        Inventory inventory = Bukkit.createInventory(new VinGUIHolder(this),(index-1)/9*9+9,VinUtils.parseString("<light_purple><b>Суффиксы"));
        for (int slot : suffixItems.keySet()) {
            inventory.setItem(slot,suffixItems.get(slot));
        }
        setInventory(inventory);
        return inventory;
    }
}
