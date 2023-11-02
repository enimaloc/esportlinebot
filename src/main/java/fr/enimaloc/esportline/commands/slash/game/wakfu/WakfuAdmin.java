package fr.enimaloc.esportline.commands.slash.game.wakfu;

import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.api.wakfu.WakfuAsset;
import fr.enimaloc.esportline.api.wakfu.WakfuBreeds;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO: 02/11/23 i18n and arg name
public class WakfuAdmin {
    public static final Logger LOGGER = LoggerFactory.getLogger(WakfuAdmin.class);

    // TODO: 02/11/23 Add this to config file
    public static final long WAKFU_ROLE_ID = 1169416037410807839L;
    public static final long WAKFU_ADMIN_ROLE_ID = 1169416034290245722L;

    public static final long WAKFU_MEMBER_CHANNEL_ID = 1169416543478759555L;
    public static final long WAKFU_JOB_CHANNEL_ID = 1169439177251762346L;

    public static final long CONST_EMOJI_SERVER_ID = 1169039013357813932L;

    private final Connection connection;

    private final List<Player> players = new ArrayList<>();

    private long playersMessageId = 0;
    private long jobsMessageId = 0;

    enum Job {
        GUNSMITH(77), // 0
        JEWELER(78), // 1
        BAKER(40), // 2
        COOK(76), // 3
        MASTER_OF_ARMS(68), // 4
        LEATHERWORKER(80), // 5
        STONECUTTER(83), // 6
        CABINETMAKER(81), // 7
        FORESTER(71), // 8
        HERBALIST(72), // 9
        MINER(73), // 10
        PEASANT(64), // 11
        FISHERMAN(75), // 12
        TRAPPER(74); // 13

        private final int assetId;

        Job(int assetId) {
            this.assetId = assetId;
        }

        public Optional<byte[]> getAsset(WakfuAsset assetRepository) {
            return assetRepository.getAsset("jobs", assetId);
        }

        public Emoji getEmoji(JDA jda) {
            return jda.getGuildById(CONST_EMOJI_SERVER_ID)
                    .getEmojisByName(name().toLowerCase(), true)
                    .get(0);
        }

        public static List<SelectOption> getAsOption(JDA jda) {
            return getAsOption(jda, unused -> true);
        }

        public static List<SelectOption> getAsOption(JDA jda, Predicate<? super Job> filter) {
            return Arrays.stream(Job.values())
                    .filter(filter)
                    .map(job -> SelectOption.of(job.name().substring(0, 1).toUpperCase() + job.name().substring(1).toLowerCase(),
                                    String.valueOf(job.ordinal()))
                            .withEmoji(job.getEmoji(jda)))
                    .toList();
        }
    }

    class Player {
        public int id;
        public long discordId;
        public String name;
        public List<WakfuBreeds> breeds;
        public Map<Job, Integer> jobs;
    }

    private Player getPlayer(int id) {
        return players.stream().filter(player -> player.id == id).findFirst().orElse(null);
    }

    private Player getPlayer(long discordId) {
        return players.stream().filter(player -> player.discordId == discordId).findFirst().orElse(null);
    }

    private Player getPlayer(User user) {
        return getPlayer(user.getIdLong());
    }

    private Player getPlayer(Member member) {
        return getPlayer(member.getIdLong());
    }

    private Player getPlayer(String name) {
        return players.stream().filter(player -> player.name.equals(name)).findFirst().orElse(null);
    }


    public WakfuAdmin(JDA jda, Connection connection) {
        this.connection = connection;
        setupDatabase();
        loadDatabase();
        loadData(jda);
        setupData(jda);
        setupEmoji(jda);
    }

