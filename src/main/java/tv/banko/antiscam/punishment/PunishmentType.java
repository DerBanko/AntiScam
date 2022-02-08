package tv.banko.antiscam.punishment;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.BanQuerySpec;
import tv.banko.antiscam.AntiScam;

public record PunishmentType(String action, int duration) {

    public static PunishmentType none() {
        return new PunishmentType("NONE", 0);
    }

    public static PunishmentType kick() {
        return new PunishmentType("KICK", 0);
    }

    public static PunishmentType ban() {
        return new PunishmentType("BAN", 0);
    }

    public static PunishmentType delete() {
        return new PunishmentType("DELETE", 0);
    }

    public static PunishmentType timeout(int duration) {
        return new PunishmentType("TIMEOUT", duration);
    }

    public static PunishmentType fromString(String s) {
        return new PunishmentType(s.split("#")[0], Integer.parseInt(s.split("#")[1]));
    }

    public void punish(AntiScam antiScam, Message message) {

        if (action.equalsIgnoreCase("NONE")) {
            return;
        }

        message.delete().onErrorStop().subscribe();

        message.getAuthorAsMember().onErrorStop().subscribe(member -> {
            try {
                switch (action) {
                    case "KICK": {
                        member.kick(antiScam.getLanguage().get("punishment_reason", member.getGuildId()))
                            .onErrorStop().subscribe();
                    }
                    case "BAN": {
                        member.ban(BanQuerySpec.builder()
                            .reason(antiScam.getLanguage().get("punishment_reason", member.getGuildId()))
                            .deleteMessageDays(0)
                            .build()).onErrorStop().subscribe();
                    }
                    case "TIMEOUT": {
                        antiScam.getDiscordAPI().timeoutMember(member, System.currentTimeMillis() +
                            (duration * 1000L));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String toString() {
        return action + "#" + duration;
    }

    public String getName() {
        if (duration == 0) {
            return action.toLowerCase();
        }

        return action.toLowerCase() + " " + duration + "s";
    }
}
