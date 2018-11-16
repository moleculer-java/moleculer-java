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
package services.moleculer.util;

import org.junit.Test;

import junit.framework.TestCase;

public class FastBuildTreeTest extends TestCase {

	@Test
	public void testCreate() throws Exception {

		FastBuildTree t = new FastBuildTree();
		assertJsonEquals("{}", t.toString(false));

		t.putUnsafe("a", "b");
		assertJsonEquals("{\"a\":\"b\"}", t.toString(false));

		t.putUnsafe("c", true);
		assertJsonEquals("{\"a\":\"b\",\"c\":true}", t.toString(false));

		t.putUnsafe("d", 3);
		assertJsonEquals("{\"a\":\"b\",\"c\":true,\"d\":3}", t.toString(false));

		System.out.println(t.toString());
	}

	// --- TEST SIMILAR / SAME NODE ---

	private static final void assertJsonEquals(String s1, String s2) {
		if (s1 != null) {
			s1 = removeFormatting(s1);
		}
		if (s2 != null) {
			s2 = removeFormatting(s2);
		}
		assertEquals(s1, s2);
	}

	private static final String removeFormatting(String txt) {
		return txt.replace("\t", " ").replace("\r", " ").replace("\n", " ").replace(" ", "").replace(".0", "");
	}

}
