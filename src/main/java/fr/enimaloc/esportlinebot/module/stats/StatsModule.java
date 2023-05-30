package fr.enimaloc.esportlinebot.module.stats;

import fr.enimaloc.enutils.jda.annotation.Init;
import fr.enimaloc.enutils.jda.annotation.On;
import fr.enimaloc.esportlinebot.toml.settings.Settings;
import io.prometheus.client.Gauge;
import io.prometheus.client.Info;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.GenericUserEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StatsModule {

    public static final Gauge GUILD_COUNTER = Gauge.build()
            .name("eline_guild")
            .help("Number of members in the guild")
            .labelNames("guild", "status")
            .unit("members")
            .register();

    public static final Gauge CHANNEL_COUNTER = Gauge.build()
            .name("eline_guild")
            .help("Number of channels in the guild")
            .labelNames("guild", "type")
            .unit("channels")
            .register();

    public static final Gauge MESSAGE_COUNTER = Gauge.build()
            .name("eline_counter")
            .help("Number of messages sent")
            .labelNames("guild", "channel", "author")
            .unit("messages")
            .register();

    public static final Gauge VOICE_TIME_COUNTER = Gauge.build()
            .name("eline_voice_time")
            .help("Time spent in voice channel")
            .labelNames("guild", "channel", "author")
            .unit("ms")
            .register();
    private final Map<VoiceTime, Long> voiceTime = new HashMap<>();

    record VoiceTime(String guild, String channel, String user) {}

    private final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1);

    public static final Info NAME_RESOLVER = Info.build()
            .name("eline_name_resolver")
            .help("Name resolver")
            .labelNames("id")
            .register();

    public static final Gauge LEVEL_COUNTER = Gauge.build()
            .name("eline_level_counter")
            .help("Level of the user")
            .labelNames("guild", "author")
            .register();

    public static final Gauge XP_COUNTER = Gauge.build()
            .name("eline_xp_counter")
            .help("Xp of the user")
            .labelNames("guild", "author")
            .register();

    public static final Gauge TOTAL_XP_COUNTER = Gauge.build()
            .name("eline_total_xp_counter")
            .help("Total xp of the user")
            .labelNames("guild", "author")
            .register();


    public StatsModule(Settings.Stats settings) throws IOException {
        if (settings.includeDefaultMetrics) {
            DefaultExports.initialize();
        }
        new HTTPServer.Builder()
                .withPort(settings.port)
                .build();
        scheduled.scheduleAtFixedRate(() -> voiceTime.forEach((id, time) -> {
            long now = System.currentTimeMillis();
            VOICE_TIME_COUNTER
                    .labels(id.guild, id.channel, id.user)
                    .inc(now - time);
            voiceTime.put(id, now);
        }), 0, 100, TimeUnit.MILLISECONDS);
    }

    @Init
    public void init(JDA jda) {
        jda.getGuildCache()
                .stream()
                .map(Guild::getChannels)
                .flatMap(Collection::stream)
                .forEach(channel -> {
                    NAME_RESOLVER.labels(channel.getId()).info("type", channel.getType().name().toLowerCase(), "name", channel.getName());
                    CHANNEL_COUNTER.labels(channel.getGuild().getId(), channel.getType().name().toLowerCase()).inc();
                });
        jda.getGuildCache()
                .stream()
                .forEach(guild -> NAME_RESOLVER.labels(guild.getId()).info("type", "guild", "name", guild.getName()));
        jda.getGuildCache()
                .stream()
                .forEach(guild -> GUILD_COUNTER.labels(guild.getId(), OnlineStatus.UNKNOWN.name().toLowerCase()).inc(guild.getMemberCount()));
        jda.getGuildCache()
                .stream()
                .map(Guild::getMembers)
                .flatMap(Collection::stream)
                .forEach(member -> setMemberCount(member.getGuild(), member.getOnlineStatus()));
        jda.getGuildCache()
                .stream()
                .map(Guild::getTextChannels)
                .flatMap(Collection::stream)
                .forEach(channel -> setDefaultMetrics(MESSAGE_COUNTER, channel));
        jda.getGuildCache()
                .stream()
                .map(Guild::getVoiceChannels)
                .flatMap(Collection::stream)
                .forEach(channel -> {
                    setDefaultMetrics(VOICE_TIME_COUNTER, channel);
                    if (channel.getMembers().size() > 0) {
                        channel.getMembers().forEach(member -> voiceTime.put(new VoiceTime(channel.getGuild().getId(), channel.getId(), member.getId()), System.currentTimeMillis()));
                    }
                });
        jda.getUserCache()
                .stream()
                .forEach(user -> NAME_RESOLVER.labels(user.getId()).info("type", "user", "name", user.getName()));
    }

    @On
    public void onMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) {
            return;
        }
        MESSAGE_COUNTER.labels(event.getGuild().getId(), event.getChannel().getId(), event.getAuthor().getId()).inc();
    }

    @On
    public void onVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getEntity().getUser().isBot()) {
            return;
        }
        if (event.getChannelLeft() != null) {
            voiceTime.remove(new VoiceTime(event.getEntity().getGuild().getId(), event.getChannelLeft().getId(), event.getEntity().getId()));
        }
        if (event.getChannelJoined() != null) {
            voiceTime.put(new VoiceTime(event.getEntity().getGuild().getId(), event.getChannelJoined().getId(), event.getEntity().getId()), System.currentTimeMillis());
        }
    }

    @On
    public void onChannelDeleted(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        removeMetricsWithLabels(MESSAGE_COUNTER, event.getChannel().asGuildChannel());
        removeMetricsWithLabels(VOICE_TIME_COUNTER, event.getChannel().asGuildChannel());
        CHANNEL_COUNTER.labels(event.getChannel().asGuildChannel().getGuild().getId(), event.getChannel().getType().name().toLowerCase()).dec();
    }

    @On
    public void onChannelCreated(ChannelCreateEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        if (event.isFromType(ChannelType.TEXT)) {
            setDefaultMetrics(MESSAGE_COUNTER, event.getChannel().asGuildChannel());
        } else if (event.isFromType(ChannelType.VOICE)) {
            setDefaultMetrics(VOICE_TIME_COUNTER, event.getChannel().asGuildChannel());
        }
        NAME_RESOLVER.labels(event.getChannel().getId()).info("type", event.getChannel().getType().name().toLowerCase(), "name", event.getChannel().getName());
        CHANNEL_COUNTER.labels(event.getChannel().asGuildChannel().getGuild().getId(), event.getChannel().getType().name().toLowerCase()).inc();
    }

    @On
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
//        NAME_RESOLVER.remove(event.getChannel().getId());
        NAME_RESOLVER.labels(event.getChannel().getId()).info("type", event.getChannel().getType().name().toLowerCase(), "name", event.getChannel().getName());
    }

    @On
    public void onUser(GenericUserEvent event) {
        NAME_RESOLVER.labels(event.getUser().getId()).info("type", "user", "name", event.getUser().getName());
    }

    @On
    public void onUserStatusChange(UserUpdateOnlineStatusEvent event) {
        if (event.getOldOnlineStatus() != event.getNewOnlineStatus()) {
            setMemberCount(event.getGuild(), event.getOldOnlineStatus(), event.getNewOnlineStatus());
        }
    }

    @On
    public void onMember(GenericGuildMemberEvent event) {
        NAME_RESOLVER.labels(event.getMember().getId()).info("type", "user", "name", event.getMember().getUser().getName());
    }

    private void removeMetricsWithLabels(Gauge gauge, GuildChannel channel) {
        for (Member member : channel.getGuild().getMembers()) {
            gauge.remove(channel.getGuild().getId(), channel.getId(), member.getId());
        }
    }

    private void setDefaultMetrics(Gauge gauge, GuildChannel channel) {
        for (Member member : channel.getGuild().getMembers()) {
            gauge.labels(channel.getGuild().getId(), channel.getId(), member.getId()).set(0);
        }
    }

    private void setMemberCount(Guild guild, OnlineStatus newStatus) {
        setMemberCount(guild, null, newStatus);
    }

    private void setMemberCount(Guild guild, OnlineStatus oldStatus, OnlineStatus newStatus) {
        if (oldStatus == null) {
            oldStatus = OnlineStatus.UNKNOWN;
        }
        GUILD_COUNTER.labels(guild.getId(), oldStatus.name().toLowerCase()).dec();
        GUILD_COUNTER.labels(guild.getId(), newStatus.name().toLowerCase()).inc();
    }
}
