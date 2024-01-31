package fr.enimaloc.ical.composant;

import fr.enimaloc.ical.Status;

import java.util.*;

public class ICalEvent extends ICalComposant {
    private final Date start;
    private final Date end;
    private final String summary;
    private final String location;

    private final String[] categories;

    private final Status status;

    private final String description;

    private final boolean transparent;
    private final int sequence;


    public ICalEvent(@NotNull String uid,
                     @NotNull Date created,
                     @NotNull Date lastModified,
                     @NotNull Date dtStamp,
                     @NotNull Date start,
                     @Nullable Date end,
                     @NotNull String summary,
                     @Nullable String location,
                     @NotNull String[] categories,
                     @NotNull Status status,
                     @NotNull String description,
                     boolean transparent,
                     int sequence) {
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
    }

    public ICalEvent(String... lines) {
        super(lines);
        Map<String, String> entries = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split("[:;]", 2);
            String key = parts[0];
            if (key.equals("BEGIN") || key.equals("END")) {
                continue;
            }
            String value = parts[1];
            entries.put(key, value);
        }
        this.start = parseDate(entries.get("DTSTART"));
        this.end = entries.containsKey("DTEND") ? parseDate(entries.get("DTEND")) : null;
        this.summary = entries.get("SUMMARY");
        this.location = entries.get("LOCATION");
        this.categories = entries.containsKey("CATEGORIES") ? entries.get("CATEGORIES").split(",") : new String[0];
        this.status = Status.valueOf(entries.get("STATUS"));
        this.description = entries.get("DESCRIPTION");
        this.transparent = entries.containsKey("TRANSP") && entries.get("TRANSP").equals("TRANSPARENT");
        this.sequence = Integer.parseInt(entries.get("SEQUENCE"));
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
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
                '}';
    }
}
