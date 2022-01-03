package tv.banko.antiscam.punishment;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.BanQuerySpec;

public record PunishmentType(String action, int duration) {

    public static PunishmentType kick() {
        return new PunishmentType("KICK", 0);
    }

    public static PunishmentType ban() {
        return new PunishmentType("BAN", 0);
    }

    public static PunishmentType delete() {
        return new PunishmentType("DELETE", 0);
    }

    public static PunishmentType fromString(String s) {
        return new PunishmentType(s.split("#")[0], Integer.parseInt(s.split("#")[1]));
    }

    public void punish(Message message) {
        message.delete().onErrorStop().block();
        Member member = message.getAuthorAsMember().onErrorStop().blockOptional().orElse(null);

        if (member == null) {
            return;
        }

        switch (action) {
            case "KICK": {
                member.kick("message contained scam content").onErrorStop().block();
            }
            case "BAN": {
                member.ban(BanQuerySpec.builder()
                        .reason("message contained scam content")
                        .deleteMessageDays(0)
                        .build()).onErrorStop().block();
            }
        }
    }

    @Override
    public String toString() {
        return action + "#" + duration;
    }
}
