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

import java.util.LinkedList;

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Groups;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.LocalActionEndpoint;
import services.moleculer.service.RemoteActionEndpoint;
import services.moleculer.service.Service;

public abstract class TransporterTest extends TestCase {

	// --- VARIABLES ---

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
		br2.waitForServices(10000, "math").waitFor();

		// Wait for "math" service on node2 (again)
		br2.waitForServices(10000, "math").waitFor();

		// Wait for "math" service on node1
		br1.waitForServices(10000, "math").waitFor();

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
			Tree rsp = br2.call("math.add", "a", i, "b", 1).waitFor();
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
		br1.waitForServices(10000, "g1_a", "g1_b", "g2_a", "g2_b").waitFor();
		
		// Broadcast
		br1.broadcast("test.a", new Tree());
		Thread.sleep(1000);
		g1_a.waitFor();
		g1_b.waitFor();
		g2_a.waitFor();
		g2_b.waitFor();
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Broadcast to group1
		br1.broadcast("test.a", new Tree(), Groups.of("group1"));
		g1_a.waitFor();
		g1_b.waitFor();
		assertEquals(1, g1_a.payloads.size());
		assertEquals(1, g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size());
		assertEquals(0, g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		
		// Broadcast to group2
		br1.broadcast("test.a", new Tree(), Groups.of("group2"));
		g2_a.waitFor();
		g2_b.waitFor();
		assertEquals(0, g1_a.payloads.size());
		assertEquals(0, g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size());
		assertEquals(1, g2_b.payloads.size());
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Broadcast to group1 and group2
		br1.broadcast("test.a", new Tree(), Groups.of("group1", "group2"));
		g1_a.waitFor();
		g1_b.waitFor();
		g2_a.waitFor();
		g2_b.waitFor();
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Emit
		br1.emit("test.a", new Tree());
		Thread.sleep(500);
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();
		g2_a.payloads.clear();
		g2_b.payloads.clear();
		
		// Emit to group1
		br1.emit("test.a", new Tree(), Groups.of("group1"));
		Thread.sleep(500);
		assertEquals(1, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(0, g2_a.payloads.size() + g2_b.payloads.size());
		g1_a.payloads.clear();
		g1_b.payloads.clear();

		// Emit to group2
		br1.emit("test.a", new Tree(), Groups.of("group2"));
		Thread.sleep(500);
		assertEquals(0, g1_a.payloads.size() + g1_b.payloads.size());
		assertEquals(1, g2_a.payloads.size() + g2_b.payloads.size());
		g2_a.payloads.clear();
		g2_b.payloads.clear();
	}
	
	private void checkPing(ServiceBroker broker, String nodeID) throws Exception {
		Tree rsp = broker.ping(nodeID).waitFor();
		assertTrue(rsp.get("source", 0L) > 0);
		assertTrue(rsp.get("target", 0L) > 0);
	}
	
	// --- SAMPLES ---
	
	protected static final class Group1Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group1")
		@Subscribe("test.*")
		public Listener evt = payload -> {
			synchronized (payloads) {
				payloads.addLast(payload);
				payloads.notifyAll();
			}
		};

		public void waitFor() throws Exception {
			synchronized (payloads) {
				if (payloads.isEmpty()) {
					payloads.wait(10000);
				}
			}
			assertTrue(!payloads.isEmpty());
		}
		
	}
	
	protected static final class Group2Listener extends Service {

		protected LinkedList<Tree> payloads = new LinkedList<>();

		@Group("group2")
		@Subscribe("test.*")
		public Listener evt = payload -> {
			synchronized (payloads) {
				payloads.addLast(payload);
				payloads.notifyAll();
			}
		};

		public void waitFor() throws Exception {
			synchronized (payloads) {
				if (payloads.isEmpty()) {
					payloads.wait(10000);
				}
			}
			assertTrue(!payloads.isEmpty());
		}

	}
	
	protected static final class TestService extends Service {

		public Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
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
		br2.waitForServices(10000, "marker").waitFor();
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