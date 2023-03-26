package fr.enimaloc.esportlinebot.toml.customization;

import com.electronwill.nightconfig.core.file.FileConfig;
import fr.enimaloc.esportlinebot.toml.TomlReader;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import static net.dv8tion.jda.api.interactions.DiscordLocale.*;

public class Customization extends TomlReader {
    public static final Logger LOGGER = LoggerFactory.getLogger(Customization.class);

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
            @SettingsComment("First formatted string is for volume bar")
            @SettingsComment("Second formatted string is for volume command mention, formatted string")
            public Map<DiscordLocale, String> volume = new EnumMap<>(Map.of(
                    ENGLISH_UK, "Volume: %s %s",
                    ENGLISH_US, "Volume: %s %s",
                    FRENCH, "Volume: %s %s",
                    GERMAN, "Lautstärke: %s %s",
                    ITALIAN, "Volume: %s %s",
                    SPANISH, "Volumen: %s %s"
            ));
            @SettingsEntry
            @SettingsComment("First formatted string is for volume command mention")
            public Map<DiscordLocale, String> volumeCommand = new EnumMap<>(Map.of(
                    ENGLISH_UK, "_Edit with %s_",
                    ENGLISH_US, "_Edit with %s_",
                    FRENCH, "_Modifier avec %s_",
                    GERMAN, "_Bearbeiten mit %s_",
                    ITALIAN, "_Modifica con %s_",
                    SPANISH, "_Editar con %s_"
            ));
            @SettingsEntry
            @SettingsComment("First formatted string is for volume name")
            public Map<DiscordLocale, String> volumeNoCommand = new EnumMap<>(Map.of(
                    ENGLISH_UK, "",
                    ENGLISH_US, "",
                    FRENCH, "",
                    GERMAN, "",
                    ITALIAN, "",
                    SPANISH, ""
            ));
            @SettingsEntry
            @SettingsComment("First formatted string is for remaining duration")
            public Map<DiscordLocale, String> remaining = new EnumMap<>(Map.of(
                    ENGLISH_UK, "Remaining: %s",
                    ENGLISH_US, "Remaining: %s",
                    FRENCH, "Restant: %s",
                    GERMAN, "Verbleibend: %s",
                    ITALIAN, "Rimanente: %s",
                    SPANISH, "Restante: %s"
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
            @SettingsComment("First formatted string is for title")
            @SettingsComment("Second formatted string is for url")
            @SettingsComment("Third formatted string is for author")
            public Map<DiscordLocale, String> queueTitle = new EnumMap<>(Map.of(
                    ENGLISH_UK, "[%s](%s) by %s",
                    ENGLISH_US, "[%s](%s) by %s",
                    FRENCH, "[%s](%s) par %s",
                    GERMAN, "[%s](%s) von %s",
                    ITALIAN, "[%s](%s) di %s",
                    SPANISH, "[%s](%s) por %s"
            ));
            @SettingsEntry
            @SettingsComment("First formatted int is for remaining tracks")
            @SettingsComment("Second formatted string is for command mention")
            public Map<DiscordLocale, String> queueMore = new EnumMap<>(Map.of(
                    ENGLISH_UK, "_and **%d** more..._%n%s",
                    ENGLISH_US, "_and **%d** more..._%n%s",
                    FRENCH, "_et **%d** autres..._%n%s",
                    GERMAN, "_und **%d** mehr..._%n%s",
                    ITALIAN, "_e **%d** altri..._%n%s",
                    SPANISH, "_y **%d** más..._%n%s"
            ));
            @SettingsEntry
            @SettingsComment("First formatted string command mention")
            public Map<DiscordLocale, String> queueMoreCommand = new EnumMap<>(Map.of(
                    ENGLISH_UK, "_More details with %s_",
                    ENGLISH_US, "_More details with %s_",
                    FRENCH, "_Plus de détails avec %s_",
                    GERMAN, "_Mehr Details mit %s_",
                    ITALIAN, "_Ulteriori dettagli con %s_",
                    SPANISH, "_Más detalles con %s_"
            ));
            @SettingsEntry
            @SettingsComment("First formatted string is for command name")
            public Map<DiscordLocale, String> queueMoreNoCommand = new EnumMap<>(Map.of(
                    ENGLISH_UK, "",
                    ENGLISH_US, "",
                    FRENCH, "",
                    GERMAN, "",
                    ITALIAN, "",
                    SPANISH, ""
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
