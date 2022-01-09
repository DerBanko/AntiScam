package tv.banko.antiscam.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import discord4j.common.util.Snowflake;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tv.banko.antiscam.AntiScam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ScamAPI {

    private final String url;
    private final AntiScam antiScam;
    private final OkHttpClient client;

    private JsonArray domains;
    private long updateIn;

    public ScamAPI(AntiScam antiScam) {
        this.url = "https://raw.githubusercontent.com/nikolaischunk/discord-phishing-links/main/domain-list.json";
        this.antiScam = antiScam;
        this.client = new OkHttpClient();

        this.updateIn = 0;
        update();
    }

    public boolean containsScam(String message, Snowflake guildId) {
        if (updateIn < System.currentTimeMillis()) {
            update();
        }

        for (JsonElement jsonElement : domains) {
            String link = jsonElement.getAsString();

            if (message.toLowerCase(Locale.ROOT).contains(link.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return antiScam.getMongoDB().getScamCollection().containsScam(message, guildId);
    }

    private void update() {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return;
            }

            domains = JsonParser.parseString(Objects.requireNonNull(response.body()).string())
                    .getAsJsonObject().getAsJsonArray("domains");

            this.updateIn = System.currentTimeMillis() + (1000 * 60 * 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getGithubDomains() {
        List<String> list = new ArrayList<>();

        for (JsonElement domain : domains) {
            list.add(domain.getAsString());
        }

        return list;
    }

    public String getUrl() {
        return url;
    }
}
