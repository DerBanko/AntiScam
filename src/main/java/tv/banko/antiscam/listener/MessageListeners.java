package tv.banko.antiscam.listener;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.util.Optional;

public class MessageListeners extends DefaultListener {

    public MessageListeners(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        super(antiScam, client, gateway);

        gateway.getEventDispatcher().on(MessageCreateEvent.class, event -> {
            try {
                if (event.getMessage().getContent().equalsIgnoreCase("")) {
                    return Mono.empty();
                }

                Optional<Snowflake> guildId = event.getGuildId();

                if (guildId.isEmpty()) {
                    return Mono.empty();
                }

                if (!antiScam.isScam(event.getMessage().getContent(), guildId.get())) {
                    antiScam.getViolation().createDetector(event.getMessage()).check();
                    return Mono.empty();
                }

                antiScam.punish(event.getMessage());
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();

        gateway.getEventDispatcher().on(MessageUpdateEvent.class, event -> {
            try {
                if (!event.isContentChanged()) {
                    return Mono.empty();
                }

                event.getMessage().subscribe(message -> {

                    if (message.getContent().equalsIgnoreCase("")) {
                        return;
                    }

                    Optional<Snowflake> guildId = event.getGuildId();

                    if (guildId.isEmpty()) {
                        return;
                    }

                    if (!antiScam.isScam(message.getContent(), guildId.get())) {
                        antiScam.getViolation().createDetector(message).check();
                        return;
                    }

                    antiScam.punish(message);
                });
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();
    }
}
