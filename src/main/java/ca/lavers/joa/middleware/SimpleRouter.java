package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.MiddlewareChain;
import ca.lavers.joa.core.NextMiddleware;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple router middleware that only supports exact path matching.
 * If a path is matched, middleware following the router will not
 * be called; otherwise it will fall through to the next middleware.
 *
 * ex.
 * Router router = new Router();
 * router.get("/foo", (ctx, next) -> ctx.response().body("Foobar"));
 */
public class SimpleRouter implements Middleware {

    private Map<String, MiddlewareChain> routes = new HashMap<>();

    @Override
    public void call(Context ctx, NextMiddleware next) {
        String method = ctx.request().method();
        String path = ctx.request().path();
        MiddlewareChain chain = routes.get(method + ":" + path);
        if(chain != null) {
            chain.call(ctx);
        }
        else {
            next.run();
        }
    }

    /**
     * Add a route
     * @param method HTTP method to match
     * @param path Path to match
     * @param middleware Middleware(s) to execute (as a chain) when the route is matched
     */
    public void addRoute(String method, String path, Middleware... middleware) {
        routes.put(method + ":" + path, new MiddlewareChain(middleware));
    }

    /**
     * Add a GET route
     * @param path Path to match
     * @param middleware Middleware(s) to execute (as a chain) when the route is matched
     */
    public void get(String path, Middleware... middleware) {
        this.addRoute("GET", path, middleware);
    }

    /**
     * Add a PUT route
     * @param path Path to match
     * @param middleware Middleware(s) to execute (as a chain) when the route is matched
     */
    public void put(String path, Middleware... middleware) {
        this.addRoute("PUT", path, middleware);
    }

    /**
     * Add a POST route
     * @param path Path to match
     * @param middleware Middleware(s) to execute (as a chain) when the route is matched
     */
    public void post(String path, Middleware... middleware) {
        this.addRoute("POST", path, middleware);
    }

    /**
     * Add a DELETE route
     * @param path Path to match
     * @param middleware Middleware(s) to execute (as a chain) when the route is matched
     */
    public void delete(String path, Middleware... middleware) {
        this.addRoute("DELETE", path, middleware);
    }

}
