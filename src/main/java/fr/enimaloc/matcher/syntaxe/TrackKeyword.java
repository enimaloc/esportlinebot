package fr.enimaloc.matcher.syntaxe;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.enimaloc.esportlinebot.module.music.GuildMusicManager;
import fr.enimaloc.matcher.Matcher;

public class TrackKeyword {

    public static final String TRACK_KEY = "internal.track";
    public static final String TRACK_OVERRIDE_TITLE_KEY = "internal.track.override.title";
    public static final String TRACK_OVERRIDE_AUTHOR_KEY = "internal.track.override.author";
    public static final String TRACK_OVERRIDE_DURATION_KEY = "internal.track.override.duration";
    public static final String TRACK_OVERRIDE_URI_KEY = "internal.track.override.uri";
    public static final String TRACK_OVERRIDE_IDENTIFIER_KEY = "internal.track.override.identifier";
    public static final String TRACK_OVERRIDE_IS_STREAM_KEY = "internal.track.override.isStream";
    public static final String TRACK_OVERRIDE_POSITION_KEY = "internal.track.override.position";

    TrackKeyword() {
    }

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                title(),
                author(),
                duration(),
                uri(),
                identifier(),
                isStream(),
                position()
        };
    }

    private static AudioTrack getTrack(Matcher matcher) {
        return matcher.getenv().containsKey(TRACK_KEY) ? (AudioTrack) matcher.getenv().get(TRACK_KEY) : null;
    }

    public static Keyword title() {
        return new Keyword("music.track.title", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_TITLE_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_TITLE_KEY));
            }
            return getTrack(matcher) == null ? "" : getTrack(matcher).getInfo().title;
        });
    }

    public static Keyword author() {
        return new Keyword("music.track.author", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_AUTHOR_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_AUTHOR_KEY));
            }
            return getTrack(matcher) == null ? "" : getTrack(matcher).getInfo().author;
        });
    }

    public static Keyword duration() {
        return new Keyword("music.track.duration", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_DURATION_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_DURATION_KEY));
            }
            return getTrack(matcher) == null ? "0" : String.valueOf(getTrack(matcher).getDuration());
        });
    }

    public static Keyword uri() {
        return new Keyword("music.track.uri", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_URI_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_URI_KEY));
            }
            return getTrack(matcher) == null ? "" : getTrack(matcher).getInfo().uri;
        });
    }

    public static Keyword identifier() {
        return new Keyword("music.track.identifier", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_IDENTIFIER_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_IDENTIFIER_KEY));
            }
            return getTrack(matcher) == null ? "" : getTrack(matcher).getInfo().identifier;
        });
    }

    public static Keyword isStream() {
        return new Keyword("music.track.isStream", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_IS_STREAM_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_IS_STREAM_KEY));
            }
            return getTrack(matcher) == null ? "false" : String.valueOf(getTrack(matcher).getInfo().isStream);
        });
    }

    public static Keyword position() {
        return new Keyword("music.track.position", (matcher, instruction) -> {
            if (matcher.getenv().containsKey(TRACK_OVERRIDE_POSITION_KEY)) {
                return String.valueOf(matcher.getenv().get(TRACK_OVERRIDE_POSITION_KEY));
            }
            return getTrack(matcher) == null ? "0" : String.valueOf(getTrack(matcher).getPosition());
        });
    }
}

