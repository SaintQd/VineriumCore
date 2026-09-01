package org.saintqd.vineriumcore.mythicmobs.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.saintqd.vineriumlib.utils.VinUtils;

@MythicMechanic(name = "vinaddattribute",
        aliases = "addattribute",
        author = "SaintQd",
        description = "Adds attribute value to player. May be transparent (permanent) or not."
)
public final class VinAddAttributeMechanic implements ITargetedEntitySkill {

    private final NamespacedKey attributeTypeKey;
    private final NamespacedKey nameKey;
    private final AttributeModifier.Operation operation;
    private final PlaceholderDouble value;
    private final boolean permanent;

    public VinAddAttributeMechanic(@NonNull MythicMechanicLoadEvent event) {
        MythicLineConfig mlc = event.getConfig();

        String attributeTypeName = mlc.getPlaceholderString(new String[]{"attribute", "attr","a"},"").get();
        this.attributeTypeKey = NamespacedKey.fromString(attributeTypeName);

        String attributeName = mlc.getPlaceholderString(new String[]{"name", "n"},"").get();
        this.nameKey = NamespacedKey.fromString(attributeName);

        this.operation = AttributeModifier.Operation.valueOf(mlc.getString(new String[]{"operation", "o"},"ADD_NUMBER"));
        this.value = mlc.getPlaceholderDouble(new String[]{"value", "v"},"1.0");
        this.permanent = mlc.getBoolean(new String[]{"permanent", "p"},false);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        VinUtils.sendDebugMessage(3,"MythicMobsMechanic: vinaddattribute");

        Attribute attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(attributeTypeKey);
        if (attribute == null) {
            return SkillResult.INVALID_CONFIG;
        }

        Entity bukkitEntity = target.getBukkitEntity();
        if (!(bukkitEntity instanceof Attributable attributable))
            return SkillResult.INVALID_TARGET;

        AttributeInstance attributeInstance = attributable.getAttribute(attribute);
        if (attributeInstance == null)
            return SkillResult.INVALID_TARGET;

        double parsedValue = value.get(PlaceholderContext.builder().meta(data).entity(target).build());
        AttributeModifier modifier = new AttributeModifier(nameKey,parsedValue,operation);

        if (attributeInstance.getModifier(nameKey) != null)
            return SkillResult.SUCCESS;
        if (permanent)
            attributeInstance.addModifier(modifier);
        else
            attributeInstance.addTransientModifier(modifier);

        return SkillResult.SUCCESS;
    }
}
