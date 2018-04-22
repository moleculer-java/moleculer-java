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
		promise = new Promise().then(in -> {
			return "x";
		}).catchError(err -> {
			return "y";
		});
		promise.complete(new Exception());
		out = promise.waitFor();
		assertEquals("y", out.asString());
		
		// Change value (completed)
		promise = new Promise("y").then(in -> {
			return in.asString() + "x";
		});
		out = promise.waitFor();
		assertEquals("yx", out.asString());

		// Change value (uncompleted)
		promise = new Promise().then(in -> {
			return in.asString() + "x";
		});
		promise.complete("z");
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

	@Test
	public void testWaterfall() throws Exception {
		Promise promise = new Promise().then(in -> {
			
			// Scalar processing - return int
			return in.asInteger() + 1;
			
		}).then(in -> {
			
			// Scalar processing - return String
			return in.asString() + "X";
			
		}).then(in -> {
			
			// Generating the first error
			if (in.asString().equals("6X")) {
				throw new IllegalArgumentException("Sample error");
			}
			return "OK";
			
		}).catchError(err -> {
			
			// Handle error
			if (err.toString().contains("Sample error")) {
				return 7;
			}
			return -1;
			
		}).then(in -> {
			
			// Processing error handler's result
			return in.asInteger() * 2;
			
		}).then(in -> {
			
			// Second error
			if (in.asInteger() == 14) {
				throw new IllegalStateException("Another error");
			}
			return -1;
			
		}).catchError(err -> {
			
			// Second error handler
			if (err.toString().contains("Another error")) {
				return 5;
			}
			return 0;

		}).then(in -> {
			
			// Generating JSON structure
			Tree out = new Tree();
			out.put("num", in.asInteger());
			out.put("str", "abc");
			out.put("bool", "true");
			return out;
			
		}).then(in -> {
			
			// Modify and forward structure
			in.put("str", "xyz");
			return in;
			
		}).then(in -> {
			
			// Do nothing, just check the input
			assertEquals("xyz", in.get("str", ""));
			
		});
		
		// Start waterfall
		promise.complete(5);
		
		// Wait for result (in blocking style)
		Tree result = promise.waitFor();

		// Check result
		assertEquals(5, result.get("num", -1));
		assertTrue(result.get("bool", false));
	}
	
}