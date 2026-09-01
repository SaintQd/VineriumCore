package org.saintqd.vineriumcore.managers

import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.bukkit.BukkitAdapter
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.saintqd.vineriumcore.VineriumCore
import java.nio.file.Paths

class CauldronRecipesManager {

    // ID - Рецепт
    val recipes = hashMapOf<String, CauldronRecipe>()
    // Предмет из рецепта - Набор рецептов
    val definingItems = hashMapOf<String, HashSet<String>>()
    // Предмет - Рецепт
    val combineItems = hashMapOf<String, HashSet<String>>()

    companion object {
        @JvmField
        val INSTANCE : CauldronRecipesManager = CauldronRecipesManager()
    }

    data class CauldronRecipe(
        val resultItem: Pair<String, Int>,
        val items: HashMap<String, Int>,
        val removeCombineItem : Boolean
    )

    fun loadRecipes(plugin : VineriumCore) {
        val filePath = Paths.get(plugin.dataFolder.path, "CauldronRecipes.yml")
        val file = filePath.toFile()
        file.createNewFile()

        val cauldronYaml = YamlConfiguration.loadConfiguration(file)
        cauldronYaml.getConfigurationSection("CauldronRecipes")?.let { config ->
            for (recipeId in config.getKeys(false)) {
                config.getConfigurationSection(recipeId)?.let { recipeConfig ->
                    if (!recipeConfig.contains("Result"))
                        continue
                    val resultItemData = recipeConfig.getString("Result")!!.split(",")
                    val resultItemName = resultItemData[0]
                    val resultItemAmount = if (resultItemData.size > 1) resultItemData[1].toInt() else 1
                    val combineItemName = recipeConfig.getString("CombineItemName",Material.GLASS_BOTTLE.name)!!
                    val items = hashMapOf<String, Int>()
                    recipeConfig.getConfigurationSection("Items")?.let { itemsConfig ->
                        for (itemName in itemsConfig.getKeys(false)) {
                            items[itemName] = itemsConfig.getInt(itemName)
                        }
                    }
                    val removeCombineItem = recipeConfig.getBoolean("RemoveCombineItem",true)
                    val cauldronRecipe = CauldronRecipe(Pair(resultItemName,resultItemAmount),items,removeCombineItem)
                    recipes[recipeId] = cauldronRecipe

                    val combineItemSet = combineItems[combineItemName] ?: hashSetOf()
                    combineItemSet.add(recipeId)
                    combineItems[combineItemName] = combineItemSet

                    for (itemName in items.keys) {
                        val recipeSet = definingItems[itemName] ?: hashSetOf()
                        recipeSet.add(recipeId)
                        definingItems[itemName] = recipeSet
                    }
                }
            }
        }
    }

    fun checkRecipe(combineItemStack : ItemStack, location : Location) : CauldronRecipe? {
        var combineItemName = combineItemStack.type.name
        MythicProvider.get().itemManager.getMythicTypeFromItem(combineItemStack)?.let { itemName ->
            combineItemName = "mm:$itemName"
        }
        val combineItemSet = combineItems[combineItemName] ?: return null
        val items = location.world.getNearbyEntitiesByType(Item::class.java,location,0.6)
        if (items.isEmpty())
            return null
        for (droppedItem in items) {
            val definingItemStack = droppedItem.itemStack

            var definingItemName = definingItemStack.type.name
            MythicProvider.get().itemManager.getMythicTypeFromItem(definingItemStack)?.let { itemName ->
                definingItemName = "mm:$itemName"
            }
            val definingItemSet = definingItems[definingItemName] ?: return null

            val intersectedSet = combineItemSet.intersect(definingItemSet)
            for (recipeId in intersectedSet) {
                recipes[recipeId]?.let { cauldronRecipe ->
                    val requiredItems = hashMapOf<String, Int>()
                    requiredItems.putAll(cauldronRecipe.items)
                    for (droppedItem in items) {
                        val droppedItemStack = droppedItem.itemStack

                        var droppedItemName = droppedItemStack.type.name
                        MythicProvider.get().itemManager.getMythicTypeFromItem(droppedItemStack)?.let { itemName ->
                            droppedItemName = "mm:$itemName"
                        }
                        if (requiredItems.containsKey(droppedItemName)) {
                            requiredItems[droppedItemName] = requiredItems.getOrDefault(droppedItemName, 0) - droppedItemStack.amount
                            if (requiredItems[droppedItemName]!! <= 0)
                                requiredItems.remove(droppedItemName)
                        }
                    }
                    if (requiredItems.isEmpty()) {
                        val itemsToRemove = hashMapOf<String, Int>()
                        itemsToRemove.putAll(cauldronRecipe.items)
                        for (droppedItem in items) {
                            val droppedItemStack = droppedItem.itemStack

                            var droppedItemName = droppedItemStack.type.name
                            MythicProvider.get().itemManager.getMythicTypeFromItem(droppedItemStack)?.let { itemName ->
                                droppedItemName = "mm:$itemName"
                            }
                            if (itemsToRemove.containsKey(droppedItemName)) {
                                val removedAmount = itemsToRemove[droppedItemName]!!
                                if (droppedItemStack.amount <= removedAmount) {
                                    droppedItem.remove()
                                    itemsToRemove[droppedItemName] = itemsToRemove.getOrDefault(droppedItemName,0) - removedAmount
                                    if (itemsToRemove[droppedItemName]!! <= 0)
                                        itemsToRemove.remove(droppedItemName)
                                }
                                else {
                                    droppedItemStack.amount -= removedAmount
                                    itemsToRemove.remove(droppedItemName)
                                }
                            }
                        }
                        val resultItemName = cauldronRecipe.resultItem.first
                        val resultItemAmount = cauldronRecipe.resultItem.second
                        var finalItemStack : ItemStack? = null
                        if (resultItemName.startsWith("mm:")) {
                            val mythicMobsItemName = resultItemName.replace("mm:","")
                            MythicProvider.get().itemManager.getItem(mythicMobsItemName)?.let { optionalItem ->
                                if (optionalItem.isPresent) {
                                    finalItemStack = BukkitAdapter.adapt(optionalItem.get().generateItemStack(resultItemAmount))
                                }
                            }
                        }
                        else {
                            val material = Material.valueOf(resultItemName.uppercase())
                            finalItemStack = ItemStack.of(material,resultItemAmount)
                        }
                        finalItemStack?.let { finalItemStack ->
                            location.world.spawn(location,Item::class.java, CreatureSpawnEvent.SpawnReason.CUSTOM) { item ->
                                item.setCanPlayerPickup(true)
                                item.itemStack = finalItemStack
                                item.velocity = Vector(0.0, 0.35, 0.0)
                            }
                            location.world
                                .playSound(location, Sound.ENTITY_PLAYER_SPLASH, SoundCategory.BLOCKS, 1f, 2f)
                            location.world.spawnParticle(
                                Particle.BUBBLE_POP,
                                location.add(0.0, 0.5, 0.0),
                                20,
                                0.3,
                                0.2,
                                0.3,
                                0.0
                            )
                        }
                        return cauldronRecipe
                    }
                }
            }
        }
        return null
    }
}