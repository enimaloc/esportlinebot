package fr.enimaloc.esportlinebot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.command.SlashCommand;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import fr.enimaloc.esportlinebot.commands.ClearCommand;
import fr.enimaloc.esportlinebot.commands.ForceDrawCommand;
import fr.enimaloc.esportlinebot.commands.StatusCommand;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import org.jetbrains.annotations.NotNull;
import fr.enimaloc.enutils.classes.NumberUtils;

import javax.security.auth.login.LoginException;

public class ESportLineBot extends ListenerAdapter {

    public static final boolean DEBUG           = System.getenv("DEV") != null;
    public static final String  THUMBS_UP       = "\uD83D\uDC4D";
    public static final int     DAY_TO_CLOSE    = Calendar.SATURDAY;
    public static final String  GUILD_ID        = DEBUG ? "793693128661008394" : "373139766109011970";
    public static final String  STAFF_ROLE_ID   = DEBUG ? "793693415089897523" : "374235587198189570";
    public static final String  MINOZEN_USER_ID = "532813962874585098";

    public final Pattern[] links;
    public final long      voteChannel;
    public final long      destChannel;
    public final long      threadsChannel;
    public final long      voiceChannel;
    public final String    emote;
    public final long[]    ignoreMessage;
    public final String    channelNameTemplate = "%s's channels";

    public       boolean       ended = false;
    public final CommandClient client;

    public static void main(String[] args) {
        new ESportLineBot(System.getenv("TOKEN"),
                          System.getenv("VOTE_CHANNEL"),
                          System.getenv("DEST_CHANNEL"),
                          System.getenv("THREADS_CHANNEL"),
                          System.getenv("VOICE_CHANNEL"),
                          Arrays.stream(System.getenv()
                                              .getOrDefault("LINK",
                                                            "https://www.gifyourgame.com/," +
                                                                    "https://www.youtube.com/," +
                                                                    "https://clips.twitch.tv/"
                                              )
                                              .split(","))
                                .map(Pattern::quote)
                                .map(quote -> ".*(?<link>" + quote + "[^ ]*) ?.*")
                                .map(Pattern::compile)
                                .toArray(Pattern[]::new),
                          System.getenv()
                                .getOrDefault("EMOJI", THUMBS_UP),
                          System.getenv("IGNORE_MSG")
                                .split(",")
        );
    }

