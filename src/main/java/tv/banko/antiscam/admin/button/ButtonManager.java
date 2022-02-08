package tv.banko.antiscam.admin.button;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import tv.banko.antiscam.AntiScam;

import java.time.Instant;
import java.util.Optional;

public class ButtonManager {

    private final AntiScam antiScam;

    public ButtonManager(AntiScam antiScam) {
        this.antiScam = antiScam;
    }

    public void sendApprovePhrase(String phrase) {
        antiScam.getGateway().getChannelById(Snowflake.of(System.getenv("PHRASE_CHANNEL_ID"))).cast(GuildMessageChannel.class)
            .subscribe(channel -> channel.createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":newspaper: | " + antiScam.getLanguage().get("new_phrase"))
                    .description("**" + antiScam.getLanguage().get("phrase") + "**: `" + phrase + "`")
                    .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"),
                        "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                    .footer(phrase, "")
                    .build())
                .addComponent(ActionRow.of(Button.primary("approve", antiScam.getLanguage().get("approve_phrase"))))
                .build()).onErrorStop().subscribe());
    }

    public void editMessage(Message message) {
        Optional<Embed> optionalEmbed = message.getEmbeds().stream().findFirst();

        if (optionalEmbed.isEmpty()) {
            return;
        }

        Optional<Embed.Footer> optionalFooter = optionalEmbed.get().getFooter();

        if (optionalFooter.isEmpty()) {
            return;
        }

        String phrase = optionalFooter.get().getText();

        message.edit(MessageEditSpec.builder()
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("phrase_approved"))
                .description("**" + antiScam.getLanguage().get("phrase") + "**: `" + phrase + "`")
                .addField(EmbedCreateFields.Field.of(antiScam.getLanguage().get("timestamp"),
                    "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                .footer(phrase, "")
                .color(Color.of(104, 255, 59))
                .build())
            .addComponent(ActionRow.of(Button.secondary("remove", antiScam.getLanguage().get("remove_phrase"))))
            .build()).onErrorStop().subscribe();
    }

}
