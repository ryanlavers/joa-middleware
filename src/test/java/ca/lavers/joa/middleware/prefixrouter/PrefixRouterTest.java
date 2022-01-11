package ca.lavers.joa.middleware.prefixrouter;

import ca.lavers.joa.test.MockRequest;
import ca.lavers.joa.test.TestMiddleware;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrefixRouterTest {

  @Test
  void testMatch() {
    TestMiddleware handlerMatch = new TestMiddleware();
    TestMiddleware handlerNoMatch = new TestMiddleware();
    TestMiddleware next = new TestMiddleware();

    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", handlerNoMatch);
    router.prefix("/bar", handlerMatch);

    MockRequest.get("/bar/stuff").run(router, next);

    assertFalse(handlerNoMatch.ran());
    assertTrue(handlerMatch.ran());
    assertFalse(next.ran());
  }

  @Test
  void testNoMatch() {
    TestMiddleware handlerNoMatch = new TestMiddleware();
    TestMiddleware next = new TestMiddleware();

    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", handlerNoMatch);
    router.prefix("/bar", handlerNoMatch);

    MockRequest.get("/baz/stuff").run(router, next);

    assertFalse(handlerNoMatch.ran());
    assertTrue(next.ran());
  }

  @Test
  void testSubPath() {
    TestMiddleware doRun = new TestMiddleware();
    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", doRun, (ctx, next) -> {
      assertEquals("/bar", ctx.request().path());
    });

    MockRequest.get("/foo/bar").run(router);

    assertTrue(doRun.ran());
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

    MockRequest.get("/bar").run(router);

    assertFalse(foo.ran());
    assertTrue(bar.ran());
    assertTrue(baz.ran());
  }

  @Test
  void testFallthroughWithMatch() {
    TestMiddleware foo = new TestMiddleware();
    TestMiddleware bar = new TestMiddleware().runNext(false); // bar is the "match" so it should not call next
    TestMiddleware baz = new TestMiddleware();

    PrefixRouter router = new PrefixRouter().withFallthrough();
    router.prefix("/foo", foo);
    router.prefix("/bar", bar);
    router.prefix("/baz", baz);

    MockRequest.get("/foo").run(router);

    assertTrue(foo.ran());
    assertTrue(bar.ran());
    assertFalse(baz.ran());
  }

  @Test
  void testFallAllTheWayThrough() {
    TestMiddleware foo = new TestMiddleware().runNext(true);
    TestMiddleware bar = new TestMiddleware().runNext(true);

    PrefixRouter router = new PrefixRouter().withFallthrough();
    router.prefix("/foo", foo);
    router.prefix("/bar", bar);

    TestMiddleware next = new TestMiddleware(); // Comes after the PrefixRouter
    MockRequest.get("/foo").run(router, next);

    assertTrue(foo.ran());
    assertTrue(bar.ran());
    assertTrue(next.ran());
  }

  @Test
  void testContextAttributesPreserved() {
    TestMiddleware doRun = new TestMiddleware();
    PrefixRouter router = new PrefixRouter();
    router.prefix("/foo", doRun, (ctx, next) -> {
      assertEquals("baz", ctx.get("foo", "bar", String.class).get());
    });

    MockRequest.get("/foo")
            .contextAttr("foo", "bar", "baz")
            .run(router);

    assertTrue(doRun.ran());
  }

}
