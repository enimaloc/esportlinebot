package fr.enimaloc.esportlinebot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import fr.enimaloc.esportlinebot.ESportLineBot;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class ForceDrawCommand extends SlashCommand {

    private final ESportLineBot eSportLineBot;

    public ForceDrawCommand(ESportLineBot eSportLineBot) {
        this.eSportLineBot = eSportLineBot;
        this.name = "force-draw";
        this.help = "Force a draw";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{ESportLineBot.STAFF_ROLE_ID};
        this.enabledUsers = new String[]{ESportLineBot.MINOZEN_USER_ID};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        TextChannel textChannel = event.getJDA().getTextChannelById(eSportLineBot.voteChannel);
        if (textChannel == null) {
            return;
        }
        eSportLineBot.ended = false;
        eSportLineBot.draw(textChannel, true);

        event.reply("Done").setEphemeral(true).queue();
    }

}
