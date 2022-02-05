package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.punishment.PunishmentType;
import tv.banko.antiscam.violation.ViolationType;

import java.util.Optional;

public class ViolationCollection {

    private final AntiScam antiScam;
    private final String collectionName;
    private final MongoDB mongoDB;

    public ViolationCollection(AntiScam antiScam, MongoDB mongoDB) {
        this.antiScam = antiScam;
        this.collectionName = "violation";
        this.mongoDB = mongoDB;
    }

    public void setState(Snowflake guild, boolean enabled) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guild.asString()))).first();

        if (document == null) {
            collection.insertOne(new Document().append("guildId", guild.asString()).append("enabled", enabled));
            return;
        }

        document.append("enabled", enabled);

        collection.updateOne(Filters.eq("guildId", guild.asString()), new Document("$set", document));
    }

    public boolean isEnabled(Snowflake guild) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guild.asString()))).first();

        if (document == null) {
            return false;
        }

        return document.getBoolean("enabled", false);
    }

    public void setViolationPunishment(Snowflake guild, ViolationType violation, PunishmentType type) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.and(Filters.eq("guildId", guild.asString()))).first();

        if (document == null) {
            collection.insertOne(new Document().append("guildId", guild.asString()).append(violation.toString(), type.toString()));
            return;
        }

        document.append(violation.toString(), type.toString());

        collection.updateOne(Filters.eq("guildId", guild.asString()), new Document("$set", document));
    }

    public PunishmentType punish(Message message, ViolationType violation) {
        Optional<Snowflake> optional = message.getGuildId();

        if (optional.isEmpty()) {
            return PunishmentType.none();
        }

        if (!isEnabled(optional.get())) {
            return PunishmentType.none();
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        Document document = collection.find(Filters.eq("guildId", optional.get().asString())).first();
        PunishmentType type;

        if (document == null || !document.containsKey(violation.toString())) {
            if (violation.equals(ViolationType.EXTREME)) {
                type = PunishmentType.delete();
            } else {
                type = PunishmentType.none();
            }
        } else {
            type = PunishmentType.fromString(document.getString(violation.toString()));
        }

        type.punish(antiScam, message);

        return type;
    }

    private String getCollectionName() {
        return collectionName;
    }

}
