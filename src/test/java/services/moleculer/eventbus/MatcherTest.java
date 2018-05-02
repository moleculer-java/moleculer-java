/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
		assertMatch("$aa.bb.cc", "$aa.*.cc");

		assertMatch("$aa.bb.cc", "$aa.*.cc");
		assertMatch("$aa.bb.cc", "$aa.**");
		assertMatch("$aa.bb.cc", "$aa.**.cc");
		assertMatch("$aa.bb.cc", "$aa.??.cc");
		assertMatch("$aa.bb.cc", "?aa.bb.cc");
		assertNotMatch("$aa.bb.cc", "aa.bb.cc");
		assertMatch("$aa.bb.cc", "**.bb.cc");
		assertMatch("$aa.bb.cc", "**.cc");
		assertMatch("$aa.bb.cc", "**");
		assertNotMatch("$aa.bb.cc", "*");
	}

	public void assertMatch(String text, String pattern) {
		assertTrue(Matcher.matches(text, pattern));
	}

	public void assertNotMatch(String text, String pattern) {
		assertFalse(Matcher.matches(text, pattern));
	}

}