package org.saintqd.vineriumcore.managers

import com.nexomc.nexo.api.NexoItems
import io.lumine.mythic.api.MythicProvider
import io.lumine.mythic.bukkit.BukkitAdapter
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import org.bukkit.inventory.Merchant
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.plugin.Plugin
import org.saintqd.vineriumcore.VineriumCore
import org.saintqd.vineriumlib.utils.VinUtils
import java.io.File
import java.util.logging.Level
import kotlin.collections.set

class TradeManager {

    // Название набора трейдов, название меню, список трейдов
    val tradeSets = hashMapOf<String, VinTradeSet>()

    data class VinTradeSet(
        val title : Component,
        val permission : String,
        val trades : List<VinTrade>
    )

    data class VinTrade(
        val firstItem : Pair<String, Int>,
        val secondItem : Pair<String, Int>?,
        val resultItem : Pair<String, Int>
    )

    companion object {
        val instance : TradeManager = TradeManager()
    }

    fun loadParams(plugin : Plugin) {
        tradeSets.clear()
        val tradesDir = File(plugin.dataFolder, "Trades")
        if (!tradesDir.exists()) {
            plugin.logger.log(Level.INFO,"Trades directory does not exist, creating it.")
            if (!tradesDir.mkdir()) {
                plugin.logger.log(Level.SEVERE,"Could not create Trades directory!")
                return;
            }
        }
        val filePaths = VinUtils.listFilesInFolder(plugin.dataFolder.path + File.separator + "Trades")
        for (filePath in filePaths) {
            val config = YamlConfiguration.loadConfiguration(filePath.toFile())
            for (tradeSetName in config.getKeys(false)) {
                val title = VinUtils.parseString(config.getString("$tradeSetName.Title",""))
                val permission = config.getString("$tradeSetName.Permission","vineriumcore.tradeset.default")!!
                val trades = mutableListOf<VinTrade>()

                for (tradeName in config.getConfigurationSection("$tradeSetName.Trades")!!.getKeys(false)) {

                    val firstItemData = config.getString("$tradeSetName.Trades.$tradeName.FirstItem","stone,1")!!.split(",")
                    val firstItem = Pair(firstItemData[0], if (firstItemData.size > 1) firstItemData[1].toInt() else 1)

                    val secondItemData = if (config.contains("$tradeSetName.Trades.$tradeName.SecondItem"))
                        config.getString("$tradeSetName.Trades.$tradeName.SecondItem")!!.split(",")
                    else null
                    val secondItem = if (secondItemData != null) Pair(secondItemData[0],if (secondItemData.size > 1) secondItemData[1].toInt() else 1) else null

                    val resultItemData = config.getString("$tradeSetName.Trades.$tradeName.ResultItem","stone,1")!!.split(",")
                    val resultItem = Pair(resultItemData[0], if (resultItemData.size > 1) resultItemData[1].toInt() else 1)

                    val trade = VinTrade(firstItem,secondItem,resultItem)
                    trades.add(trade)
                }
                tradeSets[tradeSetName] = VinTradeSet(title,permission,trades)
            }
        }
    }

    fun createMerchant(tradeSet: VinTradeSet) : Merchant {
        val merchant : Merchant = Bukkit.createMerchant()
        val tradeList = mutableListOf<MerchantRecipe>()

        for (trade in tradeSet.trades) {

            var resultItem = ItemStack.of(Material.STONE)

            val isNexoEnabled = VineriumCore.inst().isNexoEnabled
            val isMythicMobsEnabled = VineriumCore.inst().isMythicMobsEnabled
            if (trade.resultItem.first.startsWith("nexo:")) {
                if (isNexoEnabled) {
                    val itemStackBuilder = NexoItems.itemFromId(trade.resultItem.first.replace("nexo:", ""))
                    resultItem = itemStackBuilder?.build() ?: ItemStack.of(Material.STONE)
                }
            }
            else if (trade.resultItem.first.startsWith("mm:")) {
                if (isMythicMobsEnabled) {
                    val mythicItem = MythicProvider.get().itemManager.getItem(trade.resultItem.first.replace("mm:", ""))
                    resultItem = if (mythicItem.isPresent) BukkitAdapter.adapt(mythicItem.get().generateItemStack(1))
                        else ItemStack.of(Material.STONE)
                }
            }
            else {
                val material = Registry.MATERIAL.get(NamespacedKey.minecraft(trade.resultItem.first))
                resultItem = if (material != null) ItemStack.of(material) else ItemStack.of(Material.STONE)
            }

            resultItem.amount = trade.resultItem.second

            val recipe = MerchantRecipe(resultItem,10000)

            for (tradeItem in listOf(trade.firstItem,trade.secondItem)) {
                tradeItem?.let { tradeItem ->
                    var itemStack = ItemStack.of(Material.STONE)
                    if (tradeItem.first.startsWith("nexo:")) {
                        if (isNexoEnabled) {
                            val itemStackBuilder = NexoItems.itemFromId(tradeItem.first.replace("nexo:", ""))
                            itemStack = itemStackBuilder?.build() ?: ItemStack.of(Material.STONE)
                        }
                    } else if (tradeItem.first.startsWith("mm:")) {
                        if (isMythicMobsEnabled) {
                            val mythicItem =
                                MythicProvider.get().itemManager.getItem(tradeItem.first.replace("mm:", ""))
                            itemStack =
                                if (mythicItem.isPresent) BukkitAdapter.adapt(mythicItem.get().generateItemStack(1))
                                else ItemStack.of(Material.STONE)
                        }
                    }
                    else {
                        val material = Registry.MATERIAL.get(NamespacedKey.minecraft(tradeItem.first))
                        itemStack = if (material != null) ItemStack.of(material) else ItemStack.of(Material.STONE)
                    }
                    itemStack.amount = tradeItem.second
                    recipe.addIngredient(itemStack)
                }
            }

            recipe.setIgnoreDiscounts(true)
            tradeList.add(recipe)
        }
        merchant.recipes = tradeList
        return merchant
    }

    fun openMerchant(player: Player, tradeSet: VinTradeSet, merchant: Merchant) {
        val merchantView = MenuType.MERCHANT.builder()
            .merchant(merchant)
            .title(tradeSet.title)
            .build(player)
        player.openInventory(merchantView)
    }
}