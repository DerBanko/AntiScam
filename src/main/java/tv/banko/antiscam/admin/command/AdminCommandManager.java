package tv.banko.antiscam.admin.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.util.ArrayList;
import java.util.List;

public class AdminCommandManager {

    private final AntiScam antiScam;
    private final List<DefaultGuildCommand> commands;

    public AdminCommandManager(AntiScam antiScam, long guildId) {
        this.antiScam = antiScam;
        this.commands = new ArrayList<>();

        registerCommand(guildId);
        registerEvent();
    }

    public void registerCommand(long guildId) {
        addCommand(new AdminCommand(antiScam, guildId));
    }

    public void addCommand(DefaultGuildCommand command) {

        if (commands.contains(command)) {
            return;
        }

        this.commands.add(command);
    }

    private void registerEvent() {
        antiScam.getGateway().on(ChatInputInteractionEvent.class, event -> {
            for (DefaultGuildCommand command : commands) {
                try {
                    Mono<?> mono = command.response(event).onErrorStop();

                    if (mono.equals(Mono.empty())) {
                        continue;
                    }

                    if (mono.equals(Mono.justOrEmpty(true))) {
                        return Mono.empty();
                    }

                    return mono;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return Mono.empty();
        }).onErrorContinue((throwable, o) -> throwable.printStackTrace()).subscribe();
    }

}
