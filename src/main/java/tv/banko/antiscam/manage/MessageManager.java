package tv.banko.antiscam.manage;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import tv.banko.antiscam.AntiScam;

public class MessageManager {

    private final AntiScam antiScam;

    public MessageManager(AntiScam antiScam) {
        this.antiScam = antiScam;
    }

    public void sendSetupMessage(GuildMessageChannel channel) {
        channel.getEffectivePermissions(antiScam.getGateway().getSelfId()).subscribe(permissions -> {
            if (!permissions.contains(Permission.SEND_MESSAGES)) {
                return;
            }

            channel.createMessage(EmbedCreateSpec.builder()
                .title(":bookmark_tabs: | " + antiScam.getLanguage().get("help", channel.getGuildId()))
                .description(antiScam.getLanguage().get("help_detailed", channel.getGuildId()))
                .build()).subscribe();
        });


    }

}
