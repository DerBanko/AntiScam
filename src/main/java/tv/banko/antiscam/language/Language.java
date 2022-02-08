package tv.banko.antiscam.language;

import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import tv.banko.antiscam.AntiScam;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Language {

    private final AntiScam antiScam;
    private final EmbedTemplate embed;

    public Language(AntiScam antiScam) {
        this.antiScam = antiScam;
        this.embed = new EmbedTemplate(this);
    }

    public String get(String key) {

        ResourceBundle bundle = getDefaultResourceBundle();

        if (!bundle.containsKey(key)) {
            return key;
        }

        return bundle.getString(key);
    }

    public String get(String key, Snowflake guildId) {

        ResourceBundle bundle = getResourceBundle(guildId);

        if (!bundle.containsKey(key)) {
            bundle = getDefaultResourceBundle();

            if (!bundle.containsKey(key)) {
                return key;
            }
        }

        return replaceTextFormatter(bundle.getString(key));
    }

    public EmbedCreateSpec getEmbed(String key, Snowflake guildId) {
        return embed.get(key, guildId);
    }

    private ResourceBundle getResourceBundle(Snowflake guildId) {
        if (guildId == null) {
            return getDefaultResourceBundle();
        }

        Locale locale = antiScam.getMongoDB().getSettingsCollection().getLocale(guildId);

        try {
            return ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException e) {
            return getDefaultResourceBundle();
        }
    }

    private ResourceBundle getDefaultResourceBundle() {
        return ResourceBundle.getBundle("messages", Locale.US);
    }

    private String replaceTextFormatter(String value) {
        return value.replace("<s>", "~~")
            .replace("</s>", "~~")
            .replace("<u>", "__")
            .replace("</u>", "__")
            .replace("<b>", "**")
            .replace("</b>", "**")
            .replace("<i>", "*")
            .replace("</i>", "*");
    }

}
