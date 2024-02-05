package fr.enimaloc.esportline.utils.function;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ThrowableBiFunction<T, U, R> extends BiFunction<T, U, R> {
    R applyThrows(T t, U u) throws Exception;

    @Override
    default R apply(T t, U u) {
        try {
            return applyThrows(t, u);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
