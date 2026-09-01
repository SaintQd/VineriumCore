package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedDouble
import io.lumine.mythic.core.skills.SkillCondition
import io.lumine.mythic.core.utils.annotations.MythicCondition
import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.entity.LivingEntity

@MythicCondition(author = "SaintQd", name = "vinitemdurability")
class VinItemDurabilityCondition(event : MythicConditionLoadEvent) : IEntityCondition {

    val percent: Boolean = event.config.getBoolean(arrayOf("percent", "p"), false)
    val durability: RangedDouble = RangedDouble(event.config.getString(arrayOf("durability", "d"), ">0.0"))

    override fun check(abstractEntity: AbstractEntity?): Boolean {
        abstractEntity?.let { abstractEntity ->
            val entity = abstractEntity.bukkitEntity
            if (entity is LivingEntity) {
                entity.equipment?.let { equipment ->
                    if (equipment.itemInMainHand.type != Material.AIR && equipment.itemInMainHand.hasData(
                            DataComponentTypes.MAX_DAMAGE)) {
                        val maxDurability = equipment.itemInMainHand.getData(
                            DataComponentTypes.MAX_DAMAGE)
                        var currentDurability = equipment.itemInMainHand.getData(
                            DataComponentTypes.DAMAGE)
                        if (currentDurability == null || maxDurability == null)
                            return true
                        currentDurability = maxDurability - currentDurability
                        if (percent) {
                            val currentPercentage = currentDurability.toDouble() / maxDurability.toDouble()
                            return durability.equals(currentPercentage)
                        }
                        else {
                            return durability.equals(currentDurability.toDouble())
                        }
                    }
                }
            }
        }
        return true
    }
}