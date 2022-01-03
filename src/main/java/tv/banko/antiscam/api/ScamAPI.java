package tv.banko.antiscam.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class ScamAPI {

    private JsonArray domains;
    private long updateIn;

    public ScamAPI() {
        this.updateIn = 0;
        update();
    }

    public boolean containsScam(String content) {
        if(updateIn < System.currentTimeMillis()) {
            update();
        }

        for (JsonElement jsonElement : domains) {
            String link = jsonElement.getAsString();

            if (content.toLowerCase(Locale.ROOT).contains(link.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private void update() {
        try {
            URL url = new URL(" https://raw.githubusercontent.com/nikolaischunk/discord-phishing-links/main/domain-list.json");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.connect();

            int status = con.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;

            StringBuilder builder = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }

            in.close();

            domains = JsonParser.parseString(builder.toString()).getAsJsonObject().getAsJsonArray("domains");

            this.updateIn = System.currentTimeMillis() + (1000 * 60 * 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
