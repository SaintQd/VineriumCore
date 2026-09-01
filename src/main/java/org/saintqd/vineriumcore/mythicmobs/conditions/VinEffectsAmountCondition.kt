package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RangedDouble
import io.lumine.mythic.core.utils.annotations.MythicCondition
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity

@MythicCondition(author = "SaintQd", name = "vineffectsamount")
class VinEffectsAmountCondition(event : MythicConditionLoadEvent) : IEntityCondition {

    val amount = RangedDouble(event.config.getString(arrayOf("amount", "a"),">0"))
    val effects = hashSetOf<NamespacedKey>()

    init {
        for (effectName in event.config.getString(arrayOf("effects", "e"), "").split(",")) {
            val possibleEffect = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(NamespacedKey.fromString(effectName)!!)
            if (possibleEffect != null)
                effects.add(possibleEffect.key)
        }
    }

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        if (entity !is LivingEntity) return false

        var totalEffects = 0
        for (effect in entity.activePotionEffects) {
            if (effect.type.key in effects) {
                totalEffects++
            }
        }
        return amount.equals(totalEffects)
    }

}