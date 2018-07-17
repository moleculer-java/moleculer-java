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
package services.moleculer.service;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.error.RequestRejectedError;
import services.moleculer.monitor.ConstantMonitor;

public class TimeoutTest extends TestCase {

	// --- VARIABLES ---

	protected DefaultServiceRegistry sr;
	protected ServiceBroker br;

	// --- TEST METHODS ---

	@Test
	public void testTimeout() throws Exception {

		// Create slow service
		br.createService(new TimeoutService());

		// Invoke slow service
		assertTrue(invokeSlowService(0));

		assertFalse(invokeSlowService(100));
		assertFalse(invokeSlowService(500));

		assertTrue(invokeSlowService(2000));
		assertTrue(invokeSlowService(5000));
		assertTrue(invokeSlowService(10000));

		for (int i = 0; i < 10; i++) {
			assertFalse(invokeSlowService(100));
		}
		
		// --- DISTRIBUTED TIMEOUT ---
		
		// Create services
		Level1Service level1Service = new Level1Service();
		br.createService(level1Service);
		br.createService(new Level2Service());
		
		assertTrue(invokeServices(1500, 500));
		assertFalse(level1Service.timeouted);
	
		assertFalse(invokeServices(500, 1500));		
		assertFalse(level1Service.timeouted);
		
		Thread.sleep(1200);
		assertTrue(level1Service.timeouted);
	}

	protected boolean invokeSlowService(long timeout) {
		try {
			br.call("timeout.slow", CallOptions.timeout(timeout)).waitFor();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Name("timeout")
	protected static final class TimeoutService extends Service {

		@Name("slow")
		public Action action = ctx -> {
			Thread.sleep(1000);
			return 0;
		};

	}
	
	// --- DISTRIBUTED TIMEOUT ---
	
	protected boolean invokeServices(long timeout, long sleep) {
		try {
			Tree req = new Tree();
			long a = Math.abs(sleep * System.nanoTime() % 10);
			req.put("a", a);
			long b = Math.abs(timeout * System.currentTimeMillis() % 10);
			req.put("b", b);
			req.put("sleep", sleep);
			Tree rsp = br.call("level1Service.action", req, CallOptions.timeout(timeout)).waitFor();			

			return rsp.asLong() == a * b;
			
		} catch (Exception e) {
			return false;
		}
	}

	protected static final class Level1Service extends Service {

		public boolean timeouted;
		
		public Action action = ctx -> {
			timeouted = false;
			long sleep = ctx.params.get("sleep", 0L);		
			Thread.sleep(sleep);
			return ctx.call("level2Service.action", ctx.params).catchError(err -> {
				if (err instanceof RequestRejectedError) {
					
					// Rejected
					timeouted = true;
				}
			});				
		};

	}

	protected static final class Level2Service extends Service {

		public Action action = ctx -> {
			long a = ctx.params.get("a", 0L);
			long b = ctx.params.get("b", 0L);
			return a * b;
		};

	}

	// --- SET UP ---

	@Override
	protected void setUp() throws Exception {
		sr = new DefaultServiceRegistry();
		br = ServiceBroker.builder().monitor(new ConstantMonitor()).registry(sr).nodeID("local").build();
		br.start();
	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

}
