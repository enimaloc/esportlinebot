package fr.enimaloc.esportlinebot.toml.customization;

import com.electronwill.nightconfig.core.file.FileConfig;
import fr.enimaloc.esportlinebot.toml.TomlReader;
import fr.enimaloc.matcher.Matcher;
import fr.enimaloc.matcher.syntaxe.*;
import fr.enimaloc.matcher.syntaxe.predefined.*;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static net.dv8tion.jda.api.interactions.DiscordLocale.*;

public class Customization extends TomlReader {
    public static final Logger LOGGER = LoggerFactory.getLogger(Customization.class);

    public static Matcher getMatcher(TomlReader instance) {
        return getMatcher(Collections.emptyMap(), instance);
    }

    public static Matcher getMatcher(Map<String, Object> env, TomlReader instance) {
        Matcher matcher = new Matcher();
        matcher.getenv().putAll(env);
        matcher.getenv().put(CustomizationKeyword.TOML_PATH_KEY, instance.config);
        matcher.register(ComparaisonKeyword.getKeywords());
        matcher.register(VariableKeyword.getKeywords());
        matcher.register(MathKeyword.getKeywords());
        matcher.register(LogicalKeyword.getKeywords());
        matcher.register(BitwiseKeyword.getKeywords());
        matcher.register(FunctionKeyword.getKeywords());
        matcher.register(StringKeyword.getKeywords());
        matcher.register(StringUtilsKeyword.getKeywords());
        matcher.register(DateTimeKeyword.getKeywords());
        matcher.register(JDAKeyword.getKeywords());
        matcher.register(MusicPlayerKeyword.getKeywords());
        matcher.register(MatcherKeyword.getKeywords());
        matcher.register(CustomizationKeyword.getKeywords());
        return matcher;
    }

    @SettingsEntry
    public Music music = new Music(this);

    public Customization(Path path) {
        super(FileConfig.builder(path).autosave().concurrent().build());
    }

    public static class Music extends TomlReader {
        @SettingsEntry
        public NowPlaying nowPlaying = new NowPlaying(this);

        protected Music(TomlReader parent) {
            super("music", parent);
        }

        public static class NowPlaying extends TomlReader {
            @SettingsEntry
            public Button button = new Button(this);
            @SettingsEntry
            public Map<DiscordLocale, String> embedDescription = new EnumMap<>(Map.of(
                    ENGLISH_UK, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nVolume: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Edit with {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nRemaining: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}",
                    ENGLISH_US, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nVolume: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Edit with {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nRemaining: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}",
                    FRENCH, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nVolume: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Modifier avec {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nRestant: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}",
                    GERMAN, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nLautstärke: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Bearbeiten mit {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nVerbleibend: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}",
                    ITALIAN, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nVolume: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Modifica con {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nRimanente: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}",
                    SPANISH, "{time.format:{music.track.position}} {strutils.progressBar:15:─:⊚:─:{music.track.duration}:{music.track.position}:{if.lower:{music.track.position}:0}} {if:{if.lower:{music.track.position}:0}:-:}{time.format:{music.track.duration}}" +
                            "\nVolumen: {strutils.progressBar:15:▮:▮:▯:150:{music.player.volume}} _Editar con {jda.commandMention:music.volume:`/music volume`}_" +
                            "\nRestante: {if:{if.lower:{music.list.duration}:0}:∞:{time.format:{music.list.duration}}}"
            ));
            @SettingsEntry
            public Map<DiscordLocale, String> queue = new EnumMap<>(Map.of(
                    ENGLISH_UK, "Queue",
                    ENGLISH_US, "Queue",
                    FRENCH, "File d'attente",
                    GERMAN, "Warteschlange",
                    ITALIAN, "Coda",
                    SPANISH, "Cola"
            ));
            @SettingsEntry
            public Map<DiscordLocale, String> queueTitle = new EnumMap<>(Map.of(
                    ENGLISH_UK, "[{music.track.title}]({music.track.uri}) by {music.track.author}",
                    ENGLISH_US, "[{music.track.title}]({music.track.uri}) by {music.track.author}",
                    FRENCH, "[{music.track.title}]({music.track.uri}) par {music.track.author}",
                    GERMAN, "[{music.track.title}]({music.track.uri}) von {music.track.author}",
                    ITALIAN, "[{music.track.title}]({music.track.uri}) di {music.track.author}",
                    SPANISH, "[{music.track.title}]({music.track.uri}) por {music.track.author}"
            ));
            @SettingsEntry
            public Map<DiscordLocale, String> queueMore = new EnumMap<>(Map.of(
                    ENGLISH_UK, "_and **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** more..._\n_More details with {jda.commandMention:music.queue:`/music queue`}_",
                    ENGLISH_US, "_and **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** more..._\n_More details with {jda.commandMention:music.queue:`/music queue`}_",
                    FRENCH, "_et **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** autres..._\n_Plus de détails avec {jda.commandMention:music.queue:`/music queue`}_",
                    GERMAN, "_und **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** mehr..._\n_Mehr Details mit {jda.commandMention:music.queue:`/music queue`}_",
                    ITALIAN, "_e **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** altri..._\n_Ulteriori dettagli con {jda.commandMention:music.queue:`/music queue`}_",
                    SPANISH, "_y **{math.sub:{music.queue.size}:{var.get:const.queueSize}}** más..._\n_Más detalles con {jda.commandMention:music.queue:`/music queue`}_"
            ));
            @SettingsEntry
            public Map<DiscordLocale, String> warningNoTrack = new EnumMap<>(Map.of(
                    ENGLISH_UK, "There is no track playing.",
                    ENGLISH_US, "There is no track playing.",
                    FRENCH, "Il n'y a pas de piste en cours de lecture.",
                    GERMAN, "Es wird gerade kein Titel abgespielt.",
                    ITALIAN, "Non c'è nessuna traccia in riproduzione.",
                    SPANISH, "No hay ninguna pista en reproducción."
            ));

