package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.api.skills.conditions.ILocationCondition
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedDouble
import io.lumine.mythic.core.skills.SkillCondition
import io.lumine.mythic.core.utils.annotations.MythicCondition

@MythicCondition(author = "SaintQd", name = "vintemperature")
class VinBlockTemperatureCondition(event : MythicConditionLoadEvent) : IEntityCondition, ILocationCondition {

    val temperature = RangedDouble(event.config.getString(arrayOf("value", "v"), ">0.0"))

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        return temperature.equals(entity.location.block.temperature)
    }

    override fun check(abstractLocation: AbstractLocation): Boolean {
        return temperature.equals(BukkitAdapter.adapt(abstractLocation).block.temperature)
    }

}