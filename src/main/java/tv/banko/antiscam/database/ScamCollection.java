package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;

import java.util.ArrayList;
import java.util.List;

public class ScamCollection {

    private final String BOT_OWNER_ID = System.getenv("BOT_OWNER_ID");
    private final AntiScam antiScam;

    private final String collectionName;
    private final MongoDB mongoDB;

    private final List<String> list;

    public ScamCollection(AntiScam antiScam, MongoDB mongoDB) {
        this.antiScam = antiScam;

        this.collectionName = "scam";
        this.mongoDB = mongoDB;

        this.list = getApprovedPhrases();
    }

    public void addPhrase(String phrase, Snowflake userId, Snowflake guildId) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (isRegisteredPhrase(phrase)) {
            return;
        }

        Document document = new Document()
                .append("phrase", phrase.toLowerCase())
                .append("guildId", guildId.asString())
                .append("userId", userId.asString())
                .append("approved", userId.asString().equals(BOT_OWNER_ID));

        antiScam.getStats().sendNewPhrase(phrase, userId, guildId);
        collection.insertOne(document);
    }

    public void approvePhrase(String phrase) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (!isRegisteredPhrase(phrase) || containsScam(phrase)) {
            return;
        }

        list.add(phrase.toLowerCase());

        collection.updateOne(Filters.eq("phrase", phrase.toLowerCase()),
                new Document("$set", new Document().append("approved", true)));
    }

    public boolean containsScam(String phrase, Snowflake guildId) {
        for (String s : list) {
            if (phrase.contains(s)) {
                return true;
            }
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        for (Document document : collection.find(Filters.eq("guildId", guildId.asString()))) {

            if(document == null) {
                continue;
            }

            if(phrase.toLowerCase().contains(document.getString("phrase"))) {
                return true;
            }
        }

        return false;
    }

    private boolean containsScam(String phrase) {
        for (String s : list) {
            if (phrase.contains(s)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getApprovedPhrases() {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        List<String> list = new ArrayList<>();

        for (Document document : collection.find(Filters.eq("approved", true))) {
            if (document == null) {
                continue;
            }

            list.add(document.getString("phrase").toLowerCase());
        }

        return list;
    }

    public List<String> getGuildNonApprovedPhrases(Snowflake guildId) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        List<String> list = new ArrayList<>();

        for (Document document : collection.find(Filters.and(Filters.eq("approved", false),
                Filters.eq("guildId", guildId.asString())))) {
            if (document == null) {
                continue;
            }

            list.add(document.getString("phrase").toLowerCase());
        }

        return list;
    }

    /**
     * This phrase could be unapproved!
     */
    public boolean isRegisteredPhrase(String phrase) {

        if (list.contains(phrase)) {
            return true;
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());
        return collection.find(Filters.eq("phrase", phrase.toLowerCase())).first() != null;
    }

    /**
     * This phrase could be unapproved!
     */
    public boolean isApprovedPhrase(String phrase) {
        return list.contains(phrase);
    }

    /**
     * This phrase could be unapproved!
     */
    public boolean isRegisteredByGuild(String phrase, Snowflake guildId) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());
        return collection.find(Filters.and(Filters.eq("phrase", phrase.toLowerCase()),
                Filters.eq("guildId", guildId.asString()))).first() != null;
    }

    public void removePhrase(String phrase) {
        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        list.remove(phrase.toLowerCase());
        collection.deleteOne(Filters.eq("phrase", phrase.toLowerCase()));
    }

    private String getCollectionName() {
        return collectionName;
    }

}
