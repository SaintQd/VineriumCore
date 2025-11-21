package org.saintqd.vineriumcore.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.*;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.saintqd.vineriumcore.VineriumCore;
import github.scarsz.discordsrv.objects.MessageFormat;

import java.util.*;
import java.util.List;
import java.util.function.BiFunction;

public class DiscordSRVManager {

    private HashMap<String,MessageFormat> messageFormats = new HashMap<>();

    public void loadDiscordData(VineriumCore plugin) {
        this.messageFormats = new HashMap<>();
        ConfigurationSection messageFormatsConfig = plugin.getConfig().getConfigurationSection("Compatibility.DiscordSRV.MessageFormats");
        if (messageFormatsConfig != null) {
            for (String messageKey : messageFormatsConfig.getKeys(false)) {
                MessageFormat message = createMessageFromConfiguration(messageKey,messageFormatsConfig.getConfigurationSection(messageKey));
                messageFormats.put(messageKey,message);
            }
        }
    }

    public HashMap<String, MessageFormat> getMessageFormats() {
        return messageFormats;
    }

    public static MessageFormat createMessageFromConfiguration(String messageKey, ConfigurationSection config) {
        MessageFormat messageFormat = new MessageFormat();
        if (config.contains("Embed") && config.getBoolean("Embed.Enabled",true)) {
            String hexColor = config.getString("Embed.Color",null);
            if (hexColor != null) {
                String hex = hexColor.trim();
                if (!hex.startsWith("#")) {
                    hex = "#" + hex;
                }
                if (hex.length() == 7) {
                    messageFormat.setColorRaw(Integer.valueOf(hex.substring(1, 7), 16));
                } else {
                    DiscordSRV.debug("Invalid color hex: " + hex + " (in " + messageKey + ".Embed.Color)");
                }
            } else {
                int colorRaw = config.getInt("Embed.Color",-1);
                if (colorRaw != -1)
                    messageFormat.setColorRaw(colorRaw);
            }
            String embedData = "";
            if (config.contains("Embed.Author")) {
                embedData = config.getString("Embed.Author.Name","");
                if (!embedData.isEmpty())
                    messageFormat.setAuthorName(embedData);
                embedData = config.getString("Embed.Author.Url","");
                if (!embedData.isEmpty())
                    messageFormat.setAuthorUrl(embedData);
                embedData = config.getString("Embed.Author.ImageUrl","");
                if (!embedData.isEmpty())
                    messageFormat.setAuthorImageUrl(embedData);
            }
            embedData = config.getString("Embed.ThumbnailUrl","");
            if (!embedData.isEmpty())
                messageFormat.setThumbnailUrl(embedData);
            embedData = config.getString("Embed.Title.Text","");
            if (!embedData.isEmpty())
                messageFormat.setTitle(embedData);
            embedData = config.getString("Embed.Title.Url","");
            if (!embedData.isEmpty())
                messageFormat.setTitleUrl(embedData);
            embedData = config.getString("Embed.Description","");
            if (!embedData.isEmpty())
                messageFormat.setDescription(embedData);
            List<String> fields = config.getStringList("Embed.Fields");
            if (!fields.isEmpty()) {
                List<MessageEmbed.Field> fieldsList = new ArrayList<>();
                for (String s : fields) {
                    if (s.contains(";")) {
                        String[] parts = s.split(";");
                        if (parts.length >= 2) {
                            boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                            fieldsList.add(new MessageEmbed.Field(parts[0], parts[1], inline, true));
                        }
                    } else {
                        boolean inline = Boolean.parseBoolean(s);
                        fieldsList.add(new MessageEmbed.Field("\u200e", "\u200e", inline, true));
                    }
                }
                messageFormat.setFields(fieldsList);
            }
            embedData = config.getString("Embed.ImageUrl","");
            if (!embedData.isEmpty())
                messageFormat.setImageUrl(embedData);
            if (config.contains("Embed.Footer")) {
                embedData = config.getString("Embed.Footer.Text","");
                if (!embedData.isEmpty())
                    messageFormat.setFooterText(embedData);
                embedData = config.getString("Embed.Footer.IconUrl","");
                if (!embedData.isEmpty())
                    messageFormat.setFooterIconUrl(embedData);
            }
            if (config.getBoolean("Embed.Timestamp",false)) {
                messageFormat.setTimestamp((new Date()).toInstant());
            }
        }
        if (config.contains("Webhook") && config.getBoolean("Webhook.Enabled",false)) {
            messageFormat.setUseWebhooks(true);
            String webhookData = config.getString("Webhook.AvatarUrl","");
            if (!webhookData.isEmpty())
                messageFormat.setWebhookAvatarUrl(webhookData);
            webhookData = config.getString("Webhook.Name","");
            if (!webhookData.isEmpty())
                messageFormat.setWebhookName(webhookData);
        }
        String content = config.getString("Content","");
        if (!content.isEmpty()) {
            messageFormat.setContent(content);
        }
        return messageFormat.isAnyContent() ? messageFormat : null;
    }

    public static void runMessageAsync(String channelType, OfflinePlayer actorPlayer, MessageFormat messageFormat, String... args) {
        if (messageFormat == null) return;
        String avatarUrl = actorPlayer != null
                ? DiscordSRV.getAvatarUrl(actorPlayer.getName(),actorPlayer.getUniqueId())
                : "";
        String actorPlayerName = actorPlayer != null
                ? actorPlayer.getName()
                : "null";
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelType);
        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) {
                return null;
            } else {
                content = content.replaceAll("%time%|%date%", TimeUtil.timeStamp())
                        .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(actorPlayerName) : actorPlayerName)
                        .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(actorPlayerName) : actorPlayerName)
                        .replace("%usernamenoescapes%", actorPlayerName)
                        .replace("%displaynamenoescapes%", actorPlayerName)
                        .replace("%embedavatarurl%", avatarUrl)
                        .replace("%botavatarurl%", botAvatarUrl)
                        .replace("%botname%", botName);
                int index = 1;
                for (String arg : args) {
                    content = content.replace("{"+index+"}",arg);
                    index++;
                }
                if (destinationChannel != null) {
                    content = DiscordUtil.translateEmotes(content, destinationChannel.getGuild());
                }
                content = PlaceholderUtil.replacePlaceholdersToDiscord(content, actorPlayer);
                return content;
            }
        };
        Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
        if (discordMessage != null) {
            TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelType);
            if (messageFormat.getWebhookName() != null) {
                String webhookName = translator.apply(messageFormat.getWebhookName(), false);
                String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);
                WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl, discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
            } else {
                DiscordUtil.queueMessage(textChannel, discordMessage, true);
            }
        }
    }

}
