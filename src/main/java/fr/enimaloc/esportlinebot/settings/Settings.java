/*
 * Constant
 *
 * 0.0.1
 *
 * 18/05/2022
 */
package fr.enimaloc.esportlinebot.settings;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import fr.enimaloc.enutils.classes.ObjectUtils;
import fr.enimaloc.enutils.jda.annotation.MethodTarget;
import fr.enimaloc.enutils.jda.annotation.SlashCommand;
import fr.enimaloc.enutils.jda.annotation.SlashCommand.GroupProvider;
import fr.enimaloc.enutils.jda.annotation.SlashCommand.Option;
import fr.enimaloc.enutils.jda.annotation.SlashCommand.Sub;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import fr.enimaloc.esportlinebot.jagtag.DiscordLibrairies;
import me.jagrosh.jagtag.JagTag;
import me.jagrosh.jagtag.Method;
import me.jagrosh.jagtag.Parser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@SlashCommand(name = "settings",
        description = "Adjust settings for the bot",
        permission = @fr.enimaloc.enutils.jda.annotation.Permission(permissions = {Permission.MANAGE_SERVER}))
public class Settings extends ESettings {
    public static       boolean DEBUG  = System.getenv("DEV") != null;
    public static final Logger  LOGGER = LoggerFactory.getLogger(Settings.class);

    @SettingsEntry
    public String databasePath = "database.db";
    @SettingsEntry
    public String token        = ObjectUtils.getOr(System.getenv("DISCORD_TOKEN"), "");
    @SettingsEntry
    public long   guildId      = 0;

    @GroupProvider
    @SettingsEntry
    public Ticket      ticket      = new Ticket(this);
    @GroupProvider
    @SettingsEntry
    public TempChannel tempChannel = new TempChannel(this);
    @GroupProvider
    @SettingsEntry
    public Music       music       = new Music(this);

    public Settings(Path configPath) {
        super(FileConfig.builder(configPath).autosave().concurrent().build());
    }

    public void load() {
        config.load();

        ConfigSpec spec = new ConfigSpec();
        spec = spec(spec);
        int correct = spec.correct(config, (action, path, incorrectValue, correctedValue) -> LOGGER.warn("Corrected {} from {} to {}", path, incorrectValue, correctedValue));

        load(config);
        if (correct > 0) {
            save(config);
        }
    }

    @Sub(name = "database", description = "Change the database path")
    public void database(GuildSlashCommandEvent event,
                         @Option(name = "path", description = "The new database path") Optional<String> path) {
        path.ifPresent(s -> {
            this.databasePath = s;
            save(config);
        });
        event.replyEphemeral("Database path: " + this.databasePath).queue();
    }

    public static class Ticket extends ESettings {
        @SettingsEntry
        public long    forumId = 0L;
        @SettingsEntry
        public boolean enabled = true;

        protected Ticket(ESettings parent) {
            super("ticket", parent);
        }

        @Sub(name = "enable", description = "Enable the ticket system")
        public void enable(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Boolean> enable
        ) {
            enable.ifPresent(b -> {
                enabled = b;
                save();
            });
            event.replyEphemeral("Le systéme de ticket est " + (enabled ? "activé" : "désactivé"))
                    .queue();
        }

        @Sub(name = "forum", description = "Set the forum")
        public void forum(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<ForumChannel> forum
        ) {
            forum.ifPresent(f -> {
                this.forumId = f.getIdLong();
                save();
            });
            event.replyEphemeral("Le forum est <#" + forumId + ">")
                    .queue();
        }
    }

    public static class TempChannel extends ESettings {
        @SettingsEntry
        public boolean enabled        = true;
        @SettingsEntry
        public long    triggerChannel = 0L;
        @SettingsEntry
        public long    categoryId     = 0L;
        @SettingsEntry
        public int     position       = -1;
        @SettingsEntry
        public String  template       = "{user:name} channel";

        protected TempChannel(ESettings parent) {
            super("tempchannel", parent);
        }

        @Sub(name = "enable", description = "Enable the temp channel system")
        public void enable(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Boolean> enable
        ) {
            enable.ifPresent(b -> {
                enabled = b;
                save();
            });
            event.replyEphemeral("Le systéme de channel temporaire est " + (enabled ? "activé" : "désactivé"))
                    .queue();
        }

        @Sub(name = "trigger", description = "Set the trigger channel")
        public void trigger(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<VoiceChannel> trigger
        ) {
            trigger.ifPresent(f -> {
                this.triggerChannel = f.getIdLong();
                save();
            });
            event.replyEphemeral("Le channel de trigger est <#" + triggerChannel + ">")
                    .queue();
        }

        @Sub(name = "category", description = "Set the category")
        public void category(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Category> category
        ) {
            category.ifPresent(f -> {
                this.categoryId = f.getIdLong();
                save();
            });
            event.replyEphemeral("La catégorie est <#" + categoryId + ">")
                    .queue();
        }

        @Sub(name = "position", description = "Set the position")
        public void position(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Integer> position
        ) {
            position.ifPresent(f -> {
                this.position = f;
                save();
            });
            event.replyEphemeral("La position est " + this.position)
                    .queue();
        }

