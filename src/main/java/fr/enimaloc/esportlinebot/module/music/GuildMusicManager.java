/*
 * GuildMusicManager
 *
 * 0.0.1
 *
 * 20/12/2022
 */
package fr.enimaloc.esportlinebot.module.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.function.Consumer;

/**
 *
 */
public abstract class GuildMusicManager {
    /**
     * Audio player for the guild.
     */
    public final AudioPlayer    player;
    /**
     * Track scheduler for the player.
     */
    public final TrackScheduler scheduler;

    private long messageId = -1;

    /**
     * Creates a player and a track scheduler.
     *
     * @param manager     Audio player manager to use for creating the player.
     * @param onNextTrack
     */
    public GuildMusicManager(AudioPlayerManager manager, Consumer<AudioTrack> onNextTrack) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player, onNextTrack.andThen(t -> save()));
        player.addListener(scheduler);
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }

    public long messageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public abstract void load();

    public abstract void save();
}
