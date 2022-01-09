package tv.banko.antiscam;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import tv.banko.antiscam.api.DiscordAPI;
import tv.banko.antiscam.api.ScamAPI;
import tv.banko.antiscam.command.CommandManager;
import tv.banko.antiscam.database.MongoDB;
import tv.banko.antiscam.listener.GuildListeners;
import tv.banko.antiscam.listener.MessageListeners;
import tv.banko.antiscam.listener.MonitorListeners;
import tv.banko.antiscam.manage.MessageManager;
import tv.banko.antiscam.manage.Monitor;
import tv.banko.antiscam.manage.Stats;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AntiScam {

    private final DiscordClient client;
    private final GatewayDiscordClient gateway;

    private final MongoDB mongoDB;
    private final ScamAPI scamAPI;
    private final DiscordAPI discordAPI;
    private final MessageManager message;

    private final Monitor monitor;
    private final Stats stats;

    public AntiScam(String token) {
        this.client = DiscordClient.create(token);
        this.gateway = client.gateway().setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES,
                Intent.GUILD_INTEGRATIONS, Intent.GUILDS)).login().block();

        this.scamAPI = new ScamAPI(this);
        this.discordAPI = new DiscordAPI(token);
        this.mongoDB = new MongoDB(this);
        new CommandManager(this);
        this.monitor = new Monitor(this);
        this.stats = new Stats(this);

        this.message = new MessageManager(this);

        if (this.gateway == null) {
            System.out.println("null");
            return;
        }

        new MessageListeners(this, this.client, this.gateway);
        new GuildListeners(this, this.client, this.gateway);
        new MonitorListeners(this, this.client, this.gateway);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> this.gateway
                .updatePresence(ClientPresence.online(ClientActivity.listening("Scam URLs | /antiscam | " + this.client
                        .getGuilds().count().block() + " guilds"))).block(), 0, 10, TimeUnit.SECONDS);

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            monitor.sendError(e);
            e.printStackTrace();
        });

        gateway.onDisconnect().block();
    }

    public DiscordClient getClient() {
        return client;
    }

    public GatewayDiscordClient getGateway() {
        return gateway;
    }

    public boolean isScam(String message, Snowflake guildId) {
        return scamAPI.containsScam(message.replace("\\", ""), guildId);
    }

    public void punish(Message message) {
        sendMessage(message);
        mongoDB.getSettingsCollection().punish(message);
    }

    public void sendMessage(Message message) {
        Optional<Snowflake> snowflake = message.getGuildId();

        if (snowflake.isEmpty()) {
            return;
        }

        Optional<Member> optionalMember = message.getAuthorAsMember().onErrorStop().blockOptional();

        if (optionalMember.isEmpty()) {
            return;
        }

        Member member = optionalMember.get();

        Optional<MessageChannel> optionalChannel = message.getChannel().onErrorStop().blockOptional();

        if (optionalChannel.isEmpty()) {
            return;
        }

        GuildMessageChannel channel = (GuildMessageChannel) optionalChannel.get();

        stats.sendScam(message);

        mongoDB.getLogCollection().sendMessage(snowflake.get(), EmbedCreateSpec.builder()
                .title(":no_entry_sign: | Scam found")
                .description("**Sender**: " + member.getMention() + " (" + member.getTag() + ")\n" +
                        "**Channel**: " + channel.getMention() + " (" + channel.getName() + ")\n")
                .addField(EmbedCreateFields.Field.of("Message", message.getContent(), false))
                .addField(EmbedCreateFields.Field.of("Timestamp", "<t:" + Instant.now().getEpochSecond() + ":f>", false))
                .addField(EmbedCreateFields.Field.of("IDs", "```ini" + "\n" +
                        "userId = " + member.getId().asString() + "\n" +
                        "channelId = " + channel.getId().asString() + "\n" +
                        "messageId = " + message.getId().asString() + "\n" +
                        "```", false))
                .build());
    }

    public MongoDB getMongoDB() {
        return mongoDB;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public Stats getStats() {
        return stats;
    }

    public MessageManager getMessage() {
        return message;
    }

    public DiscordAPI getDiscordAPI() {
        return discordAPI;
    }

    public ScamAPI getScamAPI() {
        return scamAPI;
    }
}
