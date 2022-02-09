package tv.banko.antiscam;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import tv.banko.antiscam.admin.AdminManager;
import tv.banko.antiscam.api.DiscordAPI;
import tv.banko.antiscam.api.ScamAPI;
import tv.banko.antiscam.command.CommandManager;
import tv.banko.antiscam.database.MongoDB;
import tv.banko.antiscam.language.Language;
import tv.banko.antiscam.language.MessageTemplate;
import tv.banko.antiscam.listener.GuildListeners;
import tv.banko.antiscam.listener.MessageListeners;
import tv.banko.antiscam.listener.MonitorListeners;
import tv.banko.antiscam.manage.MessageManager;
import tv.banko.antiscam.manage.Monitor;
import tv.banko.antiscam.manage.Stats;
import tv.banko.antiscam.violation.ViolationManager;

import java.util.Objects;
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
    private final ViolationManager violation;

    private final Language language;
    private final MessageTemplate template;

    private final Monitor monitor;
    private final Stats stats;

    private AdminManager admin;

    public AntiScam(String token) {
        this.language = new Language(this);

        this.client = DiscordClient.create(token);
        this.gateway = client.gateway().setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES,
            Intent.GUILD_INTEGRATIONS, Intent.GUILDS)).login().block();

        this.scamAPI = new ScamAPI(this);
        this.discordAPI = new DiscordAPI(token);
        this.mongoDB = new MongoDB(this);
        new CommandManager(this);
        this.monitor = new Monitor(this);
        this.stats = new Stats(this);
        this.template = new MessageTemplate(this);

        if(!Objects.equals(System.getenv("PHRASE_CHANNEL_ID"), "null")) {
            this.admin = new AdminManager(this);
        }

        this.message = new MessageManager(this);
        this.violation = new ViolationManager(this);

        if (this.gateway == null) {
            System.out.println("null");
            return;
        }

        new MessageListeners(this, this.client, this.gateway);
        new GuildListeners(this, this.client, this.gateway);
        new MonitorListeners(this, this.client, this.gateway);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> this.client
                .getGuilds().count().subscribe(count -> this.gateway.updatePresence(ClientPresence.online(
                    ClientActivity.listening(language.get("presence").replace("%count%", "" + count)))).subscribe()),
            0, 10, TimeUnit.SECONDS);

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

        Snowflake guildId = snowflake.get();

        message.getAuthorAsMember().onErrorStop().subscribe(member ->
            message.getChannel().onErrorStop().cast(GuildMessageChannel.class).subscribe(channel -> {
                stats.sendScam(message);

                mongoDB.getLogCollection().sendMessage(guildId, template.getScamURLDetected(member, channel, message, guildId));
            }));
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

    public MessageTemplate getTemplate() {
        return template;
    }

    public ViolationManager getViolation() {
        return violation;
    }

    public DiscordAPI getDiscordAPI() {
        return discordAPI;
    }

    public ScamAPI getScamAPI() {
        return scamAPI;
    }

    public AdminManager getAdmin() {
        return admin;
    }

    public Language getLanguage() {
        return language;
    }
}
