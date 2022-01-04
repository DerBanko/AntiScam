package tv.banko.antiscam.api;

import discord4j.core.object.entity.Member;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class DiscordAPI {

    private final String token;
    private final OkHttpClient client;

    public DiscordAPI(String token) {
        this.token = token;
        this.client = new OkHttpClient();
    }

    public boolean timeoutMember(Member member, long timestamp) {
        String url = "https://discord.com/api/v9/guilds/" + member.getGuildId().asString() + "/members/" + member.getId().asString();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token)
                .addHeader("User-Agent", "Mozilla/5.0")
                .patch(RequestBody.create(("{\"communication_disabled_until\":\"" +
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochMilli(timestamp)) + "\"}").getBytes(),
                        MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }
}
