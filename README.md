# Joa Middleware

A collection of middleware for the [Joa Framework](https://github.com/ryanlavers/joa-core). These are intended to be useful enough for basic web applications, but also serve as examples of how to build middleware for the framework.

## ConsoleRequestLogger

Logs all incoming requests to stdout. Generally installed before any other middleware to ensure all requests are actually logged and to get an accurate elapsed time measurement.

Usage:
```java
server.use(new ConsoleRequestLogger());
```

## ErrorHandler

Middleware that catches and handles any exceptions thrown from further down the chain.

If the exception is a subclass of `HttpException`, it will set the status and error message appropriately. Otherwise, it will respond with a 500 Internal Server Error to prevent exception leakage to the client.

Can be optionally given a function that will be called with every caught exception, so that additional handling can be done (such as logging).

```java
server.use(new ErrorHandler(e -> {
  // Log the full stacktrace of any non-HttpException
  if(!(e instanceof HttpException)) {
    e.printStackTrace();
  }
}));
```

## Router

Router middleware supporting path parameters.

Path parameters are indicated by a path segment starting with a colon e.g: `/users/:user/preferences` matches `/users/alice/preferences` and "alice" is stored in the "user" path variable.

Captured path variables are stored in the Context attribute "router"/"match" of type `RouteMatch`.

If a route matching the request is found, the associated middleware will be called and the router WILL NOT call the next middleware in the chain (i.e. any middleware installed after the router itself). If a matching route is not found, the router WILL call next. This allows additional routers or other middleware following this router to attempt to handle any unmatched requests.

```java
router.get("/users/:user/preferences", (ctx, next) -> {
  RouteMatch match = Router.getRouteMatch(ctx);
  if(match != null) {
    String user = match.getParam("user");
    // (Fetch and respond with user's preferences here)
  });
});
```

## Cors

Simple CORS handler middleware

This implementation is only concerned with whether or not particular origins are allowed to make cross-origin requests; if the origin is allowed, it will respond allowing all methods and requested headers.

As CORS preflight requests make use of the HTTP OPTIONS method, this middleware can be configured to respond to OPTIONS requests by calling the `.implementOptions()` method if you do not have later middleware doing this.

```java
server.use(new Cors().withAllowedOrigin("http://localhost"));
```

## Batcher

Request-batching middleware

Allows clients to specify a batch of requests to carry out as a single POST request to the batch endpoint.

If a POST request to the configured path (`/batch` by default) is received, then the request body will be interpreted as a list of requests to perform (see `BatchRequestList` for the expected format) and the middleware chain following this Batcher will be called once for each of them, with the Context modified appropriately (i.e. other middleware need not be aware of or coded to support batching at all) and then all responses are combined and returned in a single response body (see `BatchResponseList` for the format).

Where this middleware is installed in the chain matters -- only middleware that comes AFTER the Batcher will be called for each individual sub-request. Any middleware that comes before it will just see the one batch request. Therefore it makes sense to install it before the router (so each sub-request can be routed properly), before the error handler (so that any errors returned by a sub-request will be reported correctly in the batch response, and, depending on requirements, before any request logger (so that each sub-request is logged the same as if it had been sent separately, rather than one log message for the whole batch).

For middleware coming after the Batcher, on batch requests there will be two attributes added to the Context:
- "batcher"/"originalRequest" is the original Request object representing the batch request itself
- "batcher"/"batchId" is a unique ID string generated for this batch request; it will be the same for all sub-requests in this same batch.

```java
server.use(new Batcher());
```

## FileServer

Serves the contents of files from the specified root directory

This middleware uses the full request path to locate files within the root directory, so if you only want to serve files under a sub-path (e.g. `/static`) then you'll either need to use a prefix-stripping middleware before it in the chain (see `PrefixRouter`) or just ensure your files are in a similarly-named directory in your root directory (e.g. configure your root directory as `/var/www` and put your files under `/var/www/static`)

```java
prefixRouter.prefix("/static", new FileServer("/var/www"));
```

## PrefixRouter

Router middleware that matches on path prefixes, with optional fallthrough

If a request path starts with one of the configured prefixes, regardless of HTTP method, the associated middleware will be called with the prefix stripped from the request path. This allows "mounting" middleware to a sub-path without that middleware needing to be aware of its prefix.

If fallthrough is enabled with `.withFallthrough()` and a configured middleware calls next, then the next configured middleware in this router will be tried even if the prefix doesn't match. For example, given this configuration:

```java
router.withFallthrough();
router.prefix("/foo", (ctx, next) -> {
    next.run();
});
router.prefix("/bar", (ctx, next) -> {
    // Handle request
});
router.prefix("/baz", (ctx, next) -> {
    // Handle request
});
```

If the request path is `/foo/stuff.txt` then the first middleware will be tried; since it calls `next.run()`, the next middleware will be called even though the request doesn't start with `/bar`. In both middleware, the `ctx.request().path()` will return `/stuff.txt`. The third middleware will not be called, since the second one did not call next.

This behaviour can be used for (among other things) API version rollup, where each version's router only needs to handle endpoints that were added or changed in that version; if a match is not found in that router, it will fall through to the next oldest version:

```java
router.withFallthrough();
router.prefix("/v2", v2router);
router.prefix("/v1", v1router);
```

In this case, if an endpoint under `/v2` is requested, but it is not explicitly handled by the v2 API, then the same path (minus `/v2` prefix) will be tried against the v1 API. If a v1 endpoint is requested, then the v1 version will be called even if there was a newer version in v2.