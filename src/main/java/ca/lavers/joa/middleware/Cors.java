package ca.lavers.joa.middleware;

import ca.lavers.joa.core.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple CORS handler middleware
 *
 * This implementation is only concerned with whether or not particular origins are allowed to
 * make cross-origin requests; if the origin is allowed, it will respond allowing all methods
 * and requested headers.
 *
 * As CORS preflight requests make use of the HTTP OPTIONS method, this middleware can be
 * configured to respond to OPTIONS requests by calling the implementOptions() method if
 * you do not have later middleware doing this.
 *
 *
 * Ex.
 * server.use(new Cors().withAllowedOrigin("http://localhost"));
 */
public class Cors implements Middleware {

    private Set<String> allowedOrigins = new HashSet<>();
    private boolean implementOptions = false;
    private final MiddlewareChain optionsHandler = new MiddlewareChain(new OptionsHandler());

    /**
     * Add an allowed origin. If the request contains an Origin header matching
     * one of the allowed origins, CORS headers will be added to the response
     * permitting the cross-origin request.
     */
    public Cors withAllowedOrigin(String origin) {
        this.allowedOrigins.add(origin);
        return this;
    }

    /**
     * If called, this Cors instance will respond to OPTIONS requests with 204 No Content, in
     * order to handle CORS preflight requests.
     *
     * Not needed (and counterproductive) if later middleware already handles OPTIONS requests.
     */
    public Cors implementOptions() {
        this.implementOptions = true;
        return this;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        final Request req = ctx.request();
        final Response resp = ctx.response();

        // TODO - Consider adding CORS headers _after_ calling next to ensure they don't get clobbered
        handleCors(req, resp);

        if(implementOptions) {
            optionsHandler.call(ctx, next);
        }
        else {
            next.run();
        }
    }

    // If Origin request header is present and matches one of our allowed origins,
    // add appropriate response headers
    private void handleCors(Request req, Response resp) {
        final String originHeader = req.header("Origin");
        if(originHeader != null && !originHeader.isEmpty()) {
            if(allowedOrigins.contains(originHeader)) {
                resp.header("Access-Control-Allow-Origin", originHeader);
                resp.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                final String requestHeaders = req.header("Access-Control-Request-Headers");
                if (requestHeaders != null && !requestHeaders.isEmpty()) {
                    resp.header("Access-Control-Allow-Headers", requestHeaders);
                }
                resp.header("Access-Control-Max-Age", "86400"); // TODO -- Make configurable
                resp.header("Vary", "Origin");
            }
        }
    }
}

// Middleware that simply responds to OPTIONS requests with a 204 No Content, otherwise
// runs the next middleware.
class OptionsHandler implements Middleware {
    @Override
    public void call(Context ctx, NextMiddleware next) {
        final Request req = ctx.request();
        final Response resp = ctx.response();

        if(req.method().equals("OPTIONS")) {
            resp.status(204);
        }
        else {
            next.run();
        }
    }
}
