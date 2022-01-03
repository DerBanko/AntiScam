package tv.banko.antiscam.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.*;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.punishment.PunishmentType;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AntiScamCommand extends DefaultCommand {

    private final AntiScam antiScam;

    public AntiScamCommand(AntiScam antiScam, Guild guild) {
        super(antiScam.getClient(), guild, "antiscam");
        this.antiScam = antiScam;

        ImmutableApplicationCommandRequest.Builder request = ApplicationCommandRequest.builder()
                .name(commandName)
                .description("Manage the AntiScam bot");

        // /antiscam log <Channel>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("log")
                .description("Set the logging channel of scam messages")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("channel")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .description("The channel")
                        .build())
                .build());

        // /antiscam punishment <Choice>

        ImmutableApplicationCommandOptionData.Builder builder = ApplicationCommandOptionData.builder()
                .name("type")
                .description("The punishment type (use one of the selection)")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true);

        for (String type : new String[]{"Message Delete#DELETE", "Kick Member#KICK", "Ban Member#BAN"}) {
            String[] s = type.split("#");

            builder.addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name(s[0])
                    .value(s[1])
                    .build());
        }

        request.addOption(ApplicationCommandOptionData.builder()
                .name("punishment")
                .description("Set the punishment when users send scam messages")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(builder.build())
                .build());

        /* TODO: Check message history
        // /antiscam check <Type>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("removetwitch")
                .description("Entferne einen Twitch-Alert")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Twitch-Channel Name")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build())
                .build());*/

        register(request.build());
    }

    @Override
    public Mono<?> response(ChatInputInteractionEvent event) {
        if (!event.getCommandName().equals(commandName)) {
            return Mono.empty();
        }

        Optional<Member> optionalMember = event.getInteraction().getMember();

        if (optionalMember.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_permission"))
                    .build());
        }

        Member member = optionalMember.get();

        Optional<PermissionSet> optionalPermissions = member.getBasePermissions().blockOptional();

        if (optionalPermissions.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_permission"))
                    .build());
        }

        if (!optionalPermissions.get().contains(Permission.ADMINISTRATOR)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_permission"))
                    .build());
        }

        List<ApplicationCommandInteractionOption> list = event.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_argument_given"))
                    .build());
        }

        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .content("<a:loadingDownload:806936664521703435> Loading...")
                .build()).block();

        ApplicationCommandInteractionOption first = list.get(0);

        if (first.getName().equalsIgnoreCase("log")) {
            return setLog(event);
        }

        if (first.getName().equalsIgnoreCase("punishment")) {
            return setPunishment(event);
        }

        return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("no_argument_given"))
                .build());
    }

    private Mono<?> setLog(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
                o.getName().equalsIgnoreCase("channel")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        Optional<Channel> optionalChannel = firstValue.asChannel().blockOptional();

        if (optionalChannel.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("channel_not_existing"))
                    .build());
        }

        Channel channel = optionalChannel.get();

        if (!(channel.getType().equals(Channel.Type.GUILD_NEWS) || channel.getType().equals(Channel.Type.GUILD_TEXT))) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_text_channel"))
                    .build());
        }

        GuildMessageChannel messageChannel = (GuildMessageChannel) channel;

        this.antiScam.getMongoDB().getLogCollection().setChannel(guild.getId(), messageChannel.getId());

        return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                        .title(":white_check_mark: | Log set")
                        .description("Successfully changed log channel to " + messageChannel.getMention() + ".")
                        .timestamp(Instant.now())
                        .build())
                .build());
    }

    private Mono<?> setPunishment(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
                o.getName().equalsIgnoreCase("channel")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        String value = firstValue.asString();

        switch (value) {
            case "KICK" -> {
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(guild.getId(), PunishmentType.kick());
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":white_check_mark: | Punishment set")
                                .description("Successfully changed punishment channel to `kick user`.")
                                .timestamp(Instant.now())
                                .build())
                        .build());
            }
            case "BAN" -> {
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(guild.getId(), PunishmentType.ban());
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":white_check_mark: | Punishment set")
                                .description("Successfully changed punishment channel to `ban user`.")
                                .timestamp(Instant.now())
                                .build())
                        .build());
            }
            default -> {
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(guild.getId(), PunishmentType.delete());
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":white_check_mark: | Punishment set")
                                .description("Successfully changed punishment channel to `delete message`.")
                                .timestamp(Instant.now())
                                .build())
                        .build());
            }
        }
    }

    private EmbedCreateSpec getEmbedToRespond(String key) {
        return switch (key) {
            case "no_permission" -> EmbedCreateSpec.builder()
                    .title(":warning: | An error occurred")
                    .description("You **need** the following permission to execute this command: `ADMINISTRATOR`.")
                    .timestamp(Instant.now())
                    .build();
            case "no_argument_given" -> EmbedCreateSpec.builder()
                    .title(":warning: | An error occurred")
                    .description("You **need** to add **arguments**.")
                    .timestamp(Instant.now())
                    .build();
            case "not_enough_arguments" -> EmbedCreateSpec.builder()
                    .title(":warning: | An error occurred")
                    .description("You **need** to add more **arguments**.")
                    .timestamp(Instant.now())
                    .build();
            case "channel_not_existing" -> EmbedCreateSpec.builder()
                    .title(":warning: | An error occurred")
                    .description("This channel is **not existing**.")
                    .timestamp(Instant.now())
                    .build();
            case "no_text_channel" -> EmbedCreateSpec.builder()
                    .title(":warning: | An error occurred")
                    .description("This channel is **no text channel**.")
                    .timestamp(Instant.now())
                    .build();
            default -> EmbedCreateSpec.builder()
                    .description(key)
                    .build();
        };
    }
}
