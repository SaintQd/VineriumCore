package org.saintqd.vineriumcore.managers

import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.saintqd.vineriumlib.VineriumLib
import java.nio.file.Paths
import java.time.LocalDate
import java.util.logging.Level
import kotlin.collections.set

class CalendarEventsManager {

    companion object {
        val instance = CalendarEventsManager()
    }

    val calendar = hashMapOf<Int, MutableList<String>>()
    val events = hashMapOf<String, VinCalendarEvent>()

    class VinCalendarEvent(val name : String, config: ConfigurationSection) {

        val model = NamespacedKey.fromString(config.getString("Model","minecraft:stone")!!)!!
        val displayName = config.getString("Display",name)
        val startDate = if (config.contains("Start")) LocalDate.parse(config.getString("Start")!!) else null
        val endDate = if (config.contains("End")) LocalDate.parse(config.getString("End")!!) else startDate
        val lore = config.getStringList("Lore")
    }

    fun loadTimedEvents(plugin: Plugin) {
        events.clear()

        val eventFilePath = Paths.get(plugin.dataFolder.path, "CalendarEvents.yml")
        val eventFile = eventFilePath.toFile()
        eventFile.createNewFile()

        val eventYaml = YamlConfiguration.loadConfiguration(eventFile)

        for (eventName in eventYaml.getKeys(false)) {
            try {
                val event = VinCalendarEvent(eventName, eventYaml.getConfigurationSection(eventName)!!)
                if (event.startDate != null && event.endDate != null) {
                    for (date in event.startDate.datesUntil(event.endDate)) {
                        val dayTimedEvents = if (!calendar.containsKey(date.dayOfYear))
                            mutableListOf() else calendar[date.dayOfYear]
                        dayTimedEvents!!.add(eventName)
                        calendar[date.dayOfYear] = dayTimedEvents
                    }
                    val dayTimedEvents = if (!calendar.containsKey(event.endDate.dayOfYear))
                        mutableListOf() else calendar[event.endDate.dayOfYear]
                    dayTimedEvents!!.add(eventName)
                    calendar[event.endDate.dayOfYear] = dayTimedEvents
                }
                events[eventName] = event
            } catch (ex: Exception) {
                plugin.logger.log(Level.WARNING, "CalendarEvents: Couldn't load timed event $eventName")
                if (VineriumLib.inst().debugLevel >= 1) ex.printStackTrace()
            }
        }
    }
}