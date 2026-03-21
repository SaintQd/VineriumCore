package org.saintqd.vineriumcore.managers

import com.google.common.base.Enums
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File
import java.util.logging.Level

class DecorationManager {

    val decorationElements = hashMapOf<String, FurnitureElement>()
    val permissionsToKeys = hashMapOf<String, String>()

    data class FurnitureElement(
        val id : String,
        val materials : HashMap<Material, Int>,
        val permission : String
    )

    companion object {
        val instance : DecorationManager = DecorationManager()
    }

    fun loadParams(plugin : Plugin) {
        decorationElements.clear()
        permissionsToKeys.clear()
        val furnitureDir = File(plugin.dataFolder, "Decorations")
        if (!furnitureDir.exists()) {
            plugin.logger.log(Level.INFO,"Decorations directory does not exist, creating it.")
            if (!furnitureDir.mkdir()) {
                plugin.logger.log(Level.SEVERE,"Could not create Decorations directory!")
                return;
            }
        }
        val filePaths = VinUtils.listFilesInFolder(plugin.dataFolder.path + File.separator + "Decorations")
        for (filePath in filePaths) {
            val config = YamlConfiguration.loadConfiguration(filePath.toFile())
            for (decorationName in config.getKeys(false)) {
                val id = config.getString("$decorationName.Id",decorationName)
                val materials = hashMapOf<Material, Int>()
                val materialConfig = config.getConfigurationSection("$decorationName.Materials")
                materialConfig?.getKeys(false)?.forEach { materialName ->
                    val possibleMaterial = Enums.getIfPresent(Material::class.java, materialName)
                    if (possibleMaterial.isPresent) {
                        val amount = materialConfig.getInt("$materialName", 1)
                        materials[possibleMaterial.get()] = amount
                    }
                }
                val permission = config.getString("$decorationName.Permission","")
                if (id != null && permission != null) {
                    val element = FurnitureElement(id,materials,permission)
                    decorationElements[decorationName] = element
                    if (permission.isNotEmpty())
                        permissionsToKeys[permission] = decorationName
                }
            }
        }
    }
}