            public NowPlaying(TomlReader parent) {
                super("nowPlaying", parent);
            }

            public static class Button extends TomlReader {
                @SettingsEntry
                public Emoji emoji = new Emoji(this);
                @SettingsEntry
                public Label label = new Label(this);

                public Button(TomlReader parent) {
                    super("button", parent);
                }

                public net.dv8tion.jda.api.interactions.components.buttons.Button getButton(DiscordLocale locale, String name, String buttonId, ButtonStyle style) {
                    return net.dv8tion.jda.api.interactions.components.buttons.Button
                            .of(style, buttonId, label.getLabel(locale, name)).withEmoji(emoji.getEmoji(name));
                }

                public static class Emoji extends TomlReader {
                    @SettingsEntry
                    public String unknown = "❓";

                    @SettingsEntry
                    public String volumeDown = "🔉";
                    @SettingsEntry
                    public String stop = "⏹";
                    @SettingsEntry
                    public String play = "▶";
                    @SettingsEntry
                    public String pause = "⏸";
                    @SettingsEntry
                    public String skip = "⏭";
                    @SettingsEntry
                    public String volumeUp = "🔊";

                    @SettingsEntry
                    public String shuffle = "🔀";
                    @SettingsEntry
                    public String repeatNone = "➡️";
                    @SettingsEntry
                    public String repeatOne = "🔂";
                    @SettingsEntry
                    public String repeatAll = "🔁";
                    @SettingsEntry
                    public String queue = "📝";
                    @SettingsEntry
                    public String lyrics = "📃";
                    @SettingsEntry
                    public String refresh = "🔄";

                    @SettingsEntry
                    public Emoji(TomlReader parent) {
                        super("emoji", parent);
                    }

                    public net.dv8tion.jda.api.entities.emoji.Emoji getEmoji(String name) {
                        try {
                            return net.dv8tion.jda.api.entities.emoji.Emoji.fromFormatted(getClass().getField(name).get(this).toString());
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        return net.dv8tion.jda.api.entities.emoji.Emoji.fromFormatted(unknown);
                    }
                }

