package tv.banko.antiscam.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import tv.banko.antiscam.AntiScam;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoDB {

    private MongoClient mongoClient;
    private MongoDatabase database;

    private LogCollection logCollection;
    private SettingsCollection settingsCollection;
    private ScamCollection scamCollection;
    private ViolationCollection violationCollection;

    public MongoDB(AntiScam antiScam) {
        Logger logger = Logger.getLogger("org.mongodb.driver");
        logger.setLevel(Level.SEVERE);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://mongodb:27017"))
                .retryWrites(true)
                .retryReads(true)
                .build();

        mongoClient = MongoClients.create(settings);
        database = getMongoClient().getDatabase("antiscam");

        logCollection = new LogCollection(antiScam, this);
        settingsCollection = new SettingsCollection(antiScam, this);
        scamCollection = new ScamCollection(antiScam, this);
        violationCollection = new ViolationCollection(antiScam, this);

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            close();

            mongoClient = MongoClients.create(settings);
            database = getMongoClient().getDatabase("antiscam");

            logCollection = new LogCollection(antiScam, MongoDB.this);
            settingsCollection = new SettingsCollection(antiScam, this);
            scamCollection = new ScamCollection(antiScam, this);
            violationCollection = new ViolationCollection(antiScam, this);

        }, 30, 30, TimeUnit.MINUTES);
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public LogCollection getLogCollection() {
        return logCollection;
    }

    public SettingsCollection getSettingsCollection() {
        return settingsCollection;
    }

    public ScamCollection getScamCollection() {
        return scamCollection;
    }

    public ViolationCollection getViolationCollection() {
        return violationCollection;
    }

    public void close() {
        mongoClient.close();
    }
}
