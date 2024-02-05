package fr.enimaloc.esportline.utils;

import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class BenchmarkUtils {
    BenchmarkUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Result benchmark(Runnable r) {
        long start = System.nanoTime();
        r.run();
        long result = System.nanoTime() - start;
        return new Result(result);
    }

    public static <T> ResultOne<T> benchmark(Supplier<T> s) {
        long start = System.nanoTime();
        T result = s.get();
        long time = System.nanoTime() - start;
        return new ResultOne<>(time, result);
    }

    public static <P, T> ResultOne<T> benchmark(Function<P, T> s, P arg1) {
        long start = System.nanoTime();
        T result = s.apply(arg1);
        long time = System.nanoTime() - start;
        return new ResultOne<>(time, result);
    }

    public static <P1, P2, T> ResultTwo<T, P1> benchmark(BiFunction<P1, P2, T> s, P1 arg1, P2 arg2) {
        long start = System.nanoTime();
        T result = s.apply(arg1, arg2);
        long time = System.nanoTime() - start;
        return new ResultTwo<>(time, result, arg1);
    }

    public static <P1, P2, P3, T> ResultThree<T, P1, P2> benchmark(TriFunction<P1, P2, P3, T> s, P1 arg1, P2 arg2, P3 arg3) {
        long start = System.nanoTime();
        T result = s.apply(arg1, arg2, arg3);
        long time = System.nanoTime() - start;
        return new ResultThree<>(time, result, arg1, arg2);
    }

    public static class Result {
        private final long time;

        Result(long time) {
            this.time = time;
        }

        public long getTime() {
            return time;
        }
    }
    public static class ResultOne<A> extends Result {
        private final A result;

        ResultOne(long time, A result) {
            super(time);
            this.result = result;
        }

        public A getResult() {
            return result;
        }
    }
    public static class ResultTwo<A, B> extends ResultOne<A> {
        private final B result2;

        ResultTwo(long time, A result, B result2) {
            super(time, result);
            this.result2 = result2;
        }

        public B getResult2() {
            return result2;
        }
    }
    public static class ResultThree<A, B, C> extends ResultTwo<A, B> {
        private final C result3;

        ResultThree(long time, A result, B result2, C result3) {
            super(time, result, result2);
            this.result3 = result3;
        }

        public C getResult3() {
            return result3;
        }
    }
}
