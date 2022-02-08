package tv.banko.antiscam.violation;

import discord4j.core.object.entity.Message;
import tv.banko.antiscam.AntiScam;

public record ViolationManager(AntiScam antiScam) {

    public Violation createDetector(Message message) {
        return new Violation(antiScam, message);
    }
}
