package fr.enimaloc.esportlinebot.listener;

import fr.enimaloc.enutils.classes.ObjectUtils;
import fr.enimaloc.enutils.tuples.Tuple2;
import fr.enimaloc.enutils.tuples.Tuple3;
import fr.enimaloc.enutils.tuples.Tuples;
import fr.enimaloc.esportlinebot.ESportLineBot;
import java.util.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

public class TempAudioChannelListener extends ListenerAdapter {
    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (event.getChannelJoined()
                 .getIdLong() != ESportLineBot.VOICE_CHANNEL || event.getMember()
                                                                     .getUser()
                                                                     .isBot() || !canCreate(event.getMember())) {
            System.out.println(
                    "Objects.requireNonNull(event.getJDA()\n                                         .getTextChannelById(ESportLineBot.THREADS_CHANNEL))\n                   .getThreadChannels()\n                   .stream()\n                   .map(this::getFirstMessage)\n                   .filter(Optional::isPresent)\n                   .map(Optional::get)\n                   .map(this::parseThreadSystemMessage)\n                   .map(Tuple3::getOptionalC)\n                   .filter(Optional::isPresent)\n                   .map(Optional::get)\n                   .map(User::getIdLong)\n                   .filter(l -> l == event.getMember().getIdLong())\n                   .count() = " +
                    Objects.requireNonNull(event.getJDA()
                                                .getTextChannelById(ESportLineBot.THREADS_CHANNEL))
                           .getThreadChannels()
                           .stream()
                           .map(this::getFirstMessage)
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .map(this::parseThreadSystemMessage)
                           .map(Tuple3::getOptionalC)
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .map(User::getIdLong)
                           .filter(l -> l == event.getMember().getIdLong())
                           .count());
            return;
        }
        TextChannel threadsChannel = event.getJDA()
                                          .getTextChannelById(ESportLineBot.THREADS_CHANNEL);
        Category category;
        if (threadsChannel == null || (category = threadsChannel.getParentCategory()) == null) {
            return;
        }
        String name = ESportLineBot.CHANNEL_NAME_TEMPLATE.formatted(event.getMember()
                                                                         .getEffectiveName());
        VoiceChannel voiceChannel = category.createVoiceChannel(name)
                                            .complete();
        event.getGuild()
             .moveVoiceMember(event.getMember(), voiceChannel)
             .complete();

        ThreadChannel thread = threadsChannel.createThreadChannel(name)
                                             .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                                             .complete();
        thread.sendMessage(voiceChannel.getId() + "\u0001" + name + "\u0001" + event.getMember().getId())
              .flatMap(Message::pin)
              .complete();
        thread.addThreadMember(event.getMember())
              .complete();

