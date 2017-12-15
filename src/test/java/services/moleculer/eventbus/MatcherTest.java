package services.moleculer.eventbus;

import org.junit.Test;

import junit.framework.TestCase;

public class MatcherTest extends TestCase {

	@Test
	public void testMatcher() throws Exception {

		assertMatch("1.2.3", "1.2.3");
		assertMatch("a.b.c.d", "a.b.c.d");
		assertMatch("aa.bb.cc", "aa.bb.cc");
		
		assertMatch("aa.bb.cc", "aa.bb.*");
		assertMatch("bb.cc", "bb.*");
		assertMatch("dd", "*");

		assertNotMatch("aa.bb.cc", "aa.*");
		assertNotMatch("bb.cc", "*");

		assertMatch("aa.bb.cc", "aa.**");
		assertMatch("bb.cc", "**");

	}

	public void assertMatch(String text, String pattern) {
		assertTrue(Matcher.matches(text, pattern));
	}

	public void assertNotMatch(String text, String pattern) {
		assertFalse(Matcher.matches(text, pattern));
	}

}
