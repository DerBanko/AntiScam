package tv.banko.antiscam.language;

import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;

import java.time.Instant;

public class EmbedTemplate {

    private final Language language;

    public EmbedTemplate(Language language) {
        this.language = language;
    }

    public EmbedCreateSpec get(String key, Snowflake guildId) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
            .title(":warning: | " + language.get("error_occurred", guildId))

            .timestamp(Instant.now());
        switch (key) {
            case "no_permission" -> builder.description(language.get("following_permission_required", guildId) + ": `ADMINISTRATOR`.");
            case "not_enough_arguments" -> builder.description(language.get("arguments_required", guildId) + ".");
            case "already_registered" -> builder.description(language.get("phrase_already_registered", guildId) + ".");
            case "needs_approvement" -> builder.description(language.get("phrase_needs_approvement", guildId) + ".");
            case "already_approved" -> builder.description(language.get("phrase_not_removable", guildId) + ".");
            case "no_url" -> builder.description(language.get("phrase_no_url", guildId) + ".");
            case "admin_already_approved" -> builder.description(language.get("admin_phrase_already_approved", guildId) + ".");
            case "admin_no_phrase" -> builder.description(language.get("admin_phrase_not_exists", guildId) + ".");
            case "admin_phrase_not_found" -> builder.description(language.get("admin_phrase_not_found", guildId) + ".");

            case "channel_not_existing" -> builder.description(language.get("channel_not_existing", guildId) + ".");
            case "no_text_channel" -> builder.description(language.get("no_text_channel", guildId) + ".");
            case "category_not_found" -> builder.description(language.get("category_not_found", guildId) + ".");

            case "violation_already_enabled" -> builder.description(language.get("violation_already_enabled", guildId) + ".");
            case "violation_already_disabled" -> builder.description(language.get("violation_already_disabled", guildId) + ".");
            default -> builder.description("**" + key + "**");
        }

        return builder.build();
    }

}
