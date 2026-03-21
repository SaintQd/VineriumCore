package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.core.skills.SkillCondition
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class VinHasPdcValueMechanic(line: String, config : MythicLineConfig) : SkillCondition(line), IEntityCondition {

    val key : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("namespace", "ns"),"minecraft"),
        config.getString(arrayOf("key", "k"),"none")
    )

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        if (entity is Player) {
            return entity.persistentDataContainer.has(key)
        }
        return true
    }
}