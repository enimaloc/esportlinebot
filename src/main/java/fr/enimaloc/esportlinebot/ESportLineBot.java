package fr.enimaloc.esportlinebot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.command.SlashCommand;

import fr.enimaloc.enutils.classes.NumberUtils;
import fr.enimaloc.esportlinebot.exception.MissingEnvironmentVariableError;
import fr.enimaloc.esportlinebot.listener.PollListener;
import fr.enimaloc.esportlinebot.listener.TempAudioChannelListener;
import fr.enimaloc.esportlinebot.utils.PollUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import fr.enimaloc.esportlinebot.commands.ClearCommand;
import fr.enimaloc.esportlinebot.commands.ForceDrawCommand;
import fr.enimaloc.esportlinebot.commands.StatusCommand;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;

public class ESportLineBot extends ListenerAdapter {

    public static final boolean DEBUG           = System.getenv("DEV") != null;
    public static final String  THUMBS_UP       = "\uD83D\uDC4D";
    public static final int     DAY_TO_CLOSE    = Calendar.SATURDAY;
    public static final String  GUILD_ID        = DEBUG ? "793693128661008394" : "373139766109011970";
    public static final String  STAFF_ROLE_ID   = DEBUG ? "793693415089897523" : "374235587198189570";
    public static final String  MINOZEN_USER_ID = "532813962874585098";

    public static Pattern[] LINKS;
    public static Long      VOTE_CHANNEL;
    public static Long      DEST_CHANNEL;
    public static Long      THREADS_CHANNEL;
    public static Long      VOICE_CHANNEL;
    public static String    EMOTE;
    public static Long[]    IGNORE_MESSAGES;
    public static String    CHANNEL_NAME_TEMPLATE = "%s's channels";
    public static Integer   THREADS_LIMITS;

    public final CommandClient client;

    public static void main(String[] args) {
        VOTE_CHANNEL    = NumberUtils.getSafe(System.getenv("VOTE_CHANNEL"), Long.class).orElse(null);
        DEST_CHANNEL    = NumberUtils.getSafe(System.getenv("DEST_CHANNEL"), Long.class).orElse(null);
        THREADS_CHANNEL = NumberUtils.getSafe(System.getenv("THREADS_CHANNEL"), Long.class).orElse(null);
        VOICE_CHANNEL   = NumberUtils.getSafe(System.getenv("VOICE_CHANNEL"), Long.class).orElse(null);
        LINKS           = Arrays.stream(System.getenv()
                                              .getOrDefault("LINK",
                                                            "https://www.gifyourgame.com/"
                                              )
                                              .split(","))
                                .map(Pattern::quote)
                                .map(quote -> ".*(?<link>" + quote + "[^ ]*) ?.*")
                                .map(Pattern::compile)
                                .toArray(Pattern[]::new);
        EMOTE           = System.getenv()
                                .getOrDefault("EMOJI", THUMBS_UP);
        IGNORE_MESSAGES = Arrays.stream(System.getenv("IGNORE_MSG")
                                              .split(","))
                                .map(PollUtils::getLong)
                                .filter(n -> n != Long.MAX_VALUE)
                                .toArray(Long[]::new);
        THREADS_LIMITS = NumberUtils.getSafe(System.getenv("THREADS_LIMITS"), Integer.class)
                                    .orElse(20);

        for (Field field : ESportLineBot.class.getDeclaredFields()) {
            try {
                if (Modifier.isPublic(field.getModifiers()) &&
                    Modifier.isStatic(field.getModifiers()) &&
                    !Modifier.isFinal(field.getModifiers()) &&
                    !field.getType().isPrimitive() &&
                    field.get(null) == null) {
                    throw new MissingEnvironmentVariableError(String.format("Missing `%s` variable", field.getName()));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        new ESportLineBot(System.getenv("TOKEN"));
    }

    public ESportLineBot(String token) {
        System.out.println("Bot started at " + new Date());
        System.out.println("Patterns loaded:\n" + Arrays.stream(LINKS)
                                                        .map(Pattern::pattern)
                                                        .collect(Collectors.joining("\n -")));
        this.client = new CommandClientBuilder().setOwnerId("136200628509605888")
                                                .forceGuildOnly(GUILD_ID)
                                                .addSlashCommands(new ForceDrawCommand(this),
                                                                  new StatusCommand(),
                                                                  new ClearCommand()
                                                )
                                                .setListener(new CommandListener() {
                                                    @Override
                                                    public void onSlashCommand(
                                                            SlashCommandEvent event, SlashCommand command
                                                    ) {
                                                        if (event.getMember() != null) {
                                                            if (Arrays.stream(command.getDisabledRoles())
                                                                      .map(Long::parseLong)
                                                                      .anyMatch(id -> event.getMember()
                                                                                           .getRoles()
                                                                                           .stream()
                                                                                           .map(Role::getIdLong)
                                                                                           .toList()
                                                                                           .contains(id)) ||
                                                                Arrays.stream(command.getDisabledUsers())
                                                                      .map(Long::parseLong)
                                                                      .toList()
                                                                      .contains(event.getUser()
                                                                                     .getIdLong())) {
                                                                return;
                                                            }
                                                            if ((command.getEnabledRoles().length != 0 &&
                                                                 Arrays.stream(command.getEnabledRoles())
                                                                       .map(Long::parseLong)
                                                                       .noneMatch(id -> event.getMember()
                                                                                             .getRoles()
                                                                                             .stream()
                                                                                             .map(Role::getIdLong)
                                                                                             .toList()
                                                                                             .contains(id))) &&
                                                                (command.getEnabledUsers().length != 0) &&
                                                                !Arrays.stream(command.getEnabledUsers())
                                                                       .map(Long::parseLong)
                                                                       .toList()
                                                                       .contains(event.getUser()
                                                                                      .getIdLong())) {
                                                                return;
                                                            }
                                                        }
                                                        CommandListener.super.onSlashCommand(event, command);
                                                    }
                                                })
                                                .setStatus(OnlineStatus.ONLINE)
                                                .build();

        try {
            JDABuilder.create(token,
                              GatewayIntent.GUILD_MESSAGE_REACTIONS,
                              GatewayIntent.GUILD_MESSAGES,
                              GatewayIntent.GUILD_MEMBERS,
                              GatewayIntent.GUILD_VOICE_STATES
                      )
                      .addEventListeners(this, client, new TempAudioChannelListener(), new PollListener(this))
                      .disableCache(Arrays.asList(CacheFlag.values()))
                      .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                      .setMemberCachePolicy(MemberCachePolicy.ALL)
                      .setRawEventsEnabled(true)
                      .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

}
