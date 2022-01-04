package tv.banko.antiscam.api;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.safebrowsing.Safebrowsing;
import com.google.api.services.safebrowsing.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SafebrowsingAPI {

    private final JacksonFactory GOOGLE_JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final String API_KEY = System.getenv("GOOGLE_API_KEY");
    private final String CLIENT_KEY = System.getenv("GOOGLE_CLIENT_ID");
    private final String GOOGLE_APPLICATION_NAME = System.getenv("GOOGLE_APPLICATION_NAME");
    private final List<String> THREAT_TYPES = List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE",
            "POTENTIALLY_HARMFUL_APPLICATION");
    private final List<String> PLATFORM_TYPES = List.of("ANY_PLATFORM");
    private final List<String> THREAT_ENTRYTYPES = List.of("THREAT_ENTRY_TYPE_UNSPECIFIED", "URL");

    private final List<String> threats;
    private final NetHttpTransport httpTransport;

    public SafebrowsingAPI() throws GeneralSecurityException, IOException {
        this.threats = new ArrayList<>();

        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    public boolean isScam(String message) {
        try {

            if (message.equalsIgnoreCase("")) {
                return false;
            }

            List<String> list = new ArrayList<>();

            for (String s : message.split(" ")) {
                if (threats.contains(s.toLowerCase())) {
                    return true;
                }

                if (!s.contains(".")) {
                    continue;
                }

                s = s.replace("<", "")
                        .replace(">", "")
                        .replace("..", "");

                if(s.equalsIgnoreCase("")) {
                    continue;
                }

                list.add(s);
            }

            if (list.size() == 0) {
                return false;
            }

            FindThreatMatchesRequest request = create(list);

            if (request.isEmpty()) {
                return false;
            }

            Safebrowsing.Builder safebrowsingBuilder = new Safebrowsing.Builder
                    (httpTransport, GOOGLE_JSON_FACTORY, null)
                    .setApplicationName(GOOGLE_APPLICATION_NAME);

            Safebrowsing safebrowsing = safebrowsingBuilder.build();
            FindThreatMatchesResponse findThreatMatchesResponse = safebrowsing
                    .threatMatches()
                    .find(request)
                    .setKey(API_KEY)
                    .execute();

            List<ThreatMatch> threatMatches = findThreatMatchesResponse.getMatches();

            if (threatMatches != null && threatMatches.size() > 0) {
                for (ThreatMatch threatMatch : threatMatches) {
                    String url = threatMatch.getThreat().getUrl().toLowerCase();

                    if (threats.contains(url)) {
                        continue;
                    }

                    threats.add(url);
                }
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private FindThreatMatchesRequest create(List<String> urls) {
        FindThreatMatchesRequest request = new FindThreatMatchesRequest();

        ClientInfo clientInfo = new ClientInfo()
                .setClientId(CLIENT_KEY)
                .setClientVersion("0.0.1");

        List<ThreatEntry> list = new ArrayList<>();

        for (String url : urls) {
            ThreatEntry threatEntry = new ThreatEntry();
            threatEntry.set("url", url);
            list.add(threatEntry);
        }

        ThreatInfo threatInfo = new ThreatInfo()
                .setThreatTypes(THREAT_TYPES)
                .setPlatformTypes(PLATFORM_TYPES)
                .setThreatEntryTypes(THREAT_ENTRYTYPES)
                .setThreatEntries(list);

        request.setClient(clientInfo);
        request.setThreatInfo(threatInfo);
        return request;
    }
}
