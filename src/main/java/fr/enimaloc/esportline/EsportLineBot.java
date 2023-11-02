package fr.enimaloc.esportline;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.commands.context.EventCreator;
import fr.enimaloc.esportline.commands.slash.GameCommand;
import fr.enimaloc.esportline.commands.slash.game.wakfu.Wakfu;
import fr.enimaloc.esportline.commands.slash.game.wakfu.WakfuAdmin;
import fr.enimaloc.esportline.utils.PaginationMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class EsportLineBot {
    public static final Logger LOGGER = LoggerFactory.getLogger(EsportLineBot.class);

    private final JDA jda;
    private final JDAEnutils enutils;
    private final File dbDir = new File("data");

    public EsportLineBot(JDA jda) {
        this.jda = jda;
        this.jda.addEventListener(new PaginationMessage.PaginationListener());
        this.dbDir.mkdirs();
        Connection connection = null;
        // region Game Commands related
        Wakfu wakfu = new Wakfu(jda);

        WakfuAdmin wakfuAdmin = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbDir.getAbsolutePath() + "/wakfu-admin.db");
            wakfuAdmin = new WakfuAdmin(jda, connection);
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to wakfu-admin.db, disabling commands", e);
            e.printStackTrace();
        }
        // endregion
        this.enutils = JDAEnutils.builder()
                .setJda(jda)
                .setCommands(List.of(new GameCommand(wakfu, wakfuAdmin)
                ))
                .setContexts(List.of(new EventCreator("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")))
                .setListeners(List.of(wakfu, wakfuAdmin))
                .build();

        this.enutils.upsertAll(1038139412753694814L);
    }

    public static void main(String[] args) throws InterruptedException {
        new EsportLineBot(JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"),
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.customStatus("Initializing..."))
                .build().awaitReady());
    }
}
