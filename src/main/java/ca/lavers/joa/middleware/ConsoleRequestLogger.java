package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logs all incoming requests to stdout. Generally installed before
 * any other middleware to ensure all requests are actually logged and
 * to get an accurate elapsed time measurement.
 *
 * Ex.
 * server.use(new ConsoleRequestLogger());
 */
public class ConsoleRequestLogger implements Middleware {
    @Override
    public void call(Context ctx, NextMiddleware next) {
        long startTime = new Date().getTime();

        next.run();

        long elapsed = new Date().getTime() - startTime;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = df.format(startTime);

        // TODO - Include query string?
        System.out.println(String.join(" ",
                timestamp, "-",
                Integer.toString(ctx.response().status()),
                ctx.request().path(),
                ctx.request().remoteIp(),
                Long.toString(elapsed) + "ms"
        ));
    }
}
