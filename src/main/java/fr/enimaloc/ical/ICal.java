package fr.enimaloc.ical;

import fr.enimaloc.ical.composant.ICalComposant;
import fr.enimaloc.ical.composant.ICalEvent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;

public class ICal {
    
    private final String icalUrl;
    
    private String calScale;
    
    private String prodId;
    
    private String name;
    
    private String timezone;
    private List<ICalComposant> cachedComposant;
    private boolean publicDefault;

    public ICal(String icalUrl) {
        this(false, icalUrl);
    }

    public ICal(boolean publicDefault, String icalUrl) {
        if (icalUrl == null || icalUrl.isBlank()) {
            throw new IllegalArgumentException("icalUrl cannot be null or empty");
        }
        this.publicDefault = publicDefault;
        this.icalUrl = icalUrl;
    }

    
    public String getIcalUrl() {
        return icalUrl;
    }

    
    public String getCalScale() {
        return calScale;
    }

    
    public String getProdId() {
        return prodId;
    }

    
    public String getName() {
        return name;
    }

    
    public String getTimezone() {
        return timezone;
    }

    public List<ICalComposant> getCachedComposant() {
        return cachedComposant;
    }

    public List<ICalComposant> fetch() throws IOException {
        URI uri = URI.create(icalUrl);
        URLConnection connection = uri.toURL().openConnection();
        List<ICalComposant> events = new ArrayList<>();
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder composante = null;
                StringBuilder calSpec = new StringBuilder();
                StringBuilder activeBuilder = calSpec;
                try (BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), StandardCharsets.UTF_8))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (!inputLine.startsWith("BEGIN:VCALENDAR") && inputLine.startsWith("BEGIN:")) {
                            composante = new StringBuilder();
                            activeBuilder = composante;
                        }
                        activeBuilder.append(inputLine).append("\n");
                        if (!inputLine.startsWith("BEGIN:VCALENDAR") && inputLine.startsWith("END:") && composante != null) {
                            String[] lines = activeBuilder.toString().split("\n");
                            if (lines[0].equals("BEGIN:VEVENT")) {
                                if (timezone == null) {
                                    processMetaIcal(calSpec.toString().split("\n"));
                                }
                                events.add(new ICalEvent(publicDefault, timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault(), lines));
                            }
                            composante = null;
                            activeBuilder = calSpec;
                        }
                    }
                }
                processMetaIcal(calSpec.toString().split("\n"));
            }
        }
        cachedComposant = events;
        return events;
    }

    private void processMetaIcal(String... lines) {
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
        this.calScale = entries.getOrDefault("CALSCALE", "UTC");
        this.prodId = entries.get("PRODID");
        this.name = entries.getOrDefault("X-WR-CALNAME", "Unknown");
        this.timezone = entries.get("X-WR-TIMEZONE");
    }

    @Override
    public String toString() {
        return "ICal{" +
                "icalUrl='" + icalUrl + '\'' +
                ", calScale='" + calScale + '\'' +
                ", prodId='" + prodId + '\'' +
                ", name='" + name + '\'' +
                ", timezone='" + timezone + '\'' +
                ", cachedComposant{len}=" + cachedComposant.size() +
                '}';
    }
}
