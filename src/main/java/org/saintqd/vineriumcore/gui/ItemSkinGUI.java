package org.saintqd.vineriumcore.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.intellij.lang.annotations.Subst;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumcore.managers.ItemSkinManager;
import org.saintqd.vineriumlib.VineriumLib;
import org.saintqd.vineriumlib.gui.VinGUI;
import org.saintqd.vineriumlib.gui.VinGUIButton;
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemSkinGUI extends VinGUI {

    public ItemSkinGUI(Player player) {
        super(player);
    }

    public void setItemSkinsMenu(ItemStack itemStack, int page) {

        if (itemStack.getType() == Material.AIR) {
            getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_gui_item_hint"));
            return;
        }

        int size = 45;
        int loopIndex = 0;
        setInventory(Bukkit.createInventory(new VinGUIHolder(this), size,
                VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_gui_title")));
        getButtons().clear();

        ItemStack fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE);
        fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty());
        for (int slot = size - 9; slot < size; slot++) {
            getInventory().setItem(slot, fillerItem.clone());
        }

        // Создаём список пермишенов на скины, которые есть у игрока
        List<String> skinPermissions = new ArrayList<>(getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> ItemSkinManager.INSTANCE.getPermissionsToKeys().containsKey(permission)).toList());
        if (getPlayer().hasPermission("vineriumcore.admin") || getPlayer().isOp())
            skinPermissions.addAll(ItemSkinManager.INSTANCE.getPermissionsToKeys().keySet());

        // Создаём из списка пермишенов список доступных игроку скинов
        //  True - если есть пермишен
        //  False - если нет
        HashMap<String,Boolean> availableSkins = new HashMap<>();
        for (String permission : skinPermissions) {
            availableSkins.put(ItemSkinManager.INSTANCE.getPermissionsToKeys().get(permission), true);
        }

        Material itemStackMaterial = itemStack.getType();

        int slotIndex = 0;
        for (String skinName : availableSkins.keySet()) {
            if (!availableSkins.get(skinName))
                continue;

            ItemSkinManager.ItemSkin itemSkin = ItemSkinManager.INSTANCE.getItemSkins().get(skinName);
            boolean possibleTypeFound = false;
            for (String materialName : itemSkin.materials())
                if (itemStackMaterial.name().startsWith(materialName)) {
                    possibleTypeFound = true;
                    break;
                }
            if (possibleTypeFound) {

                loopIndex++;
                if (loopIndex <= (page - 1) * (size - 9)) continue; // Проверки для отображения скинов только текущей страницы
                if (loopIndex > page * (size - 9)) break;

                ItemStack skinItem = ItemStack.of(Material.STONE);
                ItemLore.Builder lore = ItemLore.lore();

                skinItem.setData(DataComponentTypes.CUSTOM_NAME, VinUtils.parseString(itemSkin.displayName()));
                skinItem.setData(DataComponentTypes.ITEM_MODEL, itemSkin.model());

                String[] loreLines = VineriumLib.inst().getLangManager().getLangLines().get(NamespacedKey.fromString("vineriumcore:item_skin_gui_item_lore")).split("<newline>");
                for (String line : loreLines)
                    lore.addLine(VinUtils.parseString(line));

                skinItem.setData(DataComponentTypes.LORE,lore.build());

                VinGUIButton showButton = new VinGUIButton().consumer(event -> {
                    setItemSkin(skinName);
                });
                getButtons().put(slotIndex,showButton);

                getInventory().setItem(slotIndex, skinItem);
                slotIndex++;
            }
        }

        if (slotIndex==0) {
            ItemStack emptyItem = ItemStack.of(Material.PAPER);
            ItemLore.Builder lore = ItemLore.lore();
            emptyItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_gui_empty"));
            String[] loreLines = VineriumLib.inst().getLangManager().getLangLines().get(NamespacedKey.fromString("vineriumcore:item_skin_gui_empty_lore")).split("<newline>");
            for (String line : loreLines)
                lore.addLine(VinUtils.parseString(line));

            emptyItem.setData(DataComponentTypes.LORE,lore.build());
            getInventory().setItem(4,emptyItem);
        }

        ItemStack defaultItem = ItemStack.of(Material.BARRIER);
        ItemLore.Builder lore = ItemLore.lore();
        defaultItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_gui_reset"));
        String[] loreLines = VineriumLib.inst().getLangManager().getLangLines().get(NamespacedKey.fromString("vineriumcore:item_skin_gui_reset_lore")).split("<newline>");
        for (String line : loreLines)
            lore.addLine(VinUtils.parseString(line));
        defaultItem.setData(DataComponentTypes.LORE,lore.build());

        VinGUIButton removeButton = new VinGUIButton().consumer(event -> {
            removeItemSkin();
            openInventory(getPlayer());
        });
        getButtons().put(size-1,removeButton);
        getInventory().setItem(size-1, defaultItem);

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
                setItemSkinsMenu(itemStack,page - 1);
                getPlayer().openInventory(getInventory());
            });
            getButtons().put(size-6,button);

            getInventory().setItem(size-6, pageItem);
        }
        if (loopIndex > page * (size - 9)) {
            ItemStack pageItem = ItemStack.of(Material.PAPER);
            @Subst("minecraft:paper") String modelName = VineriumCore.inst().getSuffixManager().getMenuModels().getOrDefault("NextPageButton","paper");
            pageItem.setData(DataComponentTypes.ITEM_MODEL, Key.key(modelName));
            pageItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_next_page"));
            pageItem.setData(DataComponentTypes.LORE,
                    ItemLore.lore()
                            .addLine(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(),"menu_next_page_lore"))
                            .build());

            VinGUIButton button = new VinGUIButton().consumer(event -> {
                setItemSkinsMenu(itemStack,page + 1);
                getPlayer().openInventory(getInventory());
            });
            getButtons().put(size-4,button);

            getInventory().setItem(size-4, pageItem);
        }

        getPlayer().openInventory(getInventory());
    }

    private void setItemSkin(String skinName) {
        ItemStack handItem = getPlayer().getInventory().getItemInMainHand();
        ItemSkinManager.ItemSkin itemSkin = ItemSkinManager.INSTANCE.getItemSkins().get(skinName);

        boolean possibleTypeFound = false;
        for (String materialName : itemSkin.materials())
            if (handItem.getType().name().startsWith(materialName)) {
                possibleTypeFound = true;
                break;
            }
        if (!possibleTypeFound) {
            getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_wrong_type"));
            return;
        }

        handItem.setData(DataComponentTypes.ITEM_MODEL,itemSkin.model());
        if (itemSkin.equippableData() != null) {
            EquipmentSlot slot = itemSkin.equippableData().getFirst();
            @Subst("minecraft:paper") String assetId = itemSkin.equippableData().getSecond();
            Equippable.Builder equippable;

            if (handItem.hasData(DataComponentTypes.EQUIPPABLE))
                equippable = handItem.getData(DataComponentTypes.EQUIPPABLE).toBuilder();
            else
                equippable = Equippable.equippable(slot);

            if (assetId != null)
                equippable.assetId(Key.key(assetId));
            else
                equippable.assetId(null);

            handItem.setData(DataComponentTypes.EQUIPPABLE,equippable.build());
        }
        handItem.editPersistentDataContainer(pdc -> pdc.set(ItemSkinManager.ITEM_SKIN_KEY, PersistentDataType.STRING,skinName));
        getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_success"));
    }

    private void removeItemSkin() {
        ItemStack handItem = getPlayer().getInventory().getItemInMainHand();
        if (handItem.getPersistentDataContainer().has(ItemSkinManager.ITEM_SKIN_KEY)) {
            handItem.resetData(DataComponentTypes.ITEM_MODEL);
            handItem.resetData(DataComponentTypes.EQUIPPABLE);
            getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_reset"));
        }
        else {
            getPlayer().sendMessage(VineriumLib.inst().getLangManager().parseLangString(VineriumCore.inst(), "item_skin_reset_not_set"));
        }
    }
}
