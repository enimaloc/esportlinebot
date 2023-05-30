package fr.enimaloc.matcher.syntaxe;

import fr.enimaloc.esportlinebot.entity.ELUser;
import fr.enimaloc.esportlinebot.toml.settings.Settings;

import java.util.Map;
import java.util.function.Supplier;

public class ELUserKeyword {

    public static final String USER_PATH_KEY = "internal.eluser";
    public static final String LEVEL_SETTINGS_PATH_KEY = "internal.level.settings";
    public static final String OVERRIDE_USER_ID_PATH_KEY = "internal.override.eluser.id";
    public static final String OVERRIDE_USER_GUILD_ID_PATH_KEY = "internal.override.eluser.guildId";
    public static final String OVERRIDE_USER_LEVEL_PATH_KEY = "internal.override.eluser.level";
    public static final String OVERRIDE_USER_XP_PATH_KEY = "internal.override.user.xp";
    public static final String OVERRIDE_USER_TOTAL_XP_PATH_KEY = "internal.override.user.xp.total";
    public static final String OVERRIDE_USER_XP_TO_NEXT_LEVEL_PATH_KEY = "internal.override.user.level.next.xp";

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                id(),
                guildId(),
                level(),
                xp(),
                xpToNextLevel(),
                totalXp(),
        };
    }

    private static ELUser getUser(Map<String, Object> env) {
        if (!env.containsKey(USER_PATH_KEY)) {
            throw new IllegalArgumentException("user is not defined");
        }
        return (ELUser) env.get(USER_PATH_KEY);
    }

    public static Keyword id() {
        return new Keyword("eluser.id", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("id() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_ID_PATH_KEY, () -> getUser(matcher.getenv()).getId());
        });
    }

    public static Keyword guildId() {
        return new Keyword("eluser.guildId", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("guildId() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_GUILD_ID_PATH_KEY, () -> getUser(matcher.getenv()).getGuildId());
        });
    }

    public static Keyword level() {
        return new Keyword("eluser.level", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("level() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_LEVEL_PATH_KEY, () -> getUser(matcher.getenv()).getLevel());
        });
    }

    public static Keyword xp() {
        return new Keyword("eluser.xp", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("xp() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_XP_PATH_KEY, () -> getUser(matcher.getenv()).getXp());
        });
    }

    public static Keyword xpToNextLevel() {
        return new Keyword("eluser.xpToNextLevel", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("xpToNextLevel() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_XP_TO_NEXT_LEVEL_PATH_KEY,
                    () -> ((Settings.Level) matcher.getenv().get(LEVEL_SETTINGS_PATH_KEY)).getXpRequired(getUser(matcher.getenv()).getLevel() + 1));
        });
    }

    public static Keyword totalXp() {
        return new Keyword("eluser.totalXp", (matcher, instruction) -> {
            if (instruction.getArgsCount() != 0) {
                throw new IllegalArgumentException("totalXp() takes no arguments");
            }
            return getOrDefault(matcher.getenv(), OVERRIDE_USER_TOTAL_XP_PATH_KEY, () -> getUser(matcher.getenv()).getTotalXp());
        });
    }

    private static String getOrDefault(Map<String, Object> env, String key, Supplier<Object> defaultValue) {
        return env.containsKey(key) ? env.get(key).toString() : defaultValue.get().toString();
    }
}
