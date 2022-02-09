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
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.database.ScamCollection;
import tv.banko.antiscam.punishment.PunishmentType;
import tv.banko.antiscam.violation.ViolationType;

import java.time.Instant;
import java.util.*;
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

        // /antiscam language <Language>

        ImmutableApplicationCommandOptionData.Builder language = ApplicationCommandOptionData.builder()
            .name("type")
            .description("The language (use one of the selection)")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true);

        List<String> keys = new ArrayList<>();

        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                if (ResourceBundle.getBundle("messages", locale) == null) {
                    continue;
                }

                String key = locale.getDisplayLanguage() + " (" + locale.getCountry() + ")";

                if (keys.contains(key)) {
                    continue;
                }

                keys.add(key);

                language.addChoice(ApplicationCommandOptionChoiceData.builder()
                    .name(key)
                    .value(locale.toLanguageTag())
                    .build());
            } catch (MissingResourceException ignored) {
            }
        }

        request.addOption(ApplicationCommandOptionData.builder()
            .name("language")
            .description("Change the language of the bot (commands are currently excluded)")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(language.build())
            .build());

        register(request.build());
    }

    @Override
    public Mono<?> response(ChatInputInteractionEvent event) {
        if (!event.getCommandName().equals(commandName)) {
            return Mono.empty();
        }

        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        Optional<Member> optionalMember = event.getInteraction().getMember();

        if (optionalMember.isEmpty()) {
            return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .ephemeral(true)
                .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                .build());
        }

        Member member = optionalMember.get();

        member.getBasePermissions().subscribe(permissions -> {

            if (permissions.isEmpty()) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                    .build()).subscribe();
                return;
            }

            if (!permissions.contains(Permission.ADMINISTRATOR)) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                    .build()).subscribe();
                return;
            }

            List<ApplicationCommandInteractionOption> list = event.getOptions();

            if (list.isEmpty()) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                    .build()).subscribe();
                return;
            }

            event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .content("<a:loadingDownload:806936664521703435> " + antiScam.getLanguage().get("loading",
                    event.getInteraction().getGuildId().orElse(null)) + "...")
                .ephemeral(true)
                .build()).subscribe();

            ApplicationCommandInteractionOption first = list.get(0);

            switch (first.getName().toLowerCase()) {
                case "help" -> help(event).subscribe();
                case "list" -> listPhrases(event).subscribe();
                case "log" -> setLog(event).subscribe();
                case "punishment" -> setPunishment(event).subscribe();
                case "add" -> addURL(event).subscribe();
                case "remove" -> removeURL(event).subscribe();
                case "violation" -> violation(event).subscribe();
                case "language" -> language(event).subscribe();
                default -> event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                    .build()).subscribe();
            }
        });

        return Mono.justOrEmpty(true);
    }

    private Mono<?> help(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":bookmark_tabs: | " + antiScam.getLanguage().get("help", guildId))
                .description(antiScam.getLanguage().get("help_detailed", guildId))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> listPhrases(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
                .build());
        }

        ScamCollection collection = antiScam.getMongoDB().getScamCollection();

        List<String> github = antiScam.getScamAPI().getGithubDomains();
        List<String> approved = collection.getApprovedPhrases();
        List<String> nonApproved = collection.getGuildNonApprovedPhrases(guildId);

        StringBuilder builder = new StringBuilder();

        for (String s : approved) {

            if ((builder.length() + (", `" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":scroll: | " + antiScam.getLanguage().get("approved_phrases", guildId))
                        .description(builder.toString())
                        .build())
                    .build()).subscribe();
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
                    .title(":scroll: | " + antiScam.getLanguage().get("approved_phrases", guildId))
                    .description(builder.toString())
                    .build())
                .build()).subscribe();
            builder = new StringBuilder();
        }

        for (String s : nonApproved) {

            if ((builder.length() + (", `" + s + "`").length()) >= Embed.MAX_DESCRIPTION_LENGTH) {
                event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .ephemeral(true)
                    .addEmbed(EmbedCreateSpec.builder()
                        .title(":scroll: | " + antiScam.getLanguage().get("non_approved_phrases", guildId))
                        .description(builder.toString())
                        .build())
                    .build()).subscribe();
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
                    .title(":scroll: | " + antiScam.getLanguage().get("non_approved_phrases", guildId))
                    .description(builder.toString())
                    .build())
                .build()).subscribe();
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":scroll: | " + antiScam.getLanguage().get("list_phrases", guildId))
                .description(antiScam.getLanguage().get("github_phrases", guildId) + ": **" + github.size() + " " +
                    (github.size() == 1 ? antiScam.getLanguage().get("phrase", guildId) :
                        antiScam.getLanguage().get("phrase_plural", guildId)) + "** ([" +
                    antiScam.getLanguage().get("source", guildId) + "](" + antiScam.getScamAPI().getUrl() + "))" +

                    "\n" + antiScam.getLanguage().get("approved_phrases", guildId) + ": **" + approved.size() + " " +
                    (approved.size() == 1 ? antiScam.getLanguage().get("phrase", guildId) :
                        antiScam.getLanguage().get("phrase_plural", guildId)) + "**" +

                    "\n" + antiScam.getLanguage().get("non_approved_phrases", guildId) + ": **" + nonApproved.size() + " "
                    + (nonApproved.size() == 1 ? antiScam.getLanguage().get("phrase", guildId) :
                    antiScam.getLanguage().get("phrase_plural", guildId)) + "**")
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> setLog(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("channel")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        firstValue.asChannel().subscribe(channel -> {
            if (!(channel.getType().equals(Channel.Type.GUILD_NEWS) || channel.getType().equals(Channel.Type.GUILD_TEXT))) {
                event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("no_text_channel", guildId))
                    .build()).subscribe();
                return;
            }

            Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

            if (optionalSnowflake.isEmpty()) {
                event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                    .build()).subscribe();
                return;
            }

            GuildMessageChannel messageChannel = (GuildMessageChannel) channel;

            this.antiScam.getMongoDB().getLogCollection().setChannel(optionalSnowflake.get(), messageChannel.getId());

            event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":white_check_mark: | " + antiScam.getLanguage().get("log_set", guildId))
                    .description(antiScam.getLanguage().get("log_set_detailed", guildId)
                        .replace("%channel%", messageChannel.getMention()))
                    .timestamp(Instant.now())
                    .build())
                .build());
        });

        return Mono.justOrEmpty(true);
    }

    private Mono<?> setPunishment(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);


        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("type")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        String value = firstValue.asString();

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
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
            case "scam" -> this.antiScam.getMongoDB().getSettingsCollection().setPunishment(guildId, type);
            case "medium" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(guildId,
                ViolationType.MEDIUM, type);
            case "high" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(guildId,
                ViolationType.HIGH, type);
            case "extreme" -> this.antiScam.getMongoDB().getViolationCollection().setViolationPunishment(guildId,
                ViolationType.EXTREME, type);
            default -> {
                return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("category_not_found", guildId))
                    .build());
            }
        }

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("punishment_set", guildId))
                .description(antiScam.getLanguage().get("punishment_set_detailed", guildId)
                    .replace("%type%", type.getName()).replace("%category%", category))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> addURL(ChatInputInteractionEvent event) {

        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("url")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        String value = firstValue.asString().toLowerCase();

        Optional<Snowflake> optionalSnowflake = event.getInteraction().getGuildId();

        if (optionalSnowflake.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("no_url", guildId))
                .build());
        }

        if (antiScam.isScam(value, optionalSnowflake.get())) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("already_registered", guildId))
                .build());
        }

        if (antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("needs_approvement", guildId))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().addPhrase(value, optionalSnowflake.get(), false);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("url_added", guildId))
                .description(antiScam.getLanguage().get("url_added_detailed", guildId).replace("%url%", value))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> removeURL(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("url")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        String value = firstValue.asString().toLowerCase();

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
                .build());
        }

        if (!antiScam.isScam(value, guildId)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("already_registered", null))
                .build());
        }

        if (!antiScam.getMongoDB().getScamCollection().isRegisteredByGuild(value, guildId) ||
            antiScam.getMongoDB().getScamCollection().isApprovedPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("already_approved", null))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().removePhrase(value);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("url_removed", guildId))
                .description(antiScam.getLanguage().get("url_removed_detailed", guildId).replace("%url%", value))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> violation(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
                .build());
        }

        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().findFirst().orElse(null);

        boolean enable = firstOption.getName().equalsIgnoreCase("enable");

        if (antiScam.getMongoDB().getViolationCollection().isEnabled(guildId) == enable) {
            if (enable) {
                return event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("violation_already_enabled", guildId))
                    .build());
            }
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("violation_already_disabled", guildId))
                .build());
        }

        antiScam.getMongoDB().getViolationCollection().setState(guildId, enable);

        if (enable) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":white_check_mark: | " + antiScam.getLanguage().get("violation_enable", guildId))
                    .description(antiScam.getLanguage().get("violation_enable_detailed", guildId))
                    .timestamp(Instant.now())
                    .build())
                .build());
        }
        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("violation_disable", guildId))
                .description(antiScam.getLanguage().get("violation_disable_detailed", guildId))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> language(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);


        ApplicationCommandInteractionOption first = event.getOptions().get(0);

        List<ApplicationCommandInteractionOption> list = first.getOptions();

        if (list.isEmpty()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOption firstOption = list.stream().filter(o ->
            o.getName().equalsIgnoreCase("type")).findFirst().orElse(null);

        if (firstOption == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        ApplicationCommandInteractionOptionValue firstValue = firstOption.getValue().orElse(null);

        if (firstValue == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", guildId))
                .build());
        }

        String value = firstValue.asString();

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
                .build());
        }

        Locale locale = Locale.forLanguageTag(value);

        antiScam.getMongoDB().getSettingsCollection().setLocale(guildId, locale);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("locale", guildId))
                .description(antiScam.getLanguage().get("locale_detailed", guildId)
                    .replace("%locale%", locale.getDisplayLanguage(locale) +
                        " (" + locale.getDisplayCountry(locale) + ")"))
                .timestamp(Instant.now())
                .build())
            .build());
    }

}
