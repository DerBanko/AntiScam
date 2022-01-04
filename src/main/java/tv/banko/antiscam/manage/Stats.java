package tv.banko.antiscam.manage;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.WebhookExecuteSpec;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.punishment.PunishmentType;

import java.time.Instant;

public class Stats {

    private final AntiScam antiScam;
    private final Webhook webhook;

    private long reset;

    private int scam;
    private int messages;

    public Stats(AntiScam antiScam) {
        this.antiScam = antiScam;

        webhook = this.antiScam.getGateway().getWebhookByIdWithToken(Snowflake.of(System.getenv("STATS_WEBHOOK_ID")),
                System.getenv("STATS_WEBHOOK_TOKEN")).blockOptional().orElse(null);

        this.reset = System.currentTimeMillis() + (1000 * 60 * 60);
        this.scam = 0;
        this.messages = 0;
    }

    public void addScam() {
        this.scam++;
    }

    public void addMessage() {
        this.messages++;

        if(reset > System.currentTimeMillis()) {
            return;
        }

        sendStats();
    }

    public void sendScam(Message message) {
        Member member = message.getAuthorAsMember().block();
        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel().block();
        Guild guild = message.getGuild().block();

        assert member != null;
        assert channel != null;
        assert guild != null;
        webhook.execute(WebhookExecuteSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title(":newspaper: | Scam detected")
                        .description("**Sender**: **" + member.getTag() + "**\n" +
                                "**Channel**: **" + channel.getName() + "**\n" +
                                "**Guild**: **" + guild.getName() + "**\n")
                        .addField(EmbedCreateFields.Field.of("Message", message.getContent(), false))
                        .addField(EmbedCreateFields.Field.of("Timestamp", "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                        .addField(EmbedCreateFields.Field.of("IDs", "```ini" + "\n" +
                                "userId = " + member.getId().asString() + "\n" +
                                "channelId = " + channel.getId().asString() + "\n" +
                                "messageId = " + message.getId().asString() + "\n" +
                                "guildId = " + guild.getId().asString() + "\n" +
                                "```", false))
                        .build())
                .build()).onErrorStop().block();
    }

    public void sendStats() {
        this.reset = System.currentTimeMillis() + (1000 * 60 * 60);
        this.scam = 0;
        this.messages = 0;

        webhook.execute(WebhookExecuteSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .title(":newspaper: | Stats of last 60 minutes")
                        .description("**Guild Count**: **" + antiScam.getGateway().getGuilds().count().block() + "**\n" +
                                "**Messages**: **" + messages + "**\n" +
                                "**Scam**: **" + scam + "**\n" +
                                "**Percentage**: **" + (messages == 0 ? 0 : ((scam * 100) / messages)) + "%**\n")
                        .build())
                .build()).onErrorStop().block();
    }

}
