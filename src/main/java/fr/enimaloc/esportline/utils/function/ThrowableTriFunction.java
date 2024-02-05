package fr.enimaloc.esportline.utils.function;

import org.apache.commons.lang3.function.TriFunction;

@FunctionalInterface
public interface ThrowableTriFunction<T, U, V, R> extends TriFunction<T, U, V, R> {
    R applyThrows(T t, U u, V v) throws Exception;

    default R apply(T t, U u, V v) {
        try {
            return applyThrows(t, u, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
