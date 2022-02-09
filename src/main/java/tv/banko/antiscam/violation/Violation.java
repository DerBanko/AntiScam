package tv.banko.antiscam.violation;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.rest.util.Permission;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.util.URLHelper;

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
        if (URLHelper.isVerified(message.getContent())) {
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
                        antiScam.getMongoDB().getLogCollection().sendMessage(guildId, antiScam.getTemplate().getScamDeleted(member,
                            channel, message, score, type, guildId));
                        return;
                    }

                    antiScam.getMongoDB().getLogCollection().sendMessage(guildId, antiScam.getTemplate().getScamDetected(member,
                        channel, message, score, type, action, guildId));
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
