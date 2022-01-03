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
import tv.banko.antiscam.api.ScamAPI;
import tv.banko.antiscam.command.CommandManager;
import tv.banko.antiscam.database.MongoDB;
import tv.banko.antiscam.listener.GuildEnterListener;
import tv.banko.antiscam.listener.MessageCreateListener;

import java.time.Instant;
import java.util.Optional;

public class AntiScam {

    private final DiscordClient client;
    private final GatewayDiscordClient gateway;
    private final CommandManager command;

    private final MongoDB mongoDB;
    private final ScamAPI scamAPI;

    public AntiScam(String token) {
        this.client = DiscordClient.create(token);
        this.gateway = client.gateway().setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES,
                Intent.GUILD_BANS, Intent.GUILD_INTEGRATIONS, Intent.GUILDS, Intent.GUILD_MEMBERS)).login().block();

        this.scamAPI = new ScamAPI();
        this.mongoDB = new MongoDB(this);
        this.command = new CommandManager(this);

        if (this.gateway == null) {
            System.out.println("null");
            return;
        }

        new MessageCreateListener(this, this.client, this.gateway);
        new GuildEnterListener(this, this.client, this.gateway);

        gateway.onDisconnect().block();
    }

    public DiscordClient getClient() {
        return client;
    }

    public GatewayDiscordClient getGateway() {
        return gateway;
    }

    public boolean isScam(String message) {
        return scamAPI.containsScam(message);
    }

    public void punish(Message message) {
        mongoDB.getSettingsCollection().punish(message);
    }

    public void sendMessage(Message message) {
        Optional<Snowflake> snowflake = message.getGuildId();

        if(snowflake.isEmpty()) {
            return;
        }

        Optional<Member> optionalMember = message.getAuthorAsMember().blockOptional();

        if(optionalMember.isEmpty()) {
            return;
        }

        Member member = optionalMember.get();

        Optional<MessageChannel> optionalChannel = message.getChannel().blockOptional();

        if(optionalChannel.isEmpty()) {
            return;
        }

        GuildMessageChannel channel = (GuildMessageChannel) optionalChannel.get();

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

    public CommandManager getCommand() {
        return command;
    }
}
