package ca.lavers.joa.middleware.util;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class ExpiringSet<E> {

    private final long expirationMillis;
    private final Map<E, Entry<E>> entries = new HashMap<>();
    private final Clock clock;

    public ExpiringSet(long expirationSeconds) {
        this.expirationMillis = expirationSeconds * 1000;
        clock = Clock.systemUTC();
    }

    ExpiringSet(long expirationSeconds, Clock clock) {
        this.expirationMillis = expirationSeconds * 1000;
        this.clock = clock;
    }

    public synchronized void add(E item) {
        this.entries.merge(item, new Entry<>(item, clock.millis()), (a, b) -> {
            a.time = Math.max(a.time, b.time);
            return a;
        });
    }

    public synchronized boolean contains(E item) {
        // TODO - don't tidy every time; just check the expiry on the found item
        tidy();
        return this.entries.containsKey(item);
    }

    // TODO -- make public and require user to periodically tidy the list
    private void tidy() {
        final long now = clock.millis();
        this.entries.values().removeIf(entry -> now - entry.time > expirationMillis);
    }

    private static class Entry<E> {
        final E item;
        long time;

        public Entry(E item, long time) {
            this.item = item;
            this.time = time;
        }
    }
}
