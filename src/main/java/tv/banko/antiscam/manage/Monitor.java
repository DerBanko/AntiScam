package tv.banko.antiscam.manage;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Webhook;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.WebhookExecuteSpec;
import tv.banko.antiscam.AntiScam;

import java.time.Instant;

public class Monitor {

    private final AntiScam antiScam;
    private Webhook webhook;

    public Monitor(AntiScam antiScam) {
        this.antiScam = antiScam;

        antiScam.getGateway().getWebhookByIdWithToken(Snowflake.of(System.getenv("MONITOR_WEBHOOK_ID")),
            System.getenv("MONITOR_WEBHOOK_TOKEN")).subscribe(webhook -> this.webhook = webhook);
    }

    public void sendGuildJoin(Guild guild) {
        webhook.execute(WebhookExecuteSpec.builder()
            .addEmbed(EmbedCreateSpec.builder()
                .title(":heavy_plus_sign: | " + antiScam.getLanguage().get("bot_added"))
                .description(antiScam.getLanguage().get("bot_added_detailed"))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("member_count"),
                    "" + guild.getMemberCount(), true))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("partnered_guilds"),
                    "" + guild.getFeatures().contains("PARTNERED"), true))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("verified_guilds"),
                    "" + guild.getFeatures().contains("VERIFIED"), true))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"),
                    "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids"), "```ini" + "\n" +
                    "guildId = " + guild.getId().asString() + "\n" +
                    "```", false))
                .build())
            .build()).onErrorStop().subscribe();
    }

    public void sendGuildLeave(Guild guild) {
        webhook.execute(WebhookExecuteSpec.builder()
            .addEmbed(EmbedCreateSpec.builder()
                .title(":heavy_plus_sign: | " + antiScam.getLanguage().get("bot_removed"))
                .description(antiScam.getLanguage().get("bot_removed_detailed"))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"),
                    "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids"), "```ini" + "\n" +
                    "guildId = " + guild.getId().asString() + "\n" +
                    "```", false))
                .build())
            .build()).onErrorStop().subscribe();
    }

    public void sendError(Throwable throwable) {
        webhook.execute(WebhookExecuteSpec.builder()
            .content("@everyone")
            .addEmbed(EmbedCreateSpec.builder()
                .title(":warning: | " + antiScam.getLanguage().get("error"))
                .description(antiScam.getLanguage().get("error_detailed")
                    .replace("%exception%", throwable.getClass().getName())
                    .replace("%stacktrace%", throwable.toString()))
                .build())
            .build()).onErrorStop().subscribe();
    }

}
