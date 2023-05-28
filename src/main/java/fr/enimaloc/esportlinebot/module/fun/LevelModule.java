package fr.enimaloc.esportlinebot.module.fun;

import fr.enimaloc.enutils.jda.annotation.On;
import fr.enimaloc.enutils.jda.annotation.SlashCommand;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import fr.enimaloc.esportlinebot.entity.ELUser;
import fr.enimaloc.esportlinebot.entity.ELUserManager;
import fr.enimaloc.esportlinebot.toml.customization.Customization;
import fr.enimaloc.esportlinebot.toml.settings.Settings;
import fr.enimaloc.esportlinebot.utils.Cache;
import fr.enimaloc.matcher.Matcher;
import fr.enimaloc.matcher.syntaxe.ELUserKeyword;
import fr.enimaloc.matcher.syntaxe.JDAKeyword;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SlashCommand(name = "level", description = "Level command")
public class LevelModule {
    public static final Logger LOGGER = LoggerFactory.getLogger(LevelModule.class);

    private final Cache<Long> talkCache = new Cache<>(10, TimeUnit.SECONDS) {
        @Override
        public Long factory(Object... objects) {
            return objects[0] instanceof Long l ? l : 0L;
        }
    };
    private final Settings.Level settings;
    private final Customization.Level customization;
    private final Connection sql;
    private final ELUserManager userManager;

    public LevelModule(Settings.Level settings, Customization.Level customization, Connection sql, ELUserManager userManager) {
        this.settings = settings;
        this.customization = customization;
        this.sql = sql;
        this.userManager = userManager;
    }

    @On
    public void onMessage(MessageReceivedEvent event) {
        if (!settings.enabled) {
            return;
        }
        long userId = event.getAuthor().getIdLong();
        long guildId = event.getGuild().getIdLong();
        ELUser elUser = userManager.getOrCreate(guildId, userId);
        if (event.getAuthor().isBot() || !event.isFromGuild() || talkCache.contains(userId)) {
            return;
        }
        talkCache.add(userId);
        int level = elUser.getLevel();
        double newXP = elUser.getXp() + ThreadLocalRandom.current().nextDouble(settings.minRandomXp, settings.maxRandomXp);
        boolean isLevelUp = settings.canLevelUp(level, newXP);
        if (isLevelUp) {
            elUser.levelUp();
        } else {
            elUser.setXp(newXP);
        }
        if (isLevelUp && settings.announce) {
            Matcher matcher = Customization.getMatcher(customization);
            matcher.getenv().put(JDAKeyword.User.USER_KEY_PATH, event.getAuthor());
            matcher.getenv().put(ELUserKeyword.USER_PATH_KEY, elUser);
            event.getChannel()
                    .sendMessage(matcher.apply(customization.levelUpMessage.get(event.getGuild().getLocale())))
                    .queue();
        }
    }

    @SlashCommand.Sub(name = "xp", description = "Get your xp")
    public void xp(GuildSlashCommandEvent event) {
        if (!settings.enabled) {
            return;
        }
        long userId = event.getUser().getIdLong();
        long guildId = event.getGuild().getIdLong();
        ELUser elUser = userManager.getOrCreate(guildId, userId);
        Map<String, Object> env = Map.of(
                JDAKeyword.User.USER_KEY_PATH, event.getUser(),
                ELUserKeyword.USER_PATH_KEY, elUser,
                ELUserKeyword.LEVEL_SETTINGS_PATH_KEY, settings
        );
        Matcher matcher = Customization.getMatcher(env, customization);
        event.reply(matcher.apply(customization.xpMessage.get(event.getGuild().getLocale())))
                .queue();
    }

    @SlashCommand.Sub(name = "leaderboard", description = "Get the leaderboard")
    public void leaderboard(GuildSlashCommandEvent event) {
        if (!settings.enabled) {
            return;
        }
        Map<String, Object> env = Map.of(
                JDAKeyword.Guild.GUILD_KEY_PATH, event.getGuild(),
                ELUserKeyword.LEVEL_SETTINGS_PATH_KEY, settings
        );
        Matcher matcher = Customization.getMatcher(env, customization);
        AtomicInteger i = new AtomicInteger(1);
        event.reply(matcher.apply(customization.leaderboardMessage.get(event.getGuild().getLocale())) + "\n" +
                        userManager.stream()
                                .filter(elUser -> elUser.getGuildId() == event.getGuild().getIdLong())
                                .sorted((o1, o2) -> Double.compare(o2.getTotalXp(), o1.getTotalXp()))
                                .limit(10)
                                .filter(elUser -> event.getGuild().getMemberById(elUser.getId()) != null)
                                .filter(elUser -> !event.getGuild().getMemberById(elUser.getId()).getUser().isBot())
                                .filter(elUser -> !event.getGuild().getMemberById(elUser.getId()).getUser().isSystem())
                                .map(elUser ->
                                        "`" + i.getAndIncrement() + ".` " + event.getGuild().getMemberById(elUser.getId()).getEffectiveName()
                                                + " - level " + elUser.getLevel() + " - "
                                                + BigDecimal.valueOf(elUser.getXp()).setScale(2, RoundingMode.HALF_UP).floatValue() + " xp")
                                .reduce((s, s2) -> s + "\n" + s2)
                                .orElse("No one yet"))
                .queue();
    }
}
