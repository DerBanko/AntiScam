package tv.banko.antiscam.listener;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import tv.banko.antiscam.AntiScam;

public class DefaultListener {

    protected final AntiScam antiScam;
    protected final DiscordClient client;
    protected final GatewayDiscordClient gateway;

    public DefaultListener(AntiScam antiScam, DiscordClient client, GatewayDiscordClient gateway) {
        this.antiScam = antiScam;
        this.client = client;
        this.gateway = gateway;
    }
}
