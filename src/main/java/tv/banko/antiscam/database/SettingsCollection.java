package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.punishment.PunishmentType;

import java.util.Locale;
import java.util.Optional;

public class SettingsCollection {

    private final AntiScam antiScam;
    private final String collectionName;
    private final MongoDB mongoDB;

    public SettingsCollection(AntiScam antiScam, MongoDB mongoDB) {
        this.antiScam = antiScam;
        this.collectionName = "settings";
        this.mongoDB = mongoDB;
    }

    public void setPunishment(Snowflake guild, PunishmentType type) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guild.asString()))).first();

        if (document == null) {
            collection.insertOne(new Document().append("guildId", guild.asString()).append("punishment", type.toString()));
            return;
        }

        document.append("punishment", type.toString());

        collection.updateOne(Filters.eq("guildId", guild.asString()), new Document("$set", document));
    }

    public void setLocale(Snowflake guildId, Locale locale) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guildId.asString()))).first();

        if (document == null) {
            collection.insertOne(new Document().append("guildId", guildId.asString()).append("locale", locale.toLanguageTag()));
            return;
        }

        document.append("locale", locale.toLanguageTag());

        collection.updateOne(Filters.eq("guildId", guildId.asString()), new Document("$set", document));
    }

    public Locale getLocale(Snowflake guildId) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guildId.asString()))).first();

        if (document == null || !document.containsKey("locale")) {
            antiScam.getGateway().getGuildById(guildId).subscribe(guild ->
                setLocale(guildId, guild.getPreferredLocale()));
            return Locale.US;
        }

        return Locale.forLanguageTag(document.getString("locale"));
    }

    public void punish(Message message) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Optional<Snowflake> optional = message.getGuildId();

        if (optional.isEmpty()) {
            return;
        }

        Document document = collection.find(Filters.eq("guildId", optional.get().asString())).first();
        PunishmentType type;

        if (document == null || !document.containsKey("punishment")) {
            type = PunishmentType.delete();
        } else {
            type = PunishmentType.fromString(document.getString("punishment"));
        }

        type.punish(antiScam, message);
    }

    private String getCollectionName() {
        return collectionName;
    }

}
