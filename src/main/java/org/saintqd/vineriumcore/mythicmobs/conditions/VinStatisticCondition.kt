package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedInt
import io.lumine.mythic.core.skills.SkillCondition
import io.lumine.mythic.core.utils.annotations.MythicCondition
import org.bukkit.Statistic
import org.bukkit.entity.Player

@MythicCondition(author = "SaintQd", name = "vinstatistic")
class VinStatisticCondition(event : MythicConditionLoadEvent) : IEntityCondition {

    val statistic = Statistic.valueOf(event.config.getString(arrayOf("statistic", "stat","s"),"PLAY_ONE_MINUTE").uppercase())
    val value: RangedInt = RangedInt(event.config.getString(arrayOf("value", "v"), ">0"))

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        if (entity is Player) {
            val currentValue = entity.getStatistic(statistic)
            return value.equals(currentValue)
        }
        return true
    }
}