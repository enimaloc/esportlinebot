package fr.enimaloc.esportlinebot.utils;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class Cache<T> implements Set<T> {

    private final Set<Entry<T>> cache;
    private       long          maxAge;

    public Cache(long maxAge) {
        this.maxAge = maxAge;
        this.cache = new HashSet<>();
    }

    public Cache(long duration, TimeUnit unit) {
        this(unit.toMillis(duration));
    }

    @Override
    public int size() {
        clean();
        return this.cache.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return this.cache.stream().anyMatch(entry -> entry.value.equals(o) && !entry.isExpired(this.maxAge));
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .map(entry -> entry.value)
                .collect(Collectors.toSet())
                .iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .map(entry -> entry.value)
                .distinct()
                .toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .map(entry -> entry.value)
                .distinct()
                .toArray(size -> a);
    }

    public abstract T factory(Object... objects);

    public T getOrCreate(int index, Object... factoryObjects) {
        return get(index).orElseGet(() -> {
            T t = factory(factoryObjects);
            add(t);
            return t;
        });
    }

    public T getOrCreate(Predicate<T> predicate, Object... factoryObjects) {
        return get(predicate).orElseGet(() -> {
            T t = factory(factoryObjects);
            add(t);
            return t;
        });
    }

    public Optional<T> get(int index) {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .map(entry -> entry.value)
                .distinct()
                .skip(index)
                .findFirst();
    }

    public Optional<T> get(Predicate<T> predicate) {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .map(entry -> entry.value)
                .distinct()
                .filter(predicate)
                .findFirst();
    }

    public Optional<Entry<T>> getWithMeta(int index) {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .distinct()
                .skip(index)
                .findFirst();
    }

    public Optional<Entry<T>> getWithMeta(Predicate<T> predicate) {
        return this.cache.stream()
                .filter(entry -> !entry.isExpired(maxAge))
                .distinct()
                .filter(entry -> predicate.test(entry.value))
                .findFirst();
    }

    @Override
    public boolean add(T t) {
        return this.cache.add(new Entry<>(t));
    }

    public boolean replace(T t) {
        if (removeIf(t::equals)) {
            add(t);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return this.cache.removeIf(entry -> entry.value.equals(o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.cache.stream().allMatch(entry -> c.contains(entry.value) && !entry.isExpired(this.maxAge));
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return this.cache.addAll((Collection<? extends Entry<T>>) c.stream().map(Entry::new).toList());
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.cache.retainAll(c.stream().map(Entry::new).toList());
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.cache.removeAll(c.stream().map(Entry::new).toList());
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    public int clean() {
        int size = cache.size();
        this.cache.removeIf(entry -> entry.isExpired(this.maxAge));
        return size - cache.size();
    }

    public void addOrUpdate(T t) {
        clean();
        if (!contains(t)) {
            add(t);
        }
    }

    public record Entry<T>(T value, long timestamp) {
        public Entry(T value) {
            this(value, System.currentTimeMillis());
        }

        public boolean isExpired(long maxAge) {
            return this.timestamp + maxAge < System.currentTimeMillis();
        }
    }
}
