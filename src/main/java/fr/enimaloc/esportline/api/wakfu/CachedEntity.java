package fr.enimaloc.esportline.api.wakfu;

import java.util.concurrent.TimeUnit;

public record CachedEntity<T>(T content, long timestamp) {
    public CachedEntity(T content) {
        this(content, System.currentTimeMillis());
    }

    public T get() {
        return content;
    }

    public boolean isExpired(TimeUnit unit, long duration) {
        return System.currentTimeMillis() - timestamp > unit.toMillis(duration);
    }
}
