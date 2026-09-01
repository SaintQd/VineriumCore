package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.api.skills.conditions.ILocationCondition
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedDouble
import io.lumine.mythic.core.utils.annotations.MythicCondition

@MythicCondition(author = "SaintQd", name = "vinhumidity")
class VinBlockHumidityCondition(event : MythicConditionLoadEvent) : IEntityCondition, ILocationCondition {

    val humidity = RangedDouble(event.config.getString(arrayOf("humidity", "h"), ">0.0"))

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        return humidity.equals(entity.location.block.humidity)
    }

    override fun check(abstractLocation: AbstractLocation): Boolean {
        return humidity.equals(BukkitAdapter.adapt(abstractLocation).block.humidity)
    }

}