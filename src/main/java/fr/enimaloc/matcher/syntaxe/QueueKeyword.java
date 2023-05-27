package fr.enimaloc.matcher.syntaxe;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.enimaloc.matcher.Matcher;

import java.util.List;

public class QueueKeyword {

    public static final String QUEUE_KEY = "internal.queue";

    QueueKeyword() {
    }

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                size(),
                duration()
        };
    }

    private static Keyword duration() {
        return new Keyword("music.queue.duration", (matcher, instruction) -> String.valueOf(getQueue(matcher).stream().mapToLong(track -> track.getInfo().length).sum()));
    }

    private static Keyword size() {
        return new Keyword("music.queue.size", (matcher, instruction) -> String.valueOf(getQueue(matcher).size()));
    }

    private static List<AudioTrack> getQueue(Matcher matcher) {
        //noinspection unchecked
        return (List<AudioTrack>) matcher.getenv().get(QUEUE_KEY);
    }
}
