package fr.enimaloc.esportline;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.commands.GlobalSlashCommandEvent;
import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.commands.TwitchBridge;
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
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
        }
        // endregion
        // region Twitch Commands related
        TwitchBridge twitchBridge = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbDir.getAbsolutePath() + "/twitch-bridge.db");
            twitchBridge = new TwitchBridge(jda, connection);
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to twitch-bridge.db, disabling commands", e);
        } catch (IOException e) {
            LOGGER.error("Failed to connect to Twitch IRC, disabling commands", e);
        }
        // endregion
        this.enutils = JDAEnutils.builder()
                .setJda(jda)
                .setCommands(List.of(new GameCommand(wakfu, wakfuAdmin), twitchBridge
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
                                client.send("[Original message: <" + originalMessageUrl + ">]\n" + Files.readString(message))
                                        .thenRun(() -> event.getHook().editOriginal("Message sent!").queue());
                            }

                            @Slash
                            public void activity(GlobalSlashCommandEvent event,
                                                 @Slash.Option Activity.ActivityType type,
                                                 @Slash.Option String name,
                                                 @Slash.Option Optional<String> url) {
                                event.replyEphemeral("Setting activity...").queue();
                                event.getJDA().getPresence().setActivity(Activity.of(type, name, url.orElse(null)));
                                event.getHook().editOriginal("Activity set!").queue();
                            }

                            @Slash
                            public void joinMe(GuildSlashCommandEvent event, @Slash.Option Optional<VoiceChannel> channel) {
                                VoiceChannel dest = null;
                                if (channel.isPresent()) {
                                    dest = channel.get();
                                } else {
                                    if (event.getMember().getVoiceState() == null || !event.getMember().getVoiceState().inAudioChannel()) {
                                        event.replyEphemeral("You are not in a voice channel!").queue();
                                        return;
                                    }
                                    dest = event.getMember().getVoiceState().getChannel().asVoiceChannel();
                                }
                                event.replyEphemeral("Joining you...").queue();
                                dest.getGuild().getAudioManager().openAudioConnection(dest);
                                dest.getGuild().getAudioManager().setSelfDeafened(true);
                                dest.getGuild().getAudioManager().setSelfMuted(true);
                                event.getHook().editOriginal("Joined!").queue();
                            }

                            @Slash
                            public void leaveMe(GuildSlashCommandEvent event) {
                                event.replyEphemeral("Leaving you...").queue();
                                event.getGuild().getAudioManager().closeAudioConnection();
                                event.getHook().editOriginal("Left!").queue();
                            }
                        }
                ))
                .setContexts(List.of(new EventCreator(System.getenv("OPENAI_API_KEY")))) // Note for Git:The API key was regenerated
                .setListeners(List.of(wakfu, wakfuAdmin, twitchBridge))
                .build();

        this.enutils.upsertAll(1038139412753694814L);
    }

    public static void main(String[] args) throws InterruptedException {
        new EsportLineBot(JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"),
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_PRESENCES)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.customStatus("Initializing..."))
                .build().awaitReady());
    }
}
