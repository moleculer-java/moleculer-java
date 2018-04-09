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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import services.moleculer.ServiceBroker;
import services.moleculer.breaker.TestTransporter;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.Service;
import services.moleculer.util.FastBuildTree;

public class EventbusTest extends TestCase {

	// --- VARIABLES ---

	protected TestTransporter tr;
	protected DefaultServiceRegistry sr;
	protected ServiceBroker br;

	// --- TEST METHODS ---

	@Test
	public void testSimpleFuctions() throws Exception {

		br.createService("test", new TestListener());
		TestListener s = (TestListener) br.getLocalService("test");

		br.broadcast("test.a1", new Tree().put("a", 15));
		assertEquals(1, s.payloads.size());
		Tree t = s.payloads.removeFirst();
		assertEquals(15, t.get("a", -1));

		br.emit("test.b", new Tree().put("b", "abc").put("c", true));
		assertEquals(1, s.payloads.size());
		t = s.payloads.removeFirst();
		assertEquals("abc", t.get("b", ""));
		assertTrue(t.get("c", false));

		br.broadcastLocal("test.c", new Tree().put("d", "x"));
		assertEquals(1, s.payloads.size());
		t = s.payloads.removeFirst();
		assertEquals("x", t.get("d", ""));

		br.broadcast("test.a.b", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());

		br.broadcast("foo.a1", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());

		br.emit("test.b.xy", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());

		br.emit("xyz.b", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());

		br.broadcastLocal("test.fg.hg", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());

		br.broadcastLocal("d", new Tree().put("d", "x"));
		assertEquals(0, s.payloads.size());
	}

