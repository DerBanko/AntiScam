package tv.banko.antiscam.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.*;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.database.ScamCollection;
import tv.banko.antiscam.punishment.PunishmentType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class AntiScamCommand extends DefaultCommand {

    private final String BOT_OWNER_ID = System.getenv("BOT_OWNER_ID");

    private final AntiScam antiScam;
    private final Pattern pattern;

    public AntiScamCommand(AntiScam antiScam) {
        super(antiScam.getClient(), "antiscam");
        this.antiScam = antiScam;
        this.pattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");

        ImmutableApplicationCommandRequest.Builder request = ApplicationCommandRequest.builder()
                .name(commandName)
                .description("manage the AntiScam bot");

        // /antiscam list

        request.addOption(ApplicationCommandOptionData.builder()
                .name("list")
                .description("List all urls")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .build());

        // /antiscam log <Channel>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("log")
                .description("Set the logging channel of scam messages")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("channel")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .description("The channel")
                        .required(true)
                        .build())
                .build());

        // /antiscam add <url>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("add")
                .description("Add a url to the system")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("url")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The url")
                        .required(true)
                        .build())
                .build());

        // /antiscam remove <url>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("remove")
                .description("Remove a url from the system")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("url")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The url")
                        .required(true)
                        .build())
                .build());

        // /antiscam approve <url>

        request.addOption(ApplicationCommandOptionData.builder()
                .name("approve")
                .description("Approve a url (only for bot owner)")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("url")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .description("The url")
                        .required(true)
                        .build())
                .build());

        // /antiscam punishment <Choice>

        ImmutableApplicationCommandOptionData.Builder builder = ApplicationCommandOptionData.builder()
                .name("type")
                .description("The punishment type (use one of the selection)")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true);

        for (String type : new String[]{"Message Delete#DELETE", "Kick Member#KICK",
                "Ban Member#BAN", "Timeout Member#TIMEOUT"}) {
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
                .addOption(ApplicationCommandOptionData.builder()
                        .name("duration")
                        .description("The duration of the punishment in seconds (only relevant for timeouts)")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .build())
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
                .ephemeral(true)
                .build()).block();

        ApplicationCommandInteractionOption first = list.get(0);

        if (first.getName().equalsIgnoreCase("list")) {
            return listPhrases(event);
        }

        if (first.getName().equalsIgnoreCase("log")) {
            return setLog(event);
        }

        if (first.getName().equalsIgnoreCase("punishment")) {
            return setPunishment(event);
        }

        if (first.getName().equalsIgnoreCase("add")) {
            return addURL(event);
        }

        if (first.getName().equalsIgnoreCase("remove")) {
            return removeURL(event);
        }

        if (first.getName().equalsIgnoreCase("approve")) {
            return approveURL(event);
        }

        return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("no_argument_given"))
                .build());
    }

    private Mono<?> listPhrases(ChatInputInteractionEvent event) {

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        ScamCollection collection = antiScam.getMongoDB().getScamCollection();

        List<String> github = antiScam.getScamAPI().getGithubDomains();
        List<String> approved = collection.getApprovedPhrases();
        List<String> nonApproved = collection.getGuildNonApprovedPhrases(optionalSnowflake.get());

        StringBuilder builder = new StringBuilder();

        for (String s : github) {

            if ((builder.length() + ("`" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":scroll: | Github phrases")
                                .description(builder.toString())
                                .url(antiScam.getScamAPI().getUrl())
                                .build())
                        .build());
                builder = new StringBuilder();
            }

            builder.append("`").append(s).append("`");
        }

        if(!builder.isEmpty()) {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .title(":scroll: | Github phrases")
                            .description(builder.toString())
                            .url(antiScam.getScamAPI().getUrl())
                            .build())
                    .build());
            builder = new StringBuilder();
        }

        for (String s : approved) {

            if ((builder.length() + ("`" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":scroll: | Approved phrases")
                                .description(builder.toString())
                                .build())
                        .build());
                builder = new StringBuilder();
            }

            builder.append("`").append(s).append("`");
        }

        if(!builder.isEmpty()) {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .title(":scroll: | Approved phrases")
                            .description(builder.toString())
                            .build())
                    .build());
            builder = new StringBuilder();
        }

        for (String s : nonApproved) {

            if ((builder.length() + ("`" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":scroll: | Non approved phrases")
                                .description(builder.toString())
                                .build())
                        .build());
                builder = new StringBuilder();
            }

            builder.append("`").append(s).append("`");
        }

        if(!builder.isEmpty()) {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addEmbed(EmbedCreateSpec.builder()
                            .title(":scroll: | Non approved phrases")
                            .description(builder.toString())
                            .build())
                    .build());
        }

        return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                        .title(":scroll: | List of phrases")
                        .description("`Github Phrases`: **" + github.size() + " Phrase" +
                                (github.size() != 1 ? "s" : "") + "**[Source](" + antiScam.getScamAPI().getUrl() + ")" +

                                "\n`Approved Phrases`: **" + approved.size() + " Phrase" +
                                (approved.size() != 1 ? "s" : "") + "** (added via `/antiscam add <URL>` and approved)" +

                                "\n`Non approved Phrases`: **" + nonApproved.size() + " Phrase"
                                + (nonApproved.size() != 1 ? "s" : "") + "** (added via `/antiscam add <URL>` on your guild)")
                        .timestamp(Instant.now())
                        .build())
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

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        GuildMessageChannel messageChannel = (GuildMessageChannel) channel;

        this.antiScam.getMongoDB().getLogCollection().setChannel(optionalSnowflake.get(), messageChannel.getId());

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
                o.getName().equalsIgnoreCase("type")).findFirst().orElse(null);

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

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("not_enough_arguments"))
                    .build());
        }

        switch (value) {
            case "KICK" -> {
                PunishmentType type = PunishmentType.kick();
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(optionalSnowflake.get(), type);
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
                PunishmentType type = PunishmentType.ban();
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(optionalSnowflake.get(), type);
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":white_check_mark: | Punishment set")
                                .description("Successfully changed punishment channel to `ban user`.")
                                .timestamp(Instant.now())
                                .build())
                        .build());
            }
            case "TIMEOUT" -> {
                ApplicationCommandInteractionOption secondOption = list.stream().filter(o ->
                        o.getName().equalsIgnoreCase("duration")).findFirst().orElse(null);

                int duration = 600;

                if (secondOption != null) {
                    ApplicationCommandInteractionOptionValue secondValue = secondOption.getValue().orElse(null);

                    if (secondValue != null) {
                        duration = (int) secondValue.asLong();
                    }
                }

                PunishmentType type = PunishmentType.timeout(duration);
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(optionalSnowflake.get(), type);
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(":white_check_mark: | Punishment set")
                                .description("Successfully changed punishment channel to `timeout " + duration + "s`.")
                                .timestamp(Instant.now())
                                .build())
                        .build());
            }
            default -> {
                PunishmentType type = PunishmentType.delete();
                this.antiScam.getMongoDB().getSettingsCollection().setPunishment(optionalSnowflake.get(), type);
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

        Snowflake guildId = optionalSnowflake.get();

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("no_url"))
                    .build());
        }

        if (!event.getInteraction().getUser().getId().asString().equals(BOT_OWNER_ID)) {
            if (!antiScam.isScam(value, optionalSnowflake.get())) {
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(getEmbedToRespond("already_registered"))
                        .build());
            }

            if (!antiScam.getMongoDB().getScamCollection().isRegisteredByGuild(value, guildId)) {
                return event.editReply(InteractionReplyEditSpec.builder()
                        .contentOrNull(null)
                        .addEmbed(getEmbedToRespond("already_approved"))
                        .build());
            }
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

        if (!event.getInteraction().getUser().getId().asString().equals(BOT_OWNER_ID)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("admin_no_permission"))
                    .build());
        }

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
