package tv.banko.antiscam.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tv.banko.antiscam.AntiScam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class ScamAPI {

    public boolean containsScam(String content) {
        try {

            URL url = new URL(" https://raw.githubusercontent.com/nikolaischunk/discord-phishing-links/main/domain-list.json");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.connect();

            int status = con.getResponseCode();

            if (status != HttpURLConnection.HTTP_OK) {
                return false;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;

            StringBuilder builder = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine);
            }

            in.close();

            JsonObject jsonObject = JsonParser.parseString(builder.toString()).getAsJsonObject();

            for (JsonElement jsonElement : jsonObject.getAsJsonArray("domains")) {
                String link = jsonElement.getAsString();

                if (content.toLowerCase(Locale.ROOT).contains(link.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
