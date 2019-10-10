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
package services.moleculer.stream;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Test;

import io.datatree.Promise;
import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.DefaultServiceRegistry;
import services.moleculer.service.Service;
import services.moleculer.transporter.Transporter;
import services.moleculer.uid.IncrementalUidGenerator;

public class SequenceTest extends TestCase {

	// --- VARIABLES ---

	protected ScheduledExecutorService scheduler;
	protected IncomingStream incomingStream;
	protected InternalStreamTest.TestListener listener;
	protected Random rnd = new Random();

	protected ServiceBroker br;

	// --- SET UP ---

	@Override
	protected void setUp() throws Exception {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		incomingStream = new IncomingStream("node1", scheduler, 0);
		listener = new InternalStreamTest.TestListener();
		incomingStream.getPacketStream().onPacket(listener);
		br = ServiceBroker.builder().transporter(new TestTransporter()).registry(new TestServiceRegistry())
				.monitor(new ConstantMonitor()).nodeID("node1").build();
		br.createService("service", new Service() {

			@SuppressWarnings("unused")
			public Action action = ctx -> {
				ctx.stream.onPacket(listener);
				return null;
			};

		});
		br.start();
	}

	// --- TESTS ---

	@Test
	public void testSequence() throws Exception {

		byte[] b1 = randomBytes(111);
		byte[] b2 = randomBytes(222);
		byte[] b3 = randomBytes(333);

		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		tmp.write(b1);
		tmp.write(b2);
		tmp.write(b3);
		byte[] all = tmp.toByteArray();

		IncrementalUidGenerator uidGenerator = new IncrementalUidGenerator();
		uidGenerator.setPrefix("test");
		String id = uidGenerator.nextUID();
		boolean shouldClose;

		// --- NORMAL SEQUENCE ---

		shouldClose = incomingStream.receive(createStartStreamingPacket(id));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 1, b1));
		assertFalse(shouldClose);
		listener.assertDataEquals(b1);
		listener.assertOpened();
		listener.assertNotFaulty();
		assertEquals(b1.length, incomingStream.stream.getTransferedBytes());
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 2, b2));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 3, b3));
		assertFalse(shouldClose);
		assertEquals(all.length, incomingStream.stream.getTransferedBytes());
		listener.assertDataEquals(all);
		listener.assertOpened();
		listener.assertNotFaulty();
		shouldClose = incomingStream.receive(createCloseStreamingPacket(id, 4));
		assertTrue(shouldClose);
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		incomingStream.reset();
		listener.reset();

		// --- SCRAMBLED SEQUENCE 1 ---

		id = uidGenerator.nextUID();
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 1, b1));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createStartStreamingPacket(id));
		assertFalse(shouldClose);
		listener.assertDataEquals(b1);
		listener.assertOpened();
		listener.assertNotFaulty();
		shouldClose = incomingStream.receive(createCloseStreamingPacket(id, 2));
		assertTrue(shouldClose);
		listener.assertDataEquals(b1);
		listener.assertClosed();
		listener.assertNotFaulty();
		incomingStream.reset();
		listener.reset();

		// --- SCRAMBLED SEQUENCE 2 ---

		id = uidGenerator.nextUID();
		shouldClose = incomingStream.receive(createCloseStreamingPacket(id, 2));
		assertFalse(shouldClose);
		listener.assertOpened();
		listener.assertNotFaulty();
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 1, b1));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createStartStreamingPacket(id));
		assertTrue(shouldClose);
		listener.assertDataEquals(b1);
		listener.assertClosed();
		listener.assertNotFaulty();
		incomingStream.reset();
		listener.reset();

		// --- SCRAMBLED SEQUENCE 3 ---

		id = uidGenerator.nextUID();
		shouldClose = incomingStream.receive(createCloseStreamingPacket(id, 4));
		assertFalse(shouldClose);
		listener.assertOpened();
		listener.assertNotFaulty();
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 1, b1));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createStartStreamingPacket(id));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 3, b3));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 2, b2));
		assertTrue(shouldClose);
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		incomingStream.reset();
		listener.reset();

		// --- SCRAMBLED SEQUENCE 4 ---

		id = uidGenerator.nextUID();
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 1, b1));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 2, b2));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createStartStreamingPacket(id));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createCloseStreamingPacket(id, 4));
		assertFalse(shouldClose);
		shouldClose = incomingStream.receive(createDataStreamingPacket(id, 3, b3));
		assertTrue(shouldClose);
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		incomingStream.reset();
		listener.reset();

		// --- INVOKE VIA BROKER 1 ---

		TestServiceRegistry serviceRegistry = (TestServiceRegistry) br.getConfig().getServiceRegistry();

		id = uidGenerator.nextUID();
		TestTransporter t = (TestTransporter) br.getConfig().getTransporter();
		t.receive(createStartStreamingPacket(id));
		assertEquals(1, serviceRegistry.getRequestStreams().size());
		t.receive(createDataStreamingPacket(id, 1, b1));
		assertEquals(1, serviceRegistry.getRequestStreams().size());
		listener.assertDataEquals(b1);
		listener.assertOpened();
		listener.assertNotFaulty();
		t.receive(createDataStreamingPacket(id, 2, b2));
		assertEquals(1, serviceRegistry.getRequestStreams().size());
		t.receive(createDataStreamingPacket(id, 3, b3));
		assertEquals(1, serviceRegistry.getRequestStreams().size());
		t.receive(createCloseStreamingPacket(id, 4));
		assertEquals(0, serviceRegistry.getRequestStreams().size());
		assertEquals(0, serviceRegistry.getResponseStreams().size());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		listener.reset();

		// --- INVOKE VIA BROKER 2 ---

		id = uidGenerator.nextUID();

		t.receive(createDataStreamingPacket(id, 1, b1));
		assertEquals(1, serviceRegistry.getRequestStreams().size());

		t.receive(createStartStreamingPacket(id));
		assertEquals(1, serviceRegistry.getRequestStreams().size());
		listener.assertDataEquals(b1);
		listener.assertOpened();
		listener.assertNotFaulty();

		t.receive(createCloseStreamingPacket(id, 4));
		assertEquals(1, serviceRegistry.getRequestStreams().size());

		t.receive(createDataStreamingPacket(id, 3, b3));
		assertEquals(1, serviceRegistry.getRequestStreams().size());

		t.receive(createDataStreamingPacket(id, 2, b2));
		assertEquals(0, serviceRegistry.getRequestStreams().size());
		assertEquals(0, serviceRegistry.getResponseStreams().size());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		listener.reset();
	}

	// --- UTILITIES ---

	public Tree createStartStreamingPacket(String id) {
		Tree n = new Tree();
		n.put("ver", "3");
		n.put("sender", "node2");
		n.put("id", id);
		n.put("stream", true);
		n.put("seq", 0);

		n.put("action", "service.action");
		n.put("level", 1);
		n.put("requestID", id);
		return n;
	}

	public Tree createDataStreamingPacket(String id, int seq, byte[] bytes) {
		Tree n = new Tree();
		n.put("ver", "3");
		n.put("sender", "node2");
		n.put("id", id);
		n.put("stream", true);
		n.put("seq", seq);

		Tree params = n.putMap("params");
		params.put("type", "Buffer");
		short[] data = new short[bytes.length];
		for (int i = 0; i < bytes.length; i++) {
			data[i] = (short) (bytes[i] & 0xFF);
		}
		params.putObject("data", data);
		return n;
	}

	public Tree createCloseStreamingPacket(String id, int seq) {
		Tree n = new Tree();
		n.put("ver", "3");
		n.put("sender", "node2");
		n.put("id", id);
		n.put("seq", seq);
		return n;
	}

	public byte[] randomBytes(int len) {
		byte[] bytes = new byte[len];
		rnd.nextBytes(bytes);
		return bytes;
	}

	public static final class TestTransporter extends Transporter {

		public void receive(Tree msg) {
			processReceivedMessage(requestChannel, msg.toBinary());
		}

		@Override
		public void connect() {

			// Do nothing
		}

		@Override
		public void publish(String channel, Tree message) {

			// Do nothing
		}

		@Override
		public Promise subscribe(String channel) {
			return Promise.resolve();
		}

	}

	public static final class TestServiceRegistry extends DefaultServiceRegistry {

		public HashMap<String, IncomingStream> getRequestStreams() {
			return requestStreams;
		}

		public HashMap<String, IncomingStream> getResponseStreams() {
			return responseStreams;
		}

	}

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
		if (br != null) {
			br.stop();
		}
	}

}
