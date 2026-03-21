package org.saintqd.vineriumcore.gui

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.saintqd.vineriumcore.VineriumCore
import org.saintqd.vineriumcore.managers.CalendarEventsManager
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.gui.VinGUI
import org.saintqd.vineriumlib.gui.holders.VinGUIHolder
import org.saintqd.vineriumlib.utils.VinUtils
import java.time.LocalDateTime

class CalendarEventGUI(player: Player) : VinGUI(player) {

    fun setMainMenu() : Inventory {
        inventory = Bukkit.createInventory(VinGUIHolder(this),54,
            VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "calendar_gui_title"))
        buttons.clear()

        val fillerItem = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE)
        fillerItem.setData(DataComponentTypes.CUSTOM_NAME, Component.empty())
        for (index in 0..53)
            inventory.setItem(index,fillerItem)

        val nowTime = LocalDateTime.now()
        val firstDayMonthTime = nowTime.withDayOfMonth(1)
        val startMonthWeekDay = firstDayMonthTime.dayOfWeek.value-1

        for (index in 0..41) { // 7 дней * 6 недель - 1
            val slotIndex = (index % 7 + 1) + (index / 7 * 9)
            val indexDate = firstDayMonthTime.minusDays(startMonthWeekDay.toLong()).plusDays(index.toLong())
            val dayEvents = CalendarEventsManager.instance.calendar[indexDate.dayOfYear]
            val nameComponent = if (indexDate.dayOfMonth == nowTime.dayOfMonth && indexDate.month.value == nowTime.month.value)
                VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "calendar_gui_day_format_today",
                    indexDate.dayOfMonth.toString(),indexDate.month.value.toString(),indexDate.year.toString())
            else VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "calendar_gui_day_format",
                indexDate.dayOfMonth.toString(),indexDate.month.value.toString(),indexDate.year.toString())
            if (dayEvents != null) {
                val eventItem = ItemStack.of(Material.WHITE_STAINED_GLASS_PANE,indexDate.dayOfMonth)
                if (indexDate.dayOfMonth == nowTime.dayOfMonth && indexDate.month.value == nowTime.month.value)
                    eventItem.setData(DataComponentTypes.ITEM_MODEL, Material.RED_STAINED_GLASS_PANE.key)
                eventItem.setData(DataComponentTypes.CUSTOM_NAME,nameComponent)

                val loreBuilder = ItemLore.lore()
                loreBuilder.addLine { VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "calendar_gui_day_text") }
                loreBuilder.addLine { Component.empty() }

                for (eventName in dayEvents) {
                    val event = CalendarEventsManager.instance.events[eventName]!!
                    eventItem.setData(DataComponentTypes.ITEM_MODEL, event.model)

                    loreBuilder.addLine { VinUtils.parseString(" ${event.displayName}<reset><yellow>:") }

                    for (line in event.lore)
                        loreBuilder.addLine { VinUtils.parseString("  $line") }
                    loreBuilder.addLine { Component.empty() }
                }
                fillerItem.setData(DataComponentTypes.LORE, loreBuilder.build())

                inventory.setItem(slotIndex, eventItem)
            }
            else {
                var material = Material.WHITE_STAINED_GLASS_PANE
                if (indexDate.month.value != nowTime.month.value)
                    material = Material.GRAY_STAINED_GLASS_PANE
                if (indexDate.dayOfMonth == nowTime.dayOfMonth && indexDate.month.value == nowTime.month.value)
                    material = Material.RED_STAINED_GLASS_PANE
                val fillerItem = ItemStack.of(material,indexDate.dayOfMonth)
                fillerItem.setData(DataComponentTypes.CUSTOM_NAME,nameComponent)
                fillerItem.setData(DataComponentTypes.LORE, ItemLore.lore(mutableListOf<Component>(
                    VineriumLib.inst().langManager.parseLangString(VineriumCore.inst(), "calendar_gui_no_events"))))
                inventory.setItem(slotIndex,fillerItem)
            }
        }

        return inventory
    }
}