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
