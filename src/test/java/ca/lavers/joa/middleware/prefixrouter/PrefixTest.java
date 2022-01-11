package ca.lavers.joa.middleware.prefixrouter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PrefixTest {

  @Test
  void testMatches() {
    Prefix p = new Prefix("/foo", null);

    assertFalse(p.matches("/"));

    assertTrue(p.matches("/foo"));
    assertTrue(p.matches("/foo/"));
    assertTrue(p.matches("/foo/bar"));
    assertTrue(p.matches("/foo/bar/baz"));

    assertFalse(p.matches("/bar"));
    assertFalse(p.matches("/bar/foo"));
    assertFalse(p.matches("/foobar"));
  }

  @Test
  void testSubpath() {
    Prefix p = new Prefix("/foo", null);

    assertEquals("/bar", p.subPath("/foo/bar"));
    assertEquals("/bar/baz", p.subPath("/foo/bar/baz"));

  }

  @Test
  void testRoot() {
    Prefix p = new Prefix("/", null);
    assertTrue(p.matches("/"));
    assertTrue(p.matches("/foo"));
    assertTrue(p.matches("/foo/bar"));
  }

  @Test
  void testTrailingSlash() {
    Prefix p = new Prefix("/foo/", null);

    assertTrue(p.matches("/foo"));
    assertEquals("/", p.subPath("/foo"));

    assertTrue(p.matches("/foo/"));
    assertEquals("/", p.subPath("/foo/"));

    assertTrue(p.matches("/foo/bar"));
    assertEquals("/bar", p.subPath("/foo/bar"));
  }

  @Test
  void testLongPrefix() {
    Prefix p = new Prefix("/foo/bar", null);

    assertTrue(p.matches("/foo/bar"));
    assertTrue(p.matches("/foo/bar/"));
    assertTrue(p.matches("/foo/bar/baz"));
    assertTrue(p.matches("/foo/bar/baz/buz"));

    assertEquals("/baz", p.subPath("/foo/bar/baz"));
    assertEquals("/baz/buz", p.subPath("/foo/bar/baz/buz"));
  }

}
