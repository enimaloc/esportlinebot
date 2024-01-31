package fr.enimaloc.ical.composant;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ICalComposant {
    private final String uid;
    private final OffsetDateTime created;
    private final OffsetDateTime lastModified;
    private final OffsetDateTime dtStamp;

    public ICalComposant(String uid, OffsetDateTime created, OffsetDateTime lastModified, OffsetDateTime dtStamp) {
        this.uid = uid;
        this.created = created;
        this.lastModified = lastModified;
        this.dtStamp = dtStamp;
    }

    public ICalComposant(ZoneId zoneId, String... lines) {
        Map<String, String> entries = new HashMap<>();
        String key = null;
        String value = null;
        for (String line : lines) {
            if (line.startsWith(" ")) {
                value += line.substring(1);
                continue;
            }
            if (key != null && value != null) {
                entries.put(key, value);
            }
            String[] parts = line.split("[:;]", 2);
            key = parts[0];
            if (key.equals("BEGIN") || key.equals("END")) {
                continue;
            }
            value = parts[1];
        }
        if (key != null && value != null) {
            entries.put(key, value);
        }
        this.uid = entries.get("UID");
        this.created = parseDate(entries.get("CREATED"), zoneId);
        this.lastModified = parseDate(entries.get("LAST-MODIFIED"), zoneId);
        this.dtStamp = parseDate(entries.get("DTSTAMP"), zoneId);
    }


    public String getUid() {
        return uid;
    }


    public OffsetDateTime getCreated() {
        return created;
    }


    public OffsetDateTime getLastModified() {
        return lastModified;
    }


    public OffsetDateTime getDtStamp() {
        return dtStamp;
    }

    @Override
    public String toString() {
        return "ICalComposant{" +
                "uid='" + uid + '\'' +
                ", created=" + created +
                ", lastModified=" + lastModified +
                ", dtStamp=" + dtStamp +
                '}';
    }

    protected static OffsetDateTime parseDate(String val, ZoneId zoneId) {
        if (val.contains("=")) {
            String[] dateParts = val.split("=")[1].split(":");
            if (dateParts[0].startsWith("DATE-TIME")) {
                return Instant.ofEpochMilli(Long.parseLong(dateParts[1])).atZone(zoneId).toOffsetDateTime();
            } else if (dateParts[0].startsWith("DATE")) {
                int year = Integer.parseInt(dateParts[1].substring(0, 4));
                int month = Integer.parseInt(dateParts[1].substring(4, 6));
                int day = Integer.parseInt(dateParts[1].substring(6, 8));
                return OffsetDateTime.parse("%04d-%02d-%02dT00:00:00.000%s".formatted(year, month, day, OffsetDateTime.now(zoneId).getOffset().getId()));
            } else {
                throw new IllegalArgumentException("Unknown date format: " + val);
            }
        } else if (val.contains("T")) {
            return OffsetDateTime.parse(val, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX"));
        } else {
            return Instant.ofEpochMilli(Long.parseLong(val)).atZone(zoneId).toOffsetDateTime();
        }
    }
}
