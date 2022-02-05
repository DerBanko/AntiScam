package tv.banko.antiscam.violation;

import discord4j.core.object.entity.Message;
import tv.banko.antiscam.AntiScam;

public class ViolationManager {

    private final AntiScam antiScam;

    public ViolationManager(AntiScam antiScam) {
        this.antiScam = antiScam;
    }

    public Violation createDetector(Message message) {
        return new Violation(antiScam, message);
    }
}
