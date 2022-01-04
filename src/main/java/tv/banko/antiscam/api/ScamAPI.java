package tv.banko.antiscam.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Objects;

public class ScamAPI {

    private final OkHttpClient client;
    private SafebrowsingAPI safebrowsing;

    private JsonArray domains;
    private long updateIn;

    public ScamAPI() {
        this.client = new OkHttpClient();

        try {
            this.safebrowsing = new SafebrowsingAPI();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        this.updateIn = 0;
        update();
    }

    public boolean containsScam(String message) {
        if(updateIn < System.currentTimeMillis()) {
            update();
        }

        for (JsonElement jsonElement : domains) {
            String link = jsonElement.getAsString();

            if (message.toLowerCase(Locale.ROOT).contains(link.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return safebrowsing.isScam(message);
    }

    private void update() {
        String URL = "https://raw.githubusercontent.com/nikolaischunk/discord-phishing-links/main/domain-list.json";
        Request request = new Request.Builder()
                .url(URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if(response.body() == null) {
                return;
            }

            domains = JsonParser.parseString(Objects.requireNonNull(response.body()).string())
                    .getAsJsonObject().getAsJsonArray("domains");

            this.updateIn = System.currentTimeMillis() + (1000 * 60 * 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
