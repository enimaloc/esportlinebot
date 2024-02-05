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
import fr.enimaloc.esportline.runner.EventFromICal;
import fr.enimaloc.esportline.utils.PaginationMessage;
import fr.enimaloc.ical.ICal;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class EsportLineBot {
    public static final Logger LOGGER = LoggerFactory.getLogger(EsportLineBot.class);

    private final JDA jda;
    private final JDAEnutils enutils;
    private final File dbDir = new File("data");
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public EsportLineBot(JDA jda) throws SQLException, IOException {
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
                        , new Object() {
                    @Slash
                    public void clear(GuildSlashCommandEvent e, @Slash.Option int n) {
                        e.replyEphemeral("Clearing " + n + " messages...").queue();
                        e.getChannel().getIterableHistory().takeAsync(n).thenAcceptAsync(messages -> {
                            e.getChannel().purgeMessages(messages);
                            e.getHook().editOriginal("Cleared " + n + " messages!").queue();
                        });
                    }

                    @Slash
                    public void hookSay(GuildSlashCommandEvent event,
                                        @Slash.Option Path message,
                                        @Slash.Option String author,
                                        @Slash.Option String avatarUrl,
                                        @Slash.Option String originalMessageUrl) throws IOException {
                        event.replyEphemeral("Sending message...").queue();
                        TextChannel textChannel = event.getChannel()
                                .asTextChannel();
                        Webhook hookSay = textChannel.retrieveWebhooks()
                                .complete()
                                .stream()
                                .filter(webhook -> webhook.getOwnerAsUser().getIdLong() == event.getJDA().getSelfUser().getIdLong())
                                .findFirst()
                                .orElseGet(() -> textChannel.createWebhook("hookSay").complete());
                        hookSay.getManager().setAvatar(Icon.from(URI.create(avatarUrl).toURL().openStream())).setName(author).complete();
                        WebhookClient client = JDAWebhookClient.from(hookSay);
                        client.send("[Original message: <"+originalMessageUrl+">]\n"+Files.readString(message))
                                .thenRun(() -> event.getHook().editOriginal("Message sent!").queue());
                    }
                }
                ))
                .setContexts(List.of(new EventCreator(System.getenv("OPENAI_API_KEY")))) // Note for Git:The API key was regenerated
                .setListeners(List.of(wakfu, wakfuAdmin))
                .build();

        for (String pair : System.getenv().getOrDefault("ICAL_EVENTS", "").split(";")) {
            String[] pairSplit = pair.split("=", 2);
            long guildId = Long.parseLong(pairSplit[0]);

            for (String cal : pairSplit[1].split(",")) {
                String[] calSplit = cal.split(Pattern.quote("|"), 2);
                executorService.scheduleAtFixedRate(new EventFromICal(jda, guildId, new ICal(calSplit.length > 1 && calSplit[1].equals("public"), calSplit[0])), 0, 1, TimeUnit.MINUTES);
            }
        }

        this.enutils.upsertAll(1038139412753694814L);
    }

    public static void main(String[] args) throws InterruptedException, SQLException, IOException {
        new EsportLineBot(JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"),
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                        GatewayIntent.SCHEDULED_EVENTS)
                .enableCache(CacheFlag.SCHEDULED_EVENTS)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.customStatus("Initializing..."))
                .build().awaitReady());
    }
}
