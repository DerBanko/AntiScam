package tv.banko.antiscam.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;
import tv.banko.antiscam.AntiScam;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final AntiScam antiScam;
    private final List<DefaultCommand> commands;

    public CommandManager(AntiScam antiScam) {
        this.antiScam = antiScam;
        this.commands = new ArrayList<>();

        //antiScam.getGateway().getGuilds().toStream().forEach(this::registerGuild);
        registerCommand();
        registerEvent();
    }

    public void registerCommand() {
        addCommand(new AntiScamCommand(antiScam));
    }

    public void addCommand(DefaultCommand command) {

        if (commands.contains(command)) {
            return;
        }

        this.commands.add(command);
    }

    private void registerEvent() {
        antiScam.getGateway().on(ChatInputInteractionEvent.class, event -> {
            for (DefaultCommand command : commands) {
                try {
                    Mono<?> mono = command.response(event).onErrorStop();

                    if (mono.equals(Mono.empty())) {
                        continue;
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