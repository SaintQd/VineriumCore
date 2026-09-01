package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.utils.annotations.MythicMechanic
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumlib.utils.VinUtils

@MythicMechanic(author = "SaintQd", name = "vingetpdcvalue")
class VinGetPdcValueMechanic(event : MythicMechanicLoadEvent) : ITargetedEntitySkill {

    val key = NamespacedKey.fromString(event.config.getPlaceholderString(arrayOf("name", "n"), "").get())!!

    val name : String = event.config.getString(arrayOf("name", "n"),"vin_${key.key}")
    val type : String = event.config.getString(arrayOf("type", "t"),"STRING")

    override fun castAtEntity(
        data: SkillMetadata,
        target: AbstractEntity
    ): SkillResult {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vingetpdcvalue")

        val bukkitEntity = target.bukkitEntity
        if (bukkitEntity !is Player)
            return SkillResult.INVALID_TARGET

        val value = bukkitEntity.persistentDataContainer.getOrDefault(key, PersistentDataType.STRING,"0")

        when(type.uppercase()) {
            "INTEGER" ->
                data.variables.putInt(name,value.toInt())
            "FLOAT" ->
                data.variables.putFloat(name,value.toFloat())
            "DOUBLE" ->
                data.variables.putDouble(name,value.toDouble())
            else ->
                data.variables.putString(name,value)
        }

        return SkillResult.SUCCESS
    }
}