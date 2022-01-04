package tv.banko.antiscam.command;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public abstract class DefaultCommand {

    protected final DiscordClient client;
    protected final String commandName;

    protected long applicationId;

    public DefaultCommand(DiscordClient client, String commandName) {
        this.client = client;
        this.commandName = commandName;

        Optional<Long> applicationIdOptional = client.getApplicationId().blockOptional();

        if (applicationIdOptional.isEmpty()) {
            return;
        }

        this.applicationId = applicationIdOptional.get();
    }

    protected final void register(ApplicationCommandRequest request) {
        client.getApplicationService().createGlobalApplicationCommand(applicationId, request).block();
    }

    public abstract Mono<?> response(ChatInputInteractionEvent event);
}
