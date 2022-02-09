package tv.banko.antiscam.language;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.violation.ViolationType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MessageTemplate {

    private final AntiScam antiScam;
    private final Map<Snowflake, Long> userInformation;

    public MessageTemplate(AntiScam antiScam) {
        this.antiScam = antiScam;
        this.userInformation = new HashMap<>();
    }

    public EmbedCreateSpec getScamURLDetected(Member member, GuildMessageChannel channel, Message message, Snowflake guildId) {
        sendUserInformation(member);

        return EmbedCreateSpec.builder()
            .title(":no_entry_sign: | " + antiScam.getLanguage().get("scam_url_detected", guildId))
            .description("**" + antiScam.getLanguage().get("sender", guildId) + "**: " + member.getMention() +
                " (" + member.getTag() + ")\n" +
                "**" + antiScam.getLanguage().get("channel", guildId) + "**: " + channel.getMention() +
                " (" + channel.getName() + ")\n")
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("message", guildId),
                message.getContent(), false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp", guildId),
                "<t:" + Instant.now().getEpochSecond() + ":f>", false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids", guildId),
                "```ini" + "\n" +
                    "userId = " + member.getId().asString() + "\n" +
                    "channelId = " + channel.getId().asString() + "\n" +
                    "messageId = " + message.getId().asString() + "\n" +
                    "```", false))
            .build();
    }

    public EmbedCreateSpec getScamDetected(Member member, GuildMessageChannel channel, Message message,
                                           int score, ViolationType type,
                                           String action, Snowflake guildId) {
        sendUserInformation(member);

        return EmbedCreateSpec.builder()
            .title(":face_with_raised_eyebrow: | " + antiScam.getLanguage().get("potential_scam_detected", guildId))
            .description("**" + antiScam.getLanguage().get("sender", guildId) + "**: " + member.getMention() +
                " (" + member.getTag() + ")\n" +
                "**" + antiScam.getLanguage().get("channel", guildId) + "**: " + channel.getMention() +
                " (" + channel.getName() + ")\n" +
                "" +
                "**" + antiScam.getLanguage().get("violation_score", guildId) + "**: " + score +
                " (**" + type.name() + "**)")
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("message", guildId),
                message.getContent(), false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp", guildId),
                "<t:" + Instant.now().getEpochSecond() + ":f>", false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("punishment", guildId),
                action, false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids", guildId),
                "```ini" + "\n" +
                    "userId = " + member.getId().asString() + "\n" +
                    "channelId = " + channel.getId().asString() + "\n" +
                    "messageId = " + message.getId().asString() + "\n" +
                    "```", false))
            .build();
    }

    public EmbedCreateSpec getScamDeleted(Member member, GuildMessageChannel channel, Message message,
                                          int score, ViolationType type, Snowflake guildId) {
        sendUserInformation(member);

        return EmbedCreateSpec.builder()
            .title(":wastebasket: | " + antiScam.getLanguage().get("potential_scam_detected", guildId))
            .description("**" + antiScam.getLanguage().get("sender", guildId) + "**: " +
                member.getMention() + " (" + member.getTag() + ")\n" +
                "**" + antiScam.getLanguage().get("channel", guildId) + "**: " + channel.getMention() +
                " (" + channel.getName() + ")\n" +
                "" +
                "**" + antiScam.getLanguage().get("violation_score", guildId) + "**: " + score +
                " (**" + type.name() + "**) " +
                "*(" + antiScam.getLanguage().get("auto_delete_information", guildId) + ")")
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("message", guildId),
                message.getContent(), false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp", guildId),
                "<t:" + Instant.now().getEpochSecond() + ":f>", false))
            .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("ids", guildId),
                "```ini" + "\n" +
                    "userId = " + member.getId().asString() + "\n" +
                    "channelId = " + channel.getId().asString() + "\n" +
                    "messageId = " + message.getId().asString() + "\n" +
                    "```", false))
            .build();
    }

    public void sendUserInformation(Member member) {

        if (userInformation.getOrDefault(member.getId(), 0L) > System.currentTimeMillis()) {
            return;
        }

        userInformation.put(member.getId(), System.currentTimeMillis() + (1000 * 60 * 60 * 24));

        member.getPrivateChannel().subscribe(privateChannel -> privateChannel.createMessage(MessageCreateSpec.builder()
            .content(antiScam.getLanguage().get("instructions_message", member.getGuildId()) + " <https://www.free-nitro.xyz/>")
            .addEmbed(EmbedCreateSpec.builder()
                .title(":alien: | " + antiScam.getLanguage().get("instructions_title", member.getGuildId()))
                .description(antiScam.getLanguage().get("instructions_description", member.getGuildId()))
                .addField(antiScam.getLanguage().get("instructions_step_1_title", member.getGuildId()),
                    antiScam.getLanguage().get("instructions_step_1_description", member.getGuildId()), false)
                .addField(antiScam.getLanguage().get("instructions_step_2_title", member.getGuildId()),
                    antiScam.getLanguage().get("instructions_step_2_description", member.getGuildId()), false)
                .addField(antiScam.getLanguage().get("instructions_step_3_title", member.getGuildId()),
                    antiScam.getLanguage().get("instructions_step_3_description", member.getGuildId()), false)
                .addField(antiScam.getLanguage().get("instructions_step_4_title", member.getGuildId()),
                    antiScam.getLanguage().get("instructions_step_4_description", member.getGuildId()), false)
                .addField(antiScam.getLanguage().get("instructions_step_5_title", member.getGuildId()),
                    antiScam.getLanguage().get("instructions_step_5_description", member.getGuildId()), false)
                .build())
            .build()).onErrorStop().subscribe());
    }
}
