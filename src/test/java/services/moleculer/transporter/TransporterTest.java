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
package services.moleculer.transporter;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.datatree.Promise;
import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Groups;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.LocalActionEndpoint;
import services.moleculer.service.RemoteActionEndpoint;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.CheckedTree;

public abstract class TransporterTest extends TestCase {

	// --- VARIABLES ---

	protected long min = 1000;
	protected long max = 10000;
	protected long timeout = 800;
	
	protected Transporter tr1;
	protected ServiceBroker br1;

	protected Transporter tr2;
	protected ServiceBroker br2;

	// --- ABSTRACT METHODS ---

	public abstract Transporter createTransporter();

	// --- COMMON TESTS ---

	@Test
	@SuppressWarnings("unused")
	public void testTransporters() throws Exception {

		// NodeIDs
		assertEquals("node1", br1.getNodeID());
		assertEquals("node2", br2.getNodeID());

		// Ping
		checkPing(br1, "node2");
		checkPing(br1, "node1");
		checkPing(br2, "node2");
		checkPing(br2, "node1");

		// Install "math" service to node1
		br1.createService("math", new TestService());

		// Wait for "math" service on node2
		br2.waitForServices(10000, "math").waitFor(20000);

		// Wait for "math" service on node2 (again)
		br2.waitForServices(10000, "math").waitFor(20000);

		// Wait for "math" service on node1
		br1.waitForServices(10000, "math").waitFor(20000);

		// Get local action
		Action action = br1.getAction("math.add");
		assertNotNull(action);
		assertTrue(action instanceof LocalActionEndpoint);

		// Get remote action
		action = br2.getAction("math.add");
		assertNotNull(action);
		assertTrue(action instanceof RemoteActionEndpoint);

		// Invoke "math" service from node2
		for (int i = 0; i < 10; i++) {
			Tree rsp = br2.call("math.add", "a", i, "b", 1).waitFor(20000);
			assertEquals(i + 1, (int) rsp.asInteger());
		}

		// Group1 on node2
		br2.createService("g1_a", new Group1Listener());
		Group1Listener g1_a = (Group1Listener) br2.getLocalService("g1_a");
		br2.createService("g1_b", new Group1Listener());
		Group1Listener g1_b = (Group1Listener) br2.getLocalService("g1_b");

		// Group2 on node2
		br2.createService("g2_a", new Group2Listener());
		Group2Listener g2_a = (Group2Listener) br2.getLocalService("g2_a");
		br2.createService("g2_b", new Group2Listener());
		Group2Listener g2_b = (Group2Listener) br2.getLocalService("g2_b");

		// Wait for listener services on node1
		br1.waitForServices(10000, "g1_a", "g1_b", "g2_a", "g2_b").waitFor(20000);

		// Broadcast
		br1.broadcast("test.a", new Tree());
		waitForMessages();
		g1_a.waitFor(20000);
		g1_b.waitFor(20000);
		g2_a.waitFor(20000);
		g2_b.waitFor(20000);
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Broadcast to group1
		Tree t = new Tree();
		t.getMeta().put("val", 34567);
		br1.broadcast("test.a", t, Groups.of("group1"));
		g1_a.waitFor(20000);
		g1_b.waitFor(20000);

		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size());
		assertEquals(0, g2_b.payloads.size());

		assertEquals(34567, g1_a.payloads.get(0).getMeta().get("val", 0));
		assertEquals(34567, g1_b.payloads.get(0).getMeta().get("val", 0));

		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Broadcast to group2
		br1.broadcast("test.a", t, Groups.of("group2"));
		g2_a.waitFor(20000);
		g2_b.waitFor(20000);

		assertEquals(0, g1_a.payloads.size());
		assertEquals(0, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());

		assertEquals(34567, g2_a.payloads.get(0).getMeta().get("val", 0));
		assertEquals(34567, g2_b.payloads.get(0).getMeta().get("val", 0));

		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Broadcast to group1 and group2
		br1.broadcast("test.a", t, Groups.of("group1", "group2"));
		g1_a.waitFor(20000);
		g1_b.waitFor(20000);
		g2_a.waitFor(20000);
		g2_b.waitFor(20000);
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Emit
		br1.emit("test.a", t);
		waitForMessages();
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		Tree v;
		if (g1_a.payloads.isEmpty()) {
			v = g1_b.payloads.peek();
		} else {
			v = g1_a.payloads.peek();
		}
		assertTrue(v.isEmpty());
		assertTrue(!v.isNull());

