package tv.banko.antiscam.listener;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

public class GuildEnterListener extends DefaultListener {

    public GuildEnterListener(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        super(antiScam, client, gateway);

        gateway.getEventDispatcher().on(GuildCreateEvent.class, event -> {
            try {
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();
    }
}