    private void setupDatabase() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS player (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "discord_id INTEGER," +
                    "name TEXT" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS player_breeds (" +
                    "id INTEGER, " +
                    "breed INTEGER, " +
                    "FOREIGN KEY(id) REFERENCES player(id))");
            statement.execute("CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "id INTEGER, " +
                    "job INTEGER, " +
                    "level INTEGER, " +
                    "FOREIGN KEY(id) REFERENCES player(id))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadDatabase() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT * FROM player");
            ResultSet result = statement.getResultSet();
            while (result.next()) {
                Player player = new Player();
                player.id = result.getInt("id");
                player.discordId = result.getLong("discord_id");
                player.name = result.getString("name");
                player.breeds = new ArrayList<>();
                player.jobs = new HashMap<>();
                players.add(player);
            }
            statement.execute("SELECT * FROM player_breeds");
            result = statement.getResultSet();
            while (result.next()) {
                int id = result.getInt("id");
                int breed = result.getInt("breed");
//                players.compute(discordId, (k, v) -> {
//                    if (v == null) {
//                        v = new ArrayList<>();
//                    }
//                    v.add(breed == -1 ? null : WakfuBreeds.values()[breed]);
//                    return v;
//                });
                getPlayer(id).breeds.add(breed == -1 ? null : WakfuBreeds.values()[breed]);
            }
            statement.execute("SELECT * FROM player_jobs NATURAL JOIN player");
            result = statement.getResultSet();
            while (result.next()) {
                int id = result.getInt("id");
                int job = result.getInt("job");
                int level = result.getInt("level");
//                jobs.compute(discordId, (k, v) -> {
//                    if (v == null) {
//                        v = new HashMap<>();
//                    }
//                    v.put(Job.values()[job], level);
//                    return v;
//                });
                getPlayer(id).jobs.put(Job.values()[job], level);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData(JDA jda) {
        jda.getTextChannelById(WAKFU_MEMBER_CHANNEL_ID)
                .getHistory()
                .retrievePast(100)
                .complete()
                .stream()
                .filter(message -> message.getAuthor().getIdLong() == jda.getSelfUser().getIdLong())
                .findFirst()
                .ifPresent(message -> playersMessageId = message.getIdLong());
        jda.getTextChannelById(WAKFU_JOB_CHANNEL_ID)
                .getHistory()
                .retrievePast(100)
                .complete()
                .stream()
                .filter(message -> message.getAuthor().getIdLong() == jda.getSelfUser().getIdLong())
                .findFirst()
                .ifPresent(message -> jobsMessageId = message.getIdLong());
    }

    private void setupData(JDA jda) {
        if (playersMessageId == 0) {
            setupPlayersMessage(jda);
        } else {
            updatePlayersMessage(jda);
        }
        if (jobsMessageId == 0) {
            setupJobsMessage(jda);
        } else {
            updateJobsMessage(jda);
        }
    }

    private void setupEmoji(JDA jda) {
        List<RichCustomEmoji> emojis = jda.getGuildById(CONST_EMOJI_SERVER_ID)
                .getEmojis();
        for (Job value : Job.values()) {
            if (emojis.stream().map(Emoji::getName).noneMatch(name -> name.equals(value.name().toLowerCase()))) {
                value.getAsset(new WakfuAsset()).ifPresent(bytes -> {
                    jda.getGuildById(CONST_EMOJI_SERVER_ID)
                            .createEmoji(value.name().toLowerCase(), Icon.from(bytes))
                            .queue();
                });
            }
        }
    }

    public Emoji getEmoji(JDA jda, Job job) {
        return jda.getGuildById(CONST_EMOJI_SERVER_ID)
                .getEmojisByName(job.name().toLowerCase(), true)
                .get(0);
    }

    // region Players list related
    private void setupPlayersMessage(JDA jda) {
        jda.getTextChannelById(WAKFU_MEMBER_CHANNEL_ID)
                .sendMessage(buildMembersMessage(jda))
                .mentionUsers(0)
                .queue(message -> playersMessageId = message.getIdLong());
    }

    public void updatePlayersMessage(JDA jda) {
        if (playersMessageId == 0) {
            throw new IllegalStateException("The players message id is not set!");
        }
        jda.getTextChannelById(WAKFU_MEMBER_CHANNEL_ID)
                .editMessageById(playersMessageId, buildMembersMessage(jda))
                .mentionUsers(0)
                .queue(RestAction.getDefaultSuccess(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                    LOGGER.warn("Players message {} unknown, resetting...", playersMessageId);
                    playersMessageId = 0;
                    setupData(jda);
                }));
    }

    public String buildMembersMessage(JDA jda) {
        StringJoiner joiner = new StringJoiner("\n");
        for (Player player : players) {
            joiner.add("- " + player.name + " | " + player.breeds
                    .stream()
                    .map((WakfuBreeds wb) -> wb == null ? Emoji.fromUnicode("\uD83D\uDC64") : wb.getEmoji(jda))
                    .map(Emoji::getFormatted)
                    .collect(Collectors.joining(" ")));
        }
        return "\uD83C\uDF00  Joueurs de la commu \uD83C\uDF00\n\n" + joiner;
    }

    @Slash.Sub
    public void addPlayer(GuildSlashCommandEvent event, @Slash.Option Member member) {
        if (getPlayer(member) != null) {
            event.replyEphemeral("This player is already registered!, if you want to change his breeds, use /game wakfu-admin edit-player")
                    .queue();
            return;
        }
        event.replyComponents(ActionRow.of(
                        event.buildComponent()
                                .selectMenu()
                                .stringSelectMenu("wakfuAdmin.addPlayer[" + member.getIdLong() + "]")
                                .withCallback(e -> {
                                    addPlayer(e.getJDA(), member.getIdLong(), e.getValues()
                                            .stream()
                                            .mapToInt(Integer::parseInt)
                                            .mapToObj(value -> value == -1 ? null : WakfuBreeds.values()[value])
                                            .toArray(WakfuBreeds[]::new));
                                    e.editMessage("Player " + member.getAsMention() + " added!")
                                            .setActionRow(e.getComponent().asDisabled())
                                            .queue();
                                })
                                .setMaxValues(WakfuBreeds.values().length)
                                .addOptions(WakfuBreeds.getAsOption(event.getJDA()))
                                .addOption("Unknown", "-1", Emoji.fromUnicode("\uD83D\uDC64"))
                                .build()
                ))
                .setEphemeral(true)
                .queue();
    }

    @Slash.Sub
    public void addPlayerNonDiscord(GuildSlashCommandEvent event, @Slash.Option String name, @Slash.Option Optional<String> discordId) {
        if (getPlayer(name) != null) {
            event.replyEphemeral("This player is already registered!, if you want to change his breeds, use /game wakfu-admin edit-player")
                    .queue();
            return;
        }
        event.replyComponents(ActionRow.of(
                        event.buildComponent()
                                .selectMenu()
                                .stringSelectMenu("wakfuAdmin.addPlayer[" + name + "]")
                                .withCallback(e -> {
                                    addPlayer(e.getJDA(), discordId.map(Long::parseLong).orElse(0L), name, e.getValues()
                                            .stream()
                                            .mapToInt(Integer::parseInt)
                                            .mapToObj(value -> value == -1 ? null : WakfuBreeds.values()[value])
                                            .toArray(WakfuBreeds[]::new));
                                    e.editMessage("Player " + name + " added!")
                                            .setActionRow(e.getComponent().asDisabled())
                                            .queue();
                                })
                                .setMaxValues(WakfuBreeds.values().length)
                                .addOptions(WakfuBreeds.getAsOption(event.getJDA()))
                                .addOption("Unknown", "-1", Emoji.fromUnicode("\uD83D\uDC64"))
                                .build()
                ))
                .setEphemeral(true)
                .queue();
    }

    @Slash.Sub
    public void editPlayer(GuildSlashCommandEvent event, @Slash.Option Member member) {
        if (getPlayer(member) == null) {
            event.replyEphemeral("This player is not registered!, if you want to add him, use /game wakfu-admin add-player")
                    .queue();
            return;
        }
        event.replyComponents(ActionRow.of(
                        event.buildComponent()
                                .selectMenu()
                                .stringSelectMenu("wakfuAdmin.editPlayer[" + member.getIdLong() + "]")
                                .withCallback(e -> editPlayer(e.getJDA(), member.getAsMention(), e.getValues()
                                        .stream()
                                        .mapToInt(Integer::parseInt)
                                        .mapToObj(value -> WakfuBreeds.values()[value])
                                        .toArray(WakfuBreeds[]::new)))
                                .setMaxValues(WakfuBreeds.values().length)
                                .addOptions(WakfuBreeds.getAsOption(event.getJDA()))
                                .build()
                ))
                .setEphemeral(true)
                .queue();
    }

    @Slash.Sub
    public void removePlayer(GuildSlashCommandEvent event, @Slash.Option Member member) {
        if (getPlayer(member) == null) {
            event.replyEphemeral("This player is already registered!, if you want to change his breeds, use /game wakfu-admin edit-player")
                    .queue();
            return;
        }
        removePlayer(event.getJDA(), member.getAsMention());
        event.reply("Player " + member.getAsMention() + " removed!")
                .setEphemeral(true)
                .queue();
    }


    public void addPlayer(JDA jda, long memberId, WakfuBreeds... breeds) {
        addPlayer(jda, memberId, jda.getUserById(memberId).getAsMention(), breeds);
    }

    public void addPlayer(JDA jda, long memberId, String name, WakfuBreeds... breeds) {
        if (getPlayer(name) != null) {
            throw new IllegalArgumentException("This player is already registered!");
        }
        int id = -1;
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO player(discord_id, name) VALUES (?, ?)");
            statement.setLong(1, memberId);
            statement.setString(2, name);
            statement.execute();
            ResultSet result = statement.getGeneratedKeys();
            if (result.next()) {
                id = result.getInt(1);
            } else {
                throw new IllegalStateException("No generated keys!");
            }
            statement = connection.prepareStatement("INSERT INTO player_breeds(id, breed) VALUES (?, ?)");
            for (WakfuBreeds breed : breeds) {
                statement.setLong(1, id);
                statement.setInt(2, breed == null ? -1 : breed.ordinal());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Player player = new Player();
        player.id = id;
        player.discordId = memberId;
        player.name = name;
        player.breeds = breeds.length == 1 && breeds[0] == null ? new ArrayList<>(){{add(null);}} : new ArrayList<>(Arrays.stream(breeds).filter(Objects::nonNull).toList());
        player.jobs = new HashMap<>();
        players.add(player);
//        players.computeIfAbsent(memberId, k -> new ArrayList<>()).addAll(List.of(breeds));
        if (jda != null) {
            updatePlayersMessage(jda);
        }
    }
    public void editPlayer(JDA jda, String name, WakfuBreeds... breeds) {
        if (getPlayer(name) == null) {
            throw new IllegalArgumentException("This player is not registered!");
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM player_breeds WHERE id = ?");
            statement.setLong(1, getPlayer(name).id);
            statement.execute();
            statement = connection.prepareStatement("INSERT INTO player_breeds(id, breed) VALUES (?, ?)");
            for (WakfuBreeds breed : breeds) {
                statement.setLong(1, getPlayer(name).id);
                statement.setInt(2, breed.ordinal());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getPlayer(name).breeds = new ArrayList<>(List.of(breeds));
        if (jda != null) {
            updatePlayersMessage(jda);
        }
    }

    public void removePlayer(JDA jda, String name) {
        if (getPlayer(name) == null) {
            throw new IllegalArgumentException("This player is not registered!");
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM player_breeds WHERE id = ?");
            statement.setLong(1, getPlayer(name).id);
            statement.execute();
            statement = connection.prepareStatement("DELETE FROM player WHERE id = ?");
            statement.setLong(1, getPlayer(name).id);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        players.remove(getPlayer(name));
        if (jda != null) {
            updatePlayersMessage(jda);
        }
    }

    // endregion

    // region Player jobs list related
    private void setupJobsMessage(JDA jda) {
        jda.getTextChannelById(WAKFU_JOB_CHANNEL_ID)
                .sendMessage(buildJobsMessage(jda))
                .mentionUsers(0)
                .queue(message -> jobsMessageId = message.getIdLong());
    }

    public void updateJobsMessage(JDA jda) {
        if (jobsMessageId == 0) {
            throw new IllegalStateException("The jobs message id is not set!");
        }
        jda.getTextChannelById(WAKFU_JOB_CHANNEL_ID)
                .editMessageById(jobsMessageId, buildJobsMessage(jda))
                .mentionUsers(0)
                .queue(RestAction.getDefaultSuccess(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                    LOGGER.warn("Jobs message {} unknown, resetting...", jobsMessageId);
                    jobsMessageId = 0;
                    setupData(jda);
                }));
    }

    public String buildJobsMessage(JDA jda) {
        Map<Job, StringJoiner> joiners = new HashMap<>();
        for (Job job : Job.values()) {
            joiners.put(job, new StringJoiner("\n"));
        }
        for (Player player : players) {
            for (Map.Entry<Job, Integer> entry : player.jobs.entrySet()) {
                joiners.get(entry.getKey()).add(" - " + player.name + " | " + entry.getValue());
            }
        }
        return "\uD83C\uDF00 Joueur par métier suivis de leur niveau \uD83C\uDF00\n\n" + joiners.entrySet()
                .stream()
                .map(entry -> "- " + entry.getKey().name() + " " + getEmoji(jda, entry.getKey()).getFormatted() + "\n" + entry.getValue())
                .collect(Collectors.joining("\n"))
                + "_Merci de nous contacter si il y a des changements à faire!_";
    }

    @Slash.Sub
    public void addPlayerToJob(GuildSlashCommandEvent event,
                               @Slash.Option Member member,
                               @Slash.Option Job job,
                               @Slash.Option int level) {
        if (getPlayer(member) == null) {
            event.replyEphemeral("This player is not registered!, if you want to add him, use /game wakfu-admin add-player")
                    .queue();
            return;
        }
        if (getPlayer(member).jobs.containsKey(job)) {
            event.replyEphemeral("This player is already registered to this job!, if you want to change his breeds, use /game wakfu-admin edit-player-job")
                    .queue();
            return;
        }
        addPlayerToJob(event.getJDA(), member.getAsMention(), job, level);
        event.reply("Player " + member.getAsMention() + " added to job " + job.name() + " at level " + level + "!")
                .setEphemeral(true)
                .queue();
    }

    @Slash.Sub
    public void addPlayerNonMemberToJob(GuildSlashCommandEvent event,
                                        @Slash.Option String name,
                                        @Slash.Option Job job,
                                        @Slash.Option int level) {
        if (getPlayer(name) == null) {
            event.replyEphemeral("This player is not registered!, if you want to add him, use /game wakfu-admin add-player")
                    .queue();
            return;
        }
        if (getPlayer(name).jobs.containsKey(job)) {
            event.replyEphemeral("This player is already registered to this job!, if you want to change his breeds, use /game wakfu-admin edit-player-job")
                    .queue();
            return;
        }
        addPlayerToJob(event.getJDA(), name, job, level);
        event.reply("Player " + name + " added to job " + job.name() + " at level " + level + "!")
                .setEphemeral(true)
                .queue();
    }

    @Slash.Sub
    public void editPlayerJob(GuildSlashCommandEvent event, @Slash.Option Member member,
                              @Slash.Option Job job,
                              @Slash.Option int level) {
        if (getPlayer(member) == null) {
            event.replyEphemeral("This player is not registered!, if you want to add him, use /game wakfu-admin add-player")
                    .queue();
            return;
        }
        editPlayerJob(event.getJDA(), member.getAsMention(), job, level);
        event.reply("Player " + member.getAsMention() + " edited to job " + job.name() + " at level " + level + "!")
                .setEphemeral(true)
                .queue();
    }


    public void addPlayerToJob(JDA jda, String name, Job job, int level) {
        if (getPlayer(name) == null) {
            throw new IllegalArgumentException("This player is not registered!");
        }
        if (getPlayer(name).jobs.containsKey(job)) {
            throw new IllegalArgumentException("This player is already registered to this job!");
        }
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO player_jobs(id, job, level) VALUES (?, ?, ?)");
            statement.setLong(1, getPlayer(name).id);
            statement.setInt(2, job.ordinal());
            statement.setInt(3, level);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getPlayer(name).jobs.put(job, level);
        if (jda != null) {
            updateJobsMessage(jda);
        }
    }
    public void editPlayerJob(JDA jda, String name, Job job, int level) {
        if (getPlayer(name) == null) {
            throw new IllegalArgumentException("This player is not registered!");
        }
        if (!getPlayer(name).jobs.containsKey(job)) {
            throw new IllegalArgumentException("This player is not registered to this job!");
        }
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE player_jobs SET level = ? WHERE id = ? AND job = ?");
            statement.setInt(1, level);
            statement.setLong(2, getPlayer(name).id);
            statement.setInt(3, job.ordinal());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getPlayer(name).jobs.put(job, level);
        if (jda != null) {
            updateJobsMessage(jda);
        }
    }

    // endregion

    @Slash.Sub
    public void forceMessageUpdate(GuildSlashCommandEvent event, @Slash.Option DataType dataType) {
        if (dataType == DataType.ALL) {
            Arrays.stream(DataType.values()).filter(dt -> dt != DataType.ALL).forEach(dt -> update(event.getJDA(), dt));
        } else {
            update(event.getJDA(), dataType);
        }
        event.reply("Message "+dataType+" updated!")
                .setEphemeral(true)
                .queue();
    }

    public void update(JDA jda, DataType dataType) {
        switch (dataType) {
            case PLAYERS -> updatePlayersMessage(jda);
            case JOBS -> updateJobsMessage(jda);
            case ALL -> throw new IllegalArgumentException("Cannot use ALL as a DataType");
        }
    }

    public enum DataType {
        PLAYERS, JOBS, ALL
    }
}
