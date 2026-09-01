package org.saintqd.vineriumcore.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.suffix.VinSuffix;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.VinGUI;
import org.saintqd.vineriumlib.gui.VinGUIButton;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.*;

public class SuffixGUI extends VinGUI {

    public SuffixGUI(Player player) {
        super(player);
    }

    public Inventory setMainMenu(int page) {
        int slotIndex = 0;
        int loopIndex = 0;
        int menuPageSize = VineriumCore.inst().getSuffixManager().getMenuPageSize();
        HashMap<Integer,ItemStack> suffixItems = new HashMap<>();

        // Создаём список пермишенов на суффиксы, которые есть у игрока
        List<String> suffixPermissions = new ArrayList<>(getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("vineriumcore.suffix")).toList());
        if (getPlayer().hasPermission("vineriumcore.admin") || getPlayer().isOp())
            suffixPermissions.addAll(VineriumCore.inst().getSuffixManager().getPermissionsToSuffix().keySet());

        // Создаём из списка пермишенов список доступных игроку суффиксов
        //  True - если есть пермишен
        //  False - если нет
        HashMap<String,Boolean> availableSuffixes = new HashMap<>();
        for (String permission : VineriumCore.inst().getSuffixManager().getPermissionsToSuffix().keySet()) {
            if (suffixPermissions.contains(permission))
                availableSuffixes.put(VineriumCore.inst().getSuffixManager().getPermissionsToSuffix().get(permission),true);
            else
                availableSuffixes.put(VineriumCore.inst().getSuffixManager().getPermissionsToSuffix().get(permission),false);
        }

        for (String suffixName : availableSuffixes.keySet()) {

            VinSuffix suffix = VineriumCore.inst().getSuffixManager().getSuffixes().get(suffixName);
            if (VineriumCore.inst().getSuffixManager().isHideWithoutPermission() && !availableSuffixes.get(suffixName)) continue;

            loopIndex++;
            if (loopIndex <= (page - 1) * (menuPageSize - 9)) continue; // Проверки для отображения суффиксов только текущей страницы
            if (loopIndex > page * (menuPageSize - 9)) break;

            ItemStack suffixItem = ItemStack.of(Material.STONE);
            suffixItem.setData(DataComponentTypes.CUSTOM_NAME, VinUtils.parseString(suffix.getDisplayName()));
            if (suffix.getItemModel() != null)
                suffixItem.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString(suffix.getItemModel().toLowerCase()));

            ItemLore.Builder loreBuilder = ItemLore.lore();

            if (!suffix.getDesc().isEmpty()) {
                loreBuilder.addLine(Component.empty());
                loreBuilder.addLines(VinUtils.parseStringList(suffix.getDesc()));
            }
            if (availableSuffixes.get(suffixName)) {
                loreBuilder.addLine(Component.empty());
                loreBuilder.addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_press_to_select"));
                loreBuilder.addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_press_to_remove"));
                VinGUIButton button = new VinGUIButton().consumer(event -> {
                    switch (event.getClick()) {
                        case LEFT -> VineriumCore.inst().getSuffixManager().changeSuffix(getPlayer(),getPlayer(),suffix);
                        case RIGHT -> VineriumCore.inst().getSuffixManager().clearSuffix(getPlayer(),getPlayer());
                    }
                });
                getButtons().put(slotIndex,button);
            }
            else {
                loreBuilder.addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"suffix_no_permission"));
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
            pageItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_prev_page"));
            pageItem.setData(DataComponentTypes.LORE,
                    ItemLore.lore()
                            .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_prev_page_lore"))
                            .build());

            VinGUIButton button = new VinGUIButton().consumer(event -> {
                setMainMenu(page - 1);
                getPlayer().openInventory(getInventory());
            });
            getButtons().put(menuPageSize-6,button);

            inventory.setItem(menuPageSize-6, pageItem);
        }
        if (loopIndex > page * (menuPageSize - 9)) {
            ItemStack pageItem = ItemStack.of(Material.PAPER);
            @Subst("minecraft:paper") String modelName = VineriumCore.inst().getSuffixManager().getMenuModels().getOrDefault("NextPageButton","paper");
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName));
            pageItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_next_page"));
            pageItem.setData(DataComponentTypes.LORE,
                    ItemLore.lore()
                            .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_next_page_lore"))
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
        closeItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_close"));
        closeItem.setData(DataComponentTypes.LORE,
                ItemLore.lore()
                        .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_close_lore"))
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
