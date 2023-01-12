/*
 * TempChannel
 *
 * 0.0.1
 *
 * 12/12/2022
 */
package fr.enimaloc.esportlinebot.module.tempChannel;

import fr.enimaloc.enutils.jda.annotation.*;
import fr.enimaloc.enutils.jda.entities.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.utils.Checks;
import fr.enimaloc.enutils.jda.utils.ChecksWR;
import fr.enimaloc.esportlinebot.settings.Settings;
import me.jagrosh.jagtag.Parser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.IMemberContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

/**
 *
 */
@SlashCommand(description = "Temporary channel commands related")
public class TempChannel {
    public static final Supplier<NoSuchElementException> NO_TEMP_CHANNEL =
            () -> new NoSuchElementException("No temp channel found");
    public static final Logger                           LOGGER          = LoggerFactory.getLogger(TempChannel.class);

    private final Settings.TempChannel             settings;
    private final Connection                       sql;
    private final List<Instance>                   instances = new ArrayList<>();
    private final Map<Long, Map<String, Template>> templates = new HashMap<>();

    @SlashCommand.GroupProvider(description = "Temporary channel blacklist commands related")
    public final Blacklist   blacklist = new Blacklist();
    @SlashCommand.GroupProvider(description = "Temporary channel whitelist  commands related")
    public final Whitelist   whitelist = new Whitelist();
    @SlashCommand.GroupProvider(description = "Temporary channel template commands related")
    public final TemplateCmd template  = new TemplateCmd();

    @SlashCommand.Sub(name = "private", description = "Define if the channel is private")
    public void private0(GuildSlashCommandEvent event, @SlashCommand.Option(name = "private") boolean private0) {
        Optional.ofNullable(event.getMember())
                .map(ISnowflake::getIdLong)
                .flatMap(this::getInstanceByOwner)
                .orElseThrow(NO_TEMP_CHANNEL)
                .setPrivate(event.getJDA(), private0).save(sql);
        event.replyEphemeral("The channel is now " + (private0 ? "private" : "public")).queue();
    }

    public TempChannel(Settings.TempChannel settings, Connection sql) {
        this.settings = settings;
        this.sql = sql;
    }

