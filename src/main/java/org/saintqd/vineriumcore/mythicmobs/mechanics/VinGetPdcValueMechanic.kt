package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.core.skills.SkillExecutor
import io.lumine.mythic.core.skills.SkillMechanic
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File

class VinGetPdcValueMechanic(manager : SkillExecutor, file : File, line : String, config : MythicLineConfig) : SkillMechanic(manager,file,line,config), ITargetedEntitySkill {

    val key : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("namespace", "ns"),"minecraft"),
        config.getString(arrayOf("key", "k"),"none")
    )
    val name : String = config.getString(arrayOf("name", "n"),"vin_${key.key}")
    val type : String = config.getString(arrayOf("type", "t"),"STRING")

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