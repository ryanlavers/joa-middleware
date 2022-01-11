package ca.lavers.joa.middleware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSlidingWindowCache {

    @Test
    void test() {
        MutableClock clock = new MutableClock();
        SlidingWindowCache<String> cache = new SlidingWindowCache<>(300, clock);

        assertEquals(1, cache.addAndGetCountFor("foo"));
        assertEquals(1, cache.addAndGetCountFor("bar"));
        assertEquals(2, cache.addAndGetCountFor("foo"));

        clock.advanceSeconds(200);

        assertEquals(3, cache.addAndGetCountFor("foo"));
        assertEquals(2, cache.addAndGetCountFor("bar"));

        clock.advanceSeconds(200);

        assertEquals(2, cache.addAndGetCountFor("foo"));
        assertEquals(3, cache.addAndGetCountFor("foo"));
        assertEquals(2, cache.addAndGetCountFor("bar"));

        clock.advanceSeconds(500);

        assertEquals(1, cache.addAndGetCountFor("foo"));
        assertEquals(1, cache.addAndGetCountFor("bar"));
    }

}
