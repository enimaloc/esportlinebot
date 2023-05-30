package fr.enimaloc.matcher.syntaxe;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.ICommandReference;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class JDAKeyword {

    public static final String JDA_KEY = "internal.jda";

    public static Keyword[] getKeywords() {
        return new Keyword[]{
                getCommandMention()
        };
    }

    public static Keyword getCommandMention() {
        return new Keyword("jda.commandMention", (matcher, instruction) -> {
            JDA jda = (JDA) matcher.getenv().get(JDA_KEY);
            String[] split = instruction.getArgs(0).mapLeft(instru -> instru.run(matcher)).getAny(String.class).split("\\.");
            try {
                ICommandReference cmdRef = jda.retrieveCommands()
                        .submit()
                        .get()
                        .stream()
                        .filter(cmd -> cmd.getName().equals(split[0]))
                        .findFirst()
                        .orElseThrow();
                for (var ref = new Object() {
                    int i = 1;
                }; ref.i < split.length; ref.i++) {
                    if (cmdRef instanceof Command cmd) {
                        if (split.length > 2) {
                            cmdRef = cmd.getSubcommandGroups()
                                    .stream()
                                    .filter(sub -> sub.getName().equals(split[ref.i]))
                                    .findFirst()
                                    .orElseThrow();
                        } else {
                            cmdRef = cmd.getSubcommands()
                                    .stream()
                                    .filter(sub -> sub.getName().equals(split[ref.i]))
                                    .findFirst()
                                    .orElseThrow();
                        }
                    } else if (cmdRef instanceof Command.SubcommandGroup group) {
                        cmdRef = group.getSubcommands()
                                .stream()
                                .filter(sub -> sub.getName().equals(split[ref.i]))
                                .findFirst()
                                .orElseThrow();
                    }
                }
                return cmdRef.getAsMention();
            } catch (InterruptedException | ExecutionException e) {
                return instruction.getArgsCount() > 1 ? instruction.getArgs(1).mapLeft(instru -> instru.run(matcher)).getAny(String.class) : "";
            }
        });
    }

    public static class User {

        public static final String USER_KEY_PATH = "internal.jda.user";

        public static Keyword[] getKeywords() {
            return new Keyword[]{
                    id(),
                    name(),
                    discriminator(),
                    avatarUrl(),
                    defaultAvatarUrl(),
                    asMention(),
                    asTag(),
                    isBot(),
                    isSystem(),
                    mutualGuilds(),
                    mutualGuildsCount(),
                    avatarId(),
                    effectiveAvatarUrl(),
                    defaultAvatarId()
            };
        }

        public static net.dv8tion.jda.api.entities.User getUser(Map<String, Object> env) {
            if (!env.containsKey(USER_KEY_PATH)) {
                throw new IllegalArgumentException("user is not defined");
            }
            return (net.dv8tion.jda.api.entities.User) env.get(USER_KEY_PATH);
        }

        public static Keyword id() {
            return new Keyword("user.id", (matcher, instruction) -> getUser(matcher.getenv()).getId());
        }

        public static Keyword name() {
            return new Keyword("user.name", (matcher, instruction) -> getUser(matcher.getenv()).getName());
        }

        public static Keyword discriminator() {
            return new Keyword("user.discriminator", (matcher, instruction) -> getUser(matcher.getenv()).getDiscriminator());
        }

        public static Keyword avatarUrl() {
            return new Keyword("user.avatarUrl", (matcher, instruction) -> getUser(matcher.getenv()).getAvatarUrl());
        }

        public static Keyword defaultAvatarUrl() {
            return new Keyword("user.defaultAvatarUrl", (matcher, instruction) -> getUser(matcher.getenv()).getDefaultAvatarUrl());
        }

        public static Keyword asMention() {
            return new Keyword("user.asMention", (matcher, instruction) -> getUser(matcher.getenv()).getAsMention());
        }

        public static Keyword asTag() {
            return new Keyword("user.asTag", (matcher, instruction) -> getUser(matcher.getenv()).getAsTag());
        }

        public static Keyword isBot() {
            return new Keyword("user.isBot", (matcher, instruction) -> String.valueOf(getUser(matcher.getenv()).isBot()));
        }

        public static Keyword isSystem() {
            return new Keyword("user.isSystem", (matcher, instruction) -> String.valueOf(getUser(matcher.getenv()).isSystem()));
        }

        public static Keyword mutualGuilds() {
            return new Keyword("user.mutualGuilds.array", (matcher, instruction) -> getUser(matcher.getenv()).getMutualGuilds().toString());
        }

        public static Keyword mutualGuildsCount() {
            return new Keyword("user.mutualGuilds.count", (matcher, instruction) -> String.valueOf(getUser(matcher.getenv()).getMutualGuilds().size()));
        }

        public static Keyword avatarId() {
            return new Keyword("user.avatar.id", (matcher, instruction) -> getUser(matcher.getenv()).getAvatarId());
        }

        public static Keyword effectiveAvatarUrl() {
            return new Keyword("user.avatar.effective.url", (matcher, instruction) -> getUser(matcher.getenv()).getEffectiveAvatarUrl());
        }

        public static Keyword defaultAvatarId() {
            return new Keyword("user.avatar.default.id", (matcher, instruction) -> getUser(matcher.getenv()).getDefaultAvatarId());
        }
    }

    public static class Guild {

        public static final String GUILD_KEY_PATH = "internal.jda.guild";

        public static Keyword[] getKeywords() {
            return new Keyword[]{
                    id(),
                    name(),
                    iconUrl(),
                    splashUrl(),
                    bannerUrl(),
                    afkChannelId(),
                    afkTimeout(),
                    verificationLevel(),
                    defaultNotificationLevel(),
                    explicitContentLevel(),
                    mfaLevel(),
                    features(),
                    iconId(),
                    splashId(),
                    bannerId(),
                    maxMembers(),
                    maxPresences(),
                    vanityUrl(),
                    vanityCode(),
                    description(),
                    boostTier(),
                    boostCount(),
                    owner(),
                    ownerId(),
                    systemChannelId(),
                    rulesChannelId(),
                    afkChannel(),
                    systemChannel(),
                    rulesChannel()
            };
        }

        public static net.dv8tion.jda.api.entities.Guild getGuild(Map<String, Object> env) {
            if (!env.containsKey(GUILD_KEY_PATH)) {
                throw new IllegalArgumentException("guild is not defined");
            }
            return (net.dv8tion.jda.api.entities.Guild) env.get(GUILD_KEY_PATH);
        }

        public static Keyword id() {
            return new Keyword("guild.id", (matcher, instruction) -> getGuild(matcher.getenv()).getId());
        }

        public static Keyword name() {
            return new Keyword("guild.name", (matcher, instruction) -> getGuild(matcher.getenv()).getName());
        }

        public static Keyword iconUrl() {
            return new Keyword("guild.icon.url", (matcher, instruction) -> getGuild(matcher.getenv()).getIconUrl());
        }

        public static Keyword splashUrl() {
            return new Keyword("guild.splash.url", (matcher, instruction) -> getGuild(matcher.getenv()).getSplashUrl());
        }

        public static Keyword bannerUrl() {
            return new Keyword("guild.banner.url", (matcher, instruction) -> getGuild(matcher.getenv()).getBannerUrl());
        }

        public static Keyword afkChannelId() {
            return new Keyword("guild.afk.channel.id", (matcher, instruction) -> getGuild(matcher.getenv()).getAfkChannel().getId());
        }

        public static Keyword afkTimeout() {
            return new Keyword("guild.afk.timeout", (matcher, instruction) -> String.valueOf(getGuild(matcher.getenv()).getAfkTimeout().getSeconds()));
        }

        public static Keyword verificationLevel() {
            return new Keyword("guild.verificationLevel", (matcher, instruction) -> getGuild(matcher.getenv()).getVerificationLevel().name());
        }

        public static Keyword defaultNotificationLevel() {
            return new Keyword("guild.defaultNotificationLevel", (matcher, instruction) -> getGuild(matcher.getenv()).getDefaultNotificationLevel().name());
        }

        public static Keyword explicitContentLevel() {
            return new Keyword("guild.explicitContentLevel", (matcher, instruction) -> getGuild(matcher.getenv()).getExplicitContentLevel().name());
        }

        public static Keyword mfaLevel() {
            return new Keyword("guild.mfaLevel", (matcher, instruction) -> getGuild(matcher.getenv()).getRequiredMFALevel().name());
        }

        public static Keyword features() {
            return new Keyword("guild.features.array", (matcher, instruction) -> getGuild(matcher.getenv()).getFeatures().toString());
        }

        public static Keyword featuresLen() {
            return new Keyword("guild.features.length", (matcher, instruction) -> String.valueOf(getGuild(matcher.getenv()).getFeatures().size()));
        }

        public static Keyword iconId() {
            return new Keyword("guild.icon.id", (matcher, instruction) -> getGuild(matcher.getenv()).getIconId());
        }

        public static Keyword splashId() {
            return new Keyword("guild.splash.id", (matcher, instruction) -> getGuild(matcher.getenv()).getSplashId());
        }

        public static Keyword bannerId() {
            return new Keyword("guild.banner.id", (matcher, instruction) -> getGuild(matcher.getenv()).getBannerId());
        }

        public static Keyword maxMembers() {
            return new Keyword("guild.members.max", (matcher, instruction) -> String.valueOf(getGuild(matcher.getenv()).getMaxMembers()));
        }

        public static Keyword maxPresences() {
            return new Keyword("guild.presences.max", (matcher, instruction) -> String.valueOf(getGuild(matcher.getenv()).getMaxPresences()));
        }

        public static Keyword vanityUrl() {
            return new Keyword("guild.invite.vanity.url", (matcher, instruction) -> getGuild(matcher.getenv()).getVanityUrl());
        }

        public static Keyword vanityCode() {
            return new Keyword("guild.invite.vanity.code", (matcher, instruction) -> getGuild(matcher.getenv()).getVanityCode());
        }

        public static Keyword description() {
            return new Keyword("guild.description", (matcher, instruction) -> getGuild(matcher.getenv()).getDescription());
        }

        public static Keyword boostTier() {
            return new Keyword("guild.boost.tier", (matcher, instruction) -> getGuild(matcher.getenv()).getBoostTier().name());
        }

        public static Keyword boostCount() {
            return new Keyword("guild.boost.count", (matcher, instruction) -> String.valueOf(getGuild(matcher.getenv()).getBoostCount()));
        }

        public static Keyword owner() {
            return new Keyword("guild.owner.tzg", (matcher, instruction) -> getGuild(matcher.getenv()).getOwner().getUser().getAsTag());
        }

        public static Keyword ownerId() {
            return new Keyword("guild.owner.id", (matcher, instruction) -> getGuild(matcher.getenv()).getOwnerId());
        }

        public static Keyword systemChannelId() {
            return new Keyword("guild.channels.system.id", (matcher, instruction) -> getGuild(matcher.getenv()).getSystemChannel().getId());
        }

        public static Keyword rulesChannelId() {
            return new Keyword("guild.channels.rules.id", (matcher, instruction) -> getGuild(matcher.getenv()).getRulesChannel().getId());
        }

        public static Keyword afkChannel() {
            return new Keyword("guild.channels.afk", (matcher, instruction) -> getGuild(matcher.getenv()).getAfkChannel().getName());
        }

        public static Keyword systemChannel() {
            return new Keyword("guild.channels.system", (matcher, instruction) -> getGuild(matcher.getenv()).getSystemChannel().getName());
        }

        public static Keyword rulesChannel() {
            return new Keyword("guild.channels.rules", (matcher, instruction) -> getGuild(matcher.getenv()).getRulesChannel().getName());
        }
    }
}
