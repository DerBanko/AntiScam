package tv.banko.antiscam.manage;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.WebhookExecuteSpec;
import tv.banko.antiscam.AntiScam;

import java.time.Instant;

public class Stats {

    private final AntiScam antiScam;
    private Webhook webhook;

    public Stats(AntiScam antiScam) {
        this.antiScam = antiScam;

        this.antiScam.getGateway().getWebhookByIdWithToken(Snowflake.of(System.getenv("STATS_WEBHOOK_ID")),
            System.getenv("STATS_WEBHOOK_TOKEN")).subscribe(webhook -> this.webhook = webhook);
    }

    public void sendScam(Message message) {
        message.getAuthorAsMember().subscribe(member ->
            message.getChannel().cast(GuildMessageChannel.class).subscribe(channel ->
                message.getGuild().subscribe(guild ->
                    webhook.execute(WebhookExecuteSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":newspaper: | " + antiScam.getLanguage().get("scam_detected"))
                            .description("**" + antiScam.getLanguage().get("sender") + "**: **" + member.getTag() + "**\n" +
                                "**" + antiScam.getLanguage().get("channel") + "**: **" + channel.getName() + "**\n" +
                                "**" + antiScam.getLanguage().get("guild") + "**: **" + guild.getName() + "**\n")
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("message"), message.getContent(), false))
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"), "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids"), "```ini" + "\n" +
                                "userId = " + member.getId().asString() + "\n" +
                                "channelId = " + channel.getId().asString() + "\n" +
                                "messageId = " + message.getId().asString() + "\n" +
                                "guildId = " + guild.getId().asString() + "\n" +
                                "```", false))
                            .build())
                        .build()).onErrorStop().subscribe())));
    }

    public void sendViolation(Message message, int score) {
        message.getAuthorAsMember().subscribe(member ->
            message.getChannel().cast(GuildMessageChannel.class).subscribe(channel ->
                message.getGuild().subscribe(guild ->
                    webhook.execute(WebhookExecuteSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":newspaper: | " + antiScam.getLanguage().get("violation_detected"))
                            .description("**" + antiScam.getLanguage().get("sender") + "**: **" + member.getTag() + "**\n" +
                                "**" + antiScam.getLanguage().get("channel") + "**: **" + channel.getName() + "**\n" +
                                "**" + antiScam.getLanguage().get("guild") + "**: **" + guild.getName() + "**\n")
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("message"), message.getContent(), false))
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"), "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("score"), "" + score, false))
                            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids"), "```ini" + "\n" +
                                "userId = " + member.getId().asString() + "\n" +
                                "channelId = " + channel.getId().asString() + "\n" +
                                "messageId = " + message.getId().asString() + "\n" +
                                "guildId = " + guild.getId().asString() + "\n" +
                                "```", false))
                            .build())
                        .build()).onErrorStop().subscribe())));
    }
}
