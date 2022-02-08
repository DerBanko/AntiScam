package tv.banko.antiscam.violation;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.util.URLHelper;

import java.time.Instant;
import java.util.Locale;

public class Violation {

    private final AntiScam antiScam;
    private final Message message;
    private final String content;

    public Violation(AntiScam antiScam, Message message) {
        this.antiScam = antiScam;
        this.message = message;
        this.content = message.getContent().toLowerCase(Locale.ROOT);
    }

    public void check() {
        if (URLHelper.isDiscordURL(message.getContent())) {
            return;
        }

        message.getAuthorAsMember().subscribe(member -> {

            if (member == null) {
                return;
            }

            Snowflake guildId = member.getGuildId();

            if (!antiScam.getMongoDB().getViolationCollection().isEnabled(guildId)) {
                return;
            }

            member.getBasePermissions().subscribe(permissions -> {

                if (permissions != null) {
                    if (permissions.contains(Permission.MANAGE_MESSAGES)) {
                        return;
                    }
                }

                if (!containsURL()) {
                    return;
                }

                int score = getScore();

                ViolationType type = ViolationType.getByScore(score);

                if (type.equals(ViolationType.NONE)) {
                    return;
                }

                message.getChannel().cast(TopLevelGuildMessageChannel.class).subscribe(channel -> {

                    if (channel == null) {
                        return;
                    }

                    String action = antiScam.getMongoDB().getViolationCollection().punish(message, type).getName();

                    antiScam.getStats().sendViolation(message, score);

                    if (action.equalsIgnoreCase("none")) {

                        if (!type.equals(ViolationType.EXTREME)) {
                            return;
                        }

                        message.delete().subscribe();
                        antiScam.getMongoDB().getLogCollection().sendMessage(guildId, EmbedCreateSpec.builder()
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
                            .build());
                        return;
                    }

                    antiScam.getMongoDB().getLogCollection().sendMessage(guildId, EmbedCreateSpec.builder()
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
                        .build());
                });
            });
        });

    }

    private boolean containsURL() {
        return content.contains("http://") ||
            content.contains("https://") ||
            content.contains("www.");
    }

    private int getScore() {
        int violationScore = 0;

        for (ViolationScore score : ViolationScore.values()) {
            for (String phrase : score.getPhrases()) {
                if (!content.contains(phrase.toLowerCase())) {
                    continue;
                }

                violationScore += score.getScore();
            }
        }

        return violationScore;
    }
}
