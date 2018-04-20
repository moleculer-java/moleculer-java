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
package services.moleculer;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;

public class PromiseTest extends TestCase {

	@Test
	public void testPromise() throws Exception {

		// Complete with null
		Tree out = Promise.resolve().waitFor();
		assertNull(out);

		// Complete with an empty structure
		out = new Promise(new Tree()).waitFor();
		assertNotNull(out);
		assertTrue(out.isEmpty());
		
		// Complete with a structure
		Promise promise = new Promise();
		promise.complete(new Tree().put("a", 3));
		out = promise.waitFor();
		assertEquals(3, out.get("a", 1));
		
		// Handle exception (completed)
		promise = Promise.reject().then(in -> {
			return null;
		}).catchError(err -> {
			return "xyz";
		}).then(in -> {
			return in.asString() + "1";
		});
		out = promise.waitFor();
		assertEquals("xyz1", out.asString());

		// Handle exception (uncompleted)
		Promise root = new Promise();
		promise = root.then(in -> {
			return "x";
		}).catchError(err -> {
			return "y";
		});
		root.complete(new Exception());
		out = promise.waitFor();
		assertEquals("y", out.asString());
		
		// Change value (completed)
		promise = new Promise("y").then(in -> {
			return in.asString() + "x";
		});
		out = promise.waitFor();
		assertEquals("yx", out.asString());

		// Change value (uncompleted)
		root = new Promise();
		promise = root.then(in -> {
			return in.asString() + "x";
		});
		root.complete("z");
		out = promise.waitFor();
		assertEquals("zx", out.asString());

		// Change value (in constructor)
		promise = new Promise(rsp -> {
			rsp.resolve("y");
		}).then(in -> {
			return in.asString() + "x";
		});
		out = promise.waitFor();
		assertEquals("yx", out.asString());
		
	}

}