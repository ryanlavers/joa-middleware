package ca.lavers.joa.middleware.router;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RouteTest {

  @Test
  public void testRoot() {
    Route route = new Route("/", null);
    Map<String, String> result = route.match("/");
    assertNotNull(result);
    assertEquals(0, result.size(), "Incorrect number of path parameter matches");
  }

  @Test
  public void testLiteralPath() {
    Route route = new Route("/foo/bar/baz", null);
    Map<String, String> result = route.match("/foo/bar/baz");
    assertNotNull(result);
    assertEquals(0, result.size(), "Incorrect number of path parameter matches");
  }

  @Test
  public void testParams() {
    Route route = new Route("/foos/:fooId/bars/:barId", null);
    Map<String, String> result = route.match("/foos/foo1/bars/bar37");
    assertNotNull(result);
    assertEquals(2, result.size(), "Incorrect number of path parameter matches");
    assertEquals("foo1", result.get("fooId"));
    assertEquals("bar37", result.get("barId"));
  }

  @Test
  public void testNoMatch() {
    Route route = new Route("/foo", null);
    Map<String, String> result = route.match("/foo/bar/baz");
    assertNull(result);
  }


}
