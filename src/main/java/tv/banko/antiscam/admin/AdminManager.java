package tv.banko.antiscam.admin;

import tv.banko.antiscam.AntiScam;
import tv.banko.antiscam.admin.button.ButtonManager;
import tv.banko.antiscam.admin.command.AdminCommandManager;
import tv.banko.antiscam.admin.listener.ButtonListener;

public class AdminManager {

    private final ButtonManager button;

    public AdminManager(AntiScam antiScam) {
        this.button = new ButtonManager(antiScam);
        new AdminCommandManager(antiScam, 927634292304134205L);
        new ButtonListener(antiScam);
    }

    public ButtonManager getButton() {
        return button;
    }
}
