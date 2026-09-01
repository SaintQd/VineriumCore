package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.core.utils.annotations.MythicCondition
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity

@MythicCondition(author = "SaintQd", name = "vinwearingany")
class VinWearingAnyCondition(event : MythicConditionLoadEvent) : IEntityCondition {

    val materialName: String = event.config.getString(arrayOf("material", "m"), "")
    val possibleMaterials = hashSetOf<Material>()

    init {
        if (materialName.isNotEmpty()) {
            if (materialName.startsWith("#")) {
                val tagName = materialName.substring(1)
                Bukkit.getTag("items", NamespacedKey.fromString(tagName)!!, Material::class.java)?.let { tag ->
                    possibleMaterials.addAll(tag.values)
                }
            } else
                possibleMaterials.add(Material.valueOf(materialName))
        }
    }

    override fun check(abstractEntity: AbstractEntity): Boolean {
        val entity = abstractEntity.bukkitEntity
        if (entity is LivingEntity) {
            entity.equipment?.let { equipment ->
                return (equipment.helmet.type in possibleMaterials
                        || equipment.chestplate.type in possibleMaterials
                        || equipment.leggings.type in possibleMaterials
                        || equipment.boots.type in possibleMaterials)
            }
        }
        return false
    }
}