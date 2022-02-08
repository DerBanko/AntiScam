package tv.banko.antiscam.admin.listener;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.listener.DefaultListener;

import java.time.Instant;
import java.util.Optional;

public class ButtonListener extends DefaultListener {

    public ButtonListener(AntiScam antiScam) {
        super(antiScam, antiScam.getClient(), antiScam.getGateway());

        gateway.getEventDispatcher().on(ButtonInteractionEvent.class).subscribe(event -> {
            try {

                if (!event.getCustomId().equalsIgnoreCase("approve") &&
                    !event.getCustomId().equalsIgnoreCase("remove")) {
                    return;
                }

                Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

                Optional<Message> optionalMessage = event.getMessage();

                if (optionalMessage.isEmpty()) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":warning: | An error occurred")
                            .description("This phrase **could not** be **approved**. Please try again later.")
                            .timestamp(Instant.now())
                            .build())
                        .build()).subscribe();
                    return;
                }

                Message message = optionalMessage.get();

                Optional<Embed> optionalEmbed = message.getEmbeds().stream().findFirst();

                if (optionalEmbed.isEmpty()) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(antiScam.getLanguage().getEmbed("admin_phrase_not_found", guildId))
                        .build()).subscribe();
                    return;
                }

                Embed embed = optionalEmbed.get();

                Optional<Embed.Footer> optionalFooter = embed.getFooter();

                if (optionalFooter.isEmpty()) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(antiScam.getLanguage().getEmbed("admin_phrase_not_found", guildId))
                        .build()).subscribe();
                    return;
                }

                String phrase = optionalFooter.get().getText();

                if (!antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(phrase)) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(antiScam.getLanguage().getEmbed("admin_no_phrase", guildId))
                        .build()).subscribe();
                    return;
                }

                if (event.getCustomId().equalsIgnoreCase("approve")) {

                    if (antiScam.getMongoDB().getScamCollection().isApprovedPhrase(phrase)) {
                        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .ephemeral(true)
                            .addEmbed(antiScam.getLanguage().getEmbed("admin_already_approved", guildId))
                            .build()).subscribe();
                        return;
                    }

                    antiScam.getMongoDB().getScamCollection().approvePhrase(phrase);

                    antiScam.getAdmin().getButton().editMessage(message);

                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":white_check_mark: | " + antiScam.getLanguage().get("url_approved", guildId))
                            .description(antiScam.getLanguage().get("url_approved_detailed", guildId).replace("%url%", phrase))
                            .timestamp(Instant.now())
                            .build())
                        .build()).subscribe();

                    return;
                }

                antiScam.getMongoDB().getScamCollection().removePhrase(phrase);

                message.delete().subscribe();

                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":white_check_mark: | " + antiScam.getLanguage().get("url_removed", guildId))
                        .description(antiScam.getLanguage().get("url_removed_detailed", guildId).replace("%url%", phrase))
                        .timestamp(Instant.now())
                        .build())
                    .build()).subscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
