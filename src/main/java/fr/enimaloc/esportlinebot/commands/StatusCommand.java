package fr.enimaloc.esportlinebot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import fr.enimaloc.esportlinebot.ESportLineBot;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.Presence;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StatusCommand extends SlashCommand {

    public StatusCommand() {
        this.name = "presence";
        this.help = "Set bot presence";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{ESportLineBot.STAFF_ROLE_ID};
        this.enabledUsers = new String[]{ESportLineBot.MINOZEN_USER_ID};

        this.options = List.of(
                new OptionData(OptionType.STRING, "status", "Set status indicator")
                        .addChoices(Arrays.stream(OnlineStatus.values())
                                .filter(status -> !status.getKey().isEmpty())
                                .map(status -> new Command.Choice(status.name(), status.getKey()))
                                .collect(Collectors.toSet())),
                new OptionData(OptionType.INTEGER, "activity-type", "Set activity type")
                        .addChoices(Arrays.stream(Activity.ActivityType.values())
                                .map(type -> new Command.Choice(type.name(), type.getKey()))
                                .collect(Collectors.toSet())),
                new OptionData(OptionType.STRING, "activity-name", "Set activity name"),
                new OptionData(OptionType.STRING, "activity-url", "Twitch url for activity STREAMING")
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        Presence presence = event.getJDA().getPresence();

        OptionMapping status = event.getOption("status");
        if (status != null) {
            presence.setStatus(OnlineStatus.fromKey(status.getAsString()));
        }

        OptionMapping activityType = event.getOption("activity-type");
        OptionMapping activityName = event.getOption("activity-name");
        OptionMapping activityUrl = event.getOption("activity-url");
        if (activityType != null && activityName != null) {
            Activity activity;
            Activity.ActivityType type = Activity.ActivityType.fromKey((int) activityType.getAsLong());
            String name = activityName.getAsString();

            if (type == Activity.ActivityType.STREAMING && activityUrl != null) {
                activity = Activity.of(type, name, activityUrl.getAsString());
            } else {
                activity = Activity.of(type, name);
            }
            presence.setActivity(activity);
        }

        event.reply("Done").setEphemeral(true).queue();
    }

}
