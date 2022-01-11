package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.HttpException;
import ca.lavers.joa.core.errors.TooManyRequestsException;
import ca.lavers.joa.middleware.util.ExpiringSet;
import ca.lavers.joa.middleware.util.SlidingWindowCache;

import java.time.Duration;

/**
 * The Bouncer rejects requests from clients that seem to be trying to cause trouble.
 *
 * All requests are tracked, and any IP address that has made too many failed requests in
 * the configurable window of time will be added to the naughty list. All requests from
 * naughty clients are rejected until they cease making requests for a long enough time period
 * (i.e. any request made by a naughty IP will reset the duration of their stay on the list).
 *
 * A "failed request" means any request that results in a 4xx HTTP status code.
 */
public class Bouncer implements Middleware {

    private final SlidingWindowCache<String> fails;
    private final int maxFails;

    private final ExpiringSet<String> naughtyList;

    // TODO - Whitelist; some way of removing an IP from the naughty list
    // TODO - Logging, or a callback for when a client gets added to the list

    /**
     * Creates a new Bouncer with the given configuration settings.
     *
     * If an IP address makes more than maxFailsPerWindow failed requests within the time
     * period specified by the windowSize, then it will be added to the naughty list.
     *
     * @param windowSize The amount of time that requests are remembered
     * @param maxFailsPerWindow The maximum number of failed requests an IP is allowed to make
     *                          without being added to the naughty list
     * @param naughtyTime The amount of time that must elapse without a request from a naughty
     *                    IP before that IP will be removed from the list
     */
    public Bouncer(Duration windowSize, int maxFailsPerWindow, Duration naughtyTime) {
        this(windowSize.toSeconds(), maxFailsPerWindow, naughtyTime.toSeconds());
    }

    /**
     * {@link #Bouncer(Duration, int, Duration)}
     * @param windowSeconds The amount of time that requests are remembered
     * @param maxFailsPerWindow The maximum number of failed requests an IP is allowed to make
     *                          without being added to the naughty list
     * @param naughtyTimeSeconds The amount of time that must elapse without a request from a naughty
     *                           IP before that IP will be removed from the list
     */
    public Bouncer(long windowSeconds, int maxFailsPerWindow, long naughtyTimeSeconds) {
        this.fails = new SlidingWindowCache<>(windowSeconds);
        this.maxFails = maxFailsPerWindow;
        this.naughtyList = new ExpiringSet<>(naughtyTimeSeconds);
    }

    @Override
    public void call(final Context ctx, final NextMiddleware next) {
        final String ip = ctx.request().remoteIp();

        if(naughtyList.contains(ip)) {
            naughtyList.add(ip); // Resets the expiry -- if they keep making requests, they stay blocked
            throw new TooManyRequestsException();
        }
        else {
            HttpException caught = null;
            try {
                next.run();
            }
            catch(HttpException e) {
                caught = e;
                throw e;
            }
            finally {
                int status = (caught != null) ? caught.getStatus() : ctx.response().status();
                if(status < 500 && status >= 400) {
                    if(fails.addAndGetCountFor(ip) > maxFails) {
                        naughtyList.add(ip);
                    }
                }
            }
        }
    }
}

