package tv.banko.antiscam.admin.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdminCommand extends DefaultGuildCommand {

    private final AntiScam antiScam;
    private final Pattern pattern;

    public AdminCommand(AntiScam antiScam, long guildId) {
        super(antiScam.getClient(), guildId, "admin");
        this.antiScam = antiScam;
        this.pattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");

        ImmutableApplicationCommandRequest.Builder request = ApplicationCommandRequest.builder()
            .name(commandName)
            .description("manage the AntiScam bot");

        // /admin add <url>

        request.addOption(ApplicationCommandOptionData.builder()
            .name("add")
            .description("Add a url")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("url")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .description("The url")
                .required(true)
                .build())
            .build());

        // /admin approve <url>

        request.addOption(ApplicationCommandOptionData.builder()
            .name("approve")
            .description("Approve a url")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("url")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .description("The url")
                .required(true)
                .build())
            .build());

        // /admin remove <url>

        request.addOption(ApplicationCommandOptionData.builder()
            .name("remove")
            .description("Remove a url")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("url")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .description("The url")
                .required(true)
                .build())
            .build());

        // /admin broadcast <Message> <Condition>

        request.addOption(ApplicationCommandOptionData.builder()
            .name("broadcast")
            .description("Send a message to every log channel")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("message")
                .required(true)
                .description("The message which will be sent")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .build())
            .addOption(ApplicationCommandOptionData.builder()
                .name("condition")
                .required(false)
                .description("What the guild needs to meet")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Violation-Detection enabled")
                    .value("violation-enabled")
                    .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Violation-Detection disabled")
                    .value("violation-disabled")
                    .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Partnered")
                    .value("partnered")
                    .build())
                .build())
            .build());

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
            .ephemeral(true)
            .build()).block();

        ApplicationCommandInteractionOption first = list.get(0);

        if (first.getName().equalsIgnoreCase("add")) {
            return addURL(event);
        }

        if (first.getName().equalsIgnoreCase("remove")) {
            return removeURL(event);
        }

        if (first.getName().equalsIgnoreCase("approve")) {
            return approveURL(event);
        }

        if (first.getName().equalsIgnoreCase("broadcast")) {
            return broadcast(event);
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(getEmbedToRespond("no_argument_given"))
            .build());
    }

    private Mono<?> addURL(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("url")).findFirst().orElse(null);

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

        String value = firstValue.asString().toLowerCase();

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("no_url"))
                .build());
        }

        if (antiScam.isScam(value, optionalSnowflake.get())) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("already_registered"))
                .build());
        }

        if (antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("needs_approvement"))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().addPhrase(value, event.getInteraction().getUser().getId(),
            optionalSnowflake.get());

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | URL added")
                .description("Successfully added the url to the system.")
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> removeURL(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("url")).findFirst().orElse(null);

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

        String value = firstValue.asString().toLowerCase();

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().removePhrase(value);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | URL removed")
                .description("Successfully removed the url from the system.")
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> approveURL(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("url")).findFirst().orElse(null);

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

        String value = firstValue.asString().toLowerCase();

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("no_url"))
                .build());
        }

        if (!antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("admin_no_phrase"))
                .build());
        }

        if (antiScam.getMongoDB().getScamCollection().isApprovedPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("admin_already_approved"))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().approvePhrase(value);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | URL approved")
                .description("Successfully approved the url.")
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> broadcast(ChatInputInteractionEvent event) {

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("message")).findFirst().orElse(null);

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

        String value = firstValue.asString().replace("\\n", "\n");

        antiScam.getMongoDB().getLogCollection().sendMessages(EmbedCreateSpec.builder()
            .title(":newspaper: | Information")
            .description(value)
            .build(), true);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | Broadcast")
                .description("Broadcasts are in **queue** now.")
                .timestamp(Instant.now())
                .build())
            .build());
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
            case "already_registered" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("This phrase is **already registered**.")
                .timestamp(Instant.now())
                .build();
            case "needs_approvement" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("This phrase is **already registered** but needs to be **approved** by Banko.")
                .timestamp(Instant.now())
                .build();
            case "already_approved" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("This phrase is a **public phrase** (already approved by Banko). You cannot remove this phrase!")
                .timestamp(Instant.now())
                .build();
            case "no_url" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("URLs have to start with **http://** or **https://** and end with a **domain ending**.")
                .timestamp(Instant.now())
                .build();
            case "admin_already_approved" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("This phrase is already approved.")
                .timestamp(Instant.now())
                .build();
            case "admin_no_phrase" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("This phrase is not registered.")
                .timestamp(Instant.now())
                .build();
            case "admin_no_permission" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("You cannot use this subcommand.")
                .timestamp(Instant.now())
                .build();
            default -> EmbedCreateSpec.builder()
                .description(key)
                .build();
        };
    }
}