    @Init
    public void init(JDA jda) {
        VoiceChannel channel = jda.getVoiceChannelById(settings.triggerChannel());
        if (settings.enabled() && channel == null) {
            settings.enabled = false;
            throw new IllegalStateException("Trigger channel not found, disabling temp channel");
        }
        try (Statement statement = sql.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS tc_instances (" +
                    "channel_id TEXT NOT NULL," +
                    "user_id TEXT NOT NULL," +
                    "state INT DEFAULT 0," +
                    "PRIMARY KEY (channel_id, user_id)" +
                    ");");
            statement.execute("CREATE TABLE IF NOT EXISTS tc_templates (" +
                    "template_name TEXT NOT NULL," +
                    "owner_id INT NOT NULL," +
                    "channel_name TEXT NOT NULL," +
                    "blacklist TEXT DEFAULT NULL," +
                    "whitelist TEXT DEFAULT NULL," +
                    "private INT DEFAULT 0 NOT NULL, " +
                    "user_limit INT DEFAULT 0 NOT NULL," +
                    "nsfw INT DEFAULT 0 NOT NULL," +
                    "region TEXT DEFAULT NULL," +
                    "PRIMARY KEY (owner_id)" +
                    ");");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        instances.addAll(Instance.loadAll(sql, jda));
        jda.getUsers().stream().map(ISnowflake::getIdLong).forEach(id -> templates.put(id, Template.loadOf(sql, id)));

        jda.getGuilds()
                .stream()
                .map(guild -> guild.getVoiceChannelById(settings.triggerChannel()))
                .filter(Objects::nonNull)
                .map(IMemberContainer::getMembers)
                .flatMap(Collection::stream)
                .forEach(this::newChannel);
    }

    @On
    public void onJoin(GuildVoiceUpdateEvent event) {
        if (event.getNewValue() == null) {
            return;
        }
        Optional<Instance> instance = getInstanceByChannel(event.getNewValue().getIdLong());
        if (event.getNewValue().getIdLong() == settings.triggerChannel()) {
            newChannel(event.getMember());
        } else if (instance.isPresent()) {
            event.getMember().mute(instance.get().isMuted(event.getMember().getIdLong()))
                    .reason("Restore mute state")
                    .queue();
            event.getMember().deafen(instance.get().isDeafened(event.getMember().getIdLong()))
                    .reason("Restore deafen state")
                    .queue();
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @On
    public void onLeave(GuildVoiceUpdateEvent event) {
        if (event.getOldValue() == null || getInstanceByChannel(event.getOldValue().getIdLong()).isEmpty()) {
            return;
        }
        Optional<Instance> instance = getInstanceByChannel(event.getOldValue().getIdLong());
        if (event.getOldValue().getMembers().isEmpty()) {
            event.getOldValue().delete().queue();
            instances.removeIf(i -> i.getChannelId() == event.getOldValue().getIdLong());
        }
        if (instance.get().isMuted(event.getMember().getIdLong())) {
            event.getMember().mute(false)
                    .reason("Reset mute state")
                    .queue();
        }
        if (instance.get().isDeafened(event.getMember().getIdLong())) {
            event.getMember().deafen(false)
                    .reason("Reset deafen state")
                    .queue();
        }
    }

    @On
    public void onMute(GuildVoiceGuildMuteEvent event) {
        Optional.ofNullable(event.getVoiceState().getChannel())
                .map(ISnowflake::getIdLong)
                .flatMap(this::getInstanceByChannel)
                .filter(i -> i.isMuted(event.getMember().getIdLong()) != event.isGuildMuted())
                .ifPresent(instance -> instance.setMuted(event.getMember().getIdLong(), event.isGuildMuted()));
    }

    @On
    public void onDeafen(GuildVoiceGuildDeafenEvent event) {
        Optional.ofNullable(event.getVoiceState().getChannel())
                .map(ISnowflake::getIdLong)
                .flatMap(this::getInstanceByChannel)
                .filter(i -> i.isDeafened(event.getMember().getIdLong()) != event.isGuildDeafened())
                .ifPresent(i -> i.setDeafened(event.getMember().getIdLong(), event.isGuildDeafened()).save(sql));
    }

    // FIXME: 09/01/2023 Not called see jda-enutils to fix it
    @Catch(NoSuchElementException.class)
    public void onInstanceNotFound(GuildSlashCommandEvent event, NoSuchElementException exception) {
        event.replyEphemeral("You don't have any temp channel").queue();
    }

    private void newChannel(Member member) {
        if (!settings.enabled()) {
            return;
        }
        Optional<Instance> filter = getInstanceByOwner(member.getIdLong());
        if (filter.isPresent()) {
            Optional<VoiceChannel> channel = filter.map(Instance::getChannelId).map(member.getJDA()::getVoiceChannelById);
            channel.ifPresentOrElse(
                    vc -> vc.getGuild().moveVoiceMember(member, vc).queue(),
                    () -> instances.remove(filter.get())
            );
            if (channel.isPresent()) {
                return;
            }
        }
        Parser parser = settings.getParser();
        parser.put("jda", member.getJDA());
        parser.put("user", member.getUser());
        parser.put("member", member);
        parser.put("guild", member.getGuild());
        Integer pos = settings.position();
        if (pos < 0) {
            pos = getCategory(member).map(Category::getVoiceChannels)
                    .map(l -> l.get(l.size() - 1))
                    .orElse(member.getGuild().getVoiceChannels().get(member.getGuild().getVoiceChannels().size() - 1))
                    .getPositionRaw() + pos;
        } else if (pos == 0) {
            pos = null;
        }
        if (pos != null) {
            pos += getCategory(member).map(Category::getVoiceChannels)
                    .map(l -> l.get(0))
                    .orElse(member.getGuild().getVoiceChannels().get(0))
                    .getPositionRaw() - 2;
        }
        ChannelAction<VoiceChannel> action = member.getGuild()
                .createVoiceChannel(parser.parse(settings.template()))
                .addMemberPermissionOverride(member.getIdLong(), List.of(Permission.MANAGE_CHANNEL, Permission.VOICE_MUTE_OTHERS, Permission.VOICE_DEAF_OTHERS), null)
                .reason("Temporary channel created by " + member.getUser().getAsTag())
                .setPosition(pos);
        //noinspection ResultOfMethodCallIgnored
        getCategory(member).ifPresent(category -> action.setParent(category).syncPermissionOverrides());
        action.flatMap(channel -> {
                    instances.add(new Instance(channel.getIdLong()).setOwner(member.getIdLong()).save(sql));
                    return channel.getGuild().moveVoiceMember(member, channel);
                })
                .queue();
    }

    @NotNull
    private Optional<Category> getCategory(Member member) {
        return Optional.ofNullable(member.getGuild().getCategoryById(settings.categoryId()));
    }

    private Optional<Instance> getInstanceByOwner(long owner) {
        return instances.stream()
                .filter(i -> i.getOwnerId() == owner)
                .findFirst();
    }

    private Optional<Instance> getInstanceByChannel(long channelId) {
        return instances.stream()
                .filter(i -> i.getChannelId() == channelId)
                .findFirst();
    }

    private Optional<Instance> getInstanceByChannelAndOwner(long channelId, long ownerId) {
        return instances.stream()
                .filter(i -> i.getChannelId() == channelId)
                .filter(i -> i.getOwnerId() == ownerId)
                .findFirst();
    }

    public class Blacklist {
        @SlashCommand.Sub(description = "Add an user or role to the blacklist")
        public void add(GuildSlashCommandEvent event, @SlashCommand.Option IMentionable target) {
            checks();

            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .addBlacklist(target).save(sql);
            event.replyEphemeral("Member added to blacklist").queue();
        }

        @SlashCommand.Sub(description = "Remove an user or role from the blacklist")
        public void remove(GuildSlashCommandEvent event, @SlashCommand.Option IMentionable target) {
            checks();

            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .removeBlacklist(target).save(sql);
            event.replyEphemeral("Member removed from blacklist").queue();
        }

        @SlashCommand.Sub(description = "List the blacklist")
        public void list(GuildSlashCommandEvent event) {
            checks();

            StringJoiner builder = new StringJoiner("\n - ");
            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .getBlacklist()
                    .forEach(id -> builder.add("<@" + (isRole(event.getJDA(), id) ? "&" : "") + id + ">"));
            event.replyEphemeral("Blacklisted members:\n - " + builder).queue();
        }
    }

    public class Whitelist {
        @SlashCommand.Sub(description = "Add an user or role to the whitelist")
        public void add(GuildSlashCommandEvent event, @SlashCommand.Option IMentionable target) {
            checks();

            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .addWhitelist(target).save(sql);
            event.replyEphemeral("Member added to whitelist").queue();
        }

        @SlashCommand.Sub(description = "Remove an user or role from the whitelist")
        public void remove(GuildSlashCommandEvent event, @SlashCommand.Option IMentionable target) {
            checks();

            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .removeWhitelist(target).save(sql);
            event.replyEphemeral("Member removed from whitelist").queue();
        }

        @SlashCommand.Sub(description = "List the whitelist")
        public void list(GuildSlashCommandEvent event) {
            checks();

            StringJoiner builder = new StringJoiner("\n - ");
            Optional.ofNullable(event.getMember())
                            .map(ISnowflake::getIdLong)
                                    .flatMap(TempChannel.this::getInstanceByOwner)
                    .orElseThrow(NO_TEMP_CHANNEL)
                    .getWhitelist()
                    .forEach(id -> builder.add("<@" + (isRole(event.getJDA(), id) ? "&" : "") + id + ">"));
            event.replyEphemeral("Whitelisted members:\n - " + builder).queue();
        }
    }

    public class TemplateCmd {
        @SlashCommand.Sub(description = "Save the current channel as template")
        public void save(GuildSlashCommandEvent event, @SlashCommand.Option String name) {
            checks(event.getUser().getIdLong());

            templates.get(event.getUser().getIdLong()).put(name,
                    Template.fromInstance(event.getJDA(), name, getInstanceByOwner(event.getUser().getIdLong())
                                    .orElseThrow(NO_TEMP_CHANNEL))
                            .save(sql));
            event.replyEphemeral("Template saved as " + name).queue();
        }

        @SlashCommand.Sub(description = "Delete a template")
        public void delete(GuildSlashCommandEvent event,
                           @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("nameAutocompletion"))) String name) {
            checks(event.getUser().getIdLong());
            Checks.contains(name, templates.get(event.getUser().getIdLong()).keySet(), "Template not found");

            try (Statement statement = sql.createStatement()) {
                statement.execute("DELETE FROM tc_templates WHERE owner_id = " + event.getMember().getIdLong() + " AND template_name = '" + name + "'");
                templates.get(event.getMember().getIdLong()).remove(name);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            event.replyEphemeral("Template removed").queue();
        }

        @SlashCommand.Sub(description = "List all templates")
        public void list(GuildSlashCommandEvent event) {
            checks(event.getUser().getIdLong());

            StringJoiner joiner = new StringJoiner("\n - ");
            templates.get(event.getUser().getIdLong()).keySet().forEach(joiner::add);
            event.replyEphemeral("Templates:\n - " + joiner).queue();
        }

        @SlashCommand.Sub(description = "Load a template")
        public void load(GuildSlashCommandEvent event,
                         @SlashCommand.Option(autoCompletion = @SlashCommand.Option.AutoCompletion(target = @MethodTarget("nameAutocompletion"))) String name) {
            checks(event.getUser().getIdLong());
            Checks.contains(name, templates.get(event.getUser().getIdLong()).keySet(), "Template not found");

            templates.get(event.getUser().getIdLong()).get(name).apply(event.getJDA(),
                    getInstanceByOwner(event.getUser().getIdLong())
                            .orElseThrow(NO_TEMP_CHANNEL));
            event.replyEphemeral("Template '" + name + "' loaded").queue();
        }

        public String[] nameAutocompletion(CommandAutoCompleteInteractionEvent event) {
            return templates.get(event.getUser().getIdLong())
                    .keySet()
                    .stream()
                    .filter(name -> name.startsWith(event.getFocusedOption().getValue()))
                    .toArray(String[]::new);
        }

        private void checks(long memberId) {
            if (!templates.containsKey(memberId)) {
                templates.put(memberId, new HashMap<>());
            }
            TempChannel.this.checks();
        }
    }

    private void checks() {
        Checks.check(settings.enabled(), "Temp channels are disabled");
    }

    public static class Instance {

        private final long channelId;

        private final List<Long> muted    = new ArrayList<>();
        private final List<Long> deafened = new ArrayList<>();

        private List<Long> blacklist = new ArrayList<>();
        private List<Long> whitelist = new ArrayList<>();

        private final List<Long> visitors = new ArrayList<>();
        private       long       ownerId;

        private boolean private0;

        public Instance(long channel) {
            this.channelId = channel;
        }

        public long getChannelId() {
            return channelId;
        }

        public VoiceChannel getChannel(JDA jda) {
            return ChecksWR.notNull(jda.getVoiceChannelById(getChannelId()),
                    "Voice channel can't be null, need destroy instance");
        }

        public Instance save(Connection sql) {
            List<Long> allUsers = getAllUsers();

            final String RQ     = "REPLACE INTO tc_instances (channel_id, user_id, state) VALUES ";
            StringJoiner values = new StringJoiner(", ");
            allUsers.forEach(unused -> values.add("(?, ?, ?)"));

            try (PreparedStatement statement = sql.prepareStatement(RQ + values)) {
                int shift = 0;
                for (long allUser : allUsers) {
                    statement.setLong(++shift, getChannelId());
                    statement.setLong(++shift, allUser);
                    long[] bits = buildBitSetFor(allUser).toLongArray();
                    statement.setLong(++shift, bits.length > 0 ? bits[0] : 0);
                }
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        private List<Long> getAllUsers() {
            List<Long> allUsers = new ArrayList<>();
            allUsers.add(getOwnerId());
            allUsers.addAll(getBlacklist());
            allUsers.addAll(getWhitelist());
            allUsers.addAll(visitors);
            return allUsers;
        }

        private BitSet buildBitSetFor(long userId) {
            BitSet bitSet = new BitSet(5);
            bitSet.set(0, muted.contains(userId));
            bitSet.set(1, deafened.contains(userId));

            bitSet.set(2, userId == getOwnerId());
            if (userId == getOwnerId()) {
                bitSet.set(3, isPrivate());
            } else {
                bitSet.set(3, blacklist.contains(userId));
                bitSet.set(4, whitelist.contains(userId));
            }
            return bitSet;
        }

        private void buildUserFromBitSet(long userId, BitSet bitSet) {
            if (bitSet.get(0)) {
                muted.add(userId);
            }
            if (bitSet.get(1)) {
                deafened.add(userId);
            }

            if (bitSet.cardinality() == 0) {
                visitors.add(userId);
            } else if (bitSet.get(2)) {
                setOwner(userId);
                private0 = bitSet.get(3);
            } else {
                if (bitSet.get(3)) {
                    blacklist.add(userId);
                }
                if (bitSet.get(4)) {
                    whitelist.add(userId);
                }
            }
        }

        public static List<Instance> loadAll(Connection sql, JDA jda) {
            try (ResultSet result = sql.createStatement().executeQuery("SELECT DISTINCT channel_id FROM tc_instances;")) {
                List<Instance> instances = new ArrayList<>();
                while (result.next()) {
                    Instance instance = new Instance(result.getLong(1));
                    if (instance.invalid(jda) || instance.canBeDestroyed(jda)) {
                        instance.destroy(sql, jda);
                    } else {
                        instances.add(instance.load(sql));
                    }
                }
                LOGGER.info("Loaded {} instances", instances.size());
                if (LOGGER.isDebugEnabled()) {
                    instances.forEach(instance -> LOGGER.debug("-> {}", instance));
                }
                return instances;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private void destroy(Connection sql, JDA jda) {
            try (PreparedStatement statement = sql.prepareStatement("DELETE FROM tc_instances WHERE channel_id = ?")) {
                statement.setLong(1, getChannelId());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            if (!invalid(jda)) {
                getChannel(jda).delete().queue();
            }
        }

        public Instance load(Connection sql) {
            try (ResultSet result = sql.createStatement().executeQuery("SELECT user_id, state FROM tc_instances WHERE channel_id = " + getChannelId() + ";")) {
                while (result.next()) {
                    long userId = result.getLong(1);
                    buildUserFromBitSet(userId, BitSet.valueOf(new long[]{result.getLong(2)}));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public long getOwnerId() {
            return ownerId;
        }

        public boolean invalid(JDA jda) {
            return jda.getVoiceChannelById(getChannelId()) == null;
        }

        public boolean canBeDestroyed(JDA jda) {
            return getChannel(jda).getMembers().isEmpty();
        }

        public Instance setOwner(long owner) {
            this.ownerId = owner;
            return this;
        }

        public List<Long> getBlacklist() {
            return blacklist;
        }

        public List<Long> getWhitelist() {
            return whitelist;
        }

        public boolean isPrivate() {
            return private0;
        }

        public Instance setPrivate(JDA jda, boolean private0) {
            this.private0 = private0;
            if (private0) {
                getChannel(jda).upsertPermissionOverride(getChannel(jda).getGuild().getPublicRole())
                        .deny(Permission.VOICE_CONNECT)
                        .queue();
            } else {
                resetPermission(getChannel(jda).getGuild().getPublicRole(),
                        new Permission[0],
                        new Permission[]{Permission.VOICE_CONNECT});
            }
            return this;
        }

        public Instance addBlacklist(IMentionable mentionable) {
            Checks.notContains(mentionable.getIdLong(), blacklist, "Member already in blacklist");
            visitors.remove(mentionable.getIdLong());
            whitelist.remove(mentionable.getIdLong());
            blacklist.add(mentionable.getIdLong());
            getChannel(((IPermissionHolder) mentionable).getGuild().getJDA()).upsertPermissionOverride((IPermissionHolder) mentionable)
                    .deny(Permission.VOICE_CONNECT)
                    .queue();
            return this;
        }

        public Instance removeBlacklist(IMentionable mentionable) {
            Checks.contains(mentionable.getIdLong(), blacklist, "Target not in blacklist");
            blacklist.remove(mentionable.getIdLong());
            visitors.add(mentionable.getIdLong());
            getChannel(((IPermissionHolder) mentionable).getGuild().getJDA()).upsertPermissionOverride((IPermissionHolder) mentionable)
                    .queue();
            resetPermission((IPermissionHolder) mentionable, new Permission[0], new Permission[]{Permission.VOICE_CONNECT});
            return this;
        }

        public Instance addWhitelist(IMentionable mentionable) {
            Checks.notContains(mentionable.getIdLong(), whitelist, "Target already in whitelist");
            visitors.remove(mentionable.getIdLong());
            blacklist.remove(mentionable.getIdLong());
            whitelist.add(mentionable.getIdLong());
            getChannel(((IPermissionHolder) mentionable).getGuild().getJDA()).upsertPermissionOverride((IPermissionHolder) mentionable)
                    .grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT)
                    .queue();
            return this;
        }

        public Instance removeWhitelist(IMentionable mentionable) {
            Checks.contains(mentionable.getIdLong(), whitelist, "Target not in whitelist");
            whitelist.remove(mentionable.getIdLong());
            visitors.add(mentionable.getIdLong());
            resetPermission((IPermissionHolder) mentionable, new Permission[]{Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT}, new Permission[0]);
            return this;
        }

        private void resetPermission(IPermissionHolder holder, Permission[] removedAllowed, Permission[] removedDenied) {
            Collection<Permission> allowed = null;
            Collection<Permission> denied  = null;
            PermissionOverride     override;
            JDA                    jda     = holder.getGuild().getJDA();
            if ((getChannel(jda).getParentCategory() != null
                    && (override = Objects.requireNonNull(getChannel(jda)
                    .getParentCategory()).getPermissionOverride(holder)) != null)) {
                allowed = override.getAllowed();
                denied = override.getDenied();
            } else if ((override = getChannel(jda).getPermissionOverride(holder)) != null) {
                allowed = override.getAllowed().stream().filter(perm -> !Arrays.asList(removedAllowed).contains(perm)).toList();
                denied = override.getDenied().stream().filter(perm -> !Arrays.asList(removedDenied).contains(perm)).toList();
            } else if ((override = getChannel(jda).getPermissionOverride(holder.getGuild().getPublicRole())) != null) {
                allowed = override.getAllowed();
                denied = override.getDenied();
            }
            getChannel(jda).upsertPermissionOverride(holder)
                    .setPermissions(allowed, denied)
                    .queue();
        }

        public Instance setMuted(long userId, boolean muted) {
            if (!getAllUsers().contains(userId)) {
                visitors.add(userId);
            }
            if (muted) {
                this.muted.add(userId);
            } else {
                this.muted.remove(userId);
            }
            return this;
        }

        public boolean isMuted(long userId) {
            return muted.contains(userId);
        }

        public Instance setDeafened(long userId, boolean deafened) {
            if (!getAllUsers().contains(userId)) {
                visitors.add(userId);
            }
            if (deafened) {
                this.deafened.add(userId);
            } else {
                this.deafened.remove(userId);
            }
            return this;
        }

        public boolean isDeafened(long userId) {
            return deafened.contains(userId);
        }

        @Override
        public String toString() {
            return "Instance{" +
                    "channelId=" + channelId +
                    ", muted=" + muted +
                    ", deafened=" + deafened +
                    ", blacklist=" + blacklist +
                    ", whitelist=" + whitelist +
                    ", visitors=" + visitors +
                    ", ownerId=" + ownerId +
                    ", private0=" + private0 +
                    '}';
        }

    }

    public record Template(
            String templateName,
            long ownerId,
            String channelName,
            List<Long> blacklist,
            List<Long> whitelist,
            boolean private0,
            int userLimit,
            boolean nsfw,
            Region region
    ) {
        public static Template fromInstance(JDA jda, String name, Instance instance) {
            return new Template(
                    name,
                    instance.ownerId,
                    instance.getChannel(jda).getName(),
                    instance.blacklist,
                    instance.whitelist,
                    instance.private0,
                    instance.getChannel(jda).getUserLimit(),
                    instance.getChannel(jda).isNSFW(),
                    instance.getChannel(jda).getRegion()
            );
        }

        public static Map<String, Template> loadOf(Connection sql, long id) {
            Map<String, Template> map = new HashMap<>();
            try (ResultSet result = sql.createStatement().executeQuery(
                    "SELECT * FROM tc_templates WHERE owner_id = " + id + ";")) {
                while (result.next()) {
                    map.put(result.getString("template_name"), new Template(
                            result.getString("template_name"),
                            result.getLong("owner_id"),
                            result.getString("channel_name"),
                            result.getString("blacklist").isEmpty()
                                    ? new ArrayList<>()
                                    : Arrays.stream(result.getString("blacklist").split(";"))
                                    .map(Long::parseLong)
                                    .toList(),
                            result.getString("whitelist").isEmpty()
                                    ? new ArrayList<>()
                                    : Arrays.stream(result.getString("whitelist").split(";"))
                                    .map(Long::parseLong)
                                    .toList(),
                            result.getBoolean("private"),
                            result.getInt("user_limit"),
                            result.getBoolean("nsfw"),
                            Region.fromKey(result.getString("region"))
                    ));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return map;
        }

        public Template save(Connection sql) {
            try (PreparedStatement statement = sql.prepareStatement("INSERT INTO tc_templates VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                statement.setString(1, templateName);
                statement.setLong(2, ownerId);
                statement.setString(3, channelName);
                statement.setString(4, String.join(";", blacklist.stream().map(String::valueOf).toList()));
                statement.setString(5, String.join(";", whitelist.stream().map(String::valueOf).toList()));
                statement.setBoolean(6, private0);
                statement.setInt(7, userLimit);
                statement.setBoolean(8, nsfw);
                statement.setString(9, region.getKey());
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public void apply(JDA jda, Instance instance) {
            instance.getChannel(jda)
                    .getManager()
                    .setName(channelName)
                    .setUserLimit(userLimit)
                    .setNSFW(nsfw)
                    .setRegion(region)
                    .reason("Applying template " + channelName)
                    .queue();
            instance.whitelist = whitelist;
            instance.blacklist = blacklist;
            instance.private0 = private0;
        }

    }

    private static boolean isRole(JDA jda, long id) {
        return jda.getRoleById(id) != null;
    }
}
