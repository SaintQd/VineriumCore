package org.saintqd.vineriumcore.mythicmobs.mechanics

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ITargetedEntitySkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble
import io.lumine.mythic.core.skills.SkillExecutor
import io.lumine.mythic.core.skills.SkillMechanic
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attributable
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File
import kotlin.collections.set

class VinAddAttributeMechanic(manager : SkillExecutor, file : File, line : String, config : MythicLineConfig) : SkillMechanic(manager,file,line,config), ITargetedEntitySkill {

    val attributeTypekey : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("a_namespace", "a_ns"),"minecraft"),
        config.getString(arrayOf("a_key", "a_k"),"none")
    )
    val nameKey : NamespacedKey = NamespacedKey(
        config.getString(arrayOf("namespace", "ns"),"minecraft"),
        config.getString(arrayOf("key", "k"),"none")
    )
    val operation : AttributeModifier.Operation = AttributeModifier.Operation.valueOf(config.getString(arrayOf("operation", "o"),"ADD_NUMBER"))
    val value : PlaceholderDouble = config.getPlaceholderDouble(arrayOf("value", "v"),"1.0")
    val permanent = config.getBoolean(arrayOf("permanent", "p"),false)

    override fun castAtEntity(
        data: SkillMetadata,
        target: AbstractEntity
    ): SkillResult {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vinaddattribute")

        val attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(attributeTypekey) ?: return SkillResult.INVALID_CONFIG

        val bukkitEntity = target.bukkitEntity
        if (bukkitEntity !is Attributable)
            return SkillResult.INVALID_TARGET

        val attributeInstance = bukkitEntity.getAttribute(attribute) ?: return SkillResult.INVALID_CONFIG

        val parsedValue = value.get(data,target)
        val modifier = AttributeModifier(nameKey,parsedValue,operation)

        if (permanent)
            attributeInstance.addModifier(modifier)
        else
            attributeInstance.addTransientModifier(modifier)

        return SkillResult.SUCCESS
    }
}