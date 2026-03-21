package org.saintqd.vineriumcore.gui

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.saintqd.vineriumcore.VineriumCore
import org.saintqd.vineriumcore.managers.DecorationManager
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.gui.VinGUI
import org.saintqd.vineriumlib.gui.VinGUIButton
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder
import org.saintqd.vineriumlib.utils.VinUtils
import kotlin.collections.set

class DecorationGUI(player: Player) : VinGUI(player) {

    fun setDecorationMenu(itemStack: ItemStack?) {
        val size = 36
        inventory = Bukkit.createInventory(
            VinGUIHolder(this), size,
            VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "item_skin_gui_title"))
        buttons.clear()

        val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
        fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
        for (slot in size - 9..< size) {
            inventory.setItem(slot, fillerItem.clone())
        }

        // Создаём список пермишенов на скины, которые есть у игрока
        val decorationPermissions = player.effectivePermissions.map { it.permission }
            .filter { permission -> DecorationManager.instance.permissionsToKeys.containsKey(permission) }.toMutableList()
        if (player.hasPermission("vineriumcore.admin") || player.isOp)
            decorationPermissions.addAll(DecorationManager.instance.permissionsToKeys.keys)

        // Создаём из списка пермишенов список доступных игроку скинов
        //  True - если есть пермишен
        //  False - если нет
        val availableDecorations = hashMapOf<String, Boolean>()
        for (permission in DecorationManager.instance.permissionsToKeys.keys) {
            DecorationManager.instance.permissionsToKeys[permission]?.let { decorationName ->
                availableDecorations[decorationName] = decorationPermissions.contains(permission)
            }
        }
        DecorationManager.instance.decorationElements.filterValues { it.permission.isEmpty() }
            .forEach { (decorationName, _) ->
            availableDecorations[decorationName] = true
        }

        var slotIndex = 0

        itemStack?.let { itemStack ->
            availableDecorations.forEach { (decorationName, state) ->
                if (!state)
                    return@forEach
                val decorationElement = DecorationManager.instance.decorationElements[decorationName]!!
                var materialFound = false
                for (material in decorationElement.materials.keys) {
                    if (itemStack.type == material) {
                        materialFound = true
                        break
                    }
                }
                if (!materialFound)
                    return@forEach
                val decorationItemStackBuilder = com.nexomc.nexo.api.NexoItems.itemFromId(decorationElement.id) ?: return@forEach
                val decorationItemStack = decorationItemStackBuilder.build()

                val matsInInventory = linkedMapOf<Material, Int>()

                for (itemStack in player.inventory) {
                    itemStack?.let {
                        if (!itemStack.persistentDataContainer.has(NamespacedKey("nexo","id")))
                            matsInInventory[it.type] = matsInInventory.getOrDefault(it.type, 0) + it.amount
                    }
                }
                if (!player.itemOnCursor.persistentDataContainer.has(NamespacedKey("nexo","id")))
                    matsInInventory[player.itemOnCursor.type] = matsInInventory.getOrDefault(player.itemOnCursor.type, 0) + player.itemOnCursor.amount

                var craftCheck = true

                val lore = ItemLore.lore()
                lore.addLine { Component.empty() }

                decorationElement.materials.forEach { (material, neededAmount) ->
                    val currentAmount = matsInInventory[material] ?: 0
                    val translationString = "<lang:"+material.translationKey()+">"
                    if (currentAmount >= neededAmount)
                        lore.addLine(VinUtils.parseString("<green>✓ $currentAmount/$neededAmount <reset><white>$translationString"));
                    else {
                        craftCheck = false
                        lore.addLine(VinUtils.parseString("<red>✗ $currentAmount/$neededAmount <reset><white>$translationString"));
                    }
                }

                if (craftCheck) {
                    lore.addLine { Component.empty() }
                    val loreLines = VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumcore:decoration_gui_item_lore")]!!
                        .split("<newline>")
                    loreLines.forEach { line ->
                        lore.addLine(VinUtils.parseString(line))
                    }
                    buttons[slotIndex] = VinGUIButton().consumer { event ->
                        createDecorationItem(decorationName)
                        setDecorationMenu(itemStack)
                        player.openInventory(inventory)
                        return@consumer
                    }
                }
                decorationItemStack.setData(DataComponentTypes.LORE,lore)

                inventory.setItem(slotIndex, decorationItemStack)
                slotIndex++
            }
        }

        if (slotIndex==0) {
            val emptyItem = ItemStack.of(Material.PAPER)
            emptyItem.setData(DataComponentTypes.CUSTOM_NAME,VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "decoration_gui_empty"));

            val lore = ItemLore.lore()
            val loreLines = VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumcore:decoration_gui_empty_lore")]!!
                .split("<newline>")
            loreLines.forEach { line ->
                lore.addLine(VinUtils.parseString(line))
            }
            emptyItem.setData(DataComponentTypes.LORE,lore)

            inventory.setItem(4, emptyItem)
        }

        val selectItem = itemStack?.clone() ?: ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE)
        if (selectItem.type == Material.YELLOW_STAINED_GLASS_PANE)
            selectItem.setData(DataComponentTypes.CUSTOM_NAME,
                VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "decoration_gui_insert"))

        val lore = ItemLore.lore()
        val loreLines = VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumcore:decoration_gui_insert_lore")]!!
            .split("<newline>")
        loreLines.forEach { line ->
            lore.addLine(VinUtils.parseString(line))
        }
        selectItem.setData(DataComponentTypes.LORE,lore)

        buttons[size - 5] = VinGUIButton().consumer { event ->
            if (event.cursor.type != Material.AIR && !event.cursor.persistentDataContainer.has(NamespacedKey("nexo","id"))) {
                val newItemStack = event.cursor.clone()
                setDecorationMenu(newItemStack)
                player.openInventory(inventory)
                return@consumer
            }
        }

        inventory.setItem(size - 5, selectItem)
    }

    private fun createDecorationItem(decorationName : String) {
        val element = DecorationManager.instance.decorationElements[decorationName]!!

        val decorationItemStackBuilder = com.nexomc.nexo.api.NexoItems.itemFromId(element.id) ?: return
        val decorationItemStack = decorationItemStackBuilder.build()

        var matsMap = element.materials.toMutableMap()

        for (itemStack in player.inventory) {
            itemStack?.let {
                matsMap[it.type]?.let { amount ->
                    val newAmount = amount - it.amount
                    if (newAmount > 0)
                        matsMap[it.type] = amount - it.amount
                    else
                        matsMap.remove(it.type)
                }
            }
        }
        if (matsMap.isNotEmpty()) {
            player.sendMessage(VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "decoration_no_items"))
            return
        }

        matsMap = element.materials.toMutableMap()

        for (itemStack in player.inventory) {
            itemStack?.let {
                matsMap[it.type]?.let { amount ->
                    val newAmount = amount - it.amount
                    if (newAmount > 0) {
                        matsMap[it.type] = amount - it.amount
                        it.amount = 0
                    }
                    else {
                        it.amount -= amount
                        matsMap.remove(it.type)
                    }
                }
            }
        }
        player.playSound(player,Sound.ENTITY_PLAYER_LEVELUP,1f,2f)
        player.give(decorationItemStack)
    }
}