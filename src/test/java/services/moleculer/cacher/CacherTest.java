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
package services.moleculer.cacher;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

public abstract class CacherTest extends TestCase {

	// --- PROPERTIES ---
	
	protected ServiceBroker br;
	protected Cacher cr;

	// --- CREATE CACHER ---
	
	protected abstract Cacher createCacher() throws Exception;

	// --- TEST METHOD ---
	
	@Test
	public void testCacher() throws Exception {
		Tree rsp, val = new Tree();
		val.put("rsp", "3");

		// 1.) simple set / get
		cr.set("a.b", val, 0);
		rsp = cr.get("a.b").waitFor();
		assertEquals(3, rsp.get("rsp", 0));
		
		// 2.) simple set / get
		val.put("rsp", 4);
		cr.set("a.c", val, 0);
		
		rsp = cr.get("a.b").waitFor();
		assertEquals(3, rsp.get("rsp", 0));
		
		rsp = cr.get("a.c").waitFor();
		assertEquals(4, rsp.get("rsp", 0));
	
		// 3.) simple set / get
		val.put("rsp", 5);
		cr.set("a.b", val, 0);
		
		rsp = cr.get("a.b").waitFor();
		assertEquals(5, rsp.get("rsp", 0));
		
		// 4.) Simple delete
		cr.del("a.b").waitFor();

		rsp = cr.get("a.b").waitFor();
		assertNull(rsp);
		
		rsp = cr.get("a.c").waitFor();
		assertEquals(4, rsp.get("rsp", 0));
		
		// 5.) Prefixed delete
		val.put("rsp", 6);
		cr.set("a.b", val, 0);

		val.put("rsp", 7);
		cr.set("b.d", val, 0);

		rsp = cr.get("a.b").waitFor();
		assertEquals(6, rsp.get("rsp", 0));

		rsp = cr.get("a.c").waitFor();
		assertEquals(4, rsp.get("rsp", 0));
		
		rsp = cr.get("b.d").waitFor();
		assertEquals(7, rsp.get("rsp", 0));

		cr.clean("a.*").waitFor();

		rsp = cr.get("a.b").waitFor();
		assertNull(rsp);//ERR

		rsp = cr.get("a.c").waitFor();
		assertNull(rsp);

		rsp = cr.get("b.d").waitFor();
		assertEquals(7, rsp.get("rsp", 0));

		cr.clean("**").waitFor();

		rsp = cr.get("b.d").waitFor();
		assertNull(rsp);

		// 6.) Multi-level + prefixed delete
		
		val.put("rsp", 1);
		cr.set("a.b", val, 0);

		val.put("rsp", 2);
		cr.set("a.b.c", val, 0);

		val.put("rsp", 3);
		cr.set("a.b.c.d", val, 0);

		rsp = cr.get("a.b").waitFor();
		assertEquals(1, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c").waitFor();
		assertEquals(2, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c.d").waitFor();
		assertEquals(3, rsp.get("rsp", 0));

		cr.clean("b.*").waitFor(); // --------

		rsp = cr.get("a.b").waitFor();
		assertEquals(1, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c").waitFor();
		assertEquals(2, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c.d").waitFor();
		assertEquals(3, rsp.get("rsp", 0));

		cr.clean("a.*").waitFor(); // --------

		rsp = cr.get("a.b").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c").waitFor();
		assertEquals(2, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c.d").waitFor();
		assertEquals(3, rsp.get("rsp", 0));

		cr.clean("a.b.*").waitFor(); // --------
		
		rsp = cr.get("a.b").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c.d").waitFor();
		assertEquals(3, rsp.get("rsp", 0));
		
		cr.clean("a.b.c.*").waitFor(); // --------
		
		rsp = cr.get("a.b").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c.d").waitFor();
		assertNull(rsp);
		
		// 7.) Multi-level + prefixed delete 2.
		
		val.put("rsp", 1);
		cr.set("a.b", val, 0);

		val.put("rsp", 2);
		cr.set("a.b.c", val, 10000);

		val.put("rsp", 3);
		cr.set("a.b.c.d", val, 0);
		
		cr.clean("a.b.**").waitFor(); // --------
		
		rsp = cr.get("a.b").waitFor();
		assertEquals(1, rsp.get("rsp", 0));

		rsp = cr.get("a.b.c").waitFor();
		assertNull(rsp);

		rsp = cr.get("a.b.c.d").waitFor();
		assertNull(rsp);
		
		// 8.) Large key get / set
		
		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < 2048; i++) {
			tmp.append(i % 9);
		}
		String key = "a." + tmp.toString();
		
		val.put("rsp", 4);
		cr.set(key, val, 0);

		rsp = cr.get(key).waitFor();
		assertEquals(4, rsp.get("rsp", 0));

		cr.del(key).waitFor();
		
		rsp = cr.get(key).waitFor();
		assertNull(rsp);

		// 9.) Large value get / set
		
		Tree large = new Tree();
		for (int i = 0; i < 100; i++) {
			Tree row = large.putMap("row" + i);
			for (int j = 0; j < 100; j++) {
				Tree cell = row.putMap("cell" + i + '_' + j);
				cell.put("value", i * j);
			}
		}
		
		cr.set("large.value", large, 0).waitFor();
		rsp = cr.get("large.value").waitFor();
		String s1 = large.toString(false).replaceAll(".0", "").replaceAll(" ", "");
		String s2 = rsp.toString(false).replaceAll(".0", "").replaceAll(" ", "");
		assertEquals(s1, s2);

		cr.del("large.value").waitFor();
		
		rsp = cr.get("large.value").waitFor();
		assertNull(rsp);
		
		// 10.) Remove entire partition	
		val.clear();
		val.put("e", true);
		cr.set("xxx.y1", val, 0);
		val.clear();
		val.put("r", false);
		cr.set("xxx.y2", val, 10000);
		
		assertTrue(cr.get("xxx.y1").waitFor().get("e", false));
		assertFalse(cr.get("xxx.y2").waitFor().get("r", true));
		
		cr.clean("y*");

		assertTrue(cr.get("xxx.y1").waitFor().get("e", false));
		assertFalse(cr.get("xxx.y2").waitFor().get("r", true));

		cr.clean("x*");
		
		assertNull(cr.get("xxx.y1").waitFor());
		assertNull(cr.get("xxx.y2").waitFor());
	}
	
	@Test
	public void testAnnotations() throws Exception {
		TestService testService = new TestService(); 
		br.createService(testService);
		
		Tree params = new Tree();
		params.put("a", 4);
		Tree rsp = br.call("test.test", params).waitFor();
		assertEquals(8, (int) rsp.asInteger());
		
		Tree rsp2 = cr.get("test.test:4").waitFor();
		assertEquals(8, (int) rsp2.asInteger());
		
		cr.clean("test.*").waitFor(); // --------

		rsp2 = cr.get("test.test:4").waitFor();
		assertNull(rsp2);
		
		params.put("b", "3");
		rsp = br.call("test.test2", params).waitFor();
		assertEquals(7, rsp.get("c", 0));
		
		rsp2 = cr.get("test.test2:4|3").waitFor();
		assertEquals(7, rsp2.get("c", 0));
	}
	
	@Name("test")
	public class TestService extends Service {
		
		@Cache(keys = {"a"})
		public Action test = ctx -> {
			return ctx.params.get("a", 0) * 2;
		};

		@Cache(keys = {"a", "b"})
		public Action test2 = ctx -> {
			Tree rsp = new Tree();
			rsp.put("c", ctx.params.get("a", 0) + ctx.params.get("b", 0));
			return rsp;
		};

	}
	
	// --- START BROKER ---
	
	@Override
	protected void setUp() throws Exception {
		cr = createCacher();
		br = ServiceBroker.builder().cacher(cr).build();
		br.start();
		cr.clean("**").waitFor();
	}

	// --- STOP BROKER ---
	
	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
			br = null;
		}
	}

}
