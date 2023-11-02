package fr.enimaloc.esportline.api.wakfu;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonKey;

import java.util.Locale;

public enum WakfuLocale {
    FRENCH("fr", "fr"),
    ENGLISH("en", "en-GB"),
    SPANISH("es", "es-ES"),
    PORTUGUESE("pt", "pt-BR");

    private final String code;
    private final String locale;

    WakfuLocale(String code, String locale) {
        this.code = code;
        this.locale = locale;
    }

    @JsonKey
    public String code() {
        return code;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(locale);
    }

    @JsonCreator
    public static WakfuLocale fromCode(String code) {
        for (WakfuLocale locale : values()) {
            if (locale.code.equals(code)) {
                return locale;
            }
        }
//        throw new IllegalArgumentException("Unknown locale code: " + code);
        return null;
    }

    public static WakfuLocale fromLocale(Locale locale) {
        return switch (locale.getLanguage()) {
            case "fr" -> FRENCH;
            case "en" -> ENGLISH;
            case "es" -> SPANISH;
            case "pt" -> PORTUGUESE;
            default -> throw new IllegalStateException("Unexpected value: " + locale.getLanguage());
        };
    }
}
