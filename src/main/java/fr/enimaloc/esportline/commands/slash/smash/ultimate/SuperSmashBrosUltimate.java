package fr.enimaloc.esportline.commands.slash.smash.ultimate;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.api.smash.SmashPlayer;
import fr.enimaloc.esportline.api.smash.SmashRegistry;
import fr.enimaloc.esportline.api.smash.SmashTournament;
import fr.enimaloc.esportline.utils.BenchmarkUtils;
import fr.enimaloc.esportline.utils.function.ThrowableFunction;
import fr.enimaloc.matcher.utils.Either;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SuperSmashBrosUltimate {
    public static final Logger LOGGER = LoggerFactory.getLogger(SuperSmashBrosUltimate.class);
    private final SmashRegistry registry;

    public SuperSmashBrosUltimate(SmashRegistry registry) {
        this.registry = registry;
    }

    @Slash.Sub
    public void player(GuildSlashCommandEvent event, @Slash.Option(name = "playerTag", description = "The player to get information about") String playerTag) {
        List<SmashPlayer> players;
        try {
            players = registry.getPlayerTable(SmashRegistry.SmashGame.ULTIMATE).getPlayersByTag(playerTag);
        } catch (SQLException | JsonProcessingException e) {
            event.reply("An error occurred while getting the player information").queue();
            LOGGER.error("An error occurred while getting the player information", e);
            return;
        }

        if (players.isEmpty()) {
            event.reply("No player found with the tag " + playerTag).queue();
            return;
        }
        if (players.size() > 1) {
            event.deferReplyEphemeral().queue();
            displayPlayerList(event, playerTag, players, 0);
            return;
        }
        displayPlayerData(String.valueOf(players.get(0).playerId()), messageData -> {
            if (messageData.isLeft()) event.reply(messageData.getLeft()).queue();
            else event.replyEmbeds(messageData.getRight()).queue();
        });
    }

    private void displayPlayerList(GuildSlashCommandEvent event, String playerTag, List<SmashPlayer> players, int offset) {
        event.getHook().editOriginal("Multiple players found with the tag " + playerTag)
                .setComponents(ActionRow.of(
                                event.buildComponent()
                                        .selectMenu()
                                        .stringSelectMenu(event.getId() + "-select")
                                        .addOptions(players.stream()
                                                .skip(offset)
                                                .limit(SelectMenu.OPTIONS_MAX_AMOUNT)
                                                .map(p -> SelectOption.of(p.tag(), String.valueOf(p.playerId()))
                                                        .withDescription("Localisation: " + p.location())).toList())
                                        .withCallback(this::displayPlayerData)
                                        .build()),
                        ActionRow.of(
                                event.buildComponent()
                                        .button()
                                        .primary(event.getId() + "-previous", "Previous page")
                                        .withDisabled(offset == 0)
                                        .withCallback(e -> {
                                            e.deferEdit().queue();
                                            displayPlayerList(event, playerTag, players, offset - SelectMenu.OPTIONS_MAX_AMOUNT);
                                        }),
                                event.buildComponent()
                                        .button()
                                        .secondary(event.getId() + "-page", "Page " + (offset / SelectMenu.OPTIONS_MAX_AMOUNT + 1) + " of " + (players.size() / SelectMenu.OPTIONS_MAX_AMOUNT + 1))
                                        .asDisabled(),
                                event.buildComponent()
                                        .button()
                                        .primary(event.getId() + "-next", "Next page")
                                        .withDisabled(offset + SelectMenu.OPTIONS_MAX_AMOUNT >= players.size())
                                        .withCallback(e -> {
                                            e.deferEdit().queue();
                                            displayPlayerList(event, playerTag, players, offset + SelectMenu.OPTIONS_MAX_AMOUNT);
                                        })
                        ))
                .queue();
    }

    private void displayPlayerData(StringSelectInteractionEvent event) {
        event.deferEdit().queue();
        String selected = event.getSelectedOptions().get(0).getValue();
        displayPlayerData(selected, messageData -> {
            if (messageData.isLeft()) event.editMessage(messageData.getLeft()).queue();
            else event.getChannel().sendMessageEmbeds(messageData.getRight()).queue();
        });
    }

    private void displayPlayerData(String playerId, Consumer<Either<String, MessageEmbed>> editFunction) {
        BenchmarkUtils.ResultOne<SmashPlayer> bench;
        bench = BenchmarkUtils.benchmark(() -> {
            try {
                return registry.getPlayerTable(SmashRegistry.SmashGame.ULTIMATE).getPlayerById(playerId);
            } catch (SQLException | JsonProcessingException e) {
                editFunction.accept(Either.left("An error occurred while getting the player information"));
                LOGGER.error("An error occurred while getting the player information", e);
                return null;
            }
        });
        if (bench.getResult() == null) return;

        SmashPlayer player = bench.getResult();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(player.tag())
                .appendDescription("**Localisation**: " + player.location() + "\n");
//                .addField("Main", player.main(), true)
//                .addField("Secondary", player.secondary(), true)
//                .addField("Tertiary", player.tertiary(), true)
//                .addField("Rank", player.rank(), true)
//                .addField("Tournament wins", String.valueOf(player.tournamentWins()), true)
//                .addField("Tournament losses", String.valueOf(player.tournamentLosses()), true)
//                .addField("Tournament draws", String.valueOf(player.tournamentDraws()), true)
//                .addField("Tournament winrate", String.format("%.2f", player.tournamentWinrate() * 100) + "%", true)
//                .addField("Set wins", String.valueOf(player.setWins()), true)
//                .addField("Set losses", String.valueOf(player.setLosses()), true)
//                .addField("Set draws", String.valueOf(player.setDraws()), true)
//                .addField("Set winrate", String.format("%.2f", player.setWinrate() * 100) + "%", true)
//                .addField("Game wins", String.valueOf(player.gameWins()), true)
//                .addField("Game losses", String.valueOf(player.gameLosses()), true)
//                .addField("Game draws", String.valueOf(player.gameDraws()), true)
//                .addField("Game winrate", String.format("%.2f", player.gameWinrate() * 100) + "%", true);
        Map<String, String[]> social = Arrays.stream(player.social())
                .map(s -> Map.entry(s.type(), s.values()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : social.entrySet()) {
            if (entry.getValue().length == 0) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append("**").append(entry.getKey()).append("**: ");
            sb.append(Arrays.stream(entry.getValue())
                    .map(s -> entry.getKey().equals("twitter") ? "[@" + s + "](https://twitter.com/" + s + ")" : s)
                    .collect(Collectors.joining(", ")));
        }
        if (sb.length() > 0) eb.addField("Socials", sb.toString(), true);
        int sumOfChar = Arrays.stream(player.characters()).mapToInt(SmashPlayer.Character::usage).sum();
        if (sumOfChar > 0) {
            sb = new StringBuilder();
            for (SmashPlayer.Character character : player.characters()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("**").append(character.name().replaceFirst("ultimate/", "")).append("**: ");
                sb.append(character.usage()).append(" games (").append(String.format("%.2f", (double) character.usage() / sumOfChar * 100)).append("%)");
            }
            eb.addField("Characters", sb.toString(), true);
        }
        sb = new StringBuilder();
        for (SmashPlayer.Placings placings : Arrays.stream(player.placings())
                .sorted(Comparator.<SmashPlayer.Placings>comparingLong(p -> {
                    try {
                        return p.getTournament().start();
                    } catch (SQLException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }).reversed())
                .limit(5)
                .toList()) {
            try {
                if (sb.length() > 0) sb.append("\n");
                SmashTournament tournament = placings.getTournament();
                sb.append("**").append(tournament.cleanedName()).append("** (")
                        .append(TimeFormat.DATE_SHORT.format(tournament.start() * 1000)).append("): ")
                        .append(placings.placing()).append("th place");
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (sb.length() > 0) eb.addField("Recent placings", sb.toString(), false);

        editFunction.accept(Either.right(eb.build()));
    }
}
