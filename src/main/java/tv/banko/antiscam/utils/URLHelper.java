package tv.banko.antiscam.utils;

import java.util.Objects;

public class URLHelper {

    public static boolean doesNotContain(String text, String content) {
        Objects.requireNonNull(text, "text is null");
        Objects.requireNonNull(content, "content is null");

        String textLowerCase = text.toLowerCase().replace("\\", "")
                .replace("%20", " ");
        String contentLowerCase = content.toLowerCase().replace("\\", "")
                .replace("%20", " ");

        if (!textLowerCase.contains(contentLowerCase)) {
            return true;
        }

        if (textLowerCase.equals(contentLowerCase)) {
            return false;
        }

        if (textLowerCase.contains("https://" + contentLowerCase + "/") ||
                textLowerCase.contains("https://" + contentLowerCase + " ")) {
            return false;
        }

        if (textLowerCase.contains("http://" + contentLowerCase + "/") ||
                textLowerCase.contains("http://" + contentLowerCase + " ")) {
            return false;
        }

        if (textLowerCase.contains("www." + contentLowerCase + "/") ||
                textLowerCase.contains("www." + contentLowerCase + " ")) {
            return false;
        }

        return !textLowerCase.endsWith("https://" + contentLowerCase) &&
                !textLowerCase.endsWith("http://" + contentLowerCase) &&
                !textLowerCase.endsWith("www." + contentLowerCase);
    }

}
