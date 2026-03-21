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
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File

class VinRemovePdcValueMechanic(manager : SkillExecutor, file : File, line : String, config : MythicLineConfig) : SkillMechanic(manager,file,line,config), ITargetedEntitySkill {

    val key : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("namespace", "ns"),"minecraft"),
        config.getString(arrayOf("key", "k"),"none")
    )

    override fun castAtEntity(
        data: SkillMetadata,
        target: AbstractEntity
    ): SkillResult {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vinremovepdcvalue")

        val bukkitEntity = target.bukkitEntity
        if (bukkitEntity !is Player)
            return SkillResult.INVALID_TARGET

        bukkitEntity.persistentDataContainer.remove(key)

        return SkillResult.SUCCESS
    }
}