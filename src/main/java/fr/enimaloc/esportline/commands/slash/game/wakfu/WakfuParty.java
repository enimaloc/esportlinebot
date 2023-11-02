package fr.enimaloc.esportline.commands.slash.game.wakfu;

import fr.enimaloc.enutils.jda.commands.CustomSlashCommandInteractionEvent;
import fr.enimaloc.esportline.api.wakfu.WakfuBreeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.*;

public class WakfuParty {
    public static final List<WakfuParty> REGISTERED_PARTIES = new ArrayList<>();
    private long messageId;
    private long voiceChannelId;
    private long owner;
    private String name;
    private String description;
    private int slots;
    private int level;
    private Map<Long, WakfuBreeds[]> members = new HashMap<>();

    public WakfuParty(CustomSlashCommandInteractionEvent event, long owner, String name, String description, int slots, int level) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.slots = slots;
        this.level = level;
    }

    public void addMember(long id, WakfuBreeds... breeds) {
        members.put(id, breeds);
    }

    public MessageEmbed createEmbed(JDA jda) {
        return new EmbedBuilder()
                .setAuthor(getOwner(jda).getAsTag(), null, getOwner(jda).getEffectiveAvatarUrl())
                .setTitle(name)
                .setDescription(description)
                .addField("Slots", slots + " slots", true)
                .addField("Level", level != -1 ? String.valueOf(level) : "any", true)
                .addField("Voice channel", getVoiceChannel(jda).getAsMention(), true)
                .addField("Members", getPlayerList(jda), false)
                .setFooter(String.valueOf(getOwnerId()), null).build();
    }

    public String getPlayerList(JDA jda) {
        StringJoiner joiner = new StringJoiner("\n");
        Map.Entry<Long, WakfuBreeds[]>[] entries = members.entrySet().toArray(Map.Entry[]::new);
        for (int i = 0; i < slots; i++) {
            if (i < entries.length) {
                joiner.add("- " + jda.getUserById(entries[i].getKey()).getAsMention()
                        + " | " + Arrays.stream(entries[i].getValue())
                        .map(breed -> breed.getEmoji(jda))
                        .map(Emoji::getFormatted)
                        .reduce("", String::concat));
            } else {
                joiner.add("- ");
            }
        }
        return joiner.toString();
    }

    public void updateMessage(JDA jda) {
        getMessage(jda).editMessageEmbeds(createEmbed(jda))
                .setComponents(getActionRows(jda))
                .queue();
    }

    public void create(TextChannel textChannel) {
        voiceChannelId = textChannel.getGuild()
                .getCategoryById(Wakfu.PARTY_CATEGORY_ID)
                .createVoiceChannel(name)
                .addMemberPermissionOverride(owner, List.of(Permission.VOICE_CONNECT, Permission.PRIORITY_SPEAKER), List.of())
                .addPermissionOverride(textChannel.getGuild().getPublicRole(), List.of(), List.of(Permission.VOICE_CONNECT))
//                .setTopic(description) TODO 31/10/2023 - Awaiting implementation in JDA
                .setUserlimit(slots)
                .complete()
                .getIdLong();
        textChannel.sendMessageEmbeds(createEmbed(textChannel.getJDA()))
                .setComponents(getActionRows(textChannel.getJDA()))
                .queue(m -> messageId = m.getIdLong());
        WakfuParty.REGISTERED_PARTIES.add(this);
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public VoiceChannel getVoiceChannel(JDA jda) {
        return jda.getVoiceChannelById(getVoiceChannelId());
    }

    public long getOwnerId() {
        return owner;
    }

    public User getOwner(JDA jda) {
        return jda.getUserById(getOwnerId());
    }

    public long getMessageId() {
        return messageId;
    }

    public Message getMessage(JDA jda) {
        return jda.getTextChannelById(Wakfu.PARTY_ANNOUNCEMENT_CHANNEL_ID).retrieveMessageById(getMessageId()).complete();
    }

    public RestAction<Void> delete(JDA jda) {
        REGISTERED_PARTIES.remove(this);
        return getMessage(jda).delete()
                .and(getVoiceChannel(jda).delete());
    }

    public String getName() {
        return name;
    }

    private ActionRow[] getActionRows(JDA jda) {
        return new ActionRow[]{
                ActionRow.of(
                        StringSelectMenu.create("wakfu@party[" + name + "].join")
                                .setPlaceholder("Join the party")
                                .setMaxValues(18)
                                .addOptions(WakfuBreeds.getAsOption(jda))
                                .build()
                ),
                ActionRow.of(Button.danger("wakfu@party[" + name + "].leave", "Leave"))
        };
    }

    public void select(StringSelectInteractionEvent event) {
        WakfuBreeds[] selected = event.getComponentId().endsWith("join") || event.getComponentId().endsWith("ownerPickClass") ?
                Arrays.stream(event.getValues().toArray(new String[0]))
                        .map(Integer::parseInt)
                        .map(i -> WakfuBreeds.values()[i])
                        .toArray(WakfuBreeds[]::new) : null;
        if (event.getComponentId().endsWith("ownerPickClass")) {
            if (WakfuParty.REGISTERED_PARTIES.stream().anyMatch(p -> p.getOwnerId() == event.getMember().getIdLong())) {
                event.editMessage("You are already the owner of a party, aborting...").queue();
                return;
            }
            if (WakfuParty.REGISTERED_PARTIES.stream().anyMatch(p -> p.getName().equals(name))) {
                event.editMessage("A party with this name already exists, aborting...").queue();
                return;
            }
            members.put(event.getUser().getIdLong(), selected);
            event.editMessage("Party created !").setActionRow(event.getComponent().asDisabled()).queue();
            create(event.getJDA().getTextChannelById(Wakfu.PARTY_ANNOUNCEMENT_CHANNEL_ID));
        } else if (event.getComponentId().endsWith("join")) {
            if (event.getUser().getIdLong() == owner) {
                event.reply("You can't join your own party !").setEphemeral(true).queue();
                return;
            }
            if (members.containsKey(event.getUser().getIdLong())) {
                event.reply("You already joined this party !").setEphemeral(true).queue();
                return;
            }
            members.put(event.getUser().getIdLong(), selected);
            event.reply("You joined the party !").setEphemeral(true).queue();
            updateMessage(event.getJDA());
        }
    }

    public void button(ButtonInteractionEvent event) {
        if (event.getComponentId().endsWith("leave")) {
            if (event.getUser().getIdLong() == owner) {
                event.reply("You can't leave your own party !").setEphemeral(true).queue();
                return;
            }
            if (!members.containsKey(event.getUser().getIdLong())) {
                event.reply("You didn't join this party !").setEphemeral(true).queue();
                return;
            }
            members.remove(event.getUser().getIdLong());
            event.reply("You left the party !").setEphemeral(true).queue();
            updateMessage(event.getJDA());
        }
    }

    public void setSlots(JDA jda, int slots) {
        this.slots = slots;
        if (jda != null)
            updateMessage(jda);
    }
}