        @Sub(name = "template", description = "Set the template")
        public void template(
                GuildSlashCommandEvent event,
                @SlashCommand.Option(
                        autoCompletion = @Option.AutoCompletion(target = @MethodTarget("templateExample"))
                ) Optional<String> template
        ) {
            template.ifPresent(f -> {
                this.template = f;
                save();
            });
            event.replyEphemeral("Le template est " + this.template)
                    .queue();
        }


        public Command.Choice[] templateExample(CommandAutoCompleteInteractionEvent event) {
            String value = event.getFocusedOption().getValue();
            if (value.length() == 0) {
                return new Command.Choice[0];
            }
            Parser parser = getParser();
            parser.put("jda", event.getJDA());
            parser.put("user", event.getUser());
            parser.put("member", event.getMember());
            parser.put("channel", event.getChannel());
            parser.put("guild", event.getGuild());

            List<Command.Choice> choices = new ArrayList<>();
            choices.add(new Command.Choice("Example: " + parser.parse(value), value));

            if (value.chars().filter(c -> c == '{').count() != value.chars().filter(c -> c == '}').count()) {
                choices.addAll(DiscordLibrairies.allMethods()
                        .stream()
                        .map(Method::getName)
                        .filter(s -> s.startsWith(value.substring(value.lastIndexOf('{') + 1)))
                        .map(s -> new Command.Choice(parser.parse(s), value.substring(0, value.lastIndexOf("{") + 1) + "{" + s + "} "))
                        .limit(24)
                        .toList());
            }

            return choices.toArray(new Command.Choice[0]);
        }

        public Parser getParser() {
            return JagTag.newDefaultBuilder().addMethods(DiscordLibrairies.allMethods()).build();
        }
    }

    public static class Music extends ESettings {
        @GroupProvider
        @SettingsEntry
        public Youtube    youtube    = new Youtube(this);
        @GroupProvider
        @SettingsEntry
        public Soundcloud soundcloud = new Soundcloud(this);
        @GroupProvider
        @SettingsEntry
        public Bandcamp   bandcamp   = new Bandcamp(this);
        @GroupProvider
        @SettingsEntry
        public Vimeo      vimeo      = new Vimeo(this);
        @GroupProvider
        @SettingsEntry
        public Twitch     twitch     = new Twitch(this);
        @GroupProvider
        @SettingsEntry
        public Beam       beam       = new Beam(this);
        @GroupProvider
        @SettingsEntry
        public Getyarn    getyarn    = new Getyarn(this);
        @GroupProvider
        @SettingsEntry
        public Http       http       = new Http(this);
        @GroupProvider
        @SettingsEntry
        public Local      local      = new Local(this);

        @SettingsEntry
        public boolean enabled = true;

        protected Music(ESettings parent) {
            super("music", parent);
        }

        @Sub(name = "enable", description = "Enable the music system")
        public void enable(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Boolean> enable
        ) {
            enable.ifPresent(b -> {
                enabled = b;
                save();
            });
            event.replyEphemeral("Le systéme de musique est " + (enabled ? "activé" : "désactivé"))
                    .queue();
        }

        public static class Youtube extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            public Youtube(ESettings parent) {
                super("youtube", parent);
            }

            @Sub(name = "enable", description = "Enable the youtube source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source youtube est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Soundcloud extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Soundcloud(ESettings parent) {
                super("soundcloud", parent);
            }

            @Sub(name = "enable", description = "Enable the soundcloud source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source soundcloud est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Bandcamp extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Bandcamp(ESettings parent) {
                super("bandcamp", parent);
            }

            @Sub(name = "enable", description = "Enable the bandcamp source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source bandcamp est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Vimeo extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Vimeo(ESettings parent) {
                super("vimeo", parent);
            }

            @Sub(name = "enable", description = "Enable the vimeo source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source vimeo est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Twitch extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Twitch(ESettings parent) {
                super("twitch", parent);
            }

            @Sub(name = "enable", description = "Enable the twitch source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source twitch est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Beam extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Beam(ESettings parent) {
                super("beam", parent);
            }

            @Sub(name = "enable", description = "Enable the beam source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source beam est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Getyarn extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Getyarn(ESettings parent) {
                super("getyarn", parent);
            }

            @Sub(name = "enable", description = "Enable the getyarn source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source getyarn est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Http extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Http(ESettings parent) {
                super("http", parent);
            }

            @Sub(name = "enable", description = "Enable the http source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source http est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }

        public static class Local extends ESettings {
            @SettingsEntry
            public boolean enabled = true;

            protected Local(ESettings parent) {
                super("local", parent);
            }

            @Sub(name = "enable", description = "Enable the local source")
            public void enable(
                    GuildSlashCommandEvent event,
                    @SlashCommand.Option Optional<Boolean> enable
            ) {
                enable.ifPresent(b -> {
                    enabled = b;
                    save();
                });
                event.replyEphemeral("La source local est " + (enabled ? "activé" : "désactivé"))
                        .queue();
            }
        }
    }
}
