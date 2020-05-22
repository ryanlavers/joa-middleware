package ca.lavers.joa.middleware.prefixrouter;

import org.junit.jupiter.api.Test;
import ca.lavers.joa.core.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrefixRouterTest {

  // Simple test middleware that records whether or not it has been run
  private class TestMiddleware implements Middleware {
    boolean ran = false;
    boolean runNext = true;
    public void call(Context ctx, NextMiddleware next) {
      ran = true;
      if(runNext) next.run();
    }
  }

  private Context mockRequest(String method, String path) {
    Request req = mock(Request.class);
    Context ctx = new Context(req, null);

    when(req.method()).thenReturn(method);
    when(req.path()).thenReturn(path);

    return ctx;
  }

  @Test
  void testMatch() {
    TestMiddleware handlerMatch = new TestMiddleware();
    TestMiddleware handlerNoMatch = new TestMiddleware();
    TestMiddleware next = new TestMiddleware();

    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", handlerNoMatch);
    router.prefix("/bar", handlerMatch);

    Context ctx = mockRequest("GET", "/bar/stuff");

    MiddlewareChain chain = new MiddlewareChain(router, next);
    chain.call(ctx);

    assertFalse(handlerNoMatch.ran);
    assertTrue(handlerMatch.ran);
    assertFalse(next.ran);
  }

  @Test
  void testNoMatch() {
    TestMiddleware handlerNoMatch = new TestMiddleware();
    TestMiddleware next = new TestMiddleware();

    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", handlerNoMatch);
    router.prefix("/bar", handlerNoMatch);

    Context ctx = mockRequest("GET", "/baz/stuff");

    MiddlewareChain chain = new MiddlewareChain(router, next);
    chain.call(ctx);

    assertFalse(handlerNoMatch.ran);
    assertTrue(next.ran);
  }

  @Test
  void testSubPath() {
    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", (ctx, next) -> {
      assertEquals("/bar", ctx.request().path());
    });

    Context ctx = mockRequest("GET", "/foo/bar");

    MiddlewareChain chain = new MiddlewareChain(router);
    chain.call(ctx);
  }

  @Test
  void testFallthrough() {
    TestMiddleware foo = new TestMiddleware();
    TestMiddleware bar = new TestMiddleware();
    TestMiddleware baz = new TestMiddleware();

    PrefixRouter router = new PrefixRouter().withFallthrough();
    router.prefix("/foo", foo);
    router.prefix("/bar", bar);
    router.prefix("/baz", baz);

    Context ctx = mockRequest("GET", "/bar");
    MiddlewareChain chain = new MiddlewareChain(router);
    chain.call(ctx);

    assertFalse(foo.ran);
    assertTrue(bar.ran);
    assertTrue(baz.ran);
  }

  @Test
  void testFallthroughWithMatch() {
    TestMiddleware foo = new TestMiddleware();
    TestMiddleware bar = new TestMiddleware();
    bar.runNext = false; // bar is the "match" so it should stop after running it
    TestMiddleware baz = new TestMiddleware();

    PrefixRouter router = new PrefixRouter().withFallthrough();
    router.prefix("/foo", foo);
    router.prefix("/bar", bar);
    router.prefix("/baz", baz);

    Context ctx = mockRequest("GET", "/foo");
    MiddlewareChain chain = new MiddlewareChain(router);
    chain.call(ctx);

    assertTrue(foo.ran);
    assertTrue(bar.ran);
    assertFalse(baz.ran);
  }

  @Test
  void testFallAllTheWayThrough() {
    TestMiddleware foo = new TestMiddleware();  // will run next
    TestMiddleware bar = new TestMiddleware(); // will run next

    PrefixRouter router = new PrefixRouter().withFallthrough();
    router.prefix("/foo", foo);
    router.prefix("/bar", bar);

    TestMiddleware next = new TestMiddleware(); // Comes after the PrefixRouter
    Context ctx = mockRequest("GET", "/foo");
    MiddlewareChain chain = new MiddlewareChain(router, next);
    chain.call(ctx);

    assertTrue(foo.ran);
    assertTrue(bar.ran);
    assertTrue(next.ran);

  }

}
