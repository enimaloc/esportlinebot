/*
 * MusicCommand
 *
 * 0.0.1
 *
 * 20/12/2022
 */
package fr.enimaloc.esportlinebot.module.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import core.GLA;
import fr.enimaloc.enutils.jda.annotation.Init;
import fr.enimaloc.enutils.jda.annotation.MethodTarget;
import fr.enimaloc.enutils.jda.annotation.On;
import fr.enimaloc.enutils.jda.annotation.SlashCommand;
import fr.enimaloc.enutils.jda.builder.processor.InteractionProcessor;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.utils.Checks;
import fr.enimaloc.esportlinebot.settings.Settings;
import fr.enimaloc.esportlinebot.utils.Cache;
import genius.SongSearch;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.ICommandReference;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 */
@SlashCommand(name = "music", description = "Music commands related")
public class MusicModule {

    private final Connection sql;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final GLA genius = new GLA();
    private final Cache<CachedTrack> cache = new Cache<>(1, TimeUnit.HOURS) {
        @Override
        public boolean add(CachedTrack cachedTrack) {
            try {
                sql.createStatement().execute("INSERT OR REPLACE INTO tracks VALUES (" +
                        "'" + cachedTrack.identifier() + "'," +
                        "'" + cachedTrack.title() + "'," +
                        "'" + cachedTrack.url() + "'," +
                        "'" + cachedTrack.author() + "'," +
                        cachedTrack.duration() +
                        ")");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return super.add(cachedTrack);
        }

        @Override
        public CachedTrack factory(Object... objects) {
            try (PreparedStatement stmt = sql.prepareStatement("SELECT * FROM tracks WHERE identifier = ?")) {
                stmt.setString(1, (String) objects[0]);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new CachedTrack(
                            rs.getString("identifier"),
                            rs.getString("title"),
                            rs.getString("url"),
                            rs.getString("author"),
                            rs.getLong("duration")
                    );
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throw new RuntimeException("Unable to find track in cache or database");
        }
    };
    @SlashCommand.GroupProvider
    public Playlist playlist = new Playlist();

    public MusicModule(Settings.Music settings, Connection sql) {
        this.settings = settings;
        this.sql = sql;

        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    // region Helper methods
    private static Optional<String> getCommandMention(JDA jda, String subcommandName) {
        try {
            return jda.retrieveCommands()
                    .submit()
                    .get()
                    .stream()
                    .filter(cmd -> cmd.getName().equals("music"))
                    .map(Command::getSubcommands)
                    .flatMap(Collection::stream)
                    .filter(sub -> sub.getName().equals(subcommandName))
                    .map(ICommandReference::getAsMention)
                    .findFirst();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatTime(long time) {
        StringJoiner joiner = new StringJoiner(":");
        long hours = time / 3600000;
        if (hours > 0) {
            joiner.add(String.format("%02d", hours));
        }
        long minutes = (time % 3600000) / 60000;
        joiner.add(String.format("%02d", minutes));
        long seconds = (time % 60000) / 1000;
        joiner.add(String.format("%02d", seconds));
        return joiner.toString();
    }

    private static String buildProgressBar(AudioTrack track, int size, String left, String center, String right) {
        return buildProgressBar(track.getInfo().isStream, track.getPosition(), track.getDuration(), size, left, center, right);
    }

    private static String buildProgressBar(long value, long max, int size, String left, String right) {
        return buildProgressBar(false, value, max, size, left, left, right);
    }

    private static String buildProgressBar(boolean undetermined, long value, long max, int size, String left, String center, String right) {
        size -= 1;
        if (undetermined) {
            return left.repeat(size) + center;
        }
        int progress = (int) (value / (double) max * size);
        return left.repeat(progress) + center + right.repeat(size - progress);
    }

    @Init
    public void init(JDA jda) {
        try {
            playlist.loadAll(sql);

            sql.createStatement().execute("CREATE TABLE IF NOT EXISTS tracks (" +
                    "identifier TEXT NOT NULL PRIMARY KEY," +
                    "title TEXT NOT NULL," +
                    "url TEXT NOT NULL," +
                    "author TEXT NOT NULL," +
                    "duration INTEGER NOT NULL" +
                    ")");
            sql.createStatement().execute("CREATE TABLE IF NOT EXISTS music_players (" +
                    "guild_id INTEGER NOT NULL PRIMARY KEY," +
                    "channel_id INTEGER NOT NULL," +
                    "volume INTEGER NOT NULL DEFAULT 100," +
                    "repeat INTEGER NOT NULL DEFAULT 0," +
                    "paused INTEGER NOT NULL DEFAULT 0," +
                    "position INTEGER NOT NULL DEFAULT 0," +
                    "track_identifier TEXT NULL" +
                    ")");
            sql.createStatement().execute("CREATE TABLE IF NOT EXISTS tracks_list (" +
                    "guild_id INTEGER NOT NULL," +
                    "track_identifier INTEGER NOT NULL," +
                    "position INTEGER NOT NULL," +
                    "PRIMARY KEY (guild_id, position)" +
                    ")");
            new Thread(() -> {
                long start = System.currentTimeMillis();
                try (ResultSet resultSet = sql.createStatement().executeQuery("SELECT * FROM tracks")) {
                    while (resultSet.next()) {
                        cache.add(new CachedTrack(
                                resultSet.getString("identifier"),
                                resultSet.getString("title"),
                                resultSet.getString("url"),
                                resultSet.getString("author"),
                                resultSet.getLong("duration")
                        ));
                    }
                    System.out.println("Loaded " + cache.size() + " tracks in " + (System.currentTimeMillis() - start) + "ms");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        GuildMusicManager musicManager = musicManagers.get(guild.getIdLong());

        if (musicManager == null) {
            JDA jda = guild.getJDA();
            musicManager = new GuildMusicManager(playerManager, next -> update(guild, false)) {
                @Override
                public void load() {
                    try {
                        try (ResultSet resultSet = sql.createStatement().executeQuery("SELECT * FROM music_players")) {
                            while (resultSet.next()) {
                                Guild guild = jda.getGuildById(resultSet.getLong("guild_id"));
                                if (guild == null) {
                                    continue;
                                }
                                VoiceChannel channel = guild.getVoiceChannelById(resultSet.getLong("channel_id"));
                                if (channel == null) {
                                    continue;
                                }
                                GuildMusicManager musicManager = getGuildAudioPlayer(guild);
                                musicManager.player.setVolume(resultSet.getInt("volume"));
//                    musicManager.player.setRepeatMode(RepeatMode.values()[resultSet.getInt("repeat")]);
                                musicManager.player.setPaused(resultSet.getBoolean("paused"));
                                String trackIdentifier = resultSet.getString("track_identifier");
                                if (trackIdentifier == null) {
                                    continue;
                                }
                                long position = resultSet.getLong("position");
                                playerManager.loadItemOrdered(musicManager,
                                        cache.getOrCreate(t -> t.identifier().equals(trackIdentifier), trackIdentifier).identifier(),
                                        new AudioLoadResultHandler() {
                                            @Override
                                            public void trackLoaded(AudioTrack track) {
                                                musicManager.player.playTrack(track);
                                                musicManager.player.getPlayingTrack().setPosition(position);
                                            }

                                            @Override
                                            public void playlistLoaded(AudioPlaylist playlist) {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public void noMatches() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public void loadFailed(FriendlyException exception) {
                                                throw new UnsupportedOperationException();
                                            }
                                        });
                            }
                        }
                        try (ResultSet resultSet = sql.createStatement().executeQuery("SELECT * FROM tracks_list")) {
                            while (resultSet.next()) {
                                Guild guild = jda.getGuildById(resultSet.getLong("guild_id"));
                                if (guild == null) {
                                    continue;
                                }
                                GuildMusicManager musicManager    = getGuildAudioPlayer(guild);
                                String            trackIdentifier = resultSet.getString("track_identifier");
                                if (trackIdentifier == null) {
                                    continue;
                                }
                                playerManager.loadItemOrdered(musicManager,
                                        cache.getOrCreate(t -> t.identifier().equals(trackIdentifier), trackIdentifier).identifier(),
                                        new AudioLoadResultHandler() {
                                            @Override
                                            public void trackLoaded(AudioTrack track) {
                                                musicManager.scheduler.queue(track);
                                            }

                                            @Override
                                            public void playlistLoaded(AudioPlaylist playlist) {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public void noMatches() {
                                                throw new UnsupportedOperationException();
                                            }

                                            @Override
                                            public void loadFailed(FriendlyException exception) {
                                                throw new UnsupportedOperationException();
                                            }
                                        });
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void save() {
                    try {
                        try (PreparedStatement stmt = sql.prepareStatement("INSERT OR REPLACE INTO music_players" +
                                "(guild_id, channel_id, volume, repeat, paused, position, track_identifier)" +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                            stmt.setLong(1, guild.getIdLong());
                            stmt.setLong(2, guild.getAudioManager().getConnectedChannel().getIdLong());
                            stmt.setInt(3, player.getVolume());
                            //                stmt.setInt(4, player.getRepeatMode().ordinal());
                            stmt.setBoolean(4, player.isPaused());
                            stmt.setLong(5, player.getPlayingTrack().getPosition());
                            stmt.setString(6, player.getPlayingTrack().getIdentifier());
                        }
                        try (PreparedStatement stmt = sql.prepareStatement("INSERT OR REPLACE INTO tracks_list" +
                                "(guild_id, track_identifier, position)" +
                                "VALUES (?, ?, ?)")) {
                            stmt.setLong(1, guild.getIdLong());
                            for (int i = 0; i < scheduler.getQueue().size(); i++) {
                                stmt.setString(2, scheduler.getQueue().get(i).getIdentifier());
                                stmt.setLong(3, i);
                                stmt.addBatch();
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            musicManager.load();
            musicManagers.put(guild.getIdLong(), musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    // region Commands
    @SlashCommand.Sub(description = "Play music by title (or link) in specified or your channel")
    public void play(GuildSlashCommandEvent event,
                     @SlashCommand.Option String title,
                     @SlashCommand.Option Optional<VoiceChannel> channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(Objects.requireNonNull(event.getGuild()));
        AudioChannel audioChannel = channel.orElseGet(() -> event.getMember().getVoiceState().getChannel().asVoiceChannel());

        if (Stream.of("http", "https").noneMatch(title::startsWith)) {
            title = "ytsearch:" + title;
        }

        String finalTitle = title;
        playerManager.loadItemOrdered(musicManager, title, new TrackLoader(
                audioChannel,
                loadedTrack -> {
                    cache.addOrUpdate(new CachedTrack(loadedTrack));
                    event.replyEphemeral("Adding to queue " + loadedTrack.getInfo().title)
                            .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(this, event.getGuild()).build())
                            .queue(unused -> update(event.getGuild()));
                },
                result -> {
                    result.getTracks().stream().map(CachedTrack::new).forEach(cache::addOrUpdate);
                    event.replyEphemeral("Result for " + finalTitle.split(":", 2)[1])
                            .setActionRow(StringSelectMenu.create("ytsearch-1:" + finalTitle)
                                    .addOption("Search for '" + finalTitle.split(":", 2)[1] + "'", finalTitle, result.getTracks().size() + " tracks")
                                    .addOptions(result.getTracks()
                                            .stream()
                                            .limit(SelectMenu.OPTIONS_MAX_AMOUNT)
                                            .map(track -> SelectOption.of(track.getInfo().title, track.getIdentifier())
                                                    .withDescription("by " + track.getInfo().author))
                                            .toList())
                                    .build())
                            .queue();
                },
                (first, playlist) -> {
                    playlist.getTracks().stream().map(CachedTrack::new).forEach(cache::addOrUpdate);
                    event.replyEphemeral("Adding to queue " + (playlist.getTracks().size() - first) + " tracks from " + playlist.getName())
                            .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(this, event.getGuild()).build())
                            .queue(unused -> update(event.getGuild()));
                },
                unused -> event.replyEphemeral("Nothing found by " + finalTitle).queue(),
                exception -> event.replyEphemeral("Could not play: " + exception.getMessage()).queue()
        ));
    }

    void play(AudioChannel audioChannel, GuildMusicManager musicManager, AudioTrack track) {
        audioChannel.getGuild().getAudioManager().openAudioConnection(audioChannel);
        if (!audioChannel.getGuild().getAudioManager().isSelfDeafened()) {
            audioChannel.getGuild().getAudioManager().setSelfDeafened(true);
        }
        musicManager.scheduler.queue(track);
    }

    @SlashCommand.Sub(description = "Skip current song")
    public void skip(
            GuildSlashCommandEvent event,
            @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("nextTracks"))) Optional<String> to/*,
            @SlashCommand.Option OptionalInt n*/
    ) {
        OptionalInt n = OptionalInt.empty();
        GuildMusicManager audioPlayer = getGuildAudioPlayer(event.getGuild());
        String reply = "Skipped ";
        if (to.isPresent()
                && audioPlayer.scheduler.getQueue()
                .stream()
                .map(AudioTrack::getInfo)
                .map(info -> info.title)
                .anyMatch(to.get()::equals)) {
            while (!audioPlayer.player.getPlayingTrack().getInfo().title.equals(to.get())) {
                audioPlayer.scheduler.nextTrack();
            }
            reply += " to " + to.get();
        } else {
            IntStream.range(0, n.orElse(1)).forEach(unused -> audioPlayer.scheduler.nextTrack());
            reply += n.orElse(1) + " tracks";
        }
        event.replyEphemeral(reply)
                .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                .queue(unused -> update(event.getGuild()));
    }

    public String[] nextTracks(CommandAutoCompleteInteractionEvent event) {
        return getGuildAudioPlayer(event.getGuild())
                .scheduler
                .getQueue()
                .stream()
                .map(AudioTrack::getInfo)
                .map(track -> track.title)
                .filter(title -> title.toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                .limit(15)
                .toArray(String[]::new);
    }

    @SlashCommand.Sub(description = "Show now playing song")
    public void nowPlaying(GuildSlashCommandEvent event, Optional<Boolean> sticky) {
        if (sticky.orElse(false)) {
            sticky(event);
            return;
        }
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        AudioTrack track = musicManager.player.getPlayingTrack();

        if (track == null) {
            event.replyEphemeral("Nothing").queue();
            return;
        }
        event.replyEphemeralEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                .queue(unused -> update(event.getGuild()));
    }

    @SlashCommand.Sub(description = "Stop the player")
    public void stop(GuildSlashCommandEvent event) {
        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());
        musicManager.player.destroy();
        musicManager.scheduler.clearQueue();
        event.getGuild().getAudioManager().closeAudioConnection();
        event.replyEphemeral("Player stopped and queue cleared").queue();
        musicManager.setMessageId(-1);
    }
    // endregion

    @SlashCommand.Sub(description = "Display lyrics of song")
    public void lyrics(GuildSlashCommandEvent event, @SlashCommand.Option Optional<String> songName) throws IOException {
        lyrics(event, event.getGuildChannel(), event.deferReply().complete(), songName);
    }

    private void lyrics(IReplyCallback event, GuildMessageChannelUnion source, InteractionHook hook, Optional<String> songName) throws IOException {
        GuildMusicManager player       = getGuildAudioPlayer(event.getGuild());
        AudioTrack        playingTrack = player.player.getPlayingTrack();
        if (songName.isEmpty() && playingTrack == null) {
            event.getHook().editOriginal("No track playing, please specify a track").queue();
        }

        String title = songName != null ? songName : playingTrack.getInfo().title;
        LinkedList<SongSearch.Hit> results = genius.search(title).getHits();
        while (results.isEmpty() && title.matches(".*[(\\[{].*[)\\]}]$")) {
            String oldTitle = title;
            // remove last parenthesis and everything inside
            title = title.replaceAll("[(\\[{].*[)\\]}]$", "").trim();
            hook.editOriginal("No lyrics found for " + oldTitle + " trying " + title + " instead")
                    .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                    .queue();
            results = genius.search(title).getHits();
        }
        if (results.isEmpty()) {
            hook.editOriginal("No lyrics found for " + title + (songName.isEmpty() ? ", try using " + getCommandMention(event.getJDA(), "lyrics").orElse("`/music lyrics song-name:<song name>`") : ""))
                    .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                    .queue();
            return;
        }
        SongSearch.Hit result = results.get(0);

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(result.getTitle(), result.getUrl())
                .setAuthor(result.getArtist().getName(), result.getArtist().getUrl(), result.getArtist().getImageUrl())
                .setThumbnail(result.getThumbnailUrl());
        if (result.fetchLyrics().length() > 15000) {
            hook.editOriginal("Lyrics found for " + title + " but likely not correct: " + result.getUrl())
                    .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                    .queue();
        } else {
            List<MessageEmbed> embeds = new ArrayList<>();
            String content = result.fetchLyrics().trim();
            long lastMessage = -1;
            while (content.length() >= MessageEmbed.DESCRIPTION_MAX_LENGTH - 2) {
                int index = content.lastIndexOf("\n\n", MessageEmbed.DESCRIPTION_MAX_LENGTH);
                if (index == -1) {
                    index = content.lastIndexOf("\n", MessageEmbed.DESCRIPTION_MAX_LENGTH);
                }
                if (index == -1) {
                    index = content.lastIndexOf(" ", MessageEmbed.DESCRIPTION_MAX_LENGTH);
                }
                if (index == -1) {
                    index = MessageEmbed.DESCRIPTION_MAX_LENGTH;
                }
                embeds.add(builder.setDescription(content.substring(0, index).trim()).build());
                content = content.substring(index).trim();
                builder = builder.setAuthor(null)
                        .setTitle(null, null)
                        .setThumbnail(null);
                if (embeds.size() == 5) {
                    if (lastMessage == -1) {
                        lastMessage = hook
                                .editOriginal("")
                                .setEmbeds(embeds)
                                .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                                .complete()
                                .getIdLong();
                    } else {
                        lastMessage = source
                                .sendMessage("")
                                .setEmbeds(embeds)
                                .setMessageReference(lastMessage)
                                .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                                .complete()
                                .getIdLong();
                    }
                    embeds.clear();
                }
            }
            embeds.add(builder.setDescription(content).build());
            if (lastMessage == -1) {
                hook.editOriginal("")
                        .setEmbeds(embeds)
                        .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                        .queue();
            } else {
                source.sendMessage("")
                        .setEmbeds(embeds)
                        .setMessageReference(lastMessage)
                        .setActionRow(InteractionProcessor.getInteraction("delete").orElseThrow().component())
                        .queue();
            }
        }
    }
    // endregion

    @SlashCommand.Sub(description = "Set player volume")
    public void volume(GuildSlashCommandEvent event, @SlashCommand.Option @SlashCommand.Range(max = 150) int volume) {
        getGuildAudioPlayer(event.getGuild()).player.setVolume(volume);
        event.replyEphemeral("Volume set to " + volume + "%")
                .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).setVolume(volume).build())
                .queue(unused -> update(event.getGuild()));
    }

    @SlashCommand.Sub(description = "Shuffle the queue")
    public void shuffle(GuildSlashCommandEvent event) {
        GuildMusicManager player = getGuildAudioPlayer(event.getGuild());
        player.scheduler.shuffleQueue();
        event.replyEphemeral("Queue shuffled")
                .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                .queue(unused -> update(event.getGuild()));
    }

    @SlashCommand.Sub(description = "View the queue")
    public void queue(GuildSlashCommandEvent event) {
        GuildMusicManager player = getGuildAudioPlayer(event.getGuild());
        if (player.scheduler.getQueue().isEmpty()) {
            event.replyEphemeral("Queue is empty").queue();
            return;
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < player.scheduler.getQueue().size() && joiner.length() < Message.MAX_CONTENT_LENGTH; i++) {
            AudioTrack track = player.scheduler.getQueue().get(i);
            joiner.add((i + 1) + ". [" + track.getInfo().title + "](<" + track.getInfo().uri + ">) (`" + (track.getInfo().isStream ? "\uD83D\uDD34 Stream" : formatTime(track.getDuration())) + "`) - " + track.getInfo().author);
        }
        String toString = joiner.toString();
        if (toString.length() > Message.MAX_CONTENT_LENGTH) {
            toString = toString.substring(0, toString.lastIndexOf("\n"));
        }
        event.replyEphemeral(toString)
                .queue(unused -> update(event.getGuild()));
    }

    @SlashCommand.Sub
    public void seek(GuildSlashCommandEvent event,
                     @SlashCommand.Option @SlashCommand.Range(min = 0, max = 60) int seconds,
                     @SlashCommand.Option @SlashCommand.Range(min = 0, max = 60) OptionalInt minutes,
                     @SlashCommand.Option @SlashCommand.Range(min = 1) OptionalInt hours) {
        GuildMusicManager player = getGuildAudioPlayer(event.getGuild());
        AudioTrack track = player.player.getPlayingTrack();
        if (track == null) {
            event.replyEphemeral("Nothing is playing").queue();
            return;
        }
        long position = track.getPosition()
                + TimeUnit.SECONDS.toMillis(seconds)
                + TimeUnit.MINUTES.toMillis(minutes.orElse(0))
                + TimeUnit.HOURS.toMillis(hours.orElse(0));
        if (position > track.getDuration()) {
            event.replyEphemeral("Cannot seek past the end of the track").queue();
            return;
        }
        track.setPosition(position);
        event.replyEphemeral("Seeked to " + formatTime(position))
                .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).setTime(position, track.getDuration(), player.scheduler.getQueue().stream().mapToLong(AudioTrack::getDuration).sum()).build())
                .queue(unused -> update(event.getGuild()));
    }

    // region Events
    @On
    public void onTrackSelect(StringSelectInteractionEvent event) {
        if (!event.getSelectMenu().getId().startsWith("ytsearch")) {
            return;
        }
        event.editSelectMenu(null).queue();
        SelectOption selected = event.getSelectedOptions().get(0);
        playerManager.loadItemOrdered(musicManagers, selected.getValue(), new TrackLoader(
                event.getMember().getVoiceState().getChannel(),
                trackLoaded -> event.getHook()
                        .editOriginal("Adding to queue " + trackLoaded.getInfo().title)
                        .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                        .queue(unused -> update(event.getGuild())),
                (first, playlist) -> event.getHook()
                        .editOriginal("Adding to queue " + (playlist.getTracks().size() - first) + " tracks from " + playlist.getName().split(":")[1])
                        .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                        .queue(unused -> update(event.getGuild())),
                unused -> event.getHook().editOriginal("Nothing found by " + selected.getLabel()).queue(),
                exception -> event.getHook().editOriginal("Could not play: " + exception.getMessage()).queue()
        ));
    }

    @On
    public void onDisplayInteraction(ButtonInteractionEvent event) throws IOException {
        GuildMusicManager manager = getGuildAudioPlayer(event.getGuild());
        AudioPlayer player = manager.player;
        TrackScheduler scheduler = manager.scheduler;
        if (!event.getComponentId().startsWith("music:") || event.getMessageIdLong() != manager.messageId()) {
            return;
        }
        switch (event.getComponentId().replaceFirst("music:", "")) {
            case "volume:down" -> {
                player.setVolume(Math.max(0, player.getVolume() - 10));
                event.editButton(event.getButton().withDisabled(player.getVolume() <= 0)).queue();
            }
            case "stop" -> {
                player.stopTrack();
                scheduler.clearQueue();
            }
            case "pause" -> {
                event.editButton(Button.primary("music:play", Emoji.fromUnicode("\u25B6"))).queue();
                player.setPaused(true);
            }
            case "play" -> {
                event.editButton(Button.primary("music:pause", Emoji.fromUnicode("\u23F8"))).queue();
                player.setPaused(false);
            }
            case "skip" -> scheduler.nextTrack();
            case "volume:up" -> {
                player.setVolume(Math.min(150, player.getVolume() + 10));
                event.editButton(event.getButton().withDisabled(player.getVolume() >= 150)).queue();
            }
            case "shuffle" -> scheduler.shuffleQueue();
            case "lyrics" -> lyrics(event, event.getGuildChannel(), event.deferReply().complete(), Optional.empty());
        }
        if (!event.isAcknowledged()) {
            event.deferEdit().queue();
        }
        update(event.getGuild(), false);
    }

    private void sticky(GuildSlashCommandEvent event) {
        if (event.getGuild().getAudioManager().getConnectedChannel() == null) {
            event.replyEphemeral("Not connected to a voice channel").queue();
            return;
        }
        GuildMusicManager manager = getGuildAudioPlayer(event.getGuild());
        event.getGuild()
                .getAudioManager()
                .getConnectedChannel()
                .asVoiceChannel()
                .sendMessageEmbeds(new EmbedBuilder().setDescription("Loading...").build())
                .setComponents(
                        ActionRow.of(
                                Button.secondary("music:volume:down", Emoji.fromUnicode("\uD83D\uDD09"))
                                        .withLabel("Volume Down")
                                        .withDisabled(manager.player.getVolume() <= 0),
                                Button.danger("music:stop", Emoji.fromUnicode("\u23F9"))
                                        .withLabel("Stop"),
                                Button.primary("music:pause", Emoji.fromUnicode("\u23F8"))
                                        .withLabel("Pause"),
//                                        Button.primary("music:play", Emoji.fromUnicode("\u25B6")).asDisabled(),
                                Button.primary("music:skip", Emoji.fromUnicode("\u23ED"))
                                        .withLabel("Skip"),
                                Button.secondary("music:volume:up", Emoji.fromUnicode("\uD83D\uDD0A"))
                                        .withLabel("Volume Up")
                                        .withDisabled(manager.player.getVolume() >= 150)
                        ),
                        ActionRow.of(
                                Button.secondary("music:shuffle", Emoji.fromUnicode("\uD83D\uDD00"))
                                        .withLabel("Shuffle"),
                                Button.secondary("music:repeat", Emoji.fromUnicode("\uD83D\uDD01"))
                                        .withLabel("Repeat")
                                        .asDisabled(),
                                Button.secondary("music:queue", Emoji.fromUnicode("\uD83D\uDCDA"))
                                        .withLabel("Queue")
                                        .asDisabled(),
                                Button.secondary("music:lyrics", Emoji.fromUnicode("\uD83C\uDFB6"))
                                        .withLabel("Lyrics"),
                                InteractionProcessor.getInteraction("delete").orElseThrow().component()
                        )
                )
                .map(ISnowflake::getIdLong)
                .queue(messageId -> {
                    manager.setMessageId(messageId);
                    update(event.getGuild(), false, unused -> update(event.getGuild(), true));
                });
        event.replyEphemeral("Sticky now playing sent in " + event.getGuild().getAudioManager().getConnectedChannel().getAsMention())
                .queue();
    }

    private void update(Guild guild) {
        update(guild, false);
    }

    private void update(Guild guild, boolean relaunch) {
        update(guild, relaunch, null);
    }
    // endregion

    private void update(Guild guild, boolean relaunch, Consumer<Message> queueSuccess) {
        GuildMusicManager player = getGuildAudioPlayer(guild);
        if (player.messageId() == -1 || guild.getAudioManager().getConnectedChannel() == null) {
            return;
        }
        RestAction<Message> map = guild.getAudioManager()
                .getConnectedChannel()
                .asVoiceChannel()
                .retrieveMessageById(player.messageId())
                .flatMap(message -> message.editMessageEmbeds(NowPlayingEmbedBuilder.fromPlayer(new EmbedBuilder().setTimestamp(Instant.now()), MusicModule.this, guild).build()));
        if (relaunch) {
            map.queueAfter(1, TimeUnit.MINUTES, queueSuccess.andThen(unused -> update(guild, false)));
        } else {
            map.queue(queueSuccess);
        }
    }

    private static class TrackViewer implements AudioLoadResultHandler {

        protected final Consumer<AudioTrack> trackConsumer;
        protected final Consumer<AudioPlaylist> searchResultConsumer;
        protected final BiConsumer<Integer, AudioPlaylist> playlistConsumer;
        protected final Consumer<Void> noMatchesConsumer;

        protected final Consumer<FriendlyException> loadFailedConsumer;

        public TrackViewer(Consumer<AudioTrack> trackConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer) {
            this(trackConsumer, null, playlistConsumer, noMatchesConsumer, null);
        }

        public TrackViewer(Consumer<AudioTrack> trackConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer, Consumer<FriendlyException> loadFailedConsumer) {
            this(trackConsumer, null, playlistConsumer, noMatchesConsumer, loadFailedConsumer);
        }

        public TrackViewer(Consumer<AudioTrack> trackConsumer, Consumer<AudioPlaylist> searchResultConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer) {
            this(trackConsumer, searchResultConsumer, playlistConsumer, noMatchesConsumer, null);
        }

        public TrackViewer(Consumer<AudioTrack> trackConsumer, Consumer<AudioPlaylist> searchResultConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer, Consumer<FriendlyException> loadFailedConsumer) {
            this.trackConsumer = trackConsumer;
            this.searchResultConsumer = searchResultConsumer;
            this.playlistConsumer = playlistConsumer;
            this.noMatchesConsumer = noMatchesConsumer;
            this.loadFailedConsumer = loadFailedConsumer;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            if (trackConsumer != null) {
                trackConsumer.accept(track);
            }
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.isSearchResult() && searchResultConsumer != null) {
                searchResultConsumer.accept(playlist);
                return;
            }
            int first = playlist.getSelectedTrack() == null ? 0 : playlist.getTracks().indexOf(playlist.getSelectedTrack());
            if (playlistConsumer != null) {
                playlistConsumer.accept(first, playlist);
            }
        }

        @Override
        public void noMatches() {
            if (noMatchesConsumer != null) {
                noMatchesConsumer.accept(null);
            }
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            if (loadFailedConsumer != null) {
                loadFailedConsumer.accept(exception);
            }
        }
    }

    private static class NowPlayingEmbedBuilder {

        public static final  int    QUEUE_SIZE         = 5;
        private static final String QUEUE_TRACK_FORMAT = "[%s](%s) by %s";

        private final @NotNull JDA jda;

        private final @NotNull EmbedBuilder builder;
        private @Nullable String title;
        private @Nullable String url;
        private @Nullable String thumbnail;
        private @Nullable String author;
        private @Nullable String authorUrl;
        private @Nullable String authorIcon;
        private @Nullable String footer;
        private @Nullable String footerIcon;
        private @Nullable Long position;
        private @Nullable Long duration = -1L;
        private @Nullable Long queueDuration = -1L;
        private @Nullable Integer volume = 0;
        private @Nullable List<String> queuedTracks;
        private @Nullable Boolean paused;

        public NowPlayingEmbedBuilder(@NotNull JDA jda) {
            this(jda, new EmbedBuilder());
        }

        public NowPlayingEmbedBuilder(@NotNull JDA jda, @NotNull EmbedBuilder builder) {
            this.jda = jda;
            this.builder = builder;
        }

        public static NowPlayingEmbedBuilder fromPlayer(MusicModule musicModule, Guild guild) {
            return fromPlayer(musicModule, guild, true);
        }

        public static NowPlayingEmbedBuilder fromPlayer(MusicModule musicModule, Guild guild, boolean waitLoadedPlayer) {
            return fromPlayer(new EmbedBuilder(), musicModule, guild, waitLoadedPlayer);
        }

        public static NowPlayingEmbedBuilder fromPlayer(EmbedBuilder builder, MusicModule musicModule, Guild guild) {
            return fromPlayer(builder, musicModule, guild, true);
        }

        public static NowPlayingEmbedBuilder fromPlayer(EmbedBuilder builder, MusicModule musicModule, Guild guild, boolean waitLoadedPlayer) {
            while (musicModule.getGuildAudioPlayer(guild).player.getPlayingTrack() == null && waitLoadedPlayer) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            GuildMusicManager player = musicModule.getGuildAudioPlayer(guild);
            AudioTrack track = player.player.getPlayingTrack();
            if (track == null) {
                return new NowPlayingEmbedBuilder(guild.getJDA());
            }
            NowPlayingEmbedBuilder embedBuilder = new NowPlayingEmbedBuilder(guild.getJDA(), builder)
                    .setTitle(track.getInfo().title, track.getInfo().uri)
                    .setAuthor(track.getInfo().author, null, null)
                    .setTime(track.getPosition(), track.getInfo().isStream ? -1 : track.getDuration(), player.scheduler.getQueue().stream().mapToLong(AudioTrack::getDuration).sum())
                    .setVolume(player.player.getVolume())
                    .setFooter(track.getIdentifier() + " from " + track.getSourceManager().getSourceName(), null)
                    .setQueuedTracks(player.scheduler.getQueue()
                            .stream()
                            .map(AudioTrack::getInfo)
                            .map(info -> String.format(QUEUE_TRACK_FORMAT, info.title, info.uri, info.author))
                            .collect(Collectors.toList()));
            if (track.getSourceManager().getSourceName().equals("youtube")) {
                embedBuilder.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/0.jpg");
            }
            return embedBuilder;
        }

        public NowPlayingEmbedBuilder setTitle(@Nullable String title, @Nullable String url) {
            this.title = title;
            this.url = url;
            return this;
        }

        public NowPlayingEmbedBuilder setThumbnail(@Nullable String thumbnail) {
            this.thumbnail = thumbnail;
            return this;
        }

        public NowPlayingEmbedBuilder setAuthor(@Nullable String author, @Nullable String authorUrl, @Nullable String authorIcon) {
            this.author = author;
            this.authorUrl = authorUrl;
            this.authorIcon = authorIcon;
            return this;
        }

        public NowPlayingEmbedBuilder setFooter(@Nullable String footer, @Nullable String footerIcon) {
            this.footer = footer;
            this.footerIcon = footerIcon;
            return this;
        }

        public NowPlayingEmbedBuilder setTime(@Nullable Long position, @Nullable Long duration, @Nullable Long queueDuration) {
            this.position = position;
            this.duration = duration;
            this.queueDuration = queueDuration;
            return this;
        }

        public NowPlayingEmbedBuilder setVolume(@Nullable Integer volume) {
            this.volume = volume;
            return this;
        }

        public NowPlayingEmbedBuilder setQueuedTracks(@Nullable List<String> queuedTracks) {
            this.queuedTracks = queuedTracks;
            return this;
        }

        public NowPlayingEmbedBuilder addQueuedTracks(@NotNull String... queuedTrack) {
            return addQueuedTracks(Arrays.asList(queuedTrack));
        }

        public NowPlayingEmbedBuilder addQueuedTracks(@NotNull List<String> queuedTracks) {
            Checks.notNull(queuedTracks, "Queued tracks");
            if (this.queuedTracks == null) {
                this.queuedTracks = new ArrayList<>();
            }
            this.queuedTracks.addAll(queuedTracks);
            return this;
        }

        public NowPlayingEmbedBuilder addQueuedTrack(@NotNull String title, @NotNull String url, @NotNull String author) {
            return addQueuedTracks(String.format(QUEUE_TRACK_FORMAT, title, url, author));
        }

        public NowPlayingEmbedBuilder setState(@Nullable Boolean paused, @Nullable Boolean repeat) {
            this.paused = paused;
            this.repeat = repeat;
            return this;
        }

        @NotNull
        public MessageEmbed build() {
            Checks.notNull(jda, "JDA");
            Checks.notNull(builder, "EmbedBuilder");

            position = position == null ? 0 : position;
            duration = duration == null ? -1 : duration;
            queueDuration = queueDuration == null ? 0 : queueDuration;
            volume = volume == null ? 100 : volume;
            queuedTracks = queuedTracks == null ? Collections.emptyList() : queuedTracks;
            paused = paused != null && paused;
            repeat = repeat != null && repeat;

            builder.setTitle(title, url)
                    .setAuthor(author, authorUrl, authorIcon)
                    .setThumbnail(thumbnail)
                    .appendDescription(formatTime(position) + " " + buildProgressBar(duration < 0, position, duration, 15, "─", "⊚", "─") + " " + (duration < 0 ? "-" + formatTime(position) : formatTime(duration)))
                    .appendDescription("\nVolume: " + buildProgressBar(volume, 150, 15, "▮", "▯") + getCommandMention(jda, "volume").map(mention -> " *Edit with " + mention + "*").orElse(""))
                    .appendDescription("\nRemaining: " + (duration < 0 ? "∞" : formatTime((duration - position) + queueDuration)))
                    .setFooter(footer, footerIcon);
            if (!queuedTracks.isEmpty()) {
                builder.addField("Queue", queuedTracks.stream()
                        .limit(QUEUE_SIZE)
                        .collect(Collectors.joining("\n")) +
                        "\n*And **" + (queuedTracks.size() - QUEUE_SIZE) + "** more..." +
                        getCommandMention(jda, "queue").map(mention -> "\nMore details with " + mention)
                                .orElse("") + "*", false);
            }
            return builder.build();
        }
    }

    public class Playlist {
        List<PlaylistObj> playlists = new ArrayList<>();

        public void loadAll(Connection sql) throws SQLException {
            sql.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS playlists (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "user_id BIGINT NOT NULL," +
                            "name VARCHAR(255) NOT NULL," +
                            "track_reference VARCHAR(255) NOT NULL," +
                            "track_title VARCHAR(255) NOT NULL," +
                            "track_uri VARCHAR(255) NOT NULL," +
                            "stamp_cached BIGINT NOT NULL" +
                            ")");

            playlists = PlaylistObj.loadAll(sql);
        }

        private PlaylistObj get(long userId, String name) {
            return get(userId, name, true);
        }

        @Nullable
        private PlaylistObj get(long userId, String name, boolean create) {
            return playlists.stream()
                    .filter(playlistObj -> playlistObj.userId == userId && playlistObj.name.equals(name))
                    .findFirst()
                    .orElseGet(() -> {
                        if (create) {
                            PlaylistObj playlistObj = new PlaylistObj(userId, name, new ArrayList<>());
                            playlists.add(playlistObj);
                            return playlistObj;
                        }
                        return null;
                    });
        }

        @SlashCommand.Sub
        public void addAll(GuildSlashCommandEvent event,
                           @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name) {
            event.deferReplyEphemeral().queue();
            GuildMusicManager player = getGuildAudioPlayer(event.getGuild());
            if (player.scheduler.getQueue().isEmpty()) {
                event.replyEphemeral("Queue is empty").queue();
                return;
            }
            PlaylistObj obj = get(event.getUser().getIdLong(), name);
            obj.add(sql, new CachedTrack(player.player.getPlayingTrack()));
            obj.addAll(sql, player.scheduler.getQueue().stream().map(CachedTrack::new).toList());
            event.getHook().editOriginal("Added " + player.scheduler.getQueue().size() + " tracks to playlist " + name).queue();
        }

        @SlashCommand.Sub
        public void remove(GuildSlashCommandEvent event,
                           @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name,
                           @SlashCommand.Option String trackName) {
            get(event.getUser().getIdLong(), name).remove(sql, cache.get(t -> t.title().equalsIgnoreCase(trackName)).orElseThrow());
            event.replyEphemeral("Removed track from playlist " + name).queue();
        }

        @SlashCommand.Sub
        public void play(GuildSlashCommandEvent event,
                         @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name) {
            GuildMusicManager player = getGuildAudioPlayer(event.getGuild());
            PlaylistObj obj = get(event.getUser().getIdLong(), name);
            List<String> playlist = obj.references();
            if (playlist.isEmpty()) {
                event.replyEphemeral("Playlist is empty").queue();
                return;
            }
            event.replyEphemeral("Loading " + playlist.size() + " tracks from playlist " + name).queue();
            for (int i = 0, playlistSize = playlist.size(); i < playlistSize; i++) {
                int finalI = i;
                CachedTrack track = cache.getOrCreate(t -> t.identifier().equals(playlist.get(finalI)), playlist.get(finalI));
                String trackUrl = track.url();
                TrackLoader loader = new TrackLoader(
                        event.getMember().getVoiceState().getChannel(),
                        trackLoaded -> {
                            event.getHook()
                                    .editOriginal("Loading tracks from " + name + " playlist [" + (finalI + 1) + "/" + playlistSize + "]\n" +
                                            "[" + buildProgressBar(finalI, playlistSize - 1, 20, "▮", "▯") + "]")
                                    .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                                    .setCheck(() -> finalI % (playlistSize / 10) == 0)
                                    .queue(unused -> update(event.getGuild()));
                            cache.addOrUpdate(new CachedTrack(trackLoaded));
                            if (finalI == playlistSize - 1) {
                                event.getHook()
                                        .editOriginal("Loaded " + playlistSize + " tracks from playlist " + name)
                                        .setEmbeds(NowPlayingEmbedBuilder.fromPlayer(MusicModule.this, event.getGuild()).build())
                                        .queue(unused -> update(event.getGuild()));
                            }
                        },
                        null,
                        unused -> event.getHook().editOriginal("Nothing found by " + trackUrl).queue(),
                        exception -> event.getHook().editOriginal("Could not play: " + exception.getMessage()).queue()
                );
                playerManager.loadItemOrdered(musicManagers, track.identifier(), loader);
            }
            event.getHook().editOriginal("Added " + playlist.size() + " tracks to queue").queue();
        }

        @SlashCommand.Sub
        public void view(GuildSlashCommandEvent event,
                         @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name) {
            List<String> references = get(event.getUser().getIdLong(), name).references();
            if (references.isEmpty()) {
                event.replyEphemeral("Playlist is empty").queue();
                return;
            }
            StringJoiner joiner = new StringJoiner("\n");
            for (int i = 0, playlistSize = references.size(); i < playlistSize; i++) {
                int finalI = i;
                Cache.Entry<CachedTrack> entry = cache.getWithMeta(t -> t.identifier().equals(references.get(finalI))).orElseThrow();
                CachedTrack track = entry.value();
                joiner.add((i + 1) + ". [" + track.title() + "](<" + track.url() + ">) _cached " + TimeFormat.RELATIVE.format(entry.timestamp()) + "_");
            }
            String toString = joiner.toString();
            while (toString.length() >= Message.MAX_CONTENT_LENGTH) {
                toString = toString.substring(0, toString.lastIndexOf("\n"));
            }
            event.replyEphemeral(toString)
                    .queue();
        }

        @SlashCommand.Sub
        public void clear(GuildSlashCommandEvent event,
                          @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name) {
            PlaylistObj obj = get(event.getUser().getIdLong(), name);
            if (obj.references().isEmpty()) {
                event.replyEphemeral("Playlist is empty").queue();
                return;
            }
            obj.clear(sql);
            event.replyEphemeral("Cleared playlist " + name).queue();
        }

        @SlashCommand.Sub
        public void delete(GuildSlashCommandEvent event,
                           @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name) {
            PlaylistObj obj = get(event.getUser().getIdLong(), name, false);
            if (obj == null) {
                event.replyEphemeral("Playlist does not exist").queue();
                return;
            }
            obj.delete(sql);
            playlists.remove(obj);
            event.replyEphemeral("Deleted playlist " + name).queue();
        }

        @SlashCommand.Sub
        public void rename(GuildSlashCommandEvent event,
                           @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("playlistAutocomplete"))) String name,
                           @SlashCommand.Option String newName) {
            PlaylistObj obj = get(event.getUser().getIdLong(), name, false);
            if (obj == null) {
                event.replyEphemeral("Playlist does not exist").queue();
                return;
            }
            playlists.remove(obj);
            playlists.add(obj.rename(sql, newName));
            event.replyEphemeral("Renamed playlist " + name + " to " + newName).queue();
        }

        @SlashCommand.Sub
        public void list(GuildSlashCommandEvent event) {
            List<String> playlistNames = playlists.stream()
                    .filter(p -> p.userId == event.getUser().getIdLong())
                    .map(PlaylistObj::name)
                    .collect(Collectors.toList());
            if (playlistNames.isEmpty()) {
                event.replyEphemeral("No playlists found").queue();
                return;
            }
            event.replyEphemeral(String.join("\n", playlistNames)).queue();
        }

        public String[] playlistAutocomplete(CommandAutoCompleteInteractionEvent event) {
            return playlists.stream()
                    .filter(p -> p.userId == event.getUser().getIdLong())
                    .map(PlaylistObj::name)
                    .filter(s -> s.startsWith(event.getFocusedOption().getValue()))
                    .toArray(String[]::new);
        }

        record PlaylistObj(long userId, String name, List<String> references) {

            public static final BiFunction<Long, String, Predicate<PlaylistObj>> GETTER_FILTER = (userId, name) -> p -> p.userId == userId && p.name.equals(name);

            public static List<PlaylistObj> loadAll(Connection sql) {
                List<PlaylistObj> playlists = new ArrayList<>();
                try (ResultSet result = sql.createStatement().executeQuery("SELECT * FROM playlists")) {
                    while (result.next()) {
                        long userId = result.getLong("user_id");
                        String name = result.getString("name");
                        playlists.stream()
                                .filter(GETTER_FILTER.apply(userId, name))
                                .findFirst()
                                .orElseGet(() -> {
                                    PlaylistObj playlistObj = new PlaylistObj(userId, name, new ArrayList<>());
                                    playlists.add(playlistObj);
                                    return playlistObj;
                                })
                                .references()
                                .add(result.getString("track_reference"));
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                return playlists;
            }

            public PlaylistObj rename(Connection sql, String newName) {
                try (PreparedStatement statement = sql.prepareStatement("UPDATE playlists SET name = ? WHERE user_id = ? AND name = ?")) {
                    statement.setString(1, newName);
                    statement.setLong(2, userId);
                    statement.setString(3, name);
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                return new PlaylistObj(userId, newName, references);
            }

            public void save(Connection sql) {
                try (PreparedStatement statement = sql.prepareStatement("INSERT INTO playlists (user_id, name, track_reference) VALUES (?, ?, ?)")) {
                    statement.setLong(1, userId);
                    statement.setString(2, name);
                    for (String reference : references) {
                        statement.setString(3, reference);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void delete(Connection sql) {
                try (Statement statement = sql.createStatement()) {
                    statement.executeUpdate("DELETE FROM playlists WHERE user_id = " + userId + " AND name = '" + name + "'");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            public void addAll(Connection sql, Collection<? extends CachedTrack> tracks) {
                this.references.addAll(tracks.stream().map(CachedTrack::identifier).toList());
                save(sql);
            }

            public void add(Connection sql, CachedTrack track) {
                references.add(track.identifier());
                save(sql);
            }

            public void remove(Connection sql, CachedTrack track) {
                references.remove(track.identifier());
                try (PreparedStatement statement = sql.prepareStatement("DELETE FROM playlists WHERE user_id = ? AND name = ? AND track_reference = ?")) {
                    statement.setLong(1, userId);
                    statement.setString(2, name);
                    statement.setString(3, track.identifier());
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void removeIf(Connection sql, Predicate<? super String> filter) {
                try (PreparedStatement statement = sql.prepareStatement("DELETE FROM playlists WHERE user_id = ? AND name = ? AND track_uri = ?")) {
                    statement.setLong(1, userId);
                    statement.setString(2, name);
                    for (String reference : references.stream().filter(filter).toList()) {
                        statement.setString(3, reference);
                        statement.addBatch();
                        references.remove(reference);
                    }
                    statement.executeBatch();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public void clear(Connection sql) {
                references.clear();
                try (PreparedStatement statement = sql.prepareStatement("DELETE FROM playlists WHERE user_id = ? AND name = ?")) {
                    statement.setLong(1, userId);
                    statement.setString(2, name);
                    statement.executeUpdate();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

        }

    }

    // region Helper classes
    private class TrackLoader extends TrackViewer {


        private final AudioChannel audioChannel;

        public TrackLoader(AudioChannel audioChannel, Consumer<AudioTrack> trackConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer) {
            super(trackConsumer, playlistConsumer, noMatchesConsumer);
            this.audioChannel = audioChannel;
        }

        public TrackLoader(AudioChannel audioChannel, Consumer<AudioTrack> trackConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer, Consumer<FriendlyException> loadFailedConsumer) {
            super(trackConsumer, playlistConsumer, noMatchesConsumer, loadFailedConsumer);
            this.audioChannel = audioChannel;
        }

        public TrackLoader(AudioChannel audioChannel, Consumer<AudioTrack> trackConsumer, Consumer<AudioPlaylist> searchResultConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer) {
            super(trackConsumer, searchResultConsumer, playlistConsumer, noMatchesConsumer);
            this.audioChannel = audioChannel;
        }

        public TrackLoader(AudioChannel audioChannel, Consumer<AudioTrack> trackConsumer, Consumer<AudioPlaylist> searchResultConsumer, BiConsumer<Integer, AudioPlaylist> playlistConsumer, Consumer<Void> noMatchesConsumer, Consumer<FriendlyException> loadFailedConsumer) {
            super(trackConsumer, searchResultConsumer, playlistConsumer, noMatchesConsumer, loadFailedConsumer);
            this.audioChannel = audioChannel;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            play(audioChannel, getGuildAudioPlayer(audioChannel.getGuild()), track);
            super.trackLoaded(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.isSearchResult() && searchResultConsumer != null) {
                searchResultConsumer.accept(playlist);
                return;
            }
            int first = playlist.getSelectedTrack() == null ? 0 : playlist.getTracks().indexOf(playlist.getSelectedTrack());
            playlist.getTracks().subList(first, playlist.getTracks().size()).forEach(track -> play(audioChannel, getGuildAudioPlayer(audioChannel.getGuild()), track));
            if (playlistConsumer != null) {
                playlistConsumer.accept(first, playlist);
            }
        }

    }
    // endregion
}
