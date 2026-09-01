package org.saintqd.vineriumcore.mythicmobs.conditions

import io.lumine.mythic.api.adapters.AbstractEntity
import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.conditions.IEntityCondition
import io.lumine.mythic.api.skills.conditions.ILocationCondition
import io.lumine.mythic.api.skills.conditions.ISkillMetaComparisonCondition
import io.lumine.mythic.api.skills.placeholders.PlaceholderString
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.core.logging.MythicLogger
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext
import io.lumine.mythic.core.utils.annotations.MythicCondition

@MythicCondition(author = "SaintQd", name = "vinstringcontains")
class VinStringContainsCondition(event : MythicConditionLoadEvent) : IEntityCondition, ILocationCondition, ISkillMetaComparisonCondition {

    val firstValue : PlaceholderString = try {
        PlaceholderString.of(event.config.getString(arrayOf("value1","val1","v1","string","s"),null))
    }
    catch (e: Exception) {
        MythicLogger.errorGenericConfig(event.config, "First variable name must be set.")
        PlaceholderString.of("")
    }
    val secondValue : PlaceholderString = try {
        PlaceholderString.of(event.config.getString(arrayOf("value1","val1","v1","string","s"),null))
    }
    catch (e: Exception) {
        MythicLogger.errorGenericConfig(event.config, "Second variable name must be set.")
        PlaceholderString.of("")
    }

    override fun check(target: AbstractEntity): Boolean {
        val compare1: String = firstValue.get(PlaceholderContext.builder().entity(target).build())
        val compare2: String = secondValue.get(PlaceholderContext.builder().entity(target).build())
        return compare1.contains(compare2)
    }

    override fun check(target: AbstractLocation): Boolean {
        val compare1: String = firstValue.get()
        val compare2: String = secondValue.get()
        return compare1.contains(compare2)
    }

    override fun check(
        data: SkillMetadata,
        target: AbstractEntity
    ): Boolean {
        val compare1: String = firstValue.get(PlaceholderContext.builder().meta(data).entity(target).build())
        val compare2: String = secondValue.get(PlaceholderContext.builder().meta(data).entity(target).build())
        return compare1.contains(compare2)
    }
}