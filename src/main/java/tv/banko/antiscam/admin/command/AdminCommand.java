package tv.banko.antiscam.admin.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.lang.management.ManagementFactory;
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

        // /admin info

        request.addOption(ApplicationCommandOptionData.builder()
            .name("info")
            .description("Information of anything")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build());

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

        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        Optional<Member> optionalMember = event.getInteraction().getMember();

        if (optionalMember.isEmpty()) {
            return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                .build());
        }

        Member member = optionalMember.get();

        member.getBasePermissions().subscribe(permissions -> {

            if (permissions.isEmpty()) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                    .build()).subscribe();
                return;
            }

            if (!permissions.contains(Permission.ADMINISTRATOR)) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed(antiScam.getLanguage().getEmbed("no_permission", guildId))
                    .build()).subscribe();
                return;
            }

            List<ApplicationCommandInteractionOption> list = event.getOptions();

            if (list.isEmpty()) {
                event.reply(InteractionApplicationCommandCallbackSpec.builder()
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
                case "info" -> info(event).subscribe();
                case "add" -> addURL(event).subscribe();
                case "remove" -> removeURL(event).subscribe();
                case "approve" -> approveURL(event).subscribe();
                case "broadcast" -> broadcast(event).subscribe();
                default -> event.editReply(InteractionReplyEditSpec.builder()
                    .contentOrNull(null)
                    .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments",
                        event.getInteraction().getGuildId().orElse(null)))
                    .build()).subscribe();
            }
        });

        return Mono.justOrEmpty(true);
    }

    private Mono<?> info(ChatInputInteractionEvent event) {
        antiScam.getGateway().getGuilds().collectList().subscribe(list -> {

            int count = list.size();
            int memberCount = 0;

            int partnered = 0;
            int verified = 0;

            for (Guild guild : list) {
                memberCount += guild.getMemberCount();

                if (guild.getFeatures().contains("PARTNERED")) {
                    partnered++;
                }

                if (guild.getFeatures().contains("VERIFIED")) {
                    verified++;
                }
            }

            Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

            event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(EmbedCreateSpec.builder()
                    .title(":information_source: | " + antiScam.getLanguage().get("information", guildId))
                    .description(":clock3: | " + antiScam.getLanguage().get("uptime", guildId) + ": `" +
                        (ManagementFactory.getRuntimeMXBean().getUptime() / 1000) + "s`" +
                        "\n" +
                        "\n" + ":computer: | **" + antiScam.getLanguage().get("guilds", guildId) + "**" +
                        "\n" + "" +
                        "\n" + " - **" + count + "** " + antiScam.getLanguage().get("guilds", guildId) +
                        "\n" + " - **" + memberCount + "** " + antiScam.getLanguage().get("members", guildId) +
                        "\n" + " - **" + verified + "** " + antiScam.getLanguage().get("verified_guilds", guildId) +
                        "\n" + " - **" + partnered + "** " + antiScam.getLanguage().get("partnered_guilds", guildId) +
                        "\n" + "")
                    .timestamp(Instant.now())
                    .build())
                .build()).subscribe();
        });

        return Mono.justOrEmpty(true);
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

        if (guildId == null) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("not_enough_arguments", null))
                .build());
        }

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("no_url", guildId))
                .build());
        }

        if (antiScam.isScam(value, guildId)) {
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

        antiScam.getMongoDB().getScamCollection().addPhrase(value, guildId, true);

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

    private Mono<?> approveURL(ChatInputInteractionEvent event) {

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

        if (!pattern.matcher(value).matches()) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("no_url", guildId))
                .build());
        }

        if (!antiScam.getMongoDB().getScamCollection().isRegisteredPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("admin_no_phrase", guildId))
                .build());
        }

        if (antiScam.getMongoDB().getScamCollection().isApprovedPhrase(value)) {
            return event.editReply(InteractionReplyEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(antiScam.getLanguage().getEmbed("admin_already_approved", guildId))
                .build());
        }

        antiScam.getMongoDB().getScamCollection().approvePhrase(value);

        return event.editReply(InteractionReplyEditSpec.builder()
            .contentOrNull(null)
            .addEmbed(EmbedCreateSpec.builder()
                .title(":white_check_mark: | " + antiScam.getLanguage().get("url_approved", guildId))
                .description(antiScam.getLanguage().get("url_approved_detailed", guildId).replace("%url%", value))
                .timestamp(Instant.now())
                .build())
            .build());
    }

    private Mono<?> broadcast(ChatInputInteractionEvent event) {

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
            o.getName().equalsIgnoreCase("message")).findFirst().orElse(null);

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

        String value = firstValue.asString().replace("\\n", "\n");

        antiScam.getMongoDB().getLogCollection().sendMessages(EmbedCreateSpec.builder()
            .title(":newspaper: | " + antiScam.getLanguage().get("information", guildId))
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
}
