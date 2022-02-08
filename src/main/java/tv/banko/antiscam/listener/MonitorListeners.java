package tv.banko.antiscam.listener;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.util.logging.Logger;

public class MonitorListeners extends DefaultListener {

    public MonitorListeners(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        super(antiScam, client, gateway);

        gateway.getEventDispatcher().on(ReadyEvent.class, event -> {
            try {
                Logger logger = Logger.getLogger("AntiScam");

                gateway.getGuilds().collectList().subscribe(list -> {

                    int count = list.size();
                    int memberCount = 0;

                    for (Guild guild : list) {
                        memberCount += guild.getMemberCount();
                    }

                    logger.finest(" ┌ ");
                    logger.finest(" │ AntiScam is now ready.");
                    logger.finest(" │ ");
                    logger.finest(" │ Guilds: " + count);
                    logger.finest(" │ Members: " + memberCount);
                    logger.finest(" └ ");
                });
                return Mono.empty();
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }
        }).doOnError(Throwable::printStackTrace).subscribe();
    }
}
