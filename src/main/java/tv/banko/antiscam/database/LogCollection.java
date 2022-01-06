package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LogCollection {

    private final AntiScam antiScam;

    private final String collectionName;
    private final MongoDB mongoDB;

    public LogCollection(AntiScam antiScam, MongoDB mongoDB) {
        this.antiScam = antiScam;

        this.collectionName = "log";
        this.mongoDB = mongoDB;
    }

    public void setChannel(Snowflake guild, Snowflake channel) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = new Document().append("guildId", guild.asString()).append("channelId", channel.asString());

        if (collection.find(Filters.eq("guildId", guild.asString())).first() == null) {
            collection.insertOne(document);
            return;
        }

        collection.updateOne(Filters.eq("guildId", guild.asString()),
                new Document("$set", document));
    }

    public void removeChannel(Snowflake guild) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (collection.find(Filters.eq("guildId", guild.asString())).first() == null) {
            return;
        }

        collection.deleteMany(Filters.eq("guildId", guild.asString()));
    }

    public void sendMessage(Snowflake guild, EmbedCreateSpec spec) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.eq("guildId", guild.asString())).first();

        if (document == null) {
            return;
        }

        Optional<Channel> optional = antiScam.getGateway().getChannelById(Snowflake.of(
                document.getString("channelId"))).blockOptional();

        if (optional.isEmpty()) {
            removeChannel(guild);
            return;
        }

        GuildMessageChannel channel = (GuildMessageChannel) optional.get();

        channel.createMessage(spec).onErrorStop().block();
    }

    public void sendMessages(EmbedCreateSpec spec, boolean pingHere) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        collection.find().forEach(document -> {
            try {
                if (document == null) {
                    return;
                }

                Optional<Channel> optional = antiScam.getGateway().getChannelById(Snowflake.of(
                        document.getString("channelId"))).blockOptional();

                if (optional.isEmpty()) {
                    return;
                }

                GuildMessageChannel channel = (GuildMessageChannel) optional.get();

                try {
                    List<Embed> list = Objects.requireNonNull(channel.getLastMessage().block()).getEmbeds();

                    if (list.get(0).getTitle().isPresent() && list.get(0).getTitle().get().equals(spec.title().get())){
                        return;
                    }

                } catch (Exception ignored) { }

                channel.createMessage(spec).withContent(pingHere ? "@here" : "").onErrorStop().block();
            } catch (Exception ignored) {
            }
        });
    }

    private String getCollectionName() {
        return collectionName;
    }

}
