package fr.enimaloc.esportline.commands;

import fr.enimaloc.enutils.jda.commands.GlobalSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.On;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.api.irc.IRCCapabilities;
import fr.enimaloc.esportline.api.irc.IRCChannel;
import fr.enimaloc.esportline.api.irc.IRCListener;
import fr.enimaloc.esportline.api.irc.IRCUser;
import fr.enimaloc.esportline.api.irc.listener.LoggedIOIrc;
import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCChannel;
import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCClient;
import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;

import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

public class TwitchBridge {
    private final JDA jda;
    private final Connection connection;
    private final TwitchIRCClient ircClient;
    private final Map<IRCChannel, Long> channels = new HashMap<>();

    public TwitchBridge(JDA jda, Connection connection) throws IOException {
        this.jda = jda;
        this.connection = connection;
        this.ircClient = new TwitchIRCClient(new DiscordListener(), new LoggedIOIrc());
        this.ircClient.connect(System.getenv("TWITCH_TOKEN"), System.getenv("TWITCH_USERNAME"), IRCCapabilities.TWITCH_COMMANDS, IRCCapabilities.TWITCH_MEMBERSHIP, IRCCapabilities.TWITCH_TAGS);
    }

    @On
    public void onActivityChange(UserActivityStartEvent event) {
        if (event.getUser().equals(jda.getSelfUser())
                || event.getNewActivity().getType() != Activity.ActivityType.STREAMING
                || event.getNewActivity().getUrl() == null
                || event.getMember().getVoiceState() == null
                || !event.getMember().getVoiceState().inAudioChannel()
                || event.getMember().getVoiceState().getChannel() == null
        ) {
            return;
        }
        String channelName = event.getNewActivity().getUrl().substring(event.getNewActivity().getUrl().lastIndexOf('/') + 1);
        TwitchIRCChannel channel = ircClient.join("#" + channelName).join();
        ircClient.join("#" + ircClient.getSelfUser()).join().sendMessage("Connected to " + channelName + " !");
        channels.put(channel, event.getMember().getVoiceState().getChannel().getIdLong());
    }

    @Slash
    public void twitchBridge(GlobalSlashCommandEvent event, @Slash.Option String channelName, @Slash.Option Optional<TextChannel> channel) {
        event.deferReply().queue();
        channelName = channelName.contains("/") ? channelName.substring(channelName.lastIndexOf('/') + 1) : channelName;
        String finalChannelName = channelName;
        ircClient.join("#" + channelName).whenComplete((ircChannel, throwable) -> {
            if (throwable != null) {
                event.getHook().sendMessage("Failed to connect to " + finalChannelName + " !").queue();
                return;
            }
            ircClient.join("#" + ircClient.getSelfUser().getNickname()).thenAccept(irc -> irc.sendMessage("Connected to " + finalChannelName + " !"));
            channels.put(ircChannel, channel.map(TextChannel::getIdLong).orElse(event.getChannel().getIdLong()));
            event.getHook().sendMessage("Connected to " + finalChannelName + " !").queue();
        });
    }

    private class DiscordListener implements IRCListener<TwitchIRCMessage> {
        private Map<String, Events> events = new HashMap<>();

        public DiscordListener() {
            Executors.newSingleThreadScheduledExecutor()
                    .schedule(() -> {
                        for (Map.Entry<String, Events> entry : events.entrySet()) {
                            List<IRCUser> joins = new ArrayList<>();
                            entry.getValue().joins.drainTo(joins);
                            List<IRCUser> parts = new ArrayList<>();
                            entry.getValue().parts.drainTo(parts);
                            if (!joins.isEmpty()) {
                                jda.getTextChannelById(channels.get(entry.getKey()))
                                        .sendMessage("Joined: " + joins.stream().map(IRCUser::getNickname).reduce((s, s2) -> s + ", " + s2).orElse("")).queue();
                            }
                            if (!parts.isEmpty()) {
                                jda.getTextChannelById(channels.get(entry.getKey()))
                                        .sendMessage("Left: " + parts.stream().map(IRCUser::getNickname).reduce((s, s2) -> s + ", " + s2).orElse("")).queue();
                            }
                        }
                    }, 1, java.util.concurrent.TimeUnit.MINUTES);
        }

        @Override
        public void receivedMessage(TwitchIRCMessage message) {
            if (channels.containsKey(message.getChannel())) {
                if (message.getCommand().equals("PRIVMSG")) {
                    jda.getTextChannelById(channels.get(message.getChannel()))
                            .sendMessage(message.getSender().getNickname() + ": " + message.getTrailing())
                            .queue();
                } else if (message.getCommand().equals("JOIN")) {
                    events.computeIfAbsent(message.getChannel().getName(), s -> new Events(new SynchronousQueue<>(), new SynchronousQueue<>()))
                            .joins.offer(message.getSender());
                } else if (message.getCommand().equals("PART")) {
                    events.computeIfAbsent(message.getChannel().getName(), s -> new Events(new SynchronousQueue<>(), new SynchronousQueue<>()))
                            .parts.offer(message.getSender());
                }
            }
        }

        private record Events(SynchronousQueue<IRCUser> joins, SynchronousQueue<IRCUser> parts) {
        }
    }
}
