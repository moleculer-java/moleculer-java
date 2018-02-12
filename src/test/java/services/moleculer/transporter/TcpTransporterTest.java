/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.transporter.tcp.NodeDescriptor;

public class TcpTransporterTest extends TestCase {

	// --- VARIABLES ---

	protected TcpTransporter tr;
	protected ServiceBroker br;

	// --- GOSSIP REQUEST ---

	@Test
	public void testSendGossipRequest() throws Exception {

		// Add "node2"
		tr.nodes.put("node2", createOnlineDescriptorWithoutInfo(false, "node2"));

		Tree req = tr.sendGossipRequest();
		assertEquals(req.get("ver", "?"), ServiceBroker.PROTOCOL_VERSION);
		assertEquals(req.get("sender", "?"), "node1");
		assertEquals(req.get("online.node1[0]", -1), 1);
		assertEquals(req.get("online.node1[1]", -1), 0);
		assertEquals(req.get("online.node1[2]", -1), 0);
		assertEquals(2, req.get("online").size());
		assertEquals(req.get("online.node2[0]", -1), 1);
		assertEquals(req.get("online.node2[1]", -1), 0);
		assertEquals(req.get("online.node2[2]", -1), 0);

		// Add "node3"
		tr.nodes.put("node3", createOnlineDescriptorWithoutInfo(false, "node3"));

		req = tr.sendGossipRequest();
		assertEquals(3, req.get("online").size());
		assertEquals(req.get("online.node3[0]", -1), 1);
		assertEquals(req.get("online.node3[1]", -1), 0);
		assertEquals(req.get("online.node3[2]", -1), 0);

		// Node3 -> offline
		tr.nodes.get("node3").markAsOffline();

		req = tr.sendGossipRequest();
		assertEquals(2, req.get("online").size());
		assertNull(req.get("online.node3"));
		assertEquals(2, req.get("offline.node3", -1));
		assertEquals(1, req.get("offline").size());

		// Node4 -> offline & hidden
		tr.registerAsNewNode("node4", "host4", 1004);

		req = tr.sendGossipRequest();
		assertNull(req.get("online.node4"));
		assertNull(req.get("offline.node4"));
		assertEquals(2, req.get("online").size());
		assertEquals(1, req.get("offline").size());

		// CPU -> 2
		tr.nodes.get("node2").updateCpu(2);

		req = tr.sendGossipRequest();
		assertEquals(req.get("online.node2[1]", -1), 1);
		assertEquals(req.get("online.node2[2]", -1), 2);

		// CPU -> seq + 4
		tr.nodes.get("node2").updateCpu(5, 4);

		req = tr.sendGossipRequest();
		assertEquals(req.get("online.node2[1]", -1), 5);
		assertEquals(req.get("online.node2[2]", -1), 4);

		// CPU -> wrong seq + 4
		tr.nodes.get("node2").updateCpu(4, 3);

		req = tr.sendGossipRequest();
		assertEquals(req.get("online.node2[1]", -1), 5);
		assertEquals(req.get("online.node2[2]", -1), 4);

		// Wrong values
		assertException(() -> {
			tr.nodes.get("node2").updateCpu(-1);
		});
		assertException(() -> {
			tr.nodes.get("node2").updateCpu(101);
		});
		assertException(() -> {
			tr.nodes.get("node2").updateCpu(0, 50);
		});
		assertException(() -> {
			tr.nodes.get("node2").updateCpu(10, 200);
		});
		assertException(() -> {
			tr.nodes.get("node2").updateCpu(0, 0);
		});
		assertException(() -> {
			tr.registerAsNewNode(null, "node6", 1006);
		});		
		assertException(() -> {
			tr.registerAsNewNode("node6", null, 1006);
		});		
		assertException(() -> {
			tr.registerAsNewNode("node6", "", 1006);
		});		
		assertException(() -> {
			tr.registerAsNewNode("", "node6", 1006);
		});		
		assertException(() -> {
			tr.registerAsNewNode("node6", "node6", 0);
		});		
		assertException(() -> {
			tr.udpPacketReceived("node7", "", 1007);
		});		
		assertException(() -> {
			tr.udpPacketReceived(null, "node7", 1007);
		});				
		assertException(() -> {
			tr.udpPacketReceived("node7", "node7", 0);
		});
		assertException(() -> {
			tr.nodes.get("node2").markAsOnline(null);
		});						
		assertException(() -> {
			tr.nodes.get("node2").markAsOnline(new Tree());
		});						
	}

