package tv.banko.antiscam.admin.command;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public abstract class DefaultGuildCommand {

    protected final DiscordClient client;
    protected final String commandName;
    protected final long guildId;

    protected long applicationId;

    public DefaultGuildCommand(DiscordClient client, long guildId, String commandName) {
        this.client = client;
        this.commandName = commandName;
        this.guildId = guildId;

        Optional<Long> applicationIdOptional = client.getApplicationId().blockOptional();

        if (applicationIdOptional.isEmpty()) {
            return;
        }

        this.applicationId = applicationIdOptional.get();
    }

    protected final void register(ApplicationCommandRequest request) {
        if (client.getApplicationService().getGuildApplicationCommands(applicationId, guildId).toStream().anyMatch(data ->
            data.name().equalsIgnoreCase(commandName))) {
            client.getApplicationService().bulkOverwriteGuildApplicationCommand(applicationId, guildId, List.of(request)).subscribe();
            return;
        }

        client.getApplicationService().createGuildApplicationCommand(applicationId, guildId, request).subscribe();
    }

    public abstract Mono<?> response(ChatInputInteractionEvent event);
}
