package fr.enimaloc.esportlinebot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import fr.enimaloc.esportlinebot.ESportLineBot;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ClearCommand extends SlashCommand {
    public ClearCommand() {
        this.name = "clear";
        this.help = "Clear bot message";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{ESportLineBot.STAFF_ROLE_ID};

        this.options = List.of(
                new OptionData(OptionType.INTEGER, "number", "Number of message to delete", true) {
                    @NotNull
                    @Override
                    public DataObject toData() {
                        return super.toData().put("min_value", 1).put("max_value", 100);
                    }
                }
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        event.deferReply(true).queue();
        long number = Objects.requireNonNull(event.getOption("number")).getAsLong();
        event.getChannel()
                .getHistory()
                .retrievePast((int) number)
                .map(messages -> event.getChannel()
                        .purgeMessages(messages))
                .queue(success -> event.getHook()
                                .editOriginalFormat("Successfully deleted %d messages", number)
                                .queue(),
                        failure -> event.getHook()
                                .editOriginalFormat("An error was occurred:\n`%s: %s`",
                                        failure.getClass().getName(),
                                        failure.getLocalizedMessage())
                                .queue());
    }

}
