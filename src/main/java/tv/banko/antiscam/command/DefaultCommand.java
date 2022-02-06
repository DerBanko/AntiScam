package tv.banko.antiscam.command;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;
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
        /*client.getGuilds().toStream().forEach(guild -> {
            long guildId = guild.id().asLong();
            for (ApplicationCommandData data : client.getApplicationService().getGuildApplicationCommands(applicationId, guildId)
                    .toStream().filter(data -> data.name().equalsIgnoreCase(commandName)).toList()) {
                System.out.println("removed " + guildId + " " + data.id());
                client.getApplicationService().deleteGuildApplicationCommand(applicationId, guildId, Long.parseLong(data.id())).subscribe();
            }
        });*/

        if (client.getApplicationService().getGlobalApplicationCommands(applicationId).toStream().anyMatch(data ->
            data.name().equalsIgnoreCase(commandName))) {
            client.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, List.of(request)).subscribe();
            return;
        }

        client.getApplicationService().createGlobalApplicationCommand(applicationId, request).block();
    }

    public abstract Mono<?> response(ChatInputInteractionEvent event);
}
