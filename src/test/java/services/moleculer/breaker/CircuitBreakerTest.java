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
package services.moleculer.breaker;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.context.CallOptions;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

public class CircuitBreakerTest extends TestCase {

	// --- VARIABLES ---

	protected TestTransporter tr;
	protected DefaultServiceRegistry sr;
	protected ServiceBroker br;
	protected DefaultCircuitBreaker cb;

	// --- TEST METHODS ---

	protected int currentID = 1;

	public String getCurrentID() {
		String nodeID = "node" + currentID;
		currentID++;
		if (currentID > 9) {
			currentID = 0;
		}
		return nodeID;
	}

	@Test
	public void testRoundRobin() throws Exception {

		// Simple round-robin test
		currentID = 1;
		for (int i = 0; i < 20; i++) {
			Promise p = br.call("test.test", (Tree) null);
			assertTrue(tr.hasMessage(getCurrentID()));
			assertEquals(1, tr.getMessageCount());
			createResponse(true);
			boolean ok = false;
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
			}
			assertTrue(ok);
		}

		// Create fault
		Promise p = br.call("test.test", (Tree) null);
		String nodeID = createResponse(false);
		boolean ok = true;
		try {
			p.waitFor();
		} catch (Exception e) {
			ok = e.toString().contains("unknown error");
		}
		assertFalse(ok);
		ErrorCounter ec = cb.errorCounters.get(new EndpointKey(nodeID, "test.test"));
		assertNotNull(ec);
		assertEquals(1, ec.pointer);
		long now = ec.timestamps[0];
		assertTrue(ec.isAvailable(now));

		// Create faults2
		p = br.call("test.test", (Tree) null);
		nodeID = createResponse(false);
		try {
			p.waitFor();
			ok = true;
		} catch (Exception e) {
			ok = e.toString().contains("unknown error");
		}
		assertFalse(ok);
		ec = cb.errorCounters.get(new EndpointKey(nodeID, "test.test"));
		assertNotNull(ec);
		assertEquals(1, ec.pointer);

