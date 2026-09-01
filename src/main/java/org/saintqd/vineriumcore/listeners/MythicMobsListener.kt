package org.saintqd.vineriumcore.listeners

import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.skills.ISkillMechanic
import io.lumine.mythic.api.skills.conditions.ISkillCondition
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent
import io.lumine.mythic.core.skills.SkillExecutor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.saintqd.vineriumcore.mythicmobs.conditions.VinBlockHumidityCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinBlockTemperatureCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinHasPdcValueCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinItemDurabilityCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinStatisticCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinStringContainsCondition
import org.saintqd.vineriumcore.mythicmobs.conditions.VinStringStartsWithCondition
import org.saintqd.vineriumcore.mythicmobs.mechanics.VinAddAttributeMechanic
import org.saintqd.vineriumcore.mythicmobs.mechanics.VinGetPdcValueMechanic
import org.saintqd.vineriumcore.mythicmobs.mechanics.VinRemoveAttributeMechanic
import org.saintqd.vineriumcore.mythicmobs.mechanics.VinRemovePdcValueMechanic
import org.saintqd.vineriumcore.mythicmobs.mechanics.VinSetPdcValueMechanic
import java.io.File

class MythicMobsListener : Listener {

    private val mechanics = hashMapOf<String,(SkillExecutor, File, String, MythicLineConfig) -> ISkillMechanic>()
    private val conditions = hashMapOf<String,(String, MythicLineConfig) -> ISkillCondition>()

    fun registerMechanics() {
        //mechanics["vingetpdcvalue"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinGetPdcValueMechanic(manager,file,line,config) }
        //mechanics["vinsetpdcvalue"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinSetPdcValueMechanic(manager,file,line,config) }
        //mechanics["vinremovepdcvalue"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinRemovePdcValueMechanic(manager,file,line,config) }
        //mechanics["vinaddattribute"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinAddAttributeMechanic(manager,file,line,config) }
        //mechanics["vinremoveattribute"] = { manager : SkillExecutor, file : File, line : String, config : MythicLineConfig -> VinRemoveAttributeMechanic(manager,file,line,config) }
    }

    fun registerConditions() {
        //conditions["vinitemdurability"] = { line: String, mlc : MythicLineConfig -> VinItemDurabilityCondition(line,mlc) }
        //conditions["vinhaspdcvalue"] = { line: String, mlc : MythicLineConfig -> VinHasPdcValueCondition(line,mlc) }
        //conditions["vinstringstartswith"] = { line: String, mlc : MythicLineConfig -> VinStringStartsWithCondition(line,mlc) }
        //conditions["vinstringcontains"] = { line: String, mlc : MythicLineConfig -> VinStringContainsCondition(line,mlc) }
        //conditions["vintemperature"] = { line: String, mlc : MythicLineConfig -> VinBlockTemperatureCondition(line,mlc) }
        //conditions["vinhumidity"] = { line: String, mlc : MythicLineConfig -> VinBlockHumidityCondition(line,mlc) }
        //conditions["vinstatistic"] = { line: String, mlc : MythicLineConfig -> VinStatisticCondition(line,mlc) }
    }

    @EventHandler
    @Suppress("UNUSED")
    fun onMythicMechanicLoad(event: MythicMechanicLoadEvent) {
        val file = event.container.file
        val configLine = event.container.config.line
        val manager = event.container.manager
        mechanics[event.mechanicName.lowercase()]?.let { mechanic ->
            event.register(mechanic.invoke(manager, file, configLine,event.config))
        }
    }

    @EventHandler
    @Suppress("UNUSED")
    fun onMythicConditionLoad(event: MythicConditionLoadEvent) {
        conditions[event.conditionName.lowercase()]?.let { condition ->
            event.register(condition.invoke(event.argument,event.config))
        }
    }
}