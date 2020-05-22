package ca.lavers.joa.middleware.router;

import org.junit.jupiter.api.Test;
import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.Request;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RouterTest {

  @Test
  public void basicRouting() {
    // Set up a few different literal routes, call the router with one of those
    // routes. Verify that only the correct route middleware was run, and that
    // the router doesn't fall through to the next middleware after it.

    Router router = new Router();
    Middleware doRun = mock(Middleware.class);
    Middleware dontRun = mock(Middleware.class);
    router.get("/theRightOne", doRun);
    router.get("/notTheRightOne", dontRun);
    router.post("/theRightOne" /* butTheWrongMethod */, dontRun);

    Request req = mock(Request.class);
    Context ctx = new Context(req, null);

    when(req.method()).thenReturn("GET");
    when(req.path()).thenReturn("/theRightOne");

    NextMiddleware next = mock(NextMiddleware.class);
    router.call(ctx, next);

    verify(doRun).call(same(ctx), any());
    verify(dontRun, never()).call(any(), any());
    verify(next, never()).run();
  }

  @Test
  public void fallthrough() {
    // Set up one route, then call the router with a different route.
    // Verify that no route middleware was run, and that the router will
    // fall through to the next middleware after it.

    Router router = new Router();
    Middleware dontRun = mock(Middleware.class);
    router.get("/some-path", dontRun);

    Request req = mock(Request.class);
    Context ctx = new Context(req, null);

    when(req.method()).thenReturn("GET");
    when(req.path()).thenReturn("/a-different-path");

    NextMiddleware next = mock(NextMiddleware.class);
    router.call(ctx, next);

    verify(dontRun, never()).call(any(), any());
    verify(next).run();
  }

  @Test
  public void pathParams() {
    // Set up some routes with named path parameters and call the
    // router with one of them. Verify that only the correct route
    // middleware was run, and that it received the correct parameter
    // values in the Context.

    Router router = new Router();
    Middleware doRun = mock(Middleware.class);
    Middleware dontRun = mock(Middleware.class);
    router.get("/foos/:fooName/bars/:barName", doRun);
    router.get("/bars/:barName/foos/:fooName", dontRun);

    Request req = mock(Request.class);
    Context ctx = new Context(req, null);

    when(req.method()).thenReturn("GET");
    when(req.path()).thenReturn("/foos/alice/bars/bob");

    router.call(ctx, null);

    verify(doRun).call(same(ctx), any());
    verify(dontRun, never()).call(any(), any());

    RouteMatch params = ctx.get("router", "match", RouteMatch.class).get();
    assertEquals("alice", params.getParam("fooName"));
    assertEquals("bob", params.getParam("barName"));
  }

}
