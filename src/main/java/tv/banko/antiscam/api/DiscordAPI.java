package tv.banko.antiscam.api;

import discord4j.core.object.entity.Member;
import okhttp3.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DiscordAPI {

    private final String token;
    private final OkHttpClient client;

    private final SimpleDateFormat format;

    public DiscordAPI(String token) {
        this.token = token;
        this.client = new OkHttpClient();

        this.format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        format.setTimeZone(TimeZone.getTimeZone("CET"));
    }

    public boolean timeoutMember(Member member, long timestamp) {
        String url = "https://discord.com/api/v9/guilds/" + member.getGuildId().asString() + "/members/" + member.getId().asString();

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bot " + token)
            .patch(RequestBody.create(("{\"communication_disabled_until\":\"" +
                    format.format(new Date(timestamp)) + "\"}").getBytes(),
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
