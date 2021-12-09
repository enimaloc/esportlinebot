package fr.enimaloc.esportlinebot.listener.command;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Arrays;

public class CommandListener implements com.jagrosh.jdautilities.command.CommandListener {
    @Override
    public void onSlashCommand(
            SlashCommandEvent event, SlashCommand command
    ) {
        if (event.getMember() != null) {
            if (Arrays.stream(command.getDisabledRoles())
                      .map(Long::parseLong)
                      .anyMatch(id -> event.getMember()
                                           .getRoles()
                                           .stream()
                                           .map(Role::getIdLong)
                                           .toList()
                                           .contains(id)) || Arrays.stream(command.getDisabledUsers())
                                                                   .map(Long::parseLong)
                                                                   .toList()
                                                                   .contains(event.getUser()
                                                                                  .getIdLong())) {
                return;
            }
            if ((command.getEnabledRoles().length != 0 && Arrays.stream(command.getEnabledRoles())
                                                                .map(Long::parseLong)
                                                                .noneMatch(id -> event.getMember()
                                                                                      .getRoles()
                                                                                      .stream()
                                                                                      .map(Role::getIdLong)
                                                                                      .toList()
                                                                                      .contains(id))) &&
                    (command.getEnabledUsers().length != 0) && !Arrays.stream(command.getEnabledUsers())
                                                                      .map(Long::parseLong)
                                                                      .toList()
                                                                      .contains(event.getUser()
                                                                                     .getIdLong())) {
                return;
            }
        }
        com.jagrosh.jdautilities.command.CommandListener.super.onSlashCommand(event, command);
    }
}
