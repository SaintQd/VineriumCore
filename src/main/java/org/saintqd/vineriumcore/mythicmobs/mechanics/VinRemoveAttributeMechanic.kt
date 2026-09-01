package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.utils.annotations.MythicMechanic
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attributable
import org.saintqd.vineriumlib.utils.VinUtils

@MythicMechanic(author = "SaintQd", name = "vinremoveattribute")
class VinRemoveAttributeMechanic(event : MythicMechanicLoadEvent) : ITargetedEntitySkill {

    val attributeTypeKey = NamespacedKey.fromString(event.config.getPlaceholderString(arrayOf("attribute", "attr", "a"), "").get())!!
    val nameKey = NamespacedKey.fromString(event.config.getPlaceholderString(arrayOf("name", "n"), "").get())!!

    override fun castAtEntity(
        data: SkillMetadata,
        target: AbstractEntity
    ): SkillResult {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vinremoveattribute")

        val attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(attributeTypeKey) ?: return SkillResult.INVALID_CONFIG

        val bukkitEntity = target.bukkitEntity
        if (bukkitEntity !is Attributable)
            return SkillResult.INVALID_TARGET

        val attributeInstance = bukkitEntity.getAttribute(attribute) ?: return SkillResult.INVALID_CONFIG
        if (attributeInstance.getModifier(nameKey) == null)
            return SkillResult.SUCCESS
        attributeInstance.removeModifier(nameKey)

        return SkillResult.SUCCESS
    }
}