	protected static final class TestListener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Subscribe("test.*")
		public Listener evt = payload -> {
			payloads.addLast(payload);
		};

	}

	@Test
	public void testBroadcast() throws Exception {

		// Create two listeners
		br.createService("test1", new TestListener());
		TestListener s1 = (TestListener) br.getLocalService("test1");
		TestListener s2 = new TestListener();
		br.createService("test2", s2);

		br.broadcast("test.a", new Tree());
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		br.broadcastLocal("test.a", new Tree());
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		br.broadcast("test.a.x", new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(0, s2.payloads.size());

		br.broadcastLocal("test.a.x", new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(0, s2.payloads.size());

		br.broadcast("foo.a", new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(0, s2.payloads.size());

		br.broadcastLocal("foo.a", new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(0, s2.payloads.size());

		br.broadcast("test.a", new Tree(), Groups.of("test1"));
		assertEquals(1, s1.payloads.size());
		assertEquals(0, s2.payloads.size());
		s1.payloads.clear();

		br.broadcast("test.a", new Tree(), Groups.of("test2"));
		assertEquals(0, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s2.payloads.clear();

		br.broadcast("test.a", new Tree(), Groups.of("test1", "test2"));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		br.broadcastLocal("test.a", new Tree(), Groups.of("test1"));
		assertEquals(1, s1.payloads.size());
		assertEquals(0, s2.payloads.size());
		s1.payloads.clear();

		br.broadcastLocal("test.a", new Tree(), Groups.of("test2"));
		assertEquals(0, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s2.payloads.clear();

		br.broadcastLocal("test.a", new Tree(), Groups.of("test1", "test2"));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
	}

	@Test
	public void testEmit() throws Exception {

		// Create two listeners
		br.createService("test1", new TestListener());
		TestListener s1 = (TestListener) br.getLocalService("test1");
		TestListener s2 = new TestListener();
		br.createService("test2", s2);

		br.emit("test.a", new Tree());
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		br.emit("test.a", new Tree(), Groups.of("test1"));
		assertEquals(1, s1.payloads.size());
		assertEquals(0, s2.payloads.size());
		s1.payloads.clear();

		br.emit("test.a", new Tree(), Groups.of("test2"));
		assertEquals(0, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s2.payloads.clear();

		br.emit("test.a.x", new Tree(), Groups.of("test1"));
		assertEquals(0, s1.payloads.size());
		assertEquals(0, s2.payloads.size());

		br.emit("test.a", new Tree(), Groups.of("test1", "test2"));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
	}

	@Test
	public void testRemoteBroadcast() throws Exception {

		// Create two listeners
		br.createService("test1", new TestListener());
		TestListener s1 = (TestListener) br.getLocalService("test1");
		br.createService("test2", new TestListener());
		TestListener s2 = (TestListener) br.getLocalService("test2");

		putIncomingMessage("test.test", true, null, new Tree().put("y", 123));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		putIncomingMessage("test.a", true, null, new Tree().put("y", 123));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		assertEquals(123, s1.payloads.remove().get("y", 0));
		assertEquals(123, s2.payloads.remove().get("y", 0));

		putIncomingMessage("test.a", true, Groups.of("test1"), new Tree());
		assertEquals(1, s1.payloads.size());
		assertEquals(0, s2.payloads.size());
		s1.payloads.clear();

		putIncomingMessage("test.a", true, Groups.of("test2"), new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s2.payloads.clear();
		
		putIncomingMessage("test.a", true, Groups.of("test1", "test2"), new Tree().put("y", 321));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		assertEquals(321, s1.payloads.remove().get("y", 0));
		assertEquals(321, s2.payloads.remove().get("y", 0));
	}

	@Test
	public void testRemoteEmit() throws Exception {

		// Create two listeners
		br.createService("test1", new TestListener());
		TestListener s1 = (TestListener) br.getLocalService("test1");
		br.createService("test2", new TestListener());
		TestListener s2 = (TestListener) br.getLocalService("test2");

		putIncomingMessage("test.test", false, null, new Tree().put("y", 123));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s1.payloads.clear();
		s2.payloads.clear();

		putIncomingMessage("test.a", false, null, new Tree().put("y", 123));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		assertEquals(123, s1.payloads.remove().get("y", 0));
		assertEquals(123, s2.payloads.remove().get("y", 0));

		putIncomingMessage("test.a", false, Groups.of("test1"), new Tree());
		assertEquals(1, s1.payloads.size());
		assertEquals(0, s2.payloads.size());
		s1.payloads.clear();

		putIncomingMessage("test.a", false, Groups.of("test2"), new Tree());
		assertEquals(0, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		s2.payloads.clear();
		
		putIncomingMessage("test.a", false, Groups.of("test1", "test2"), new Tree().put("y", 321));
		assertEquals(1, s1.payloads.size());
		assertEquals(1, s2.payloads.size());
		assertEquals(321, s1.payloads.remove().get("y", 0));
		assertEquals(321, s2.payloads.remove().get("y", 0));
	}
	
	protected void putIncomingMessage(String name, boolean broadcast, Groups groups, Tree payload) throws Exception {
		FastBuildTree msg = new FastBuildTree(6);
		msg.putUnsafe("ver", ServiceBroker.PROTOCOL_VERSION);
		msg.putUnsafe("sender", "node5");
		msg.putUnsafe("event", name);
		msg.putUnsafe("broadcast", broadcast);
		if (groups != null) {
			String[] array = groups.groups();
			if (array != null && array.length > 0) {
				msg.putUnsafe("groups", array);
			}
		}
		if (payload != null) {
			msg.putUnsafe("data", payload);
		}
		tr.received(tr.eventChannel, msg);
	}

	// --- GROUPED LISTENERS ---

	@Test
	public void testGroups() throws Exception {
		
		// Group1
		br.createService("g1_a", new Group1Listener());
		Group1Listener g1_a = (Group1Listener) br.getLocalService("g1_a");
		br.createService("g1_b", new Group1Listener());
		Group1Listener g1_b = (Group1Listener) br.getLocalService("g1_b");
		
		// Group2
		br.createService("g2_a", new Group2Listener());		
		Group2Listener g2_a = (Group2Listener) br.getLocalService("g2_a");
		br.createService("g2_b", new Group2Listener());
		Group2Listener g2_b = (Group2Listener) br.getLocalService("g2_b");
		
		// Broadcast
		br.broadcast("test.a", new Tree());
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Broadcast to group1
		br.broadcast("test.a", new Tree(), Groups.of("group1"));
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size());
		assertEquals(0, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Broadcast to group2
		br.broadcast("test.a", new Tree(), Groups.of("group2"));
		assertEquals(0, g1_a.payloads.size());
		assertEquals(0, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Broadcast to group1 and group2
		br.broadcast("test.a", new Tree(), Groups.of("group1", "group2"));
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Local broadcast
		br.broadcastLocal("test.a", new Tree());
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Local broadcast to group1
		br.broadcastLocal("test.a", new Tree(), Groups.of("group1"));
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size());
		assertEquals(0, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Local broadcast to group2
		br.broadcastLocal("test.a", new Tree(), Groups.of("group2"));
		assertEquals(0, g1_a.payloads.size());
		assertEquals(0, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Local broadcast to group1 and group2
		br.broadcastLocal("test.a", new Tree(), Groups.of("group1", "group2"));
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Emit
		br.emit("test.a", new Tree());
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Emit to group1
		br.emit("test.a", new Tree(), Groups.of("group1"));
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Emit to group2
		br.emit("test.a", new Tree(), Groups.of("group2"));
		assertEquals(0, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Emit to group1 and group2
		br.emit("test.a", new Tree(), Groups.of("group1", "group2"));
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Incoming remote broadcast to group1
		putIncomingMessage("test.a", true, Groups.of("group1"), new Tree());
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size());
		assertEquals(0, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		
		// Incoming remote emit to group1
		putIncomingMessage("test.a", false, Groups.of("group1"), new Tree());
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Incoming remote emit to group1 and group2
		putIncomingMessage("test.a", false, Groups.of("group1", "group2"), new Tree());
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
	}
	
	protected static final class Group1Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group1")
		@Subscribe("test.*")
		public Listener evt = payload -> {
			payloads.addLast(payload);
		};

	}
	
	protected static final class Group2Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group2")
		@Subscribe("test.*")
		public Listener evt = payload -> {
			payloads.addLast(payload);
		};

	}
	
	// --- SET UP ---

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void setUp() throws Exception {
		sr = new DefaultServiceRegistry();
		tr = new TestTransporter();
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
				.executor(ex).build();
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