	// --- GOSSIP REQUEST PROCESSING ---
	
	@Test
	public void testProcessGossipRequest() throws Exception {
		
		// Add "node2" (hidden)
		tr.nodes.put("node2", createOfflineDescriptor(false, "node2"));
		
		// Simple request
		Tree req = createGossipRequest("node3", 1, 2, 3);
		
		Tree rsp = tr.processGossipRequest(req);
		
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(1, rsp.get("online.node1").size());
		assertNotNull(rsp.get("online.node1[0].hostname"));
		assertTrue(rsp.get("online.node1[0].port", 0) > 0);
		assertTrue(rsp.get("online.node1[0].seq", 0) > 0);
		assertTrue(rsp.get("online.node1[0].services").size() > 0);
		
		// Update local CPU
		tr.getDescriptor().updateCpu(4);

		rsp = tr.processGossipRequest(req);
		assertEquals(3, rsp.get("online.node1").size());
		assertEquals(1, rsp.get("online.node1[1]", 0));
		assertEquals(4, rsp.get("online.node1[2]", 0));
		
		tr.getDescriptor().updateCpu(5);
		
		rsp = tr.processGossipRequest(req);
		assertEquals(2, rsp.get("online.node1[1]", 0));
		assertEquals(5, rsp.get("online.node1[2]", 0));
		
		tr.getDescriptor().updateCpu(0);
		rsp = tr.processGossipRequest(req);
		assertEquals(3, rsp.get("online.node1[1]", 0));
		assertEquals(0, rsp.get("online.node1[2]", 0));

		// Add offline node
		
		// Node4 -> offline & hidden
		tr.registerAsNewNode("node3", "host3", 1003);
		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(3, rsp.get("online.node1").size());
		
		// Node5 -> online
		NodeDescriptor node4 = createOnlineDescriptorWithInfo(false, "node4");
		node4.info.put("seq", "3");
		node4.seq = 3;
		tr.nodes.put("node4", node4);
		
		rsp = tr.processGossipRequest(req);		
		assertEquals(2, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(3, rsp.get("online.node1").size());
		assertEquals(1, rsp.get("online.node4").size());
		assertEquals(1, rsp.get("online.node4[0].port", 0));
		assertEquals(3, rsp.get("online.node4[0].seq", 0));
		assertEquals("node4", rsp.get("online.node4[0].hostname", "?"));
		
		// Add "node4" to request (low seq)
		req.get("online").putList("node4").add(1).add(1).add(0);
		
		rsp = tr.processGossipRequest(req);
		assertEquals(2, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(3, rsp.get("online.node1").size());
		assertEquals(3, rsp.get("online.node1[1]", 0));
		assertEquals(0, rsp.get("online.node1[2]", 0));
		
		// Add "node4" to request (correct seq)
		req.get("online").get("node4").clear().add(3).add(1).add(0);
		
		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertEquals(3, rsp.get("online.node1").size());

		// Add "node4" to request (high seq)
		req.get("online").get("node4").clear().add(4).add(1).add(0);

		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(3, rsp.get("online.node1").size());
		
		// Add "node4" to request (higher CPU seq)
		req.get("online").get("node4").clear().add(3).add(2).add(1);
		
		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertEquals(1, tr.getCpuUsage("node4"));

		// Add "node4" to request (lower CPU seq)
		req.get("online").get("node4").clear().add(3).add(1).add(2);
		
		rsp = tr.processGossipRequest(req);

		assertEquals(2, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(2, rsp.get("online.node4").size());
		assertEquals(2, rsp.get("online.node4[0]", 0));
		assertEquals(1, rsp.get("online.node4[1]", 0));
		assertEquals(1, tr.getCpuUsage("node4"));
		
		// Add "node4" to request (same CPU seq)
		req.get("online").get("node4").clear().add(3).add(2).add(2);

		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertNull(rsp.get("online.node4"));
		assertEquals(1, tr.getCpuUsage("node4"));

		// Add "node4" to request (higher CPU seq)
		req.get("online").get("node4").clear().add(3).add(3).add(5);
		
		rsp = tr.processGossipRequest(req);		
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertNull(rsp.get("online.node4"));
		assertEquals(5, tr.getCpuUsage("node4"));

		// Add "node4" to request (lower CPU seq)
		req.get("online").get("node4").clear().add(3).add(1).add(2);

		rsp = tr.processGossipRequest(req);		
		assertEquals(2, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		assertEquals(2, rsp.get("online.node4").size());
		assertEquals(3, rsp.get("online.node4[0]", 0));
		assertEquals(5, rsp.get("online.node4[1]", 0));
		assertEquals(5, tr.getCpuUsage("node4"));

		// TODO Move into "createGossipRequest"! Add new info block to "node4"
		req = createGossipRequest(null, 0, 0, 0);
		Tree info = createOnlineDescriptorWithInfo(false, "node4").info;
		info.put("seq", 4);
		info.putList("services").putMap("test2");
		Tree list = req.get("online").putList("node4");
		list.addObject(info);
		list.add(0).add(0);
		
		rsp = tr.processGossipRequest(req);
		assertEquals(1, rsp.get("online").size());
		assertNull(rsp.get("offline"));
		
		System.out.println(tr.nodes.get("node4").info.get("services[0]").getName());
		
		System.out.println(rsp);
	}
	
	// --- GOSSIP RESPONSE PROCESSING ---
	
	// TODO
	
	// --- UTILITIES --

	protected Tree createGossipRequest(String nodeID, int seq, int cpuSeq, int cpu) {
		Tree req = new Tree();
		req.put("sender", nodeID);
		req.put("ver", ServiceBroker.PROTOCOL_VERSION);
		Tree online = req.putList("online");
		if (nodeID != null) {
			online.putList(nodeID).add(seq).add(cpuSeq).add(cpu);
		}
		return req;
	}
	
	protected void assertException(Runnable runnable) {
		try {
			runnable.run();
			throw new AssertionFailedError();
		} catch (Exception e) {
		}
	}

	@Override
	protected void setUp() throws Exception {
		tr = new TcpTransporter();
		tr.setGossipPeriod(Integer.MAX_VALUE);
		tr.setMulticastPeriod(Integer.MAX_VALUE);
		br = ServiceBroker.builder().transporter(tr).monitor(new ConstantMonitor()).nodeID("node1").build();
		br.start();
		tr.writer.disconnect();
	}

	@Override
	protected void tearDown() throws Exception {
		if (br != null) {
			br.stop();
		}
	}

	protected NodeDescriptor createOfflineDescriptor(boolean local, String nodeID) {
		Tree info = new Tree();
		info.put("seq", 0);
		info.put("port", 1);
		info.put("hostname", nodeID);
		NodeDescriptor nd = new NodeDescriptor(nodeID, true, local, info);
		nd.offlineSince = 1;
		return nd;
	}
	
	protected NodeDescriptor createOnlineDescriptorWithoutInfo(boolean local, String nodeID) {
		Tree info = new Tree();
		info.put("seq", 1);
		info.put("port", 1);
		info.put("hostname", nodeID);
		return new NodeDescriptor(nodeID, true, local, info);
	}

	protected NodeDescriptor createOnlineDescriptorWithInfo(boolean local, String nodeID) {
		Tree info = new Tree();
		info.put("seq", 1);
		info.put("port", 1);
		info.put("hostname", nodeID);
		info.putList("services").putMap("test");
		return new NodeDescriptor(nodeID, true, local, info);
	}
	
}