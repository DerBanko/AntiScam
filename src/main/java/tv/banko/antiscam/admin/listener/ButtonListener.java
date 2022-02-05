package tv.banko.antiscam.admin.listener;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Color;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.listener.DefaultListener;

import java.time.Instant;
import java.util.List;
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

                if(optionalEmbed.isEmpty()) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":warning: | An error occurred")
                            .description("Could **not find phrase**. Please try again later.")
                            .timestamp(Instant.now())
                            .build())
                        .build()).subscribe();
                    return;
                }

                Embed embed = optionalEmbed.get();

                Optional<Embed.Footer> optionalFooter = embed.getFooter();

                if(optionalFooter.isEmpty()) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":warning: | An error occurred")
                            .description("Could **not find phrase**. Please try again later.")
                            .timestamp(Instant.now())
                            .build())
                        .build()).subscribe();
                    return;
                }

                String phrase = optionalFooter.get().getText();

                if(!antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(phrase)) {
                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":warning: | An error occurred")
                            .description("This phrase is **not registered**.")
                            .timestamp(Instant.now())
                            .build())
                        .build()).subscribe();
                    return;
                }

                if(event.getCustomId().equalsIgnoreCase("approve")) {

                    if(antiScam.getMongoDB().getScamCollection().isApprovedPhrase(phrase)) {
                        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .ephemeral(true)
                            .addEmbed(EmbedCreateSpec.builder()
                                .title(":warning: | An error occurred")
                                .description("This phrase is **already approved**.")
                                .timestamp(Instant.now())
                                .build())
                            .build()).subscribe();
                        return;
                    }

                    antiScam.getMongoDB().getScamCollection().approvePhrase(phrase);

                    antiScam.getAdmin().getButton().editMessage(message);

                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .ephemeral(true)
                        .addEmbed(EmbedCreateSpec.builder()
                            .title(":white_check_mark: | Phrase approved")
                            .description("The phrase `" + phrase + "` has been **successfully approved**.")
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
                        .title(":white_check_mark: | Phrase removed")
                        .description("The phrase `" + phrase + "` has been **successfully removed**.")
                        .timestamp(Instant.now())
                        .build())
                    .build()).subscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
