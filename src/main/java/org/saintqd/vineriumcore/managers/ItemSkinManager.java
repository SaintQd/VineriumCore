package org.saintqd.vineriumcore.managers;

import kotlin.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.utils.VinUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class ItemSkinManager {

    public static final ItemSkinManager INSTANCE = new ItemSkinManager();
    public static final NamespacedKey ITEM_SKIN_KEY = new NamespacedKey(VineriumCore.inst(), "item_skin");

    @Getter
    private final HashMap<String, ItemSkin> itemSkins = new HashMap<>();
    @Getter
    private final HashMap<String,String> permissionsToKeys = new HashMap<>();

    public void loadItemSkins(Plugin plugin) {
        itemSkins.clear();
        permissionsToKeys.clear();
        File skinsDir = new File(plugin.getDataFolder().getPath() + File.separator + "ItemSkins");
        if (!skinsDir.exists()) {
            plugin.getLogger().log(Level.INFO,"ItemSkins directory does not exist, creating it.");
            if (!skinsDir.mkdir()) {
                plugin.getLogger().log(Level.SEVERE,"Could not create ItemSkins directory!");
                return;
            }
        }
        val filePaths = VinUtils.listFilesInFolder(plugin.getDataFolder().getPath() + File.separator + "ItemSkins");
        for (Path filePath : filePaths) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(filePath.toFile());
            for (String skinName : config.getKeys(false)) {
                String displayName = config.getString(skinName + ".DisplayName",skinName);
                List<String> materials = config.getStringList(skinName + ".Materials");
                String permission = config.getString(skinName + ".Permission","vineriumcore.itemskin."+skinName);
                NamespacedKey model = NamespacedKey.fromString(config.getString(skinName + ".Model",skinName.toLowerCase()));
                if (model != null) {
                    Pair<EquipmentSlot,String> equippableData = config.contains(skinName + ".Equippable")
                            ? new Pair<>(EquipmentSlot.valueOf(config.getString(skinName + ".Equippable.Slot")),config.getString(skinName + ".Equippable.AssetId"))
                            : null;
                    ItemSkin itemSkin = new ItemSkin(displayName, materials, permission, model,equippableData);
                    itemSkins.put(skinName, itemSkin);
                    permissionsToKeys.put(permission, skinName);
                }
            }
        }
    }

    public record ItemSkin(@NonNull String displayName, @NonNull List<String> materials, @NonNull String permission, @NonNull NamespacedKey model, @Nullable
                           Pair<EquipmentSlot,String> equippableData) { }
}
