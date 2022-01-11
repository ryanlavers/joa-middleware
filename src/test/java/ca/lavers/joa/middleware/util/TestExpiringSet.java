package ca.lavers.joa.middleware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestExpiringSet {

    @Test
    void test() {
        MutableClock clock = new MutableClock();
        ExpiringSet<String> set = new ExpiringSet<>(300, clock);

        set.add("foo");
        assertTrue(set.contains("foo"));

        clock.advanceSeconds(200);

        set.add("bar");
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("bar"));

        clock.advanceSeconds(200);

        assertFalse(set.contains("foo"));
        assertTrue(set.contains("bar"));

        clock.advanceSeconds(200);

        assertFalse(set.contains("bar"));
    }

}
