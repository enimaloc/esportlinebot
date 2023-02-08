/*
 * TrackScheduler
 *
 * 0.0.1
 *
 * 20/12/2022
 */
package fr.enimaloc.esportlinebot.module.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 *
 */
public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer               player;
    private final BlockingQueue<AudioTrack> queue;
    private final Consumer<AudioTrack>      onNextTrack;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this(player, null);
    }

    /**
     * @param player      The audio player this scheduler uses
     * @param onNextTrack
     */
    public TrackScheduler(AudioPlayer player, Consumer<AudioTrack> onNextTrack) {
        this.player = player;
        this.onNextTrack = onNextTrack;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.
        AudioTrack next = queue.poll();
        player.startTrack(next, false);
        if (onNextTrack != null) {
            onNextTrack.accept(next);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    public List<AudioTrack> getQueue() {
        return queue.stream().toList();
    }

    public void shuffleQueue() {
        List<AudioTrack> tracks = new ArrayList<>(queue.stream().toList());
        Collections.shuffle(tracks);
        queue.clear();
        queue.addAll(tracks);
    }

    public void clearQueue() {
        queue.clear();
    }
}