		assertEquals(34567, v.getMeta().get("val", 0));

		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Emit NULL
		br1.emit("test.a", (Tree) null);
		waitForMessages();
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		v = null;
		if (g1_a.payloads.isEmpty()) {
			v = g1_b.payloads.peek();
		} else {
			v = g1_a.payloads.peek();
		}
		assertNull(v);
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Emit to group1
		br1.emit("test.a", t, Groups.of("group1"));
		waitForMessages();
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Emit NULL to group1
		br1.emit("test.a", (Tree) null, Groups.of("group1"));
		waitForMessages();
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		v = null;
		if (g1_a.payloads.isEmpty()) {
			v = g1_b.payloads.peek();
		} else {
			v = g1_a.payloads.peek();
		}
		assertNull(v);
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Emit to group2
		br1.emit("test.a", t, Groups.of("group2"));
		waitForMessages();
		assertEquals(0, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		v = null;
		if (g2_a.payloads.isEmpty()) {
			v = g2_b.payloads.peek();
		} else {
			v = g2_a.payloads.peek();
		}

		assertEquals(34567, v.getMeta().get("val", 0));

		g2_a.payloads.clear();
		g2_b.payloads.clear();

		// Test null service
		br1.createService(new NullService());
		br2.waitForServices("nullService").waitFor(20000);
		Tree rsp = br2.call("nullService.nullAction", (Tree) null).waitFor(20000);
		assertNull(rsp);

		br1.createService(new MetaEchoService());

		// Meta test
		br1.createService(new MetaEchoService());
		br2.waitForServices("metaEchoService").waitFor(20000);
		Tree params = new Tree();
		params.getMeta().put("test", 456);
		rsp = br2.call("metaEchoService.action", params).waitFor(5000);
		assertEquals(456, rsp.get("meta-req.test", 0));
		assertEquals(123, rsp.getMeta().get("reply", 0));
		assertEquals(123, rsp.get("_meta.reply", 0));

		// Stream test with two listeners
		StreamListener sl = new StreamListener();
		br1.createService(sl);
		br2.waitForServices("streamListener").waitFor(20000);

		StreamListener s2 = new StreamListener();
		br2.createService(s2);

		PacketStream ps = br2.createStream();
		br2.broadcast("stream.receive", ps);
		waitForMessages();

		assertNotNull(sl.ctx);
		assertEquals(1, sl.ctx.level);
		assertEquals("stream.receive", sl.ctx.name);
		assertEquals(0, sl.buffer.toByteArray().length);

		assertNotNull(s2.ctx);
		assertEquals(1, s2.ctx.level);
		assertEquals("stream.receive", s2.ctx.name);
		assertEquals(0, s2.buffer.toByteArray().length);

		ps.sendData("12345".getBytes());
		ps.sendData("67890".getBytes());

		assertFalse(sl.streamClosed);
		assertFalse(s2.streamClosed);

		ps.sendClose();
		waitForMessages(3);

		assertEquals("1234567890", new String(sl.buffer.toByteArray()));
		assertTrue(sl.streamClosed);

		assertEquals("1234567890", new String(s2.buffer.toByteArray()));
		assertTrue(s2.streamClosed);

		sl.buffer.reset();
		sl.ctx = null;
		sl.streamClosed = false;

		s2.buffer.reset();
		s2.ctx = null;
		s2.streamClosed = false;

		ps = br2.createStream();
		t = new CheckedTree(ps);
		t.getMeta().put("x", "y");
		br2.emit("stream.receive", t);
		ps.sendData("abcdefg".getBytes());
		ps.sendData("12345".getBytes());
		ps.sendData("67".getBytes());
		ps.sendClose();
		waitForMessages(3);

		StreamListener s, d;
		if (sl.streamClosed) {
			s = sl;
			d = s2;
		} else {
			s = s2;
			d = sl;
		}
		assertEquals("abcdefg1234567", new String(s.buffer.toByteArray()));
		assertEquals("y", s.ctx.params.getMeta().get("x", ""));
		assertTrue(s.streamClosed);

		assertNull(d.ctx);
		assertFalse(d.streamClosed);

		// Meta & event
		Level1EventService l1e = new Level1EventService();
		Level2EventService l2e = new Level2EventService();
		Level3EventService l3e = new Level3EventService();

		br1.createService(l1e).createService(l3e);
		br2.createService(l2e);

		br1.waitForServices("level2EventService").waitFor(20000);
		br2.waitForServices("level1EventService", "level3EventService").waitFor(20000);

		br2.createService(new Service("metasender") {
			Action action = ctx -> {
				assertTrue(ctx.params.isEmpty());
				assertEquals(1, ctx.level);

				Tree params = new Tree();
				params.put("b", true);
				params.getMeta().put("a", 123);
				ctx.broadcast("level1.a", params);
				return 12345;
			};
		});
		br2.call("metasender.action");

		waitForMessages(3);
		assertNotNull(l3e.ctx);
		assertEquals(4, l3e.ctx.level);
		assertEquals(123, l3e.ctx.params.getMeta().get("a", 0));

		System.out.println(l3e.ctxB.params);
		assertEquals(123, l3e.ctxB.params.getMeta().get("a", 0));
		assertEquals("Y", l3e.ctxB.params.get("X", ""));

		// Call chained meta
		rsp = br2.call("level1EventService.level1Action", "_meta.l0", "v0").waitFor(20000);
		assertEquals("v0", rsp.getMeta().get("l0", ""));
		assertEquals("v1", rsp.getMeta().get("l1", ""));
		assertEquals("v2", rsp.getMeta().get("l2", ""));
		assertEquals("v3", rsp.getMeta().get("l3", ""));

		// Chained meta
		br2.createService(new Service("ttt") {
			Action first = ctx -> {
				
				// Modify meta (first time)
				ctx.params.getMeta().put("a", "John");
				
				return ctx.call("ttt.second").then(rsp -> {

					// Prints: { a: "John", b: 5 }
					Tree m = rsp.getMeta();
					assertEquals("John", m.get("a", ""));
					assertEquals("5", m.get("b", ""));
					assertEquals("y", m.get("x", ""));
					
					m.put("Q", 123);
				});
			};
			Action second = ctx -> {

				Tree m = ctx.params.getMeta();
				assertEquals("John", m.get("a", ""));
				assertEquals("y", m.get("x", ""));	
				assertNull(m.get("Q"));

				// Modify meta (second time)
				m.put("b", 5);
				return null;
			};
		});
		br1.waitForServices("ttt").waitFor(20000);
		br2.waitForServices("ttt").waitFor(20000);
		
		rsp = br1.call("ttt.first", "_meta.x", "y").waitFor(20000);
		Tree m = rsp.getMeta();
		assertEquals("John", m.get("a", ""));
		assertEquals("5", m.get("b", ""));
		assertEquals("y", m.get("x", ""));
		assertEquals(123, m.get("Q", 0));
		
		rsp = br2.call("ttt.first", "_meta.x", "y").waitFor(20000);
		m = rsp.getMeta();
		assertEquals("John", m.get("a", ""));
		assertEquals("5", m.get("b", ""));
		assertEquals("y", m.get("x", ""));
		assertEquals(123, m.get("Q", 0));
		
		// Chained meta
		br1.createService(new Service("ttt2") {
			Action first = ctx -> {
				
				// Modify meta (first time)
				ctx.params.getMeta().put("a", "John");
				
				return ctx.call("ttt3.second").then(rsp -> {

					// Prints: { a: "John", b: 5 }
					Tree m = rsp.getMeta();
					assertEquals("John", m.get("a", ""));
					assertEquals("5", m.get("b", ""));
					assertEquals("y", m.get("x", ""));
					
					m.put("Q", 123);
				});
			};
		});
		br2.createService(new Service("ttt3") {
			Action second = ctx -> {

				Tree m = ctx.params.getMeta();
				assertEquals("John", m.get("a", ""));
				assertEquals("y", m.get("x", ""));	
				assertNull(m.get("Q"));
				
				m.put("b", 5);
				return null;
			};
		});
		br1.waitForServices("ttt3").waitFor(20000);
		br2.waitForServices("ttt2").waitFor(20000);
		
		rsp = br1.call("ttt2.first", "_meta.x", "y").waitFor(20000);
		m = rsp.getMeta();
		assertEquals("John", m.get("a", ""));
		assertEquals("5", m.get("b", ""));
		assertEquals("y", m.get("x", ""));
		assertEquals(123, m.get("Q", 0));
		
		rsp = br2.call("ttt2.first", "_meta.x", "y").waitFor(20000);
		m = rsp.getMeta();
		assertEquals("John", m.get("a", ""));
		assertEquals("5", m.get("b", ""));
		assertEquals("y", m.get("x", ""));
		assertEquals(123, m.get("Q", 0));

		// LAST test: reject on disconnect
		br1.createService(new SlowService());
		br2.waitForServices("slowService").waitFor(20000);
		try {
			Promise p = br2.call("slowService.slowAction", (Tree) null);
			br2.stop();
			p.waitFor(20000);
			fail();
		} catch (Exception e) {
			String msg = e.getMessage();
			assertEquals("Request is rejected when call 'slowService.slowAction' action on 'node2' node.", msg);
		}
		br2 = null;
	}

