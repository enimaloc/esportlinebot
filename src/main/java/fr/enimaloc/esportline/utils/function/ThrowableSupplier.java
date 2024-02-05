package fr.enimaloc.esportline.utils.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowableSupplier<T> extends Supplier<T> {
    T getThrows() throws Exception;

    @Override
    default T get() {
        try {
            return getThrows();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
