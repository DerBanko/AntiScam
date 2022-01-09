package tv.banko.antiscam.manage;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import tv.banko.antiscam.AntiScam;

public class MessageManager {

    private final AntiScam antiScam;

    public MessageManager(AntiScam antiScam) {
        this.antiScam = antiScam;

        // if the bot gets verified lol
        // sendUpdateLogInLogs();
    }

    public void sendUpdateLogInLogs() {
        antiScam.getMongoDB().getLogCollection().sendMessages(EmbedCreateSpec.builder()
                .title(":newspaper: | Verified")
                .description("""
                        Hey!
                        
                        Thank you for using the **AntiScam** Bot.
                        We reached **100 Servers** and got **verified today**, so thank you.
                        
                        Because some people requested it: use `/antiscam list` to list all phrases.
                        
                        Please consider joining the bot creators discord server **<https://discord.gg/banko>**.
                        
                        Have a great day,
                        **Banko**""")
                .build(), true);
    }

    public boolean sendSetupMessage(GuildMessageChannel channel) {
        try {
            if (!channel.getEffectivePermissions(antiScam.getGateway().getSelfId()).blockOptional()
                    .orElse(PermissionSet.none()).contains(Permission.SEND_MESSAGES)) {
                return false;
            }

            channel.createMessage(EmbedCreateSpec.builder()
                    .title(":newspaper: | Setup")
                    .description("""
                            Welcome to **AntiScam**. Thank you for using our system!\s
                                                    
                            Whats **AntiScam**? AntiScam is a bot which deletes messages containing **scam urls**.\s
                                                    
                            Modify the bot using `/antiscam log <Channel>` and `/antiscam punishment <Type>`.\s
                                                    
                            But it only works with **your** help! Let us know if there is any **new url** by submitting it using `/antiscam add <URL>`.\s
                            (To get the url in the global ban list the bot owner <@252497015655759872> approves them manually)\s
                             
                            Thanks for using **AntiScam** and consider joining my discord server <https://discord.gg/banko>\s
                            If you have any **improvement tips** check out the <#928440142841151529> on my discord server!\s
                                                    
                            Have a great day,
                            **Banko**""")
                    .build()).block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
