package fr.enimaloc.ical.composant;

import fr.enimaloc.ical.Status;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ICalEvent extends ICalComposant {

    private final OffsetDateTime start;
    private final OffsetDateTime end;

    private final String summary;
    private final String location;

    private final String[] categories;

    private final Status status;

    private final String description;

    private final boolean transparent;

    private final int sequence;

    private final boolean public0;


    public ICalEvent(String uid,
                     OffsetDateTime created,
                     OffsetDateTime lastModified,
                     OffsetDateTime dtStamp,
                     OffsetDateTime start,
                     OffsetDateTime end,
                     String summary,
                     String location,
                     String[] categories,
                     Status status,
                     String description,
                     boolean transparent,
                     int sequence,
                     boolean public0) {
        super(uid, created, lastModified, dtStamp);
        this.start = start;
        this.end = end;
        this.summary = summary;
        this.location = location;
        this.categories = categories;
        this.status = status;
        this.description = description;
        this.transparent = transparent;
        this.sequence = sequence;
        this.public0 = public0;
    }

    public ICalEvent(boolean publicDefault, ZoneId zoneId, String... lines) {
        super(zoneId, lines);
        Map<String, String> entries = new HashMap<>();
        String key = null;
        String value = null;
        for (String line : lines) {
            String[] parts = line.split("[:;]", 2);
            if (line.startsWith(" ")) {
                value += line.substring(1);
                continue;
            }
            if (key != null && value != null) {
                entries.put(key, value);
            }
            key = parts[0];
            if (key.equals("BEGIN") || key.equals("END")) {
                continue;
            }
            value = parts[1];
        }
        if (key != null && value != null) {
            entries.put(key, value);
        }
        this.start = parseDate(entries.get("DTSTART"), zoneId);
        this.end = entries.containsKey("DTEND") ? parseDate(entries.get("DTEND"), zoneId) : null;
        this.summary = entries.get("SUMMARY");
        this.location = entries.getOrDefault("LOCATION", "Unknown");
        this.categories = entries.containsKey("CATEGORIES") ? entries.get("CATEGORIES").split(",") : new String[0];
        this.status = Status.valueOf(entries.getOrDefault("STATUS", Status.UNKNOWN.name()));
        this.description = entries.get("DESCRIPTION");
        this.transparent = entries.containsKey("TRANSP") && entries.get("TRANSP").equals("TRANSPARENT");
        this.sequence = Integer.parseInt(entries.get("SEQUENCE"));
        this.public0 = (!entries.containsKey("CLASS") && publicDefault) || (entries.containsKey("CLASS") && entries.get("CLASS").equals("PUBLIC"));
    }


    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }


    public String getSummary() {
        return summary;
    }


    public String getLocation() {
        return location;
    }


    public String[] getCategories() {
        return categories;
    }


    public Status getStatus() {
        return status;
    }


    public String getDescription() {
        return description;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public int getSequence() {
        return sequence;
    }

    public boolean isPublic() {
        return public0;
    }

    @Override
    public String toString() {
        return "ICalEvent{" +
                "uid='" + getUid() + '\'' +
                ", created=" + getCreated() +
                ", lastModified=" + getLastModified() +
                ", dtStamp=" + getDtStamp() +
                ", start=" + start +
                ", end=" + end +
                ", summary='" + summary + '\'' +
                ", location='" + location + '\'' +
                ", categories=" + Arrays.toString(categories) +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", transparent=" + transparent +
                ", sequence=" + sequence +
                ", public=" + public0 +
                '}';
    }
}
