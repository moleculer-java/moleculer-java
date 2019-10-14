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

	protected long sleep = 700;

	protected Transporter tr1;
	protected ServiceBroker br1;

	protected Transporter tr2;
	protected ServiceBroker br2;

	// --- ABSTRACT METHODS ---

	public abstract Transporter createTransporter();

	// --- COMMON TESTS ---

	@Test
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
		Thread.sleep(sleep);
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
		Thread.sleep(sleep);
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
		Thread.sleep(sleep);
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
		Thread.sleep(sleep);
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Emit NULL to group1
		br1.emit("test.a", (Tree) null, Groups.of("group1"));
		Thread.sleep(sleep);
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
		Thread.sleep(sleep);
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

		// Stream test
		StreamListener sl = new StreamListener();
		br1.createService(sl);
		br2.waitForServices("streamListener").waitFor(20000);

		PacketStream ps = br2.createStream();
		br2.broadcast("stream.receive", ps);
		Thread.sleep(sleep);
		assertNotNull(sl.ctx);
		assertEquals(1, sl.ctx.level);
		assertEquals("stream.receive", sl.ctx.name);
		assertEquals(0, sl.buffer.toByteArray().length);

		ps.sendData("12345".getBytes());
		ps.sendData("67890".getBytes());
		assertFalse(sl.streamClosed);
		ps.sendClose();
		Thread.sleep(sleep * 3);
		assertEquals("1234567890", new String(sl.buffer.toByteArray()));
		assertTrue(sl.streamClosed);

		sl.buffer.reset();
		sl.ctx = null;
		sl.streamClosed = false;

		ps = br2.createStream();
		t = new CheckedTree(ps);
		t.getMeta().put("x", "y");
		br2.emit("stream.receive", t);
		ps.sendData("abcdefg".getBytes());
		ps.sendClose();
		Thread.sleep(sleep * 3);
		assertEquals("abcdefg", new String(sl.buffer.toByteArray()));
		assertEquals("y", sl.ctx.params.getMeta().get("x", ""));
		
		// TODO test reversed order
		assertTrue(sl.streamClosed);

		// Meta & event
		Level1EventService l1e = new Level1EventService();
		Level2EventService l2e = new Level2EventService();
		Level3EventService l3e = new Level3EventService();

		br1.createService(l1e).createService(l3e);
		br2.createService(l2e);

		br1.waitForServices("level2EventService").waitFor(20000);
		br2.waitForServices("level1EventService", "level3EventService").waitFor(20000);
		
		br2.createService(new Service("metasender") {
			@SuppressWarnings("unused")
			public Action action = ctx -> {
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
		
		Thread.sleep(sleep * 3);
		assertNotNull(l3e.ctx);
		assertEquals(4, l3e.ctx.level);	
		assertEquals(123, l3e.ctx.params.getMeta().get("a", 0));
		
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

		public Action nullAction = ctx -> {
			assertNull(ctx.params);
			return null;
		};

	}

	protected static final class SlowService extends Service {

		public Action slowAction = ctx -> {
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
			}
			return "slow";
		};

	}

	protected static final class MetaEchoService extends Service {

		public Action action = ctx -> {
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
		public Listener evt = ctx -> {
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
		public Listener evt = ctx -> {
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
		public Listener evt = ctx -> {
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

		public Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
		};

	}

	protected static final class Level1EventService extends Service {

		@Subscribe("level1.*")
		public Listener evt = ctx -> {
			logger.info("Level1EventService invoked.");
			assertEquals(2, ctx.level);			
			ctx.broadcast("level2.xyz", "a", 4);
		};

	}

	protected static final class Level2EventService extends Service {

		@Subscribe("level2.xyz")
		public Listener evt = ctx -> {
			logger.info("Level2EventService invoked.");
			assertEquals(3, ctx.level);			
			ctx.broadcast("level3.xyz", "a", 5);
		};
		
	}

	protected static final class Level3EventService extends Service {

		protected Context ctx;

		@Subscribe("level3.*")
		public Listener evt = ctx -> {
			logger.info("Level3EventService invoked.");
			this.ctx = ctx;
		};

	}

	// --- UTILITIES ---

	@Override
	protected void setUp() throws Exception {

		// Create transporters
		tr1 = createTransporter();
		tr2 = createTransporter();

		// Enable debug messages
		tr1.setDebug(true);
		tr2.setDebug(true);

		// Create brokers
		br1 = ServiceBroker.builder().transporter(tr1).monitor(new ConstantMonitor()).nodeID("node1").build();
		br2 = ServiceBroker.builder().transporter(tr2).monitor(new ConstantMonitor()).nodeID("node2").build();

		// Create "marker" service
		br1.createService("marker", new Service() {
		});

		// Start brokers
		br1.start();
		br2.start();

		// Wait for connecting nodes
		br2.waitForServices(15000, "marker").waitFor(15000);
	}

	@Override
	protected void tearDown() throws Exception {
		if (br1 != null) {
			br1.stop();
		}
		if (br2 != null) {
			br2.stop();
		}
	}

}