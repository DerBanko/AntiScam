package tv.banko.antiscam.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.util.URLHelper;

import java.util.ArrayList;
import java.util.List;

public class ScamCollection {

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

    public void addPhrase(String phrase, Snowflake guildId, boolean approved) {
        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (isRegisteredPhrase(s)) {
            return;
        }

        Document document = new Document()
            .append("phrase", s.toLowerCase())
            .append("guildId", guildId.asString())
            .append("approved", approved);

        if (!approved) {
            antiScam.getAdmin().getButton().sendApprovePhrase(phrase);
        }

        collection.insertOne(document);
    }

    public void approvePhrase(String phrase) {
        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        if (!isRegisteredPhrase(s) || containsScam(s)) {
            return;
        }

        list.add(s);

        collection.updateOne(Filters.eq("phrase", s),
            new Document("$set", new Document().append("approved", true)));
    }

    public boolean containsScam(String message, Snowflake guildId) {
        if (containsScam(message)) {
            return true;
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        for (Document document : collection.find(Filters.eq("guildId", guildId.asString()))) {

            if (document == null) {
                continue;
            }

            String phrase = document.getString("phrase").toLowerCase();

            if (URLHelper.doesNotContain(message, phrase)) {
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean containsScam(String message) {
        for (String phrase : list) {
            if (URLHelper.doesNotContain(message, phrase)) {
                continue;
            }

            return true;
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

        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        if (list.contains(s)) {
            return true;
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());
        return collection.find(Filters.eq("phrase", s)).first() != null;
    }

    /**
     * This phrase could be unapproved!
     */
    public boolean isApprovedPhrase(String phrase) {

        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        return list.contains(s);
    }

    /**
     * This phrase could be unapproved!
     */
    public boolean isRegisteredByGuild(String phrase, Snowflake guildId) {

        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());
        return collection.find(Filters.and(Filters.eq("phrase", s),
            Filters.eq("guildId", guildId.asString()))).first() != null;
    }

    public void removePhrase(String phrase) {
        String s = phrase.toLowerCase().replace("http://", "")
            .replace("https://", "");

        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }

        MongoCollection<Document> collection = mongoDB.getDatabase().getCollection(getCollectionName());

        list.remove(s.toLowerCase());
        collection.deleteOne(Filters.eq("phrase", s.toLowerCase()));
    }

    private String getCollectionName() {
        return collectionName;
    }

}