		// Create fault
		int node1Count = 0;
		for (int i = 0; i < 30; i++) {
			p = br.call("test.test", (Tree) null);
			nodeID = createResponse(false);
			now = System.currentTimeMillis();
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
				ok = e.toString().contains("unknown error");
			}
			assertFalse(ok);
			if (nodeID.equals("node0")) {
				node1Count++;
				ec = cb.errorCounters.get(new EndpointKey(nodeID, "test.test"));
				assertNotNull(ec);
				if (node1Count < 3) {
					if (node1Count == 1) {
						assertTrue(ec.timestamps[0] == 0);
						assertTrue(ec.timestamps[1] > 0);
						assertTrue(ec.timestamps[2] == 0);
					} else if (node1Count == 2) {
						assertTrue(ec.timestamps[0] == 0);
						assertTrue(ec.timestamps[1] > 0);
						assertTrue(ec.timestamps[2] > 0);
					}
					assertTrue(ec.isAvailable(now));
				} else {
					assertTrue(ec.timestamps[0] > 0);
					assertTrue(ec.timestamps[1] > 0);
					assertTrue(ec.timestamps[2] > 0);
					assertFalse(ec.isAvailable(now));
				}
			}
		}

		// All endpoint is locked
		for (EndpointKey key : cb.errorCounters.keySet()) {
			ec = cb.errorCounters.get(key);
			boolean avail = ec.isAvailable(now);
			assertFalse(avail);
		}

		// Retrying once
		now += 10001;
		assertTrue(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));

		// + 5 sec
		now += 5000;
		assertFalse(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));

		// + 10 sec (5000 + 5001)
		now += 5001;
		assertTrue(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));
		assertFalse(ec.isAvailable(now));
	}

	public String createResponse(boolean success) throws Exception {
		Tree msg = tr.getMessages().get(0);
		String id = msg.get("id", "");
		String channel = msg.get("channel", "");
		int i = channel.lastIndexOf('.');
		String nodeID = channel.substring(i + 1);
		Tree rsp = new Tree();
		rsp.put("ver", "3");
		rsp.put("sender", nodeID);
		rsp.put("id", id);
		rsp.put("success", success);
		rsp.put("data", (String) null);
		tr.clearMessages();
		tr.received("MOL.RES.local", rsp);
		return nodeID;
	}

	@Test
	public void testRoundRobin2() throws Exception {

		// Node0 -> fail
		currentID = 1;
		for (int i = 0; i < 30; i++) {
			Promise p = br.call("test.test", (Tree) null);
			String nodeID = getCurrentID();
			boolean success = !"node0".equals(nodeID);
			createResponse(success);
			boolean ok = false;
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
				ok = false;
			}
			assertEquals(success, ok);
		}

		// Check ErrorCounter
		long now = System.currentTimeMillis();
		ErrorCounter ec = cb.errorCounters.get(new EndpointKey("node0", "test.test"));
		assertFalse(ec.isAvailable(now));

		// Do not invoke node0
		for (int i = 0; i < 20; i++) {
			Promise p = br.call("test.test", (Tree) null);
			String nodeID = createResponse(true);
			assertFalse("node0".equals(nodeID));
			boolean ok = false;
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
				ok = false;
			}
			assertTrue(ok);
		}

		// Invoke node1 directly
		Promise p = br.call("test.test", (Tree) null, CallOptions.nodeID("node1"));
		createResponse(true);
		boolean ok = false;
		try {
			p.waitFor();
			ok = true;
		} catch (Exception e) {
			ok = false;
		}
		assertTrue(ok);
		assertFalse(ec.isAvailable(now));

		// Invoke node0 directly
		p = br.call("test.test", (Tree) null, CallOptions.nodeID("node0"));
		createResponse(true);
		try {
			p.waitFor();
			ok = true;
		} catch (Exception e) {
			ok = false;
		}
		assertTrue(ok);
		assertTrue(ec.isAvailable(now));
	}

	@Test
	public void testRetry() throws Exception {
		for (int i = 0; i < 10; i++) {
			Promise p = br.call("test.test", (Tree) null, CallOptions.retryCount(1));
			String n1 = createResponse(false);
			String n2 = createResponse(true);
			assertFalse(n1.equals(n2));
			boolean ok = false;
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
				ok = false;
			}
			assertTrue(ok);
		}
	}

	@Test
	public void testRetryWithError() throws Exception {
		for (int i = 0; i < 10; i++) {
			Promise p = br.call("test.test", (Tree) null, CallOptions.retryCount(1));
			String n1 = createResponse(false);
			String n2 = createResponse(false);
			assertFalse(n1.equals(n2));
			boolean ok = false;
			try {
				p.waitFor();
				ok = true;
			} catch (Exception e) {
				ok = false;
			}
			assertFalse(ok);
		}
	}

	@Test
	public void testSimpleCall() throws Exception {
		br.createService(new Service("math") {

			@Name("add")
			public Action add = ctx -> {

				return ctx.params.get("a", 0) + ctx.params.get("b", 0);

			};

		});
		// cb.setEnabled(true);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 50; i++) {
			int rsp = br.call("math.add", "a", i, "b", 1).waitFor().asInteger();
			assertEquals(i + 1, rsp);
		}
		assertTrue(System.currentTimeMillis() - start < 100);
	}

	// --- SET UP ---

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void setUp() throws Exception {
		sr = new DefaultServiceRegistry();
		tr = new TestTransporter();
		cb = new DefaultCircuitBreaker();
		cb.setMaxErrors(3);
		cb.setEnabled(true);
		ExecutorService ex = new ExecutorService() {

			@Override
			public void execute(Runnable command) {
				command.run();
			}

			@Override
			public <T> Future<T> submit(Runnable task, T result) {
				task.run();
				return CompletableFuture.completedFuture(result);
			}

			@Override
			public Future<?> submit(Runnable task) {
				task.run();
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public <T> Future<T> submit(Callable<T> task) {
				try {
					return CompletableFuture.completedFuture(task.call());
				} catch (Exception e) {
					CompletableFuture future = CompletableFuture.completedFuture(null);
					future.completeExceptionally(e);
					return future;
				}
			}

			@Override
			public List<Runnable> shutdownNow() {
				return Collections.emptyList();
			}

			@Override
			public void shutdown() {
			}

			@Override
			public boolean isTerminated() {
				return false;
			}

			@Override
			public boolean isShutdown() {
				return false;
			}

			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return null;
			}

			@Override
			public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
					throws InterruptedException, ExecutionException {
				return null;
			}

			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
					throws InterruptedException {
				return null;
			}

			@Override
			public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
				return null;
			}

			@Override
			public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
				return false;
			}

		};
		br = ServiceBroker.builder().monitor(new ConstantMonitor()).registry(sr).transporter(tr).nodeID("local")
				.breaker(cb).executor(ex).build();
		br.start();
		for (int i = 0; i < 10; i++) {
			Tree root = new Tree();			
			Tree config = root.putMap("node" + i);
			Tree actions = config.putMap("actions");
			LinkedHashMap<String, Object> action = new LinkedHashMap<>();
			action.put("name", "test.test");
			((Map) actions.asObject()).put("test.test", action);
			sr.addActions(config);
		}
	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

}