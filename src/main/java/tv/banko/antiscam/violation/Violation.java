package tv.banko.antiscam.violation;

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

            if (!antiScam.getMongoDB().getViolationCollection().isEnabled(member.getGuildId())) {
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
                        antiScam.getMongoDB().getLogCollection().sendMessage(member.getGuildId(), EmbedCreateSpec.builder()
                            .title(":wastebasket: | Potential Scam Deleted")
                            .description("**Sender**: " + member.getMention() + " (" + member.getTag() + ")\n" +
                                "**Channel**: " + channel.getMention() + " (" + channel.getName() + ")\n" +
                                "" +
                                "**Violation score**: " + score + " (**" + type.name() + "**) *(because this score is that high, we deleted the message)")
                            .addField(EmbedCreateFields.Field.of("Message", message.getContent(), false))
                            .addField(EmbedCreateFields.Field.of("Timestamp", "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                            .addField(EmbedCreateFields.Field.of("IDs", "```ini" + "\n" +
                                "userId = " + member.getId().asString() + "\n" +
                                "channelId = " + channel.getId().asString() + "\n" +
                                "messageId = " + message.getId().asString() + "\n" +
                                "```", false))
                            .build());
                        return;
                    }

                    antiScam.getMongoDB().getLogCollection().sendMessage(member.getGuildId(), EmbedCreateSpec.builder()
                        .title(":face_with_raised_eyebrow: | Potential Scam Found")
                        .description("**Sender**: " + member.getMention() + " (" + member.getTag() + ")\n" +
                            "**Channel**: " + channel.getMention() + " (" + channel.getName() + ")\n" +
                            "" +
                            "**Violation score**: " + score + " (**" + type.name() + "**)")
                        .addField(EmbedCreateFields.Field.of("Message", message.getContent(), false))
                        .addField(EmbedCreateFields.Field.of("Timestamp", "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                        .addField(EmbedCreateFields.Field.of("Punishment", action, false))
                        .addField(EmbedCreateFields.Field.of("IDs", "```ini" + "\n" +
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
