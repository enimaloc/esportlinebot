package fr.enimaloc.matcher.syntaxe;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.enimaloc.esportlinebot.module.music.GuildMusicManager;

import java.util.Arrays;
import java.util.stream.Stream;

public class MusicPlayerKeyword {

    public static final String GUILD_MUSIC_MANAGER_KEY = "internal.music.audioPlayer";
    public static final String REGISTERED_VOLUME_KEY = "registered.music.volume";
    public static final String REGISTERED_POSITION_KEY = "registered.music.position";
    public static final String REGISTERED_DURATION_TRACK_KEY = "registered.music.track.duration";
    public static final String REGISTERED_DURATION_LIST_KEY = "registered.music.list.duration";

    public static Keyword[] getKeywords() {
        Keyword[] playerKeywords = {
                playerVolume(),
                playerIsPaused(),
                playerIsPlaying(),
                playerIsStopped(),
                listDuration()
        };
        Keyword[] array = Stream.concat(Arrays.stream(playerKeywords), Arrays.stream(TrackKeyword.getKeywords())).toArray(Keyword[]::new);
        return Stream.concat(Arrays.stream(array), Arrays.stream(QueueKeyword.getKeywords())).toArray(Keyword[]::new);
    }

    public static Keyword playerVolume() {
        return new Keyword("music.player.volume", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(REGISTERED_VOLUME_KEY)) {
                return String.valueOf(matcher.getenv().get(REGISTERED_VOLUME_KEY));
            }
            AudioPlayer player = ((GuildMusicManager) matcher.getenv().get(GUILD_MUSIC_MANAGER_KEY)).player;
            return player == null ? "0" : String.valueOf(player.getVolume());
        });
    }

    public static Keyword playerIsPaused() {
        return new Keyword("music.player.isPaused", (matcher, instruction) -> {
            AudioPlayer player = ((GuildMusicManager) matcher.getenv().get(GUILD_MUSIC_MANAGER_KEY)).player;
            return player == null ? "false" : String.valueOf(player.isPaused());
        });
    }

    public static Keyword playerIsPlaying() {
        return new Keyword("music.player.isPlaying", (matcher, instruction) -> {
            AudioPlayer player = ((GuildMusicManager) matcher.getenv().get(GUILD_MUSIC_MANAGER_KEY)).player;
            return player == null ? "false" : String.valueOf(player.getPlayingTrack() != null);
        });
    }

    public static Keyword playerIsStopped() {
        return new Keyword("music.player.isStopped", (matcher, instruction) -> {
            AudioPlayer player = ((GuildMusicManager) matcher.getenv().get(GUILD_MUSIC_MANAGER_KEY)).player;
            return player == null ? "true" : String.valueOf(player.getPlayingTrack() == null);
        });
    }

    public static Keyword listDuration() {
        return new Keyword("music.list.duration", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(REGISTERED_DURATION_LIST_KEY)) {
                return String.valueOf(matcher.getenv().get(REGISTERED_DURATION_LIST_KEY));
            }
            GuildMusicManager manager = ((GuildMusicManager) matcher.getenv().get(GUILD_MUSIC_MANAGER_KEY));
            return manager.player == null ? "0" : String.valueOf(manager.player.getPlayingTrack().getPosition()
                    - manager.player.getPlayingTrack().getDuration()
                    + manager.scheduler.getQueue().stream().mapToLong(AudioTrack::getDuration).sum());
        });
    }

}
