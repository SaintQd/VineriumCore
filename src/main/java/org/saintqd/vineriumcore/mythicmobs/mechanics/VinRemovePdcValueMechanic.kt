package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.skills.SkillExecutor
import io.lumine.mythic.core.skills.SkillMechanic
import io.lumine.mythic.core.utils.annotations.MythicMechanic
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File

@MythicMechanic(author = "SaintQd", name = "vinremovepdcvalue")
class VinRemovePdcValueMechanic(event : MythicMechanicLoadEvent) : ITargetedEntitySkill {

    val key = NamespacedKey.fromString(event.config.getPlaceholderString(arrayOf("name", "n"), "").get())!!

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