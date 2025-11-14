package org.saintqd.vineriumcore.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.VinGUI;
import org.saintqd.vineriumlib.gui.VinGUIButton;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.HashMap;

public class SuffixGUI extends VinGUI {

    public SuffixGUI(Player player) {
        super(player);
    }

    public Inventory setMainMenu(int page) {
        int slotIndex = 0;
        int loopIndex = 0;
        int menuPageSize = VineriumCore.inst().getSuffixManager().getMenuPageSize();
        HashMap<Integer,ItemStack> suffixItems = new HashMap<>();
        for (VinSuffix suffix : VineriumCore.inst().getSuffixManager().getSuffixes().values()) {

            loopIndex++;
            if (loopIndex <= (page - 1) * (menuPageSize - 9)) continue; // Проверки для отображения суффиксов только текущей страницы
            if (loopIndex > page * (menuPageSize - 9)) break;

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
                loreBuilder.addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixPressToSelect"));
                VinGUIButton button = new VinGUIButton().consumer(event -> suffix.changeSuffix(getPlayer(),getPlayer()));
                getButtons().put(slotIndex,button);
            }
            else {
                loreBuilder.addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffixNoPermission"));
            }

            suffixItem.setData(DataComponentTypes.LORE, loreBuilder.build());
            suffixItems.put(slotIndex,suffixItem);
            slotIndex++;
        }
        Inventory inventory = Bukkit.createInventory(new VinGUIHolder(this),menuPageSize,VinUtils.parseString(VineriumCore.inst().getSuffixManager().getMenuTitle()));
        for (int slot : suffixItems.keySet()) {
            inventory.setItem(slot,suffixItems.get(slot));
        }
        if (page > 1) {
            ItemStack pageItem = ItemStack.of(Material.PAPER);
            @Subst("minecraft:paper") String modelName = VineriumCore.inst().getSuffixManager().getMenuModels().getOrDefault("PrevPageButton","paper");
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName));
            pageItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuPrevPage"));
            pageItem.setData(DataComponentTypes.LORE,
                    ItemLore.lore()
                            .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuPrevPageLore"))
                            .build());

            VinGUIButton button = new VinGUIButton().consumer(event -> {
                setMainMenu(page - 1);
                getPlayer().openInventory(getInventory());
            });
            getButtons().put(menuPageSize-6,button);

            inventory.setItem(menuPageSize-6, pageItem);
        }
        if (loopIndex > page * menuPageSize - 9) {
            ItemStack pageItem = ItemStack.of(Material.PAPER);
            @Subst("minecraft:paper") String modelName = VineriumCore.inst().getSuffixManager().getMenuModels().getOrDefault("NextPageButton","paper");
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName));
            pageItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuNextPage"));
            pageItem.setData(DataComponentTypes.LORE,
                    ItemLore.lore()
                            .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuNextPageLore"))
                            .build());

            VinGUIButton button = new VinGUIButton().consumer(event -> {
                setMainMenu(page + 1);
                getPlayer().openInventory(getInventory());
            });
            getButtons().put(menuPageSize-4,button);

            inventory.setItem(menuPageSize-4, pageItem);
        }

        ItemStack closeItem = ItemStack.of(Material.PAPER);
        @Subst("minecraft:barrier") String modelName = VineriumCore.inst().getSuffixManager().getMenuModels().getOrDefault("CloseButton","barrier");
        closeItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName));
        closeItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuClose"));
        closeItem.setData(DataComponentTypes.LORE,
                ItemLore.lore()
                        .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menuCloseLore"))
                        .build());

        VinGUIButton button = new VinGUIButton().consumer(event -> {
            getPlayer().closeInventory();
        });
        getButtons().put(menuPageSize-5,button);

        inventory.setItem(menuPageSize-5, closeItem);
        setInventory(inventory);

        return inventory;
    }
}
