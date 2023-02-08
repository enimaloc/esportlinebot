package fr.enimaloc.esportlinebot.module.music;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

record CachedTrack(String identifier, String title, String url, String author, long duration) {
    public CachedTrack(AudioTrack track) {
        this(
                track.getIdentifier(),
                track.getInfo().title,
                track.getInfo().uri,
                track.getInfo().author,
                track.getInfo().isStream ? -1 : track.getInfo().length
        );
    }

    public AudioReference audioReference() {
        return new AudioReference(url, title);
    }
}
