package tv.banko.antiscam.command;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public abstract class DefaultCommand {

    protected final DiscordClient client;
    protected final Guild guild;
    protected final String commandName;

    protected long applicationId;

    public DefaultCommand(DiscordClient client, Guild guild, String commandName) {
        this.client = client;
        this.guild = guild;
        this.commandName = commandName;

        Optional<Long> applicationIdOptional = client.getApplicationId().blockOptional();

        if (applicationIdOptional.isEmpty()) {
            return;
        }

        this.applicationId = applicationIdOptional.get();
    }

    protected final void register(ApplicationCommandRequest request) {
        client.getApplicationService().createGuildApplicationCommand(applicationId, guild.getId().asLong(), request).block();
    }

    public abstract Mono<?> response(ChatInputInteractionEvent event);
}
