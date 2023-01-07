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
import fr.enimaloc.enutils.jda.annotation.SlashCommand;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.nio.file.Path;
import java.util.Arrays;

/**
 *
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@SlashCommand(name = "settings",
        description = "Adjust settings for the bot",
        permission = @fr.enimaloc.enutils.jda.annotation.Permission(permissions = {Permission.MANAGE_SERVER}))
public class Settings {
    public        FileConfig config;


    private final BackSettings[] settings     = new BackSettings[]{};
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

    @SlashCommand.Sub(name = "modules", description = "See module state")
    public void modules(GuildSlashCommandEvent event) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Modules")
                .setDescription("Liste des modules");
        for (BackSettings settings : settings) {
            builder.addField(settings.getClass().getSimpleName(), translateBool(settings.enabled()), false);
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    public String translateBool(boolean b) {
        return b ? "✅ Activé" : "❎ Désactivé";
    }
}
