package services.moleculer.eventbus;

import org.junit.Test;

import junit.framework.TestCase;

public class MatcherTest extends TestCase {

	@Test
	public void testMatcher() throws Exception {

		// --- SIMPLE CASES ---
		
		assertMatch("1.2.3", "1.2.3");
		assertMatch("a.b.c.d", "a.b.c.d");
		assertMatch("aa.bb.cc", "aa.bb.cc");
		
		assertMatch("a1c", "a?c");
		assertMatch("a2c", "a?c");
		assertMatch("a3c", "a?c");

		assertMatch("aa.1b.c", "aa.?b.*");
		assertMatch("aa.2b.cc", "aa.?b.*");
		assertMatch("aa.3b.ccc", "aa.?b.*");
		assertMatch("aa.4b.cccc", "aa.?b.*");
		assertMatch("aa.5b.ccccc", "aa.?b.*");

		assertMatch("aa.bb.cc", "aa.bb.*");
		assertMatch("aa.bb.cc", "*.bb.*");
		assertMatch("bb.cc", "bb.*");
		assertMatch("dd", "*");

		// --- DOUBLE STARS CASES ---
		
		assertNotMatch("aa.bb.cc", "aa.*");
		assertNotMatch("aa.bb.cc", "a*");
		assertNotMatch("bb.cc", "*");

		assertNotMatch("aa.bb.cc.dd", "*.bb.*");
		assertNotMatch("aa.bb.cc.dd", "*.cc.*");

		assertNotMatch("aa.bb.cc.dd", "*bb*");
		assertNotMatch("aa.bb.cc.dd", "*cc*");

		assertNotMatch("aa.bb.cc.dd", "*b*");
		assertNotMatch("aa.bb.cc.dd", "*c*");

		assertMatch("aa.bb.cc.dd", "**.bb.**");
		assertMatch("aa.bb.cc.dd", "**.cc.**");

		assertMatch("aa.bb.cc.dd", "**bb**");
		assertMatch("aa.bb.cc.dd", "**cc**");

		assertMatch("aa.bb.cc.dd", "**b**");
		assertMatch("aa.bb.cc.dd", "**c**");

		assertMatch("aa.bb.cc", "aa.**");
		assertMatch("aa.bb.cc", "**.cc");
		
		assertMatch("bb.cc", "**");
		assertMatch("b", "**");

		assertMatch("$node.connected", "$node.**");
	}

	public void assertMatch(String text, String pattern) {
		assertTrue(Matcher.matches(text, pattern));
	}

	public void assertNotMatch(String text, String pattern) {
		assertFalse(Matcher.matches(text, pattern));
	}

}
