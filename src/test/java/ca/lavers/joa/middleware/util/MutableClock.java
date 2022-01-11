package ca.lavers.joa.middleware.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class MutableClock extends Clock {

    private Instant instant;

    public MutableClock() {
        this.instant = Instant.now();
    }

    public MutableClock(Instant instant) {
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void advanceSeconds(int seconds) {
        this.instant = this.instant.plus(seconds, ChronoUnit.SECONDS);
    }

    public void rewindSeconds(int seconds) {
        this.instant = this.instant.minus(seconds, ChronoUnit.SECONDS);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