        super.onGuildVoiceJoin(event);
    }

    private boolean canCreate(Member member) {
        return member.getJDA()
                     .getTextChannelById(ESportLineBot.THREADS_CHANNEL) != null &&
               Objects.requireNonNull(member.getJDA()
                                            .getTextChannelById(ESportLineBot.THREADS_CHANNEL))
                      .getThreadChannels()
                      .stream()
                      .map(this::getFirstMessage)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .map(this::parseThreadSystemMessage)
                      .map(Tuple3::getOptionalC)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .map(User::getIdLong)
                      .filter(l -> l == member.getIdLong())
                      .count() < ESportLineBot.THREADS_LIMITS;
    }

    // TODO: 01/12/2021 Add restore state when disconnected
    // TODO: 01/12/2021 Command /voice

    List<Long> ignore = new ArrayList<>();

    @Override
    public void onRawGateway(@NotNull RawGatewayEvent event) {
        System.out.println("event.getPackage() = " + event.getPackage());
        if (!event.getType()
                  .equals("THREAD_UPDATE")) {
            return;
        }
        DataObject metadata = event.getPayload()
                                   .getObject("thread_metadata");
        boolean archived = metadata.getBoolean("archived");

        ThreadChannel threadChannel = event.getJDA()
                                           .getThreadChannelById(event.getPayload().getString("id"));
        if (threadChannel == null) {
            return;
        }

        TextChannel parentChannel = threadChannel.getGuild()
                                                 .getTextChannelById(threadChannel.getParentChannel()
                                                                                  .getIdLong());
        Optional<Message> messageOpt = getFirstMessage(threadChannel);
        if (messageOpt.isEmpty()) {
            return;
        }
        Message                            message = messageOpt.get();
        Tuple3<AudioChannel, String, User> tuple   = parseThreadSystemMessage(message);

/*        if (tuple.getOptionalA().isEmpty()) {
            if (!archived) {
                Category category;
                if ((category = parentChannel
                        .getParentCategory()) == null) {
                    return;
                }
                category.createVoiceChannel(threadChannel.getName())
                        .complete();
            }
            return;
        }
*/
        AudioChannel voice = tuple.getOptionalA().orElse(null);
        if (!archived && tuple.getOptionalA().isEmpty()) {
            Category category;
            if ((category = parentChannel.getParentCategory()) == null) {
                return;
            }
            category.createVoiceChannel(threadChannel
                                                .getName())
                    .flatMap(vc -> Objects.requireNonNull(message)
                                          .editMessage(vc.getIdLong() + "\u0001" + tuple.b() + "\u0001" +
                                                       tuple.getOptionalC()
                                                            .map(User::getIdLong)
                                                            .orElse(0L)))
                    .complete();
            ignore.add(threadChannel.getIdLong());
        } else {
            return;
        }
        if (ignore.contains(threadChannel.getIdLong())) {
            ignore.remove(threadChannel.getIdLong());
            return;
        }
        if (!voice.getMembers()
                  .isEmpty()) {
            threadChannel
                    .getManager()
                    .setArchived(false)
                    .complete();
            return;
        }
        voice.delete()
             .complete();
        super.onRawGateway(event);
    }

    public boolean overflow(JDA jda) {
        TextChannel threadsChannel = jda.getTextChannelById(ESportLineBot.THREADS_CHANNEL);
        if (threadsChannel == null) {
            return false;
        }

        Optional<ThreadChannel> threadOpt = threadsChannel.retrieveArchivedPublicThreadChannels()
                                                          .complete()
                                                          .stream()
                                                          .filter(thread -> thread.getTimeArchiveInfoLastModified()
                                                                                  .getMinute() >
                                                                            thread.getAutoArchiveDuration()
                                                                                  .getMinutes() * 2)
                                                          .findFirst();
        if (threadOpt.isPresent()) {
            threadOpt.get().delete().complete();
            return true;
        }

        for (ThreadChannel thread : threadsChannel.getThreadChannels()) {
            Optional<Message> messageOpt = getFirstMessage(thread);
            if (messageOpt.isEmpty()) {
                continue;
            }
            Message                            message = messageOpt.get();
            Tuple3<AudioChannel, String, User> tuple   = parseThreadSystemMessage(message);
            if (tuple.getOptionalA().isPresent() && tuple.getOptionalA().get().getMembers().isEmpty()) {
                thread.delete().complete();
                return true;
            }
        }

        return false;
    }

    public Optional<AudioChannel> getLinkedChannel(ThreadChannel threadChannel) {
        return getLinkedChannelAndMessage(threadChannel).getOptionalA();
    }

    public Tuple2<AudioChannel, Message> getLinkedChannelAndMessage(ThreadChannel threadChannel) {
        Optional<Message> optionalMessage = getFirstMessage(threadChannel);
        if (optionalMessage.isEmpty()) {
            return Tuples.of(null, null);
        }
        Message message = optionalMessage.get();
        return Tuples.of(parseThreadSystemMessage(message).getA(), message);
    }

    public Optional<Message> getFirstMessage(MessageChannel channel) {
        return channel.getHistoryFromBeginning(10)
                      .map(MessageHistory::getRetrievedHistory)
                      .complete()
                      .stream()
                      .filter(Message::isPinned)
                      .filter(msg -> msg.getAuthor()
                                        .isBot())
                      .findFirst();
    }

    public Tuple3<AudioChannel, String, User> parseThreadSystemMessage(Message message) {
        return parseThreadSystemMessage(message.getJDA(), message.getContentRaw());
    }

    public Tuple3<AudioChannel, String, User> parseThreadSystemMessage(JDA jda, String contentRaw) {
        String[] split = contentRaw.split("\u0001");
        return Tuples.of(ObjectUtils.getOr(jda.getVoiceChannelById(split[0]), null),
                         ObjectUtils.getOr(split[1], null),
                         ObjectUtils.getOr(jda.getUserById(split[2]), null)
        );
    }
/*
    @Override
    public void onThreadUpdateArchive(@NotNull ThreadUpdateArchiveEvent event) {
        List<Message> messages = event.getEntity()
                                      .getHistoryFromBeginning(10)
                                      .map(MessageHistory::getRetrievedHistory)
                                      .complete();
        Optional<Message> optionalMessage = messages.stream()
                                                    .filter(Message::isPinned)
                                                    .filter(msg -> msg.getAuthor()
                                                                      .isBot())
                                                    .findFirst();
        if (optionalMessage.isEmpty()) {
            return;
        }
        Message message = optionalMessage.get();

        Matcher matcher;
        if (!(matcher = Pattern.compile("<#(?<id>[0-9]*)>")
                               .matcher(message.getContentRaw())).find()) {
            if (!event.getNewArchiveState()) {
                Category category;
                if ((category = event.getChannel()
                                     .getParent()) == null) {
                    return;
                }
                category.createVoiceChannel(event.getEntity()
                                                 .getName())
                        .complete();
            }
            return;
        }
        long id = Long.parseLong(matcher.group("id"));

        VoiceChannel voice = event.getGuild()
                                  .getVoiceChannelById(id);
        if (voice == null && !event.getNewArchiveState()) {
            Category category;
            if ((category = event.getChannel()
                                 .getParent()) == null) {
                return;
            }
            category.createVoiceChannel(event.getEntity()
                                             .getName())
                    .flatMap(vc -> message.editMessage("Bounded to " + vc.getAsMention()))
                    .complete();
        }
        if (voice == null) {
            return;
        }
        if (!voice.getMembers()
                  .isEmpty()) {
            event.getThread()
                 .getManager()
                 .setArchived(false)
                 .complete();
            return;
        }
        voice.delete()
             .complete();
        super.onThreadUpdateArchive(event);
    }
*/
}