	private void checkPing(ServiceBroker broker, String nodeID) throws Exception {
		Tree rsp = broker.ping(nodeID).waitFor(20000);
		assertTrue(rsp.get("time", 0L) > 0);
		assertTrue(rsp.get("arrived", 0L) > 0);
	}

	// --- SAMPLES ---

	protected static final class NullService extends Service {

		Action nullAction = ctx -> {
			assertNull(ctx.params);
			return null;
		};

	}

	protected static final class SlowService extends Service {

		Action slowAction = ctx -> {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			return "slow";
		};

	}

	protected static final class MetaEchoService extends Service {

		Action action = ctx -> {
			Tree reqMeta = ctx.params.getMeta();

			Tree rsp = new Tree();
			rsp.putMap("meta-req").assign(reqMeta);
			rsp.getMeta().put("reply", 123);

			return rsp;
		};

	}

	protected static final class Group1Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group1")
		@Subscribe("test.*")
		Listener evt = ctx -> {
			synchronized (payloads) {
				payloads.addLast(ctx.params);
				payloads.notifyAll();
			}
		};

		public void waitFor(long timeout) throws Exception {
			synchronized (payloads) {
				if (payloads.isEmpty()) {
					payloads.wait(timeout);
				}
			}
			assertTrue(!payloads.isEmpty());
		}

	}

	protected static final class Group2Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group2")
		@Subscribe("test.*")
		Listener evt = ctx -> {
			synchronized (payloads) {
				payloads.addLast(ctx.params);
				payloads.notifyAll();
			}
		};

		public void waitFor(long timeout) throws Exception {
			synchronized (payloads) {
				if (payloads.isEmpty()) {
					payloads.wait(timeout);
				}
			}
			assertTrue(!payloads.isEmpty());
		}

	}

	protected static final class StreamListener extends Service {

		protected ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		protected boolean streamClosed;

		protected Context ctx;

		@Subscribe("stream.*")
		Listener evt = ctx -> {
			this.ctx = ctx;
			ctx.stream.onPacket((bytes, err, closed) -> {
				if (bytes != null) {
					buffer.write(bytes);
				}
				if (closed) {
					streamClosed = true;
				}
			});
		};

	}

	protected static final class TestService extends Service {

		Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
		};

	}

	protected static final class Level1EventService extends Service {

		@Subscribe("level1.*")
		Listener evt = ctx -> {
			logger.info("Level1EventService invoked.");
			assertEquals(2, ctx.level);
			assertEquals("node2", ctx.nodeID);
			ctx.broadcast("level2.xyz", "a", 4);
		};

		Action level1Action = ctx -> {
			Tree req = new Tree();
			req.getMeta().put("l1", "v1");
			assertEquals(1, ctx.level);
			assertEquals("node2", ctx.nodeID);
			assertEquals("v0", ctx.params.getMeta().get("l0", ""));
			return ctx.call("level2EventService.level2Action", req).then(rsp -> {
				assertEquals("v0", rsp.getMeta().get("l0", ""));
				assertEquals("v1", rsp.getMeta().get("l1", ""));
				assertEquals("v2", rsp.getMeta().get("l2", ""));
				assertEquals("v3", rsp.getMeta().get("l3", ""));
				return rsp;
			});
		};

	}

	protected static final class Level2EventService extends Service {

		@Subscribe("level2.xyz")
		Listener evt = ctx -> {
			logger.info("Level2EventService invoked.");
			assertEquals(3, ctx.level);
			ctx.broadcast("level3.xyz", "a", 5);
			assertEquals("node1", ctx.nodeID);
			ctx.call("level3EventService.level3ActionB", "X", "Y");
		};

		Action level2Action = ctx -> {
			Tree req = new Tree();
			assertEquals(2, ctx.level);
			req.getMeta().put("l2", "v2");
			assertEquals("v0", ctx.params.getMeta().get("l0", ""));
			assertEquals("node1", ctx.nodeID);
			return ctx.call("level3EventService.level3Action", req).then(rsp -> {
				assertEquals("v0", rsp.getMeta().get("l0", ""));
				assertEquals("v1", rsp.getMeta().get("l1", ""));
				assertEquals("v2", rsp.getMeta().get("l2", ""));
				assertEquals("v3", rsp.getMeta().get("l3", ""));
				return rsp;
			});
		};
	}

	protected static final class Level3EventService extends Service {

		protected Context ctx;
		protected Context ctxB;

		@Subscribe("level3.*")
		Listener evt = ctx -> {
			logger.info("Level3EventService invoked.");
			assertEquals("node2", ctx.nodeID);
			this.ctx = ctx;
		};

		Action level3Action = ctx -> {
			assertEquals("node2", ctx.nodeID);
			assertEquals(3, ctx.level);
			assertEquals("v0", ctx.params.getMeta().get("l0", ""));
			assertEquals("v1", ctx.params.getMeta().get("l1", ""));
			assertEquals("v2", ctx.params.getMeta().get("l2", ""));
			Tree rsp = new Tree();
			rsp.getMeta().put("l3", "v3");
			return rsp;
		};

		Action level3ActionB = ctx -> {
			assertEquals("node2", ctx.nodeID);
			assertEquals(4, ctx.level);
			this.ctxB = ctx;
			return null;
		};

	}

	// --- UTILITIES ---

	protected AtomicInteger started = new AtomicInteger();
	protected AtomicInteger stopped = new AtomicInteger();
	protected AtomicInteger connected = new AtomicInteger();
	protected AtomicInteger disconnected = new AtomicInteger();
	
	@Override
	protected void setUp() throws Exception {
		started.set(0);
		stopped.set(0);
		connected.set(0);
		disconnected.set(0);
		
		// Create transporters
		tr1 = createTransporter();
		tr2 = createTransporter();
		
		// Enable debug messages + receiveListener
		tr1.setDebug(true);
		tr2.setDebug(true);
		
		// Create brokers
		ExecutorService executor = Executors.newCachedThreadPool();
		br1 = ServiceBroker.builder().transporter(tr1).executor(executor).monitor(new ConstantMonitor()).nodeID("node1").build();
		br2 = ServiceBroker.builder().transporter(tr2).executor(executor).monitor(new ConstantMonitor()).nodeID("node2").build();

		// Create "marker" service
		br1.createService("marker", new Service() {
			
			// --- INTERNAL EVENTS ---
			
			@Subscribe("$broker.started")
			Listener evtStarted = ctx -> {
				started.incrementAndGet();
			};

			@Subscribe("$broker.stopped")
			Listener evtStopped = ctx -> {
				stopped.incrementAndGet();
			};

			@Subscribe("$transporter.connected")
			Listener evtConnected = ctx -> {
				connected.incrementAndGet();
			};

			@Subscribe("$transporter.disconnected")
			Listener evtDisconnected = ctx -> {
				disconnected.incrementAndGet();
			};
			
		});

		assertEquals(0, started.get());
		assertEquals(0, stopped.get());

		// Start brokers
		br1.start();
		br2.start();

		// Check started/stopped
		assertEquals(1, started.get());
		assertEquals(0, stopped.get());
		
		// Wait for connecting nodes
		br2.waitForServices(20000, "marker").waitFor(20000);
		
		// Check connected/disconnected
		waitForMessages(3);
		assertEquals(1, connected.get());
		assertEquals(0, disconnected.get());		
	}

	@Override
	protected void tearDown() throws Exception {
		if (br1 != null) {	
			br1.stop();
		}
		if (br2 != null) {
			br2.stop();
		}
		
		// Check started/stopped
		assertEquals(1, started.get());
		assertEquals(1, stopped.get());
		
		// Check connected/disconnected
		assertEquals(1, connected.get());
		assertEquals(1, disconnected.get());	
	}

	// --- RECEIVE LISTENER ---
	
	public final void waitForMessages() throws InterruptedException {
		waitForMessages(1);
	}
	
	public final void waitForMessages(long multi) throws InterruptedException {
		long start = System.currentTimeMillis();
		tr1.lastReceivedMessageAt.set(start);
		tr2.lastReceivedMessageAt.set(start);
		long now, last;
		Thread.sleep(min * multi);
		for (int i = 0; i < max * multi / 100; i++) {
			if (i > 0) {
				Thread.sleep(100);
			}
			now = System.currentTimeMillis();
			if (now - start >= (max * multi)) {
				break;
			}
			last = Math.max(tr1.lastReceivedMessageAt.get(), tr2.lastReceivedMessageAt.get());
			if (now - last >= (timeout * multi)) {
				break;
			}
		}
	}

}