    public ESportLineBot(
            String token,
            String voteChannel,
            String destChannel,
            String threadsChannel,
            String voiceChannel,
            Pattern[] patterns,
            String emoji,
            String[] ignoreMsg
    ) {
        System.out.println("Bot started at " + new Date());
        this.voteChannel = Long.parseLong(voteChannel);
        this.destChannel = Long.parseLong(destChannel);
        this.threadsChannel = Long.parseLong(threadsChannel);
        this.voiceChannel = Long.parseLong(voiceChannel);
        this.links = patterns;
        this.emote = emoji;
        this.ignoreMessage = Arrays.stream(ignoreMsg)
                                   .mapToLong(this::getLong)
                                   .filter(n -> n != Long.MAX_VALUE)
                                   .toArray();
        System.out.println("Patterns loaded:\n" + Arrays.stream(patterns)
                                                        .map(Pattern::pattern)
                                                        .collect(Collectors.joining("\n -")));
        this.client = new CommandClientBuilder().setOwnerId("136200628509605888")
                                                .forceGuildOnly(GUILD_ID)
                                                .addSlashCommands(new ForceDrawCommand(this),
                                                                  new StatusCommand(),
                                                                  new ClearCommand()
                                                )
                                                .setListener(new CommandListener() {
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
                                                                                           .contains(id)) ||
                                                                    Arrays.stream(command.getDisabledUsers())
                                                                          .map(Long::parseLong)
                                                                          .toList()
                                                                          .contains(event.getUser()
                                                                                         .getIdLong())) {
                                                                return;
                                                            }
                                                            if ((command.getEnabledRoles().length != 0 &&
                                                                    Arrays.stream(command.getEnabledRoles())
                                                                          .map(Long::parseLong)
                                                                          .noneMatch(id -> event.getMember()
                                                                                                .getRoles()
                                                                                                .stream()
                                                                                                .map(Role::getIdLong)
                                                                                                .toList()
                                                                                                .contains(id))) &&
                                                                    (command.getEnabledUsers().length != 0) &&
                                                                    !Arrays.stream(command.getEnabledUsers())
                                                                           .map(Long::parseLong)
                                                                           .toList()
                                                                           .contains(event.getUser()
                                                                                          .getIdLong())) {
                                                                return;
                                                            }
                                                        }
                                                        CommandListener.super.onSlashCommand(event, command);
                                                    }
                                                })
                                                .setStatus(OnlineStatus.ONLINE)
                                                .build();

        try {
            JDABuilder.create(token,
                              GatewayIntent.GUILD_MESSAGE_REACTIONS,
                              GatewayIntent.GUILD_MESSAGES,
                              GatewayIntent.GUILD_MEMBERS,
                              GatewayIntent.GUILD_VOICE_STATES
                      )
                      .addEventListeners(this, client)
                      .disableCache(Arrays.asList(CacheFlag.values()))
                      .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                      .setMemberCachePolicy(MemberCachePolicy.ALL)
                      .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        TextChannel textChannel = event.getJDA()
                                       .getTextChannelById(voteChannel);
        if (textChannel != null) {
            List<Long> reacted   = new ArrayList<>();
            List<Long> sent      = new ArrayList<>();
            List<Long> overrides = new ArrayList<>();

            MessageHistory history = textChannel.getHistoryFromBeginning(99)
                                                .complete();
            for (Message message : history.getRetrievedHistory()) {
                if (Arrays.stream(ignoreMessage)
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
                    if (isGoodEmote(reaction)) {
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
                            react(message);
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
                    receive(message);
                }
            }

            reacted.clear();
            sent.clear();
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                TextChannel textChannel = event.getJDA()
                                               .getTextChannelById(voteChannel);
                if (textChannel == null) {
                    return;
                }
                Role everyone = textChannel.getGuild()
                                           .getPublicRole();

                PermissionOverride permissionOverride = textChannel.getPermissionOverride(everyone);
                if (Calendar.getInstance()
                            .get(Calendar.DAY_OF_WEEK) == DAY_TO_CLOSE || DEBUG) {
                    draw(textChannel,
                         ((Calendar.getInstance()
                                   .get(Calendar.HOUR_OF_DAY) == 23 && Calendar.getInstance()
                                                                               .get(Calendar.MINUTE) == 59 &&
                                 Calendar.getInstance()
                                         .get(Calendar.SECOND) >= 55) || DEBUG)
                    );
                } else {
                    if (permissionOverride != null && permissionOverride.getDenied()
                                                                        .contains(Permission.MESSAGE_WRITE)) {
                        canWrite(textChannel, everyone, true);
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

    public void draw(TextChannel textChannel, boolean ok) {
        Role everyone = textChannel.getGuild()
                                   .getPublicRole();
        PermissionOverride permissionOverride = textChannel.getPermissionOverride(everyone);
        if (permissionOverride == null || !permissionOverride.getDenied()
                                                             .contains(Permission.MESSAGE_WRITE)) {
            canWrite(textChannel, everyone, false);
        }
        if (ok && !ended) {
            ended = true;

            List<Message> history = new ArrayList<>(textChannel.getHistoryFromBeginning(99)
                                                               .complete()
                                                               .getRetrievedHistory());
            history.sort(Comparator.comparingInt(a -> getVote((Message) a))
                                   .reversed());
            EmbedBuilder builder = new EmbedBuilder().setTitle("Résultat vote");
            int          i       = 0;
            for (Message message : history) {
                if (Arrays.stream(ignoreMessage)
                          .anyMatch(id -> id == message.getIdLong())) {
                    continue;
                }
                if (i < 5 && checkPattern(message.getContentRaw())) {
                    i++;
                    builder.appendDescription("[Clip par %s](%s), %s votes\n".formatted(message.getAuthor()
                                                                                               .getAsTag(),
                                                                                        getLink(message.getContentRaw()),
                                                                                        getVote(message)
                    ));
                }
                message.delete()
                       .complete();
            }
            MessageAction messageAction = Objects.requireNonNull(textChannel.getJDA()
                                                                            .getTextChannelById(destChannel))
                                                 .sendMessageEmbeds(builder.build());

            messageAction.complete();
        }
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        receive(event.getMessage());
        super.onGuildMessageReceived(event);
    }

    @Override
    public void onGuildMessageDelete(@NotNull GuildMessageDeleteEvent event) {
        if (Arrays.stream(ignoreMessage)
                  .anyMatch(id -> id == event.getMessageIdLong())) {
            return;
        }
        MessageHistory history = event.getChannel()
                                      .getHistoryFromBeginning(99)
                                      .complete();
        for (PermissionOverride override : event.getChannel()
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
                canWrite(event.getChannel(), override.getMember(), true);
            }
        }
        super.onGuildMessageDelete(event);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getChannel()
                 .getIdLong() != voteChannel || event.getUser() == null || event.getUser()
                                                                                .isBot()) {
            return;
        }
        if (Arrays.stream(ignoreMessage)
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
                if (isGoodEmote(reaction) && reaction.retrieveUsers()
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


    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        if (event.getChannelJoined()
                 .getIdLong() != voiceChannel || event.getMember()
                                                      .getUser()
                                                      .isBot()) {
            return;
        }
        TextChannel threadsChannel = event.getJDA()
                                          .getTextChannelById(this.threadsChannel);
        Category category;
        if (threadsChannel == null || (category = threadsChannel.getParent()) == null) {
            return;
        }
        String name = channelNameTemplate.formatted(event.getMember()
                                                         .getEffectiveName());
        ThreadChannel threadChannel = ThreadChannel.create(threadsChannel, name)
                                                   .complete();
        threadChannel.join(event.getMember())
                     .complete();
        VoiceChannel voiceChannel = category.createVoiceChannel(name)
                                            .complete();
        event.getGuild()
             .moveVoiceMember(event.getMember(), voiceChannel)
             .complete();

        super.onGuildVoiceJoin(event);
    }

    @Override
    public void onRawGateway(@NotNull RawGatewayEvent event) {
        System.out.println("event.getPackage() = " + event.getPackage());
        super.onRawGateway(event);
    }

    // CLASS

    static class ThreadChannel {

        private final JDA jda;

        private final long    parentId;
        private final long    ownerId;
        private final int     rateLimitPerUser;
        private final long    guildId;
        private final String  name;
        private final long    id;
        private       boolean archived;
        //        private       Date    archiveIn;
        private       boolean locked;
        private       int     autoArchiveDuration;
        private       int     memberCount;
        private       long    lastMessageId;
        private       int     messageCount;

        public static final Route LIST_ACTIVE_THREADS           = Route.get("channels/{channel_id}/threads/active");
        public static final Route LIST_PUBLIC_ARCHIVED_THREADS  = Route.get(
                "channels/{channel_id}/threads/archived/public");
        public static final Route LIST_PRIVATE_ARCHIVED_THREADS = Route.get(
                "channels/{channel_id}/threads/archived/private");
        public static final Route LIST_JOINED_PRIVATE_THREADS   = Route.get(
                "channels/{channel_id}/users/@me/threads/archived/private");

        public static final Route NEW_THREAD              = Route.post("channels/{channel_id}/threads");
        public static final Route NEW_THREAD_WITH_MESSAGE = Route.post(
                "channels/{channel_id}/messages/{message_id}/threads");

        public static final Route JOIN_THREAD          = Route.put("channels/{channel_id}/thread-members/@me");
        public static final Route ADD_THREAD_MEMBER    = Route.put("channels/{channel_id}/thread-members/{user_id}");
        public static final Route LEAVE_THREAD         = Route.delete("channels/{channel_id}/thread-members/@me");
        public static final Route REMOVE_THREAD_MEMBER = Route.delete("channels/{channel_id}/thread-members/{user_id}");
        public static final Route GET_THREAD_MEMBER    = Route.get("channels/{channel_id}/thread-members/{user_id}");
        public static final Route LIST_THREAD_MEMBERS  = Route.get("channels/{channel_id}/thread-members");

        public static RestAction<List<ThreadChannel>> listActiveThreads(TextChannel channel) {
            Route.CompiledRoute route = LIST_ACTIVE_THREADS.compile(channel.getId());
            return new RestActionImpl<>(channel.getJDA(),
                                        route,
                                        (response, request) -> response.getArray()
                                                                       .stream(DataArray::getObject)
                                                                       .map(json -> new ThreadChannel(channel.getJDA(),
                                                                                                      json
                                                                       ))
                                                                       .toList()
            );
        }

        public static RestAction<ThreadChannel> create(TextChannel channel, String name) {
            Route.CompiledRoute route = NEW_THREAD.compile(channel.getId());
            DataObject          json  = DataObject.empty();
            json.put("name", name);
            json.put("type", 11);
            return new RestActionImpl<>(channel.getJDA(),
                                        route,
                                        json,
                                        (response, request) -> new ThreadChannel(channel.getJDA(), response.getObject())
            );
        }

        public static RestAction<ThreadChannel> create(Message message, String name) {
            Route.CompiledRoute route = NEW_THREAD_WITH_MESSAGE.compile(message.getChannel()
                                                                               .getId(), message.getId());
            DataObject json = DataObject.empty();
            json.put("name", name);
            json.put("type", 11);
            return new RestActionImpl<>(message.getJDA(),
                                        route,
                                        json,
                                        (response, request) -> new ThreadChannel(message.getJDA(), response.getObject())
            );
        }

        ThreadChannel(JDA jda, DataObject json) {
            this.jda = jda;
            DataObject metadata = json.getObject("thread_metadata");

            this.parentId = json.getLong("parent_id");
            this.ownerId = json.getLong("owner_id");
            this.rateLimitPerUser = json.getInt("rate_limit_per_user");
            this.guildId = json.getLong("guild_id");
            this.name = json.getString("name");
            this.id = json.getLong("id");
            this.archived = metadata.getBoolean("archived");
//            this.archiveIn = new Date(DateTimeFormatter.BASIC_ISO_DATE.parse(json.getString("archive_timestamp")));
            this.locked = metadata.getBoolean("locked");
            this.autoArchiveDuration = metadata.getInt("auto_archive_duration");
            this.memberCount = json.getInt("member_count");
            this.messageCount = json.getInt("message_count");
            this.lastMessageId = json.getLong("last_message_id", -1L);
        }

        public RestAction<Void> join() {
            return join(getGuild().getSelfMember());
        }

        public RestAction<Void> join(Member member) {
            Route.CompiledRoute route;
            if (member.getIdLong() == getGuild().getSelfMember()
                                                .getIdLong()) {
                route = JOIN_THREAD.compile(getId());
            } else {
                route = ADD_THREAD_MEMBER.compile(getId(), member.getId());
            }

            return new RestActionImpl<>(jda, route);
        }

        public RestAction<Void> leave() {
            return leave(getGuild().getSelfMember());
        }

        public RestAction<Void> leave(Member member) {
            Route.CompiledRoute route;
            if (member.getIdLong() == getGuild().getSelfMember()
                                                .getIdLong()) {
                route = LEAVE_THREAD.compile(getId());
            } else {
                route = REMOVE_THREAD_MEMBER.compile(getId(), member.getId());
            }

            return new RestActionImpl<>(jda, route);
        }

        // GETTERS

        public long getParentId() {
            return parentId;
        }

        public long getOwnerId() {
            return ownerId;
        }

        public int getRateLimitPerUser() {
            return rateLimitPerUser;
        }

        public Guild getGuild() {
            return jda.getGuildById(guildId);
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return getIdLong() + "";
        }

        public long getIdLong() {
            return id;
        }

        public boolean isArchived() {
            return archived;
        }

        public boolean isLocked() {
            return locked;
        }

        public int getAutoArchiveDuration() {
            return autoArchiveDuration;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public long getLastMessageId() {
            return lastMessageId;
        }

        public int getMessageCount() {
            return messageCount;
        }
    }

    // UTILS

    public void canWrite(TextChannel channel, IPermissionHolder holder, boolean can) {
        if (!can) {
            channel.getManager()
                   .putPermissionOverride(holder, 0, Permission.MESSAGE_WRITE.getRawValue())
                   .complete();
        } else {
            channel.getManager()
                   .removePermissionOverride(holder)
                   .complete();
        }
    }

    public int getVote(Message message) {
        return message.getReactions()
                      .stream()
                      .filter(this::isGoodEmote)
                      .findFirst()
                      .map(messageReaction -> messageReaction.getCount() - 1)
                      .orElse(0);
    }

    public boolean isGoodEmote(MessageReaction reaction) {
        return isLong(emote) ? (reaction.getReactionEmote()
                                        .isEmote() && reaction.getReactionEmote()
                                                              .getEmote()
                                                              .getIdLong() == getLong(emote)) : (
                reaction.getReactionEmote()
                        .isEmoji() && reaction.getReactionEmote()
                                              .getEmoji()
                                              .equals(emote));
    }

    public boolean checkPattern(String content) {
        return Arrays.stream(links)
                     .anyMatch(pattern -> pattern.matcher(content)
                                                 .matches());
    }

    public String getLink(String content) {
        for (Pattern pattern : links) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.matches()) {
                return matcher.group("link");
            }
        }
        return "";
    }

    public long getLong(String value) {
        return NumberUtils.getSafe(value, Long.class)
                          .orElseGet(() -> NumberUtils.getSafe(value.split(":")[value.split(":").length - 1],
                                                               Long.class
                                                      )
                                                      .orElse(Long.MAX_VALUE));
    }

    public boolean isLong(String value) {
        return getLong(value) != Long.MAX_VALUE;
    }

    public void receive(Message message) {
        if (message.getChannel()
                   .getIdLong() != voteChannel || message.getMember() == null) {
            return;
        }
        if (Arrays.stream(ignoreMessage)
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

    public void react(Message message) {
        Emote emoteById;
        // FIXME: 19/09/2021 Buggy part, use getAsMention instead in var
        if (NumberUtils.getSafe(emote, Long.class)
                       .isPresent() && (emoteById = message.getGuild()
                                                           .getEmoteById(getLong(emote))) != null) {
            message.addReaction(emoteById)
                   .queue();
        } else {
            message.addReaction(emote)
                   .queue();
        }
    }

}
