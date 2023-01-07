/*
 * V2
 *
 * 0.0.1
 *
 * 18/05/2022
 */
package fr.enimaloc.esportlinebot;

import fr.enimaloc.enutils.classes.ObjectUtils;
import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.annotation.Init;
import fr.enimaloc.enutils.jda.annotation.Interaction;
import fr.enimaloc.esportlinebot.settings.Settings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

/**
 *
 */
public class ESportLineBot extends ListenerAdapter {

    public static final Logger LOGGER = LoggerFactory.getLogger(ESportLineBot.class);

    private final Settings   settings;
    private final Connection sql;
    private final JDA        jda;
    private final JDAEnutils jdaEnutils;

    // TODO: Add a command Twitter

    public ESportLineBot(String... args) throws InterruptedException {
        settings = new Settings();
        settings.load(Path.of(Optional.ofNullable(ObjectUtils.getOr(() -> args[0], System.getenv("CONFIG_PATH")))
                .orElse("config.toml")));

        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + settings.databasePath());
        } catch (SQLException e) {
            LOGGER.error("Error while connecting to the database", e);
            System.exit(1);
        }
        sql = connection;

        this.jda = JDABuilder.createLight(settings.token())
                .enableCache(
                        CacheFlag.FORUM_TAGS,
                        CacheFlag.EMOJI,
                        CacheFlag.VOICE_STATE
                )
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.DIRECT_MESSAGE_TYPING,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MEMBERS
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build()
                .setRequiredScopes("applications.commands")
                .awaitReady();

        this.jdaEnutils = JDAEnutils.builder()
                .setJda(jda)
                .addCommand(settings)
                .build();
        jdaEnutils.upsertAll();
    }

    @Init
    public void onReady(JDA jda) {
        Social.registerEmoji(jda, 1041090060738629722L);
    }

    @Interaction.Button(internalId = "delete", idOrUrl = "Supprimer", emoji = "🗑️", style = ButtonStyle.DANGER)
    public void deleteButton(ButtonInteractionEvent event) {
        event.deferReply(true).queue();
        event.getMessage().delete().queue();
        event.getHook().deleteOriginal().queue();
    }

    public Settings settings() {
        return settings;
    }

    public Connection sql() {
        return sql;
    }

    public JDA jda() {
        return jda;
    }

    public JDAEnutils jdaEnutils() {
        return jdaEnutils;
    }

    public static void main(String... args) {
        try {
            new ESportLineBot(args);
        } catch (InterruptedException e) {
            LOGGER.error("Error while starting the bot", e);
            System.exit(1);
        }
    }
}
