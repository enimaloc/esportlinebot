package fr.enimaloc.ical.composant;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ICalComposant {
    @NotNull
    private final String uid;
    @NotNull
    private final Date created;
    @NotNull
    private final Date lastModified;
    @NotNull
    private final Date dtStamp;

    public ICalComposant(String uid, Date created, Date lastModified, Date dtStamp) {
        this.uid = uid;
        this.created = created;
        this.lastModified = lastModified;
        this.dtStamp = dtStamp;
    }

    public ICalComposant(String... lines) {
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
        this.uid = entries.get("UID");
        this.created = parseDate(entries.get("CREATED"));
        this.lastModified = parseDate(entries.get("LAST-MODIFIED"));
        this.dtStamp = parseDate(entries.get("DTSTAMP"));
    }

    @NotNull
    public String getUid() {
        return uid;
    }

    @NotNull
    public Date getCreated() {
        return created;
    }

    @NotNull
    public Date getLastModified() {
        return lastModified;
    }

    @NotNull
    public Date getDtStamp() {
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

    protected static Date parseDate(String val) {
        if (val.contains("=")) {
            String[] dateParts = val.split("=")[1].split(":");
            if (dateParts[0].startsWith("DATE-TIME")) {
                return new Date(Long.parseLong(dateParts[1]));
            } else if (dateParts[0].startsWith("DATE")) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[1].substring(0, 4)));
                calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1].substring(4, 6)));
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[1].substring(6, 8)));
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                return calendar.getTime();
            } else {
                throw new IllegalArgumentException("Unknown date format: " + val);
            }
        } else if (val.contains("T")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return dateFormat.parse(val);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Unknown date format: " + val);
            }
        } else {
            return new Date(Long.parseLong(val));
        }
    }
}