                public static class Label extends TomlReader {
                    @SettingsEntry
                    public Map<DiscordLocale, String> unknown = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Unknown",
                            ENGLISH_US, "Unknown",
                            FRENCH, "Inconnu",
                            GERMAN, "Unbekannt",
                            ITALIAN, "Sconosciuto",
                            SPANISH, "Desconocido"
                    ));

                    @SettingsEntry
                    public Map<DiscordLocale, String> volumeDown = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Volume down",
                            ENGLISH_US, "Volume down",
                            FRENCH, "Baisser le volume",
                            GERMAN, "Lautstärke herunter",
                            ITALIAN, "Abbassa il volume",
                            SPANISH, "Bajar el volumen"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> stop = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Stop",
                            ENGLISH_US, "Stop",
                            FRENCH, "Arrêter",
                            GERMAN, "Halt",
                            ITALIAN, "Fermare",
                            SPANISH, "Detener"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> play = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Play",
                            ENGLISH_US, "Play",
                            FRENCH, "Jouer",
                            GERMAN, "Spielen",
                            ITALIAN, "Giocare",
                            SPANISH, "Jugar"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> pause = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Pause",
                            ENGLISH_US, "Pause",
                            FRENCH, "Pause",
                            GERMAN, "Pause",
                            ITALIAN, "Pausa",
                            SPANISH, "Pausa"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> skip = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Skip",
                            ENGLISH_US, "Skip",
                            FRENCH, "Passer",
                            GERMAN, "Überspringen",
                            ITALIAN, "Salta",
                            SPANISH, "Saltar"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> volumeUp = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Volume up",
                            ENGLISH_US, "Volume up",
                            FRENCH, "Augmenter le volume",
                            GERMAN, "Lautstärke erhöhen",
                            ITALIAN, "Aumenta il volume",
                            SPANISH, "Subir el volumen"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> shuffle = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Shuffle",
                            ENGLISH_US, "Shuffle",
                            FRENCH, "Mélanger",
                            GERMAN, "Mischen",
                            ITALIAN, "Mescolare",
                            SPANISH, "Mezclar"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> repeatNone = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Repeat none",
                            ENGLISH_US, "Repeat none",
                            FRENCH, "Ne pas répéter",
                            GERMAN, "Nicht wiederholen",
                            ITALIAN, "Non ripetere",
                            SPANISH, "No repetir"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> repeatOne = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Repeat song",
                            ENGLISH_US, "Repeat song",
                            FRENCH, "Répéter la chanson",
                            GERMAN, "Lied wiederholen",
                            ITALIAN, "Ripeti la canzone",
                            SPANISH, "Repetir canción"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> repeatAll = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Repeat playlist",
                            ENGLISH_US, "Repeat playlist",
                            FRENCH, "Répéter la liste de lecture",
                            GERMAN, "Wiedergabeliste wiederholen",
                            ITALIAN, "Ripeti la playlist",
                            SPANISH, "Repetir lista de reproducción"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> queue = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Queue",
                            ENGLISH_US, "Queue",
                            FRENCH, "File d'attente",
                            GERMAN, "Warteschlange",
                            ITALIAN, "Coda",
                            SPANISH, "Cola"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> lyrics = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Lyrics",
                            ENGLISH_US, "Lyrics",
                            FRENCH, "Paroles",
                            GERMAN, "Liedtexte",
                            ITALIAN, "Testi",
                            SPANISH, "Letras"
                    ));
                    @SettingsEntry
                    public Map<DiscordLocale, String> refresh = new EnumMap<>(Map.of(
                            ENGLISH_UK, "Refresh",
                            ENGLISH_US, "Refresh",
                            FRENCH, "Rafraîchir",
                            GERMAN, "Erfrischen",
                            ITALIAN, "Aggiorna",
                            SPANISH, "Refrescar"
                    ));

                    public Label(TomlReader parent) {
                        super("label", parent);
                    }

                    public String getLabel(DiscordLocale locale, String name) {
                        try {
                            return ((Map) getClass().getField(name).get(this)).get(locale).toString();
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        return unknown.get(locale);
                    }
                }
            }
        }
    }
}
