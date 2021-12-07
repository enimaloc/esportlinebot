package fr.enimaloc.esportlinebot.utils;

import fr.enimaloc.enutils.classes.NumberUtils;
import fr.enimaloc.esportlinebot.ESportLineBot;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class PollUtils {
    public static void canWrite(TextChannel channel, IPermissionHolder holder, boolean can) {
        if (!can) {
            channel.getManager()
                   .putPermissionOverride(holder, 0, Permission.MESSAGE_SEND.getRawValue())
                   .complete();
        } else {
            channel.getManager()
                   .removePermissionOverride(holder)
                   .complete();
        }
    }

    public static int getVote(Message message) {
        return message.getReactions()
                      .stream()
                      .filter(PollUtils::isGoodEmote)
                      .findFirst()
                      .map(messageReaction -> messageReaction.getCount() - 1)
                      .orElse(0);
    }

    public static boolean isGoodEmote(MessageReaction reaction) {
        return isLong(ESportLineBot.EMOTE) ? (reaction.getReactionEmote()
                                                .isEmote() && reaction.getReactionEmote()
                                                              .getEmote()
                                                              .getIdLong() == getLong(ESportLineBot.EMOTE)) : (
                reaction.getReactionEmote()
                        .isEmoji() && reaction.getReactionEmote()
                                              .getEmoji()
                                              .equals(ESportLineBot.EMOTE));
    }

    public static boolean checkPattern(String content) {
        return Arrays.stream(ESportLineBot.LINKS)
                     .anyMatch(pattern -> pattern.matcher(content)
                                                 .matches());
    }

    public static String getLink(String content) {
        for (Pattern pattern : ESportLineBot.LINKS) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.matches()) {
                return matcher.group("link");
            }
        }
        return "";
    }

    public static long getLong(String value) {
        return NumberUtils.getSafe(value, Long.class)
                          .orElseGet(() -> NumberUtils.getSafe(value.split(":")[value.split(":").length - 1],
                                                               Long.class
                                                      )
                                                      .orElse(Long.MAX_VALUE));
    }

    public static boolean isLong(String value) {
        return getLong(value) != Long.MAX_VALUE;
    }

    public static void receive(Message message) {
        if (message.getChannel()
                   .getIdLong() != ESportLineBot.VOTE_CHANNEL || message.getMember() == null) {
            return;
        }
        if (Arrays.stream(ESportLineBot.IGNORE_MESSAGES)
                  .anyMatch(id -> id == message.getIdLong())) {
            return;
        }
        if (!checkPattern(message.getContentRaw())) {
            message.delete()
                   .complete();
            return;
        }
        canWrite(message.getTextChannel(), message.getMember(), false);
        react(message);
    }

    public static void react(Message message) {
        Emote emoteById;
        // FIXME: 19/09/2021 Buggy part, use getAsMention instead in var
        if (NumberUtils.getSafe(ESportLineBot.EMOTE, Long.class)
                       .isPresent() && (emoteById = message.getGuild()
                                                           .getEmoteById(getLong(ESportLineBot.EMOTE))) != null) {
            message.addReaction(emoteById)
                   .queue();
        } else {
            message.addReaction(ESportLineBot.EMOTE)
                   .queue();
        }
    }

    public static boolean ended = false;
    public static void draw(TextChannel textChannel, boolean ok) {
        Role everyone = textChannel.getGuild()
                                   .getPublicRole();
        PermissionOverride permissionOverride = textChannel.getPermissionOverride(everyone);
        if (permissionOverride == null || !permissionOverride.getDenied()
                                                             .contains(Permission.MESSAGE_SEND)) {
            PollUtils.canWrite(textChannel, everyone, false);
        }
        if (ok && !ended) {
            ended = true;

            List<Message> history = new ArrayList<>(textChannel.getHistoryFromBeginning(99)
                                                               .complete()
                                                               .getRetrievedHistory());
            history.sort(Comparator.comparingInt(a -> PollUtils.getVote((Message) a))
                                   .reversed());
            EmbedBuilder builder = new EmbedBuilder().setTitle("Résultat vote");
            int          i       = 0;
            for (Message message : history) {
                if (Arrays.stream(ESportLineBot.IGNORE_MESSAGES)
                          .anyMatch(id -> id == message.getIdLong())) {
                    continue;
                }
                if (i < 5 && PollUtils.checkPattern(message.getContentRaw())) {
                    i++;
                    builder.appendDescription("[Clip par %s](%s), %s votes\n".formatted(message.getAuthor()
                                                                                               .getAsTag(),
                                                                                        PollUtils.getLink(message.getContentRaw()),
                                                                                        PollUtils.getVote(message)
                    ));
                }
                message.delete()
                       .complete();
            }
            MessageAction messageAction = Objects.requireNonNull(textChannel.getJDA()
                                                                            .getTextChannelById(ESportLineBot.DEST_CHANNEL))
                                                 .sendMessageEmbeds(builder.build());

            messageAction.complete();
        }
    }
}
