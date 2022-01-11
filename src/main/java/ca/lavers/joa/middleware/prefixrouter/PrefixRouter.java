package ca.lavers.joa.middleware.prefixrouter;

import ca.lavers.joa.core.*;
import ca.lavers.joa.core.util.WrappedRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Router middleware that matches on path prefixes, with optional fallthrough
 *
 * If a request path starts with one of the configured prefixes, regardless of HTTP method,
 * the associated middleware will be called with the prefix stripped from the request path.
 * This allows "mounting" middleware to a sub-path without that middleware needing to be
 * aware of its prefix.
 *
 * If fallthrough is enabled with {@code .withFallthrough()} and a configured middleware
 * calls next, then the next configured middleware in this router will be tried even if
 * the prefix doesn't match. For example, given this configuration:
 *
 * router.withFallthrough();
 * router.prefix("/foo", (ctx, next) -> {
 *     next.run();
 * });
 * router.prefix("/bar", (ctx, next) -> {
 *     // Handle request
 * });
 * router.prefix("/baz", (ctx, next) -> {
 *     // Handle request
 * });
 *
 * If the request path is "/foo/stuff.txt" then the first middleware will be tried; since
 * it calls next.run(), the next middleware will be called even though the request doesn't
 * start with "/bar". In both middleware, the ctx.request().path() will return "/stuff.txt".
 * The third middleware will not be called, since the second one did not call next.
 *
 * This behaviour can be used for (among other things) API version rollup, where each version's
 * router only needs to handle endpoints that were added or changed in that version; if a match
 * is not found in that router, it will fall through to the next oldest version:
 *
 * router.withFallthrough();
 * router.prefix("/v2", v2router);
 * router.prefix("/v1", v1router);
 *
 * In this case, if an endpoint under /v2 is requested, but it is not explicitly handled by
 * the v2 API, then the same path (minus /v2 prefix) will be tried against the v1 API. If
 * a v1 endpoint is requested, then the v1 version will be called even if there was a newer
 * version in v2.
 *
 */
public class PrefixRouter implements Middleware {

    private boolean fallthrough = false;
    private List<Prefix> prefixes = new ArrayList<>();

    public PrefixRouter() {

    }

    /**
     * Enable fallthrough for this router
     */
    public PrefixRouter withFallthrough() {
        this.fallthrough = true;
        return this;
    }

    /**
     * Add a prefix and handler to this router
     *
     * @param prefix The path prefix to match
     * @param chain The middleare (will be chained together if multiple) to be called when this prefix matches
     */
    public PrefixRouter prefix(String prefix, Middleware... chain) {
        this.prefix(prefix, new MiddlewareChain(chain));
        return this;
    }

    /**
     * Add a prefix and handler to this router
     *
     * @param prefix The path prefix to match
     * @param chain The middleare chain to be called when this prefix matches
     */
    public PrefixRouter prefix(String prefix, MiddlewareChain chain) {
        // TODO -- check not exists already
        prefixes.add(new Prefix(prefix, chain));
        return this;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        final String path = ctx.request().path();

        int index = findMatchingIndex(path);
        if(index >= 0) {
            if(!callHandlerByIndex(index, path, ctx)) {
                // Fell all the way through my prefixes; call next
                next.run();
            }
        }
        else {
            // No match! Fall through
            next.run();
        }
    }

    private boolean callHandlerByIndex(int index, String path, Context ctx) {
        if(index >= prefixes.size()) {
            return false;
        }
        Prefix prefix = prefixes.get(index);
        Context newContext = ctx.withAlternateRequest(new SubRequest(ctx.request(), prefix.subPath(path)));
        EndChecker end = new EndChecker();
        new MiddlewareChain(prefix.chain, end).call(newContext);
        if(this.fallthrough && end.ran) {
            return callHandlerByIndex(index + 1, path, ctx);
        }
        else {
            return true;
        }
    }

    private int findMatchingIndex(String path) {
        for(int i=0; i<prefixes.size(); i++) {
            if(prefixes.get(i).matches(path)) {
                return i;
            }
        }
        return -1;
    }

}

class Prefix {

    String prefix;
    MiddlewareChain chain;

    Prefix(String prefix, MiddlewareChain chain) {
        if(!prefix.endsWith("/")) {
            prefix += "/";
        }
        this.prefix = prefix;
        this.chain = chain;
    }

    boolean matches(String path) {
        if(prefix.equals(path + "/")) {
            return true;
        }
        return path.startsWith(prefix);
    }

    String subPath(String path) {
        String subpath = path.substring(
                path.endsWith("/") ? prefix.length() : prefix.length() -1
        );
        if(!subpath.startsWith("/")) {
            subpath = "/" + subpath;
        }
        return subpath;
    }
}

class SubRequest extends WrappedRequest {
    private final String subPath;

    SubRequest(Request wrapped, String subPath) {
        super(wrapped);
        this.subPath = subPath;
    }

    @Override
    public String path() {
        return subPath;
    }
}

// Just checks to see if it was called
class EndChecker implements Middleware {
    boolean ran = false;
    public void call(Context ctx, NextMiddleware next) {
        ran = true;
    }
}