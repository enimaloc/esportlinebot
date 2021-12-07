package fr.enimaloc.esportlinebot.listener;

import fr.enimaloc.esportlinebot.ESportLineBot;
import fr.enimaloc.esportlinebot.utils.PollUtils;
import java.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

public class PollListener extends ListenerAdapter {

    private final ESportLineBot eSportLineBot;

    public PollListener(ESportLineBot eSportLineBot) {
        this.eSportLineBot = eSportLineBot;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        TextChannel textChannel = event.getJDA()
                                       .getTextChannelById(ESportLineBot.VOTE_CHANNEL);
        if (textChannel != null) {
            List<Long> reacted = new ArrayList<>();
            List<Long> sent    = new ArrayList<>();
            List<Long> overrides = new ArrayList<>();

            MessageHistory history = textChannel.getHistoryFromBeginning(99)
                                                .complete();
            for (Message message : history.getRetrievedHistory()) {
                if (Arrays.stream(ESportLineBot.IGNORE_MESSAGES)
                          .anyMatch(id -> id == message.getIdLong())) {
                    continue;
                }
                if (sent.contains(message.getAuthor()
                                         .getIdLong())) {
                    message.delete()
                           .complete();
                    continue;
                } else {
                    sent.add(message.getAuthor()
                                    .getIdLong());
                }
                for (MessageReaction reaction : message.getReactions()) {
                    if (PollUtils.isGoodEmote(reaction)) {
                        reaction.retrieveUsers()
                                .stream()
                                .filter(user -> !user.isBot())
                                .forEach(user -> {
                                    if (reacted.contains(user.getIdLong())) {
                                        reaction.removeReaction(user)
                                                .queue();
                                    } else {
                                        reacted.add(user.getIdLong());
                                    }
                                });
                        if (reaction.retrieveUsers()
                                    .stream()
                                    .noneMatch(User::isBot)) {
                            PollUtils.react(message);
                        }
                    }
                }
            }
            for (PermissionOverride override : textChannel.getMemberPermissionOverrides()) {
                if (!override.isMemberOverride()) {
                    continue;
                }
                if (!sent.contains(override.getIdLong())) {
                    override.delete()
                            .complete();
                    continue;
                }
                overrides.add(override.getIdLong());
            }

            for (Message message : history.getRetrievedHistory()) {
                if (!overrides.contains(message.getAuthor()
                                               .getIdLong())) {
                    PollUtils.receive(message);
                }
            }

            reacted.clear();
            sent.clear();
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                TextChannel textChannel = event.getJDA()
                                               .getTextChannelById(ESportLineBot.VOTE_CHANNEL);
                if (textChannel == null) {
                    return;
                }
                Role everyone = textChannel.getGuild()
                                           .getPublicRole();

                PermissionOverride permissionOverride = textChannel.getPermissionOverride(everyone);
                if (Calendar.getInstance()
                            .get(Calendar.DAY_OF_WEEK) == ESportLineBot.DAY_TO_CLOSE || ESportLineBot.DEBUG) {
                    PollUtils.draw(textChannel,
                         ((Calendar.getInstance()
                                   .get(Calendar.HOUR_OF_DAY) == 23 && Calendar.getInstance()
                                                                               .get(Calendar.MINUTE) == 59 &&
                           Calendar.getInstance()
                                   .get(Calendar.SECOND) >= 55) || ESportLineBot.DEBUG)
                    );
                } else {
                    if (permissionOverride != null && permissionOverride.getDenied()
                                                                        .contains(Permission.MESSAGE_SEND)) {
                        PollUtils.canWrite(textChannel, everyone, true);
                        for (PermissionOverride override : textChannel.getMemberPermissionOverrides()) {
                            override.delete()
                                    .complete();
                        }
                    }
                }
            }
        }, 0, 1000);

        super.onReady(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            PollUtils.receive(event.getMessage());
        }
        super.onMessageReceived(event);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild() || Arrays.stream(ESportLineBot.IGNORE_MESSAGES)
                                          .anyMatch(id -> id == event.getMessageIdLong())) {
            return;
        }
        MessageHistory history = event.getChannel()
                                      .getHistoryFromBeginning(99)
                                      .complete();
        // FIXME: 01/12/2021 Exception when deleting message
        for (PermissionOverride override : event.getGuildChannel()
                                                .getPermissionContainer()
                                                .getPermissionOverrides()) {
            if (override.isMemberOverride() && override.getMember() != null && history.getRetrievedHistory()
                                                                                      .stream()
                                                                                      .noneMatch(msg -> msg.getAuthor()
                                                                                                           .getIdLong() ==
                                                                                                        override.getMember()
                                                                                                                .getIdLong() &&
                                                                                                        msg.getIdLong() !=
                                                                                                        event.getMessageIdLong()) &&
                !override.getMember()
                         .getUser()
                         .isBot()) {
                PollUtils.canWrite((TextChannel) event.getGuildChannel(), override.getMember(), true);
            }
        }
        super.onMessageDelete(event);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getChannel()
                 .getIdLong() != ESportLineBot.VOTE_CHANNEL || event.getUser() == null || event.getUser()
                                                                                .isBot()) {
            return;
        }
        if (Arrays.stream(ESportLineBot.IGNORE_MESSAGES)
                  .anyMatch(id -> id == event.getMessageIdLong())) {
            return;
        }

        MessageHistory history = event.getChannel()
                                      .getHistoryFromBeginning(99)
                                      .complete();
        for (Message message : history.getRetrievedHistory()) {
            if (message.getIdLong() == event.getMessageIdLong()) {
                continue;
            }
            for (MessageReaction reaction : message.getReactions()) {
                if (PollUtils.isGoodEmote(reaction) && reaction.retrieveUsers()
                                                               .stream()
                                                               .anyMatch(user -> user.getIdLong() == event.getUser()
                                                                                                .getIdLong())) {
                    reaction.removeReaction(event.getUser())
                            .queue();
                }
            }
        }
        super.onMessageReactionAdd(event);
    }
}
