package fr.enimaloc.esportline.api.wakfu.rss;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import fr.enimaloc.esportline.api.wakfu.WakfuLocale;

import java.util.Date;

public final class WakfuRSS {
    @JacksonXmlProperty(isAttribute = true)
    public String version;
    public Channel channel;

    public record Channel(String title, String description, String link, WakfuLocale language, String copyright,
                   Date lastBuildDate, Date pubDate, String url,
                   @JacksonXmlElementWrapper(useWrapping = false) Channel.News[] item) {
        public record News(String title, String description, String link, String category, Date pubDate, String guid) {}
    }
}
