/*
 * Constant
 *
 * 0.0.1
 *
 * 18/05/2022
 */
package fr.enimaloc.esportlinebot.settings;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import fr.enimaloc.enutils.classes.ObjectUtils;
import fr.enimaloc.enutils.jda.annotation.MethodTarget;
import fr.enimaloc.enutils.jda.annotation.SlashCommand;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import fr.enimaloc.esportlinebot.jagtag.DiscordLibrairies;
import me.jagrosh.jagtag.JagTag;
import me.jagrosh.jagtag.Method;
import me.jagrosh.jagtag.Parser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@SlashCommand(name = "settings",
        description = "Adjust settings for the bot",
        permission = @fr.enimaloc.enutils.jda.annotation.Permission(permissions = {Permission.MANAGE_SERVER}))
public class Settings {
    public        FileConfig config;

    @SlashCommand.GroupProvider
    public TempChannel tempChannel = new TempChannel();


    private final BackSettings[] settings     = new BackSettings[]{tempChannel};
    private       String         token        = ObjectUtils.getOr(System.getenv("DISCORD_TOKEN"), "");
    private       String         databasePath = "data.db";
    private       long           guildId      = 0L;

    public void load(Path configPath) {
        config = FileConfig.builder(configPath).autosave().build();
        config.load();

        ConfigSpec spec = new ConfigSpec();
        spec.define("token", token);
        spec.define("databasePath", databasePath);
        spec.define("guildId", guildId);
        for (BackSettings bSettings : settings) {
            spec = bSettings.completeSpec(spec);
        }
        int correct = spec.correct(config);

        token = config.getOrElse("token", token);
        databasePath = config.getOrElse("databasePath", databasePath);
        guildId = config.getOrElse("guildId", guildId);
        Arrays.stream(settings).forEach(bSettings -> bSettings.load(config));

        if (correct > 0) {
            save();
        }
    }

    public void save() {
        config.set("token", token);
        config.set("databasePath", databasePath);
        config.set("guildId", guildId);
        Arrays.stream(settings).forEach(bSettings -> bSettings.save(config));
        config.save();
    }

    public String token() {
        return token;
    }

    public String databasePath() {
        return databasePath;
    }

    public long guildId() {
        return guildId;
    }

    public interface BackSettings {
        void load(Config config);

        void save(Config config);

        ConfigSpec completeSpec(ConfigSpec spec);

        boolean enabled();
    }

    public class TempChannel implements BackSettings {

        public boolean enabled        = false;
        public long    triggerChannel = 0L;
        public long    categoryId     = 0L;
        public int     position       = -1;

        public String template = "{user:channelName} channel";

        @Override
        public void load(Config config) {
            enabled = config.getOrElse("tempChannel.enabled", enabled);
            triggerChannel = config.getOrElse("tempChannel.triggerChannel", triggerChannel);
            categoryId = config.getOrElse("tempChannel.categoryId", categoryId);
            position = config.getOrElse("tempChannel.position", position);
            template = config.getOrElse("tempChannel.template", template);
        }

        @Override
        public void save(Config config) {
            config.set("tempChannel.enabled", enabled);
            config.set("tempChannel.triggerChannel", triggerChannel);
            config.set("tempChannel.categoryId", categoryId);
            config.set("tempChannel.position", position);
            config.set("tempChannel.template", template);
        }

        @Override
        public ConfigSpec completeSpec(ConfigSpec spec) {
            spec.define("tempChannel.enabled", enabled);
            spec.define("tempChannel.triggerChannel", triggerChannel);
            spec.define("tempChannel.categoryId", categoryId);
            spec.define("tempChannel.position", position);
            spec.define("tempChannel.template", template);
            return spec;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        public long triggerChannel() {
            return triggerChannel;
        }

        public long categoryId() {
            return categoryId;
        }

        public int position() {
            return position;
        }

        public String template() {
            return template;
        }

        @SlashCommand.Sub(name = "enable", description = "Enable the temp channel system")
        public void enable(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Boolean> enable
        ) {
            enable.ifPresent(b -> {
                VoiceChannel channel = event.getJDA().getVoiceChannelById(triggerChannel());
                if (b && channel == null) {
                    throw new IllegalStateException("Trigger channel not found, disabling temp channel");
                }
                enabled = b;
                Settings.this.save();
            });
            event.replyEphemeral("Temp channel system is " + (enabled() ? "enabled" : "disabled")).queue();
        }

        @SlashCommand.Sub(name = "trigger", description = "Set the trigger channel")
        public void trigger(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<VoiceChannel> channel
        ) {
            channel.ifPresent(c -> {
                triggerChannel = c.getIdLong();
                Settings.this.save();
            });
            event.replyEphemeral("Trigger channel is " + (triggerChannel != 0L ? "<#" + triggerChannel + ">" : "not set")).queue();
        }

        @SlashCommand.Sub(name = "category", description = "Set the category id")
        public void category(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Category> category
        ) {
            category.ifPresent(c -> {
                categoryId = c.getIdLong();
                Settings.this.save();
            });
            event.replyEphemeral("Category is " + (categoryId != 0L ? "<#" + categoryId + ">" : "not set")).queue();
        }

        @SlashCommand.Sub(name = "position", description = "Set the position")
        public void position(
                GuildSlashCommandEvent event,
                @SlashCommand.Option Optional<Integer> position
        ) {
            position.ifPresent(p -> {
                this.position = p;
                Settings.this.save();
            });
            event.replyEphemeral("Position is " + (this.position != -1 ? position : "not set")).queue();
        }

        @SlashCommand.Sub(name = "template", description = "Set the template")
        public void template(
                GuildSlashCommandEvent event,
                @SlashCommand.Option(
                        autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("templateExample"))
                ) Optional<String> template
        ) {
            template.ifPresent(t -> {
                this.template = t;
                Settings.this.save();
            });
            event.replyEphemeral("Template is " + (this.template != null ? template : "not set")).queue();
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

    @SlashCommand.Sub(name = "modules", description = "See module state")
    public void modules(GuildSlashCommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Modules")
                .setDescription("Modules state");
        for (BackSettings settings : settings) {
            builder.addField(settings.getClass().getSimpleName(), translateBool(settings.enabled()), false);
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    public String translateBool(boolean b) {
        return b ? "Enabled" : "Disabled";
    }
}
