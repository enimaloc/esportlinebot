/*
 * DiscordLibrairies
 *
 * 0.0.1
 *
 * 12/12/2022
 */
package fr.enimaloc.esportlinebot.jagtag;

import fr.enimaloc.esportlinebot.Constant;
import me.jagrosh.jagtag.Method;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;

import java.util.*;

import static java.util.Optional.ofNullable;

/**
 *
 */
public class DiscordLibrairies {

    public static Collection<Method> jdaMethod() {
        return Arrays.asList(
                new Method("jda.guilds.count",
                        env -> ((JDA) env.get("jda")).getGuilds().size() + ""),
                new Method("jda.guilds",
                        (env, in) -> {
                            if (in.length == 1) {
                                return ((JDA) env.get("jda")).getGuilds().get(Integer.parseInt(in[0])).getName();
                            }
                            return ((JDA) env.get("jda")).getGuilds().get(Integer.parseInt(in[0])).getMembers().get(Integer.parseInt(in[1])).getAsMention();
                        }, ":"),
                new Method("jda.guilds.members",
                        (env, in) -> {
                            if (in.length == 1) {
                                return ((JDA) env.get("jda")).getGuilds().get(Integer.parseInt(in[0])).getMembers().size() + "";
                            }
                            return ((JDA) env.get("jda")).getGuilds().get(Integer.parseInt(in[0])).getMembers().get(Integer.parseInt(in[1])).getAsMention();
                        }, ":"),
                new Method("jda.guilds.members.count",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().size()
                                ).sum() + ""),
                new Method("jda.guilds.members.online",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getOnlineStatus() != OnlineStatus.OFFLINE).toArray().length).sum() + ""),
                new Method("jda.guilds.members.offline",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getOnlineStatus() == OnlineStatus.OFFLINE).toArray().length).sum() + ""),
                new Method("jda.guilds.members.afk",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getOnlineStatus() == OnlineStatus.IDLE).toArray().length).sum() + ""),
                new Method("jda.guilds.members.dnd",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getOnlineStatus() == OnlineStatus.DO_NOT_DISTURB).toArray().length).sum() + ""),
                new Method("jda.guilds.members.streaming",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.STREAMING)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.playing",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.PLAYING)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.listening",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.LISTENING)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.watching",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.WATCHING)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.custom",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.CUSTOM_STATUS)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.competing",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getActivities().stream().anyMatch(a -> a.getType() == Activity.ActivityType.COMPETING)).toArray().length).sum() + ""),
                new Method("jda.guilds.members.bot",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> m.getUser().isBot()).toArray().length).sum() + ""),
                new Method("jda.guilds.members.user",
                        env -> ((JDA) env.get("jda"))
                                .getGuilds()
                                .stream()
                                .mapToInt(g -> g.getMembers().stream()
                                        .filter(m -> !m.getUser().isBot()).toArray().length).sum() + "")
        );
    }

    public static Collection<Method> userMethod() {
        return List.of(
                new Method("user.name",
                        env -> ((User) env.get("user")).getName(),
                        (env, in) -> {
                            try {
                                return ((JDA) env.get("jda")).retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getName();
                            } catch (NumberFormatException e) {
                                return in[0];
                            }
                        }),
                new Method("user.discriminator",
                        env -> ((User) env.get("user")).getDiscriminator(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getDiscriminator();
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::getDiscriminator)
                                        .orElse("0000");
                            }
                        }),
                new Method("user.avatar",
                        env -> ((User) env.get("user")).getAvatarUrl(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getAvatarUrl();
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::getAvatarUrl)
                                        .orElse("https://cdn.discordapp.com/embed/avatars/0.png");
                            }
                        }),
                new Method("user.mention",
                        env -> ((User) env.get("user")).getAsMention(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getAsMention();
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::getAsMention)
                                        .orElse(Constant.Discord.DELETED_USER_MENTION);
                            }
                        }),
                new Method("user.tag",
                        env -> ((User) env.get("user")).getAsTag(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getAsTag();
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::getAsTag)
                                        .orElse(Constant.Discord.DELETED_USER_TAG);
                            }
                        }),
                new Method("user.id",
                        env -> ((User) env.get("user")).getId(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Integer.parseInt(in[0]) + "";
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::getId)
                                        .orElse(Constant.Discord.DELETED_USER_ID + "");
                            }
                        }),
                new Method("user.isbot",
                        env -> ((User) env.get("user")).isBot() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .isBot() + "";
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(User::isBot)
                                        .map(String::valueOf)
                                        .orElse("false");
                            }
                        }),
                new Method("user.hasavatar",
                        env -> (((User) env.get("user")).getAvatarUrl() != null) + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return (jda.retrieveUserById(Integer.parseInt(in[0]))
                                        .complete()
                                        .getAvatarUrl() != null) + "";
                            } catch (NumberFormatException e) {
                                return ofNullable(jda.getUserByTag(in[0]))
                                        .map(u -> u.getAvatarUrl() != null)
                                        .map(String::valueOf)
                                        .orElse("false");
                            }
                        })
        );
    }

    public static Collection<Method> guildMethod() {
        return List.of(
                new Method("guild.name",
                        env -> ((Guild) env.get("guild")).getName(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            return ofNullable(jda.getGuildById(in[0]))
                                    .map(Guild::getName)
                                    .orElse(in[0]);
                        }),
                new Method("guild.icon",
                        env -> ((Guild) env.get("guild")).getIconUrl(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getIconUrl)
                                        .orElse("https://cdn.discordapp.com/embed/avatars/0.png");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getIconUrl();
                            }
                        }),
                new Method("guild.owner.name",
                        env -> ofNullable(((Guild) env.get("guild")).getOwner())
                                .map(Member::getEffectiveName)
                                .orElse(Constant.Discord.DELETED_USER_NAME),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getOwner)
                                        .map(Member::getEffectiveName)
                                        .orElse(Constant.Discord.DELETED_USER_NAME);
                            } catch (NumberFormatException e) {
                                return Optional.ofNullable(jda.getGuildsByName(in[0], true).get(0))
                                        .map(Guild::getOwner)
                                        .map(Member::getEffectiveName)
                                        .orElse(Constant.Discord.DELETED_USER_NAME);
                            }
                        }),
                new Method("guild.owner.mention",
                        env -> Optional.ofNullable(((Guild) env.get("guild")).getOwner())
                                .map(Member::getAsMention)
                                .orElse(Constant.Discord.DELETED_USER_MENTION),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getOwner)
                                        .map(Member::getAsMention)
                                        .orElse(Constant.Discord.DELETED_USER_MENTION);
                            } catch (NumberFormatException e) {
                                return Optional.ofNullable(jda.getGuildsByName(in[0], true).get(0))
                                        .map(Guild::getOwner)
                                        .map(Member::getAsMention)
                                        .orElse(Constant.Discord.DELETED_USER_MENTION);
                            }
                        }),
                new Method("guild.owner.nickname",
                        env -> Optional.ofNullable(((Guild) env.get("guild")).getOwner())
                                .map(Member::getNickname)
                                .orElse(Constant.Discord.DELETED_USER_NAME),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getOwner)
                                        .map(Member::getNickname)
                                        .orElse(Constant.Discord.DELETED_USER_NAME);
                            } catch (NullPointerException | NumberFormatException e) {
                                return Optional.ofNullable(jda.getGuildsByName(in[0], true).get(0))
                                        .map(Guild::getOwner)
                                        .map(Member::getNickname)
                                        .orElse(Constant.Discord.DELETED_USER_NAME);
                            }
                        }),
                new Method("guild.owner.id",
                        env -> ((Guild) env.get("guild")).getOwnerId(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getOwnerId)
                                        .orElse(Constant.Discord.DELETED_USER_ID + "");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getOwnerId();
                            }
                        }),
                new Method("guild.description",
                        env -> ((Guild) env.get("guild")).getDescription(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getDescription)
                                        .orElse("");
                            } catch (NullPointerException | NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getDescription();
                            }
                        }),
                new Method("guild.afkchannel",
                        env -> ((Guild) env.get("guild")).getAfkChannel().getName(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getAfkChannel)
                                        .map(VoiceChannel::getName)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return Optional.ofNullable(jda.getGuildsByName(in[0], true).get(0))
                                        .map(Guild::getAfkChannel)
                                        .map(VoiceChannel::getName)
                                        .orElse("");
                            }
                        }),
                new Method("guild.afktimeout",
                        env -> ((Guild) env.get("guild")).getAfkTimeout().getSeconds() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getAfkTimeout)
                                        .map(Guild.Timeout::getSeconds)
                                        .map(Object::toString)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getAfkTimeout().getSeconds() + "";
                            }
                        }),
                new Method("guild.defaultchannel",
                        env -> Optional.ofNullable(((Guild) env.get("guild")).getDefaultChannel())
                                .map(DefaultGuildChannelUnion::getName)
                                .orElse(""),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getDefaultChannel)
                                        .map(DefaultGuildChannelUnion::getName)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return Optional.ofNullable(jda.getGuildsByName(in[0], true).get(0))
                                        .map(Guild::getDefaultChannel)
                                        .map(DefaultGuildChannelUnion::getName)
                                        .orElse("");
                            }
                        }),
                new Method("guild.verificationlevel",
                        env -> ((Guild) env.get("guild")).getVerificationLevel().name(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getVerificationLevel)
                                        .map(Guild.VerificationLevel::name)
                                        .orElse("");
                            } catch (NullPointerException | NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getVerificationLevel().name();
                            }
                        }),
                new Method("guild.vanityurl",
                        env -> ((Guild) env.get("guild")).getVanityUrl(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getVanityUrl)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getVanityUrl();
                            }
                        }),
                new Method("guild.boosters",
                        (env, in) -> {
                            if (in.length == 1) {
                                return ((Guild) env.get("guild")).getBoosters().get(Integer.parseInt(in[0])).getAsMention();
                            }
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getBoosters)
                                        .map(list -> list.get(Integer.parseInt(in[1])).getAsMention())
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getBoosters().get(Integer.parseInt(in[1])).getAsMention();
                            }
                        }, "."),
                new Method("guild.boosters.count",
                        env -> ((Guild) env.get("guild")).getBoosters().size() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getBoosters)
                                        .map(List::size)
                                        .map(Object::toString)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getBoosters().size() + "";
                            }
                        }),
                new Method("guild.boosters.level",
                        env -> ((Guild) env.get("guild")).getBoostTier().name(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getBoostTier)
                                        .map(Guild.BoostTier::name)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getBoostTier().name();
                            }
                        }),
                new Method("guild.banner",
                        env -> ((Guild) env.get("guild")).getBannerUrl(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getBannerUrl)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getBannerUrl();
                            }
                        }),
                new Method("guild.splash",
                        env -> ((Guild) env.get("guild")).getSplashId(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getSplashId)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getSplashId();
                            }
                        }),
                new Method("guild.members.count",
                        env -> ((Guild) env.get("guild")).getMembers().size() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getGuildById(in[0]))
                                        .map(Guild::getMembers)
                                        .map(List::size)
                                        .map(Object::toString)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getGuildsByName(in[0], true).get(0).getMembers().size() + "";
                            }
                        })
        );
    }

    public static Collection<Method> channelMethod() {
        return List.of(
                new Method("channel.name",
                        env -> ((TextChannel) env.get("channel")).getName(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.getTextChannelById(Long.parseLong(in[0])).getName();
                            } catch (NullPointerException | NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getName();
                            }
                        }),
                new Method("channel.topic",
                        env -> ((TextChannel) env.get("channel")).getTopic(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getTextChannelById(Long.parseLong(in[0])))
                                        .map(TextChannel::getTopic)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getTopic();
                            }
                        }),
                new Method("channel.guild",
                        env -> ((TextChannel) env.get("channel")).getGuild().getName(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getTextChannelById(Long.parseLong(in[0])))
                                        .map(TextChannel::getGuild)
                                        .map(Guild::getName)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getGuild().getName();
                            }
                        }),
                new Method("channel.position",
                        env -> ((TextChannel) env.get("channel")).getPosition() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getTextChannelById(Long.parseLong(in[0])))
                                        .map(TextChannel::getPosition)
                                        .map(Object::toString)
                                        .orElse("");
                            } catch (NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getPosition() + "";
                            }
                        }),
                new Method("channel.nsfw",
                        env -> ((TextChannel) env.get("channel")).isNSFW() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return Optional.ofNullable(jda.getTextChannelById(Long.parseLong(in[0])))
                                        .map(TextChannel::isNSFW)
                                        .map(Object::toString)
                                        .orElse("false");
                            } catch (NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).isNSFW() + "";
                            }
                        }),
                new Method("channel.slowmode",
                        env -> ((TextChannel) env.get("channel")).getSlowmode() + "",
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.getTextChannelById(Long.parseLong(in[0])).getSlowmode() + "";
                            } catch (NullPointerException | NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getSlowmode() + "";
                            }
                        }),
                new Method("channel.mention",
                        env -> ((TextChannel) env.get("channel")).getAsMention(),
                        (env, in) -> {
                            JDA jda = env.get("jda");
                            try {
                                return jda.getTextChannelById(Long.parseLong(in[0])).getAsMention();
                            } catch (NullPointerException | NumberFormatException e) {
                                return jda.getTextChannelsByName(in[0], true).get(0).getAsMention();
                            }
                        })
        );
    }

    public static Collection<Method> allMethods() {
        Collection<Method> methods = new ArrayList<>();
        methods.addAll(jdaMethod());
        methods.addAll(userMethod());
        methods.addAll(guildMethod());
        methods.addAll(channelMethod());
        return methods;
    }
}
