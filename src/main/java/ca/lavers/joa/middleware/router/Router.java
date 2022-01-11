package ca.lavers.joa.middleware.router;

import ca.lavers.joa.core.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Router middleware supporting path parameters
 *
 * Path parameters are indicated by a path segment starting with a colon
 * e.g: /users/:user/preferences
 * matches /users/alice/preferences and "alice" is stored in the "user" path variable
 *
 * Captured path variables are stored in a RouteMatch instance on the Context. You can
 * use the convenience method {@link #getRouteMatch(Context)} to retrieve it. (Be aware
 * that nested Routers will clobber each other's RouteMatch; TODO)
 *
 * If a route matching the request is found, the associated middleware will be called and
 * the router WILL NOT call the next middleware in the chain (i.e. any middleware
 * installed after the router itself). If a matching route is not found, the router WILL
 * call next. This allows additional routers or other middleware following this router to
 * attempt to handle any unmatched requests.
 *
 * Ex:
 *     router.get("/users/:user/preferences", (ctx, next) -> {
 *       RouteMatch match = Router.getRouteMatch(ctx);
 *       if(match != null) {
 *         String user = match.getParam("user");
 *         // (Fetch and respond with user's preferences here)
 *       }
 *     });
 *
 */
public class Router implements Middleware {

  public static final String NS = Router.class.getCanonicalName();
  public static final String MATCH = "match";
  public static final String ORIGINAL_PATH = "originalPath";

  protected final static String METHOD_ALL = "*";

  // Method -> List of routes
  private final Map<String, List<Route>> routes = new HashMap<>();

  /**
   * Add a route to this Router.
   *
   * @param method The HTTP method this route should match
   *               (must be all-uppercase; e.g. GET, POST, etc.)
   * @param path The request path (not including query string) to match
   * @param middleware The middleware(s) to be invoked when this route matches
   * @return this
   */
  public Router addRoute(String method, String path, Middleware... middleware) {
    return addRoute(method, path, false, middleware);
  }

  /**
   * Add a route to this Router, optionally defined as a prefix route.
   *
   * If isPrefix is true, any request that begins with the specified path will
   * be considered a match, rather than the usual behavior of requiring the
   * whole path to match. This prefix will be removed from the beginning of the
   * request path so that the handling middleware will see only the part of the
   * path following this prefix. For example, if the path is "/foo" and the
   * request path is "/foo/bar/baz" then the middleware on that chain will see
   * "/bar/baz" as the request path. The original request path can be retrieved
   * with the utility method {@link #getOriginalPath(Context)}.
   *
   * Note that path parameters cannot be used in prefix routes.
   *
   * @param method The HTTP method this route should match
   *               (must be all-uppercase; e.g. GET, POST, etc.)
   * @param path The request path (not including query string) to match
   * @param isPrefix True if this route should be considered a prefix route
   * @param middleware The middleware(s) to be invoked when this route matches
   * @return this
   */
  public Router addRoute(String method, String path, boolean isPrefix, Middleware... middleware) {
    Route route = new Route(path, new MiddlewareChain(middleware), isPrefix);
    routes
        .computeIfAbsent(method, m -> new ArrayList<>())
        .add(route);
    return this;
  }

  public Router get(String path, Middleware... chain) {
    return this.addRoute("GET", path, chain);
  }

  public Router getPrefix(String path, Middleware... chain) {
    return this.addRoute("GET", path, true, chain);
  }

  public Router put(String path, Middleware... chain) {
    return this.addRoute("PUT", path, chain);
  }

  public Router putPrefix(String path, Middleware... chain) {
    return this.addRoute("PUT", path, true, chain);
  }

  public Router post(String path, Middleware... chain) {
    return this.addRoute("POST", path, chain);
  }

  public Router postPrefix(String path, Middleware... chain) {
    return this.addRoute("POST", path, true, chain);
  }

  public Router delete(String path, Middleware... chain) {
    return this.addRoute("DELETE", path, chain);
  }

  public Router deletePrefix(String path, Middleware... chain) {
    return this.addRoute("DELETE", path, true, chain);
  }

  public Router mount(String path, Middleware... chain) {
    return this.addRoute(METHOD_ALL, path, true, chain);
  }

  @Override
  public void call(Context ctx, NextMiddleware next) {
    final Request req = ctx.request();

    Stream<Route> possibleRoutes = Stream.concat(
            // Mounts have higher priority than method-specific routes
            // TODO - consider putting all routes into a single list so they can be considered in defn order
            routes.computeIfAbsent(METHOD_ALL, m -> new ArrayList<>()).stream(),
            routes.computeIfAbsent(req.method(), m -> new ArrayList<>()).stream()
    );

    Optional<RouteMatch> result = possibleRoutes
        .map(route -> new RouteMatch(route, route.match(req.path())))
        .filter(ram -> ram.match != null)
        .findFirst();

    if(result.isPresent()) {
      RouteMatch match = result.get();
      // TODO -- Should we create a new Context with these attributes to avoid problems with nested routers?
      ctx.put(NS, MATCH, match);
      if(match.route.isPrefix()) {
        ctx.put(NS, ORIGINAL_PATH, req.path());
        ctx = ctx.withAlternateRequest(new PrefixStrippingRequest(req, match.route.getPath()));
      }
      result.get().route.getMiddleware().call(ctx);
    }
    else {
      // Fall through to next middleware if no routes match
      next.run();
    }
  }

  /**
   * Retrieves the RouteMatch object containing the values that matched
   * any path parameters from the Context, or null if not present.
   */
  public static RouteMatch getRouteMatch(Context ctx) {
    return ctx.get(NS, MATCH, RouteMatch.class).orElse(null);
  }

  /**
   * Retrieves the original request path that hasn't been prefix-stripped
   * from the Context, or null if not present (i.e. the request didn't
   * match a prefix request).
   */
  public static String getOriginalPath(Context ctx) {
    return ctx.get(NS, ORIGINAL_PATH, String.class).orElse(null);
  }


}


