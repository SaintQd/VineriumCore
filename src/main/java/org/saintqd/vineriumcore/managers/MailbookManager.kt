package org.saintqd.vineriumcore.managers

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.WrittenBookContent
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.WritableBookMeta
import org.bukkit.plugin.Plugin
import org.saintqd.vineriumcore.VineriumCore
import org.saintqd.vineriumlib.VineriumLib
import org.saintqd.vineriumlib.utils.VinUtils
import java.nio.file.Paths
import java.util.UUID
import kotlin.collections.set
import kotlin.io.encoding.Base64

class MailbookManager {

    val unreadMailbooks = hashMapOf<UUID, List<String>>()

    companion object {
        @JvmField
        val INSTANCE : MailbookManager = MailbookManager()
        var MAILBOOK_KEY : NamespacedKey? = NamespacedKey(VineriumCore.inst(),"mailbook")
    }

    fun loadParams(plugin : Plugin) {
        unreadMailbooks.clear()
        MAILBOOK_KEY = null

        val mailbookFilePath = Paths.get(plugin.dataFolder.path, "Mailbooks.yml")
        val mailbookFile = mailbookFilePath.toFile()
        mailbookFile.createNewFile()

        val config = YamlConfiguration.loadConfiguration(mailbookFile).getConfigurationSection("Mailbooks")

        config?.let {
            for (uuidString in config.getKeys(false)) {
                val uuid = UUID.fromString(uuidString)
                val encodedItems = config.getStringList("$uuidString")
                unreadMailbooks[uuid] = encodedItems
            }
        }
        val mailbookKeyName = plugin.config.getString("Mailbooks.MailbookKey","")
        if (!mailbookKeyName.isNullOrEmpty()) {
            MAILBOOK_KEY = NamespacedKey(VineriumCore.inst(),mailbookKeyName)
        }
    }

    fun saveMailbooks(plugin : Plugin) {

        val mailbookFilePath = Paths.get(plugin.dataFolder.path, "Mailbooks.yml")
        val mailbookFile = mailbookFilePath.toFile()
        mailbookFile.createNewFile()

        val config = YamlConfiguration.loadConfiguration(mailbookFile)
        config.set("Mailbooks",null)

        val timeLimit = VineriumCore.inst().config.getLong("Mailbooks.ExpireTime",12096000)
        unreadMailbooks.forEach { (uuid, mailbookList) ->
            val changedList = mutableListOf<String>()
            changedList.addAll(mailbookList)
            changedList.removeIf { mailbookString ->
                val mailbookData = mailbookString.split(",")
                return@removeIf (if (mailbookData.size > 1) mailbookData[1].toLong() + timeLimit else VinUtils.getCurrentTick()) <= VinUtils.getCurrentTick()
            }
            if (changedList.isNotEmpty())
                config.set("Mailbooks.$uuid",mailbookList)
        }
    }

    fun createMailbook(senderPlayer : Player, itemStack : ItemStack, title : String, hideAuthor : Boolean) : String {

        val newItemStack = ItemStack.of(Material.WRITTEN_BOOK)

        val itemMeta = itemStack.itemMeta as WritableBookMeta
        val hiddenAuthor = VineriumLib.inst().langManager.langLines[NamespacedKey.fromString("vineriumcore:mailbook_default_hidden_author")] ?: ""

        val textPrefix = VineriumCore.inst().config.getString("Mailbooks.TextPrefix","")
        val writtenBookContentBuilder = WrittenBookContent.writtenBookContent(title,if (hideAuthor) hiddenAuthor else senderPlayer.name)
        for (page in itemMeta.pages) {
            if (VineriumLib.inst().isPlaceholderAPIEnabled)
                writtenBookContentBuilder.addPage(MiniMessage.miniMessage().deserialize(PlaceholderAPI.setPlaceholders(senderPlayer,textPrefix+page)))
            else
                writtenBookContentBuilder.addPage(MiniMessage.miniMessage().deserialize(textPrefix+page))
        }
        newItemStack.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT,writtenBookContentBuilder.build())

        if (itemStack.hasData(DataComponentTypes.ITEM_MODEL)) {
            itemStack.getData(DataComponentTypes.ITEM_MODEL)?.let { key ->
                if (key != itemStack.type.key)
                    newItemStack.setData(DataComponentTypes.ITEM_MODEL,key)
            }
        }

        val byteArray = newItemStack.serializeAsBytes()
        return Base64.encode(byteArray) + "," + VinUtils.getCurrentTick()
    }

    fun decodeMailbooks(uuid: UUID): List<ItemStack> {
        if (!unreadMailbooks.containsKey(uuid)) {
            return emptyList()
        }
        val itemStackList = mutableListOf<ItemStack>()
        unreadMailbooks[uuid]?.forEach { itemStackString ->
            val byteArray = Base64.decode(itemStackString.split(",")[0])
            itemStackList.add(ItemStack.deserializeBytes(byteArray))
        }
        return itemStackList
    }

}