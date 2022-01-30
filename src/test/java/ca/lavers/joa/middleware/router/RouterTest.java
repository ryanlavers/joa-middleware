package ca.lavers.joa.middleware.router;

import ca.lavers.joa.test.MockRequest;
import ca.lavers.joa.test.TestContext;
import ca.lavers.joa.test.TestMiddleware;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RouterTest {

  @Test
  public void basicRouting() {
    // Set up a few different literal routes, call the router with one of those
    // routes. Verify that only the correct route middleware was run, and that
    // the router doesn't fall through to the next middleware after it.

    Router router = new Router();
    TestMiddleware doRun = new TestMiddleware();
    TestMiddleware dontRun = new TestMiddleware();
    router.get("/theRightOne", doRun);
    router.get("/notTheRightOne", dontRun);
    router.post("/theRightOne" /* butTheWrongMethod */, dontRun);

    TestMiddleware afterRouter = new TestMiddleware();
    MockRequest.get("/theRightOne").run(router, afterRouter);

    assertTrue(doRun.ran());
    assertFalse(dontRun.ran());
    assertFalse(afterRouter.ran());
  }

  @Test
  public void fallthrough() {
    // Set up one route, then call the router with a different route.
    // Verify that no route middleware was run, and that the router will
    // fall through to the next middleware after it.

    Router router = new Router();
    TestMiddleware dontRun = new TestMiddleware();
    router.get("/some-path", dontRun);

    TestMiddleware afterRouter = new TestMiddleware();
    MockRequest.get("/a-different-path").run(router, afterRouter);

    assertFalse(dontRun.ran());
    assertTrue(afterRouter.ran());
  }

  @Test
  public void pathParams() {
    // Set up some routes with named path parameters and call the
    // router with one of them. Verify that only the correct route
    // middleware was run, and that it received the correct parameter
    // values in the Context.

    Router router = new Router();
    TestMiddleware doRun = new TestMiddleware();
    TestMiddleware dontRun = new TestMiddleware();
    router.get("/foos/:fooName/bars/:barName", doRun);
    router.get("/bars/:barName/foos/:fooName", dontRun);

    TestMiddleware afterRouter = new TestMiddleware();
    TestContext ctx = MockRequest.get("/foos/alice/bars/bob").run(router, afterRouter);

    assertTrue(doRun.ran());
    assertFalse(dontRun.ran());
    assertFalse(afterRouter.ran());

    RouteMatch params = Router.getRouteMatch(ctx);
    assertEquals("alice", params.getParam("fooName"));
    assertEquals("bob", params.getParam("barName"));
  }

  @Test
  public void prefixes() {
    Router router = new Router();
    TestMiddleware doRun = new TestMiddleware();
    TestMiddleware dontRun = new TestMiddleware();

    router.mount("/foo", doRun, (ctx, next) -> {
      // Asserting these in here because mount creates a new, modified Context for the
      // middleware under it
      assertEquals("/bar", ctx.request().path());
      assertEquals("/foo/bar", Router.getOriginalPath(ctx));
    });
    router.get("/foo/bar", dontRun);

    TestMiddleware afterRouter = new TestMiddleware();
    MockRequest.get("/foo/bar").run(router, afterRouter);

    assertTrue(doRun.ran());
    assertFalse(dontRun.ran());
    assertFalse(afterRouter.ran());
  }

  @Test
  void prefixMissingSlash() {
    Router router = new Router();
    TestMiddleware dontRun = new TestMiddleware();

    router.mount("/foo", dontRun);

    MockRequest.get("/foo1").run(router);

    assertFalse(dontRun.ran());
  }

  @Test
  void prefixExactMatch() {
    Router router = new Router();
    TestMiddleware doRun = new TestMiddleware();

    router.mount("/foo", doRun);

    MockRequest.get("/foo").run(router);

    assertTrue(doRun.ran());
  }

}
