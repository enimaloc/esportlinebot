package fr.enimaloc.esportline.runner;

import fr.enimaloc.ical.ICal;
import fr.enimaloc.ical.composant.ICalComposant;
import fr.enimaloc.ical.composant.ICalEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.restaction.ScheduledEventAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class EventFromICal implements Runnable {
    public static final Logger LOGGER = LoggerFactory.getLogger(EventFromICal.class);
    private final JDA jda;
    private final long guildId;
    private final ICal iCal;

    public EventFromICal(JDA jda, long guildId, ICal iCal) {
        this.jda = jda;
        this.guildId = guildId;
        this.iCal = iCal;
    }

    @Override
    public void run() {
        Guild guild = jda.getGuildById(guildId);
        LOGGER.debug("Checking event for {}", guild.getName());
        try {
            List<ICalComposant> composants = iCal.fetch();
            List<ScheduledEvent> events = guild.getScheduledEvents();
            for (ICalEvent event : composants.stream()
                    .filter(ICalEvent.class::isInstance)
                    .map(ICalEvent.class::cast)
                    .filter(ICalEvent::isPublic)
                    .filter(event -> event.getStart().isAfter(OffsetDateTime.now()))
                    .filter(event -> events.stream().noneMatch(dEvent -> event.getStart().toEpochSecond() ==  dEvent.getStartTime().toEpochSecond()
                            && (dEvent.getChannel() != null
                            && event.getLocation().contains(dEvent.getChannel().getId())
                            || dEvent.getChannel() == null
                            && (event.getLocation().equals(dEvent.getLocation()) && event.getEnd().toEpochSecond() == dEvent.getEndTime().toEpochSecond()))
                            && event.getSummary().equals(dEvent.getName())
                            && dEvent.getDescription().substring(dEvent.getDescription().indexOf('\0') + 1).equals(event.getDescription() == null ? "" : event.getDescription().replaceAll("!image:[^!]*!", ""))))
                    .toList()) {
                LOGGER.trace("Creating event {} from {} calendar", event.getSummary(), iCal.getName());
                String header = " \uD83D\uDD5B Event exported at " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss X").format(event.getDtStamp().atZoneSameInstant(ZoneOffset.UTC))+"\0\n\n";
                ScheduledEventAction scheduledEvent = null;
                String locLower = event.getLocation().toLowerCase();
                if (locLower.startsWith("vc:") || locLower.startsWith("voicechannel:") || locLower.startsWith("sc:") || locLower.startsWith("stagechannel:") || locLower.startsWith("discord:")) {
                    GuildChannel channel = guild.getVoiceChannelById(locLower.split(":", 2)[1]);
                    if (channel != null) {
                        scheduledEvent = guild.createScheduledEvent(event.getSummary(), channel, event.getStart());
                    }
                } else {
                    scheduledEvent = guild.createScheduledEvent(event.getSummary(), event.getLocation(), event.getStart(), event.getEnd());
                }
                String description = event.getDescription() == null ? "" : event.getDescription();
                if (description.contains("!image:")) {
                    Pattern pattern = Pattern.compile(".*!image:([^!]*)!.*", Pattern.DOTALL);
                    String url = pattern.matcher(description).replaceAll("$1").replace("\\;", ";");
                    description = description.replaceAll("!image:[^!]!", "");
                    try {
                        scheduledEvent.setImage(Icon.from(URI.create(url).toURL().openStream()));
                    } catch (Exception e) {
                        LOGGER.error("Unable to fetch image from " + url, e);
                    }
                }
                scheduledEvent.setDescription(header+(description.length() > 1000 - header.length() ? description.substring(0, 1000 - header.length()) + "..." : description))
                        .queue();
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to fetch events from " + iCal.getIcalUrl(), e);
        }
        LOGGER.debug("Checking for {} terminated", guild.getName());
    }
}
