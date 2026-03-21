package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.core.skills.SkillExecutor
import io.lumine.mythic.core.skills.SkillMechanic
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attributable
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File

class VinRemoveAttributeMechanic(manager : SkillExecutor, file : File, line : String, config : MythicLineConfig) : SkillMechanic(manager,file,line,config), ITargetedEntitySkill {

    val attributeTypekey : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("a_namespace", "a_ns"),"minecraft"),
        config.getString(arrayOf("a_key", "a_k"),"none")
    )
    val nameKey : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("namespace", "ns"),"minecraft"),
        config.getString(arrayOf("key", "k"),"none")
    )

    override fun castAtEntity(
        data: SkillMetadata,
        target: AbstractEntity
    ): SkillResult {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vinremoveattribute")

        val attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(attributeTypekey) ?: return SkillResult.INVALID_CONFIG

        val bukkitEntity = target.bukkitEntity
        if (bukkitEntity !is Attributable)
            return SkillResult.INVALID_TARGET

        val attributeInstance = bukkitEntity.getAttribute(attribute) ?: return SkillResult.INVALID_CONFIG
        attributeInstance.removeModifier(nameKey)

        return SkillResult.SUCCESS
    }
}