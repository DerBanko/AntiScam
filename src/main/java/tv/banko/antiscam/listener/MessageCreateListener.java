package tv.banko.antiscam.listener;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

public class MessageCreateListener extends DefaultListener {

    public MessageCreateListener(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        super(antiScam, client, gateway);

        gateway.getEventDispatcher().on(MessageCreateEvent.class, event -> {
            try {
                if (!antiScam.isScam(event.getMessage().getContent())) {
                    return Mono.empty();
                }

                antiScam.punish(event.getMessage());
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();
    }
}
