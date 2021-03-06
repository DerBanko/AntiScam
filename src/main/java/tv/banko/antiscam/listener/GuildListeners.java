package tv.banko.antiscam.listener;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.util.Optional;

public class GuildListeners extends DefaultListener {

    public GuildListeners(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        super(antiScam, client, gateway);

        gateway.getEventDispatcher().on(GuildCreateEvent.class, event -> {
            try {
                antiScam.getMonitor().sendGuildJoin(event.getGuild());

                event.getGuild().getSystemChannel().subscribe(textChannel ->
                    antiScam.getMessage().sendSetupMessage(textChannel));
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();

        gateway.getEventDispatcher().on(GuildDeleteEvent.class, event -> {
            try {
                Optional<Guild> guild = event.getGuild();

                if (guild.isEmpty()) {
                    return Mono.empty();
                }

                antiScam.getMonitor().sendGuildLeave(guild.get());
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();
    }
}
