package ca.lavers.joa.middleware.router;

import ca.lavers.joa.core.*;

import java.util.*;

/**
 * Router middleware supporting path parameters
 *
 * Path parameters are indicated by a path segment starting with a colon
 * e.g: /users/:user/preferences
 * matches /users/alice/preferences and "alice" is stored in the "user" path variable
 *
 * Captured path variables are stored in the Context attribute "router"/"match" of
 * type RouteMatch
 *
 * If a route matching the request is found, the associated middleware will be called and
 * the router WILL NOT call the next middleware in the chain (i.e. any middleware
 * installed after the router itself). If a matching route is not found, the router WILL
 * call next. This allows additional routers or other middleware following this router to
 * attempt to handle any unmatched requests.
 *
 * Ex:
 *     router.get("/users/:user/preferences", (ctx, next) -> {
 *       ctx.get("router", "match", RouteMatch.class).ifPresent(match -> {
 *         String user = match.getParam("user");
 *         // (Fetch and respond with user's preferences here)
 *       });
 *     });
 *
 */
public class Router implements Middleware {

  // Method -> List of routes
  private Map<String, List<Route>> routes = new HashMap<>();

  public void addRoute(String method, String path, Middleware... middleware) {
    Route route = new Route(path, new MiddlewareChain(middleware));
    routes
        .computeIfAbsent(method, m -> new ArrayList<>())
        .add(route);
  }

  public void get(String path, Middleware... chain) {
    this.addRoute("GET", path, chain);
  }

  public void put(String path, Middleware... chain) {
    this.addRoute("PUT", path, chain);
  }

  public void post(String path, Middleware... chain) {
    this.addRoute("POST", path, chain);
  }

  public void delete(String path, Middleware... chain) {
    this.addRoute("DELETE", path, chain);
  }

  @Override
  public void call(Context ctx, NextMiddleware next) {
    final Request req = ctx.request();
    Optional<RouteMatch> result = routes
        .computeIfAbsent(req.method(), m -> new ArrayList<>())
        .stream()
        .map(route -> new RouteMatch(route, route.match(req.path())))
        .filter(ram -> ram.match != null)
        .findFirst();

    if(result.isPresent()) {
      ctx.put("router", "match", result.get());
      result.get().route.getMiddleware().call(ctx);
    }
    else {
      // Fall through to next middleware if no routes match
      next.run();
    }
  }

}


