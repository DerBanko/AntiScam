package tv.banko.antiscam.manage;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import tv.banko.antiscam.AntiScam;

import java.time.Instant;

public class MessageManager {

    private final AntiScam antiScam;

    public MessageManager(AntiScam antiScam) {
        this.antiScam = antiScam;

        sendUpdateLogInLogs();
    }

    public void sendUpdateLogInLogs() {
        antiScam.getMongoDB().getLogCollection().sendMessages(EmbedCreateSpec.builder()
                .title(":newspaper: | Changelog 1.1")
                .description("""
                        First of all: thank you for using **AntiScam**!
                        I want to keep the bot updated and filled up with new links (I hope you too).\s

                        So I added some new features:\s
                         - **Add URLs**: You can use `/antiscam add <URL>` to add urls to the system. As soon as you **add a url** the bot will **delete messages** containing that phrase in **your** guild. **Manually** the bot owner <@252497015655759872> approves the urls to add them on the **global ban list** (this can take up to 24h).\s
                         - **Timeout Users**: The new **timeout system** of discord can now be used in the bot. Just use `/antiscam punishment <Timeout> <Duration>` and put the role of the bot **above** the users that should get timed out and check if the bot has the **permission** to timeout users.\s
                         
                        Thanks for using **AntiScam** and consider joining my discord server <https://discord.gg/banko>\s
                        
                        Have a great day,
                        **Banko**""")
                .build(), true);
    }

}
