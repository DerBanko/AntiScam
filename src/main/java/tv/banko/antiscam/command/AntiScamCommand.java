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
import tv.banko.antiscam.violation.ViolationType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class AntiScamCommand extends DefaultCommand {

    private final AntiScam antiScam;
    private final Pattern pattern;

    public AntiScamCommand(AntiScam antiScam) {
        super(antiScam.getClient(), "antiscam");
        this.antiScam = antiScam;
        this.pattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");

        ImmutableApplicationCommandRequest.Builder request = ApplicationCommandRequest.builder()
            .name(commandName)
            .description("Manage the AntiScam bot");

        // /antiscam help

        request.addOption(ApplicationCommandOptionData.builder()
            .name("help")
            .description("Overview")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build());

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

        ImmutableApplicationCommandOptionData.Builder punishment = ApplicationCommandOptionData.builder()
            .name("type")
            .description("The punishment type (use one of the selection)")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true);

        for (String type : new String[]{"Message Delete#DELETE", "Kick Member#KICK",
            "Ban Member#BAN", "Timeout Member#TIMEOUT"}) {
            String[] s = type.split("#");

            punishment.addChoice(ApplicationCommandOptionChoiceData.builder()
                .name(s[0])
                .value(s[1])
                .build());
        }

        // /antiscam punishment <scam/medium/high/extreme> <Choice>

        request.addOption(ApplicationCommandOptionData.builder()
            .name("punishment")
            .description("Set the punishment when users send (potential) scam messages")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(punishment.build())
            .addOption(ApplicationCommandOptionData.builder()
                .name("category")
                .description("The category of the punishment (default: scam message, set e.g. violation)")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Scam url found (default)")
                    .value("scam")
                    .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Medium violation (score: 16-35)")
                    .value("medium")
                    .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("High violation (score: 36-55)")
                    .value("high")
                    .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name("Extreme violation (score: 56+)")
                    .value("extreme")
                    .build())
                .required(false)
                .build())
            .addOption(ApplicationCommandOptionData.builder()
                .name("duration")
                .description("The duration of the punishment in seconds (only relevant for timeouts)")
                .type(ApplicationCommandOption.Type.INTEGER.getValue())
                .required(false)
                .build())
            .build());

        // /antiscam violation enable/disable

        request.addOption(ApplicationCommandOptionData.builder()
            .name("violation")
            .description("Enable/Disable the violation system (beta)")
            .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("enable")
                .description("Enable the violation system")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .build())
            .addOption(ApplicationCommandOptionData.builder()
                .name("disable")
                .description("Disable the violation system")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
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

        switch (first.getName().toLowerCase()) {
            case "help" -> help(event);
            case "list" -> listPhrases(event);
            case "log" -> setLog(event);
            case "punishment" -> setPunishment(event);
            case "add" -> addURL(event);
            case "remove" -> removeURL(event);
            case "violation" -> violation(event);
        }

        if (first.getName().equalsIgnoreCase("help")) {
            return help(event);
        }

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

        if (first.getName().equalsIgnoreCase("violation")) {
            return violation(event);
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(getEmbedToRespond("no_argument_given"))
            .build());
    }

    private Mono<?> help(ChatInputInteractionEvent event) {
        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":bookmark_tabs: | Help and overview")
                .description("""
                    Hey, thanks for using **AntiScam**.

                    :warning: Commands are only available for users having following permission: `ADMINISTRATOR`.

                    To setup the bot, use the following commands:
                     - `/antiscam log <Channel>` to **log messages** filtered by **AntiScam**
                     - `/antiscam punishment <Punishment>` to change the **punishment** the users will receive if they send scam links

                    To add new urls use `/antiscam add <URL>`.

                    **Beta**: Currently there is a **beta** version on our new **violation system**.
                    The algorithm checks if the message **contains a url**, afterwards it checks if the message contains **specific phrases** like `nitro`, ...
                    To change the punishments users will receive use `/antiscam punishment <Punishment> [Category]`.

                    If you have any ideas to improve **AntiScam**, consider joining my [Discord Server](https://discord.gg/YaWfmGmvSN).
                    This bot is [Open Source](https://github.com/DerBanko/AntiScam).
                    Consider [inviting the bot](https://banko.tv/r/invite-antiscam).
                    """)
                .timestamp(Instant.now())
                .build())
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

        for (String s : approved) {

            if ((builder.length() + (", `" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":scroll: | Approved phrases")
                        .description(builder.toString())
                        .build())
                    .build()).block();
                builder = new StringBuilder();
            }

            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append("`").append(s).append("`");
        }

        if (!builder.isEmpty()) {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                .ephemeral(true)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":scroll: | Approved phrases")
                    .description(builder.toString())
                    .build())
                .build()).block();
            builder = new StringBuilder();
        }

        for (String s : nonApproved) {

            if ((builder.length() + (", `" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":scroll: | Non approved phrases")
                        .description(builder.toString())
                        .build())
                    .build()).block();
                builder = new StringBuilder();
            }

            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append("`").append(s).append("`");
        }

        if (!builder.isEmpty()) {
            event.createFollowup(InteractionFollowupCreateSpec.builder()
                .ephemeral(true)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":scroll: | Non approved phrases")
                    .description(builder.toString())
                    .build())
                .build()).block();
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":scroll: | List of phrases")
                .description("`Github Phrases`: **" + github.size() + " Phrase" +
                    (github.size() != 1 ? "s" : "") + " ** ([Source](" + antiScam.getScamAPI().getUrl() + "))" +

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

        PunishmentType type;

        switch (value) {
            case "KICK" -> type = PunishmentType.kick();
            case "BAN" -> type = PunishmentType.ban();
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

                type = PunishmentType.timeout(duration);
            }
            default -> type = PunishmentType.delete();
        }

        String category = "scam";

        ApplicationCommandInteractionOption thirdOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("category")).findFirst().orElse(null);

        if (thirdOption != null) {
            ApplicationCommandInteractionOptionValue thirdValue = thirdOption.getValue().orElse(null);

            if (thirdValue != null) {
                category = thirdValue.asString();
            }
        }

        switch (category) {
            case "scam" -> this.antiScam.getMongoDB().getSettingsCollection().setPunishment(optionalSnowflake.get(), type);
            case "medium" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(optionalSnowflake.get(),
                ViolationType.MEDIUM, type);
            case "high" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(optionalSnowflake.get(),
                ViolationType.HIGH, type);
            case "extreme" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(optionalSnowflake.get(),
                ViolationType.EXTREME, type);
            default -> {
                return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":warning: | An error occurred")
                        .description("Could not find category `" + category + "`.")
                        .timestamp(Instant.now())
                        .build())
                    .build());
            }
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | Punishment set")
                .description("Successfully changed punishment to `" + type.getName() + "` in category `" + category + "`.")
                .timestamp(Instant.now())
                .build())
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

        Snowflake guildId = optionalSnowflake.get();

        if (!antiScam.isScam(value, optionalSnowflake.get())) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("already_registered"))
                .build());
        }

        if (!antiScam.getMongoDB().getScamCollection().isRegisteredByGuild(value, guildId) ||
            antiScam.getMongoDB().getScamCollection().isApprovedPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("already_approved"))
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

    private Mono<?> violation(ChatInputInteractionEvent event) {

        Optional<Snowflake> optional = event.getInteraction().getGuildId();

        if (optional.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        Snowflake guildId = optional.get();

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("not_enough_arguments"))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().findFirst().orElse(null);

        boolean enable = firstOption.getName().equalsIgnoreCase("enable");

        if (antiScam.getMongoDB().getViolationCollection().isEnabled(guildId) == enable) {
            if (enable) {
                return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(getEmbedToRespond("violation_already_enabled"))
                    .build());
            }
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(getEmbedToRespond("violation_already_disabled"))
                .build());
        }

        antiScam.getMongoDB().getViolationCollection().setState(guildId, enable);

        if (enable) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":white_check_mark: | Violation beta enabled")
                    .description("Successfully **enabled** the **violation beta system**.\nUse " +
                        "`/antiscam punishment <Punishment> <Category>` " +
                        "in order to **punish violations** automatically.")
                    .timestamp(Instant.now())
                    .build())
                .build());
        }
        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | Violation beta disabled")
                .description("Successfully removed the url from the system.")
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
            case "violation_already_enabled" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("The **violation beta** is already enabled.")
                .timestamp(Instant.now())
                .build();
            case "violation_already_disabled" -> EmbedCreateSpec.builder()
                .title(":warning: | An error occurred")
                .description("The **violation beta** is already disabled.")
                .timestamp(Instant.now())
                .build();
            default -> EmbedCreateSpec.builder()
                .description(key)
                .build();
        };
    }
}
