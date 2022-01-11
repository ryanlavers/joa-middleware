package ca.lavers.joa.middleware.util;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SlidingWindowCache<T> {

    private final Deque<Entry<T>> window = new ArrayDeque<>();
    private final Map<T, Integer> counts = new HashMap<>();
    private final Clock clock;

    private final long windowSizeMillis;

    public SlidingWindowCache(long windowSeconds) {
        this.windowSizeMillis = windowSeconds * 1000;
        this.clock = Clock.systemUTC();
    }

    SlidingWindowCache(long windowSeconds, Clock clock) {
        this.windowSizeMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    public synchronized int addAndGetCountFor(final T item) {
        trimWindow();
        window.addFirst(new Entry<>(item, clock.millis()));
        return counts.merge(item, 1, Integer::sum);
    }

    private void trimWindow() {
        final long now = clock.millis();
        while(window.size() > 0) {
            final Entry<T> entry = window.peekLast();
            if(now - entry.time > windowSizeMillis) {
                window.removeLast();
                counts.computeIfPresent(entry.item, (i, value) -> value == 1 ? null : value - 1);
            }
            else {
                break;
            }
        }
    }

    private static class Entry<T> {
        final T item;
        final long time;

        public Entry(T item, long time) {
            this.item = item;
            this.time = time;
        }
    }
}

