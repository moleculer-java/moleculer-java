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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.datatree.Promise;
import io.datatree.Tree;
import io.datatree.dom.BASE64;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.error.MoleculerError;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.transporter.Transporter;

public abstract class StreamTest extends TestCase {

	// --- VARIABLES ---

	protected Transporter tr1;
	protected ServiceBroker br1;

	protected Transporter tr2;
	protected ServiceBroker br2;

	protected Random rnd = new Random();

	// --- ABSTRACT METHODS ---

	public abstract Transporter createTransporter();

	// --- COMMON TESTS ---

	@Test
	public void testStreams() throws Exception {

		// --- TEST 1 ---

		// byte[] bytes1 = randomBytes(300);
		byte[] bytes1 = "HELLO".getBytes();
		
		PacketStream stream = br1.createStream();
		Tree rsp = br1.call("stream-receiver.receive", stream).waitFor(1000000);
		assertEquals(123, (int) rsp.asInteger());
		assertTrue(stream.sendData(bytes1));

		StreamReceiverService listener = (StreamReceiverService) br2.getLocalService("stream-receiver");
		listener.waitForPackets(1);

		listener.assertOpened();
		listener.assertNotFaulty();
		
		System.out.println(BASE64.encode(bytes1) + " " + new String(bytes1));
		System.out.println(BASE64.encode(listener.getBytes()) + " " + new String(listener.getBytes()));
		
		listener.assertDataEquals(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		byte[] bytes2 = randomBytes(300);
		assertTrue(stream.sendData(bytes2));
		listener.waitForPackets(2);

		listener.assertOpened();
		listener.assertNotFaulty();
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		byte[] bytes3 = randomBytes(300);
		assertTrue(stream.sendData(bytes3));
		listener.waitForPackets(3);

		listener.assertOpened();
		listener.assertNotFaulty();
		assertEquals(bytes1.length + bytes2.length + bytes3.length, stream.getTransferedBytes());

		byte[] all = new byte[bytes1.length + bytes2.length + bytes3.length];
		System.arraycopy(bytes1, 0, all, 0, bytes1.length);
		System.arraycopy(bytes2, 0, all, bytes1.length, bytes2.length);
		System.arraycopy(bytes3, 0, all, bytes1.length + bytes2.length, bytes3.length);

		listener.assertDataEquals(all);

		assertTrue(stream.sendClose());
		listener.waitForPackets(4);

		listener.assertClosed();
		listener.assertNotFaulty();

		assertFalse(stream.sendError(new Exception()));
		listener.assertClosed();
		listener.assertNotFaulty();

		assertFalse(stream.sendClose());
		assertFalse(stream.sendData(bytes1));

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 2 ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		assertTrue(stream.sendData(bytes1));
		listener.waitForPackets(1);

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertDataEquals(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());
		listener.assertDataEquals(bytes1);

		assertTrue(stream.sendError(new Exception()));
		listener.waitForPackets(2);

		listener.assertClosed();
		listener.assertFaulty();
		listener.assertDataEquals(bytes1);

		assertFalse(stream.sendData(bytes2));
		assertFalse(stream.sendClose());
		assertFalse(stream.sendError(new Exception()));

		listener.assertDataEquals(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		// --- TEST 3 (BUFFERED STREAM) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());

		OutputStream out = stream.asOutputStream();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		out.write(bytes1);
		out.write(bytes2);
		out.write(bytes3);

		assertEquals(0, stream.getTransferedBytes());

		out.close();
		listener.waitForPackets(2);

		assertEquals(all.length, stream.getTransferedBytes());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 4 (BUFFERED CHANNEL) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());

		WritableByteChannel channel = stream.asWritableByteChannel();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes1));
		channel.write(ByteBuffer.wrap(bytes2));
		channel.write(ByteBuffer.wrap(bytes3));

		assertEquals(0, stream.getTransferedBytes());

		channel.close();
		listener.waitForPackets(2);

		assertEquals(all.length, stream.getTransferedBytes());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 5 (UNBUFFERED STREAM) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());
		stream.setPacketSize(0);

		out = stream.asOutputStream();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		out.write(bytes1);
		listener.waitForPackets(1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		out.write(bytes2);
		listener.waitForPackets(2);
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		out.write(bytes3);
		listener.waitForPackets(3);
		assertEquals(all.length, stream.getTransferedBytes());

		out.close();
		listener.waitForPackets(4);

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 6 (UNBUFFERED CHANNEL) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());
		stream.setPacketSize(0);

		channel = stream.asWritableByteChannel();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes1));
		listener.waitForPackets(1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes2));
		listener.waitForPackets(2);
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes3));
		listener.waitForPackets(3);
		assertEquals(all.length, stream.getTransferedBytes());

		channel.close();
		listener.waitForPackets(4);

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 7 (BUFFER SIZE = 10 BYTES) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());
		stream.setPacketSize(10);

		out = stream.asOutputStream();

		long trans, prev = -1;
		for (int i = 0; i < 1001; i++) {
			out.write(i);
			trans = stream.getTransferedBytes();
			assertTrue(trans >= prev);
			prev = trans;
			assertTrue(trans % 10 == 0);
		}

		out.close();
		listener.waitForPackets(102);

		// --- TEST 8 (LOAD FROM FILE) ---

		listener.reset();
		stream = br1.createStream();
		rsp = br1.call("stream-receiver.receive", stream).waitFor(20000);
		assertEquals(123, (int) rsp.asInteger());
		listener.assertEmpty();

		File f1 = File.createTempFile("MoleculerStreamTest", ".tmp");
		save(f1, all);

		stream.transferFrom(f1).waitFor(7000);
		f1.delete();
		for (int i = 0; i < 30; i++) {
			Thread.sleep(100);
			if (listener.closed) {
				break;
			}
		}

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 9 (SERVICE RETURNS WITH A PROMISE) ---

		br1.createService(new PromiseProducerService());
		br2.waitForServices("promise-producer").waitFor(5000);

		int sum = 0;
		for (byte b : all) {
			sum += b;
		}

		stream = br2.createStream();
		Promise promise = br2.call("promise-producer.sum", stream);

		stream.sendData(bytes1);
		stream.sendData(bytes2);
		stream.sendData(bytes3);
		stream.sendClose();

		Tree prsp = promise.waitFor(6000);
		assertEquals(sum, (int) prsp.asInteger());

		// --- TEST 10 (FAULTY SERVICE) ---

		br2.createService(new FaultyService());
		br1.waitForServices("faulty-receiver").waitFor(5000);

		stream = br1.createStream();
		promise = br1.call("faulty-receiver.receive", stream);

		// Ok
		stream.sendData("12".getBytes());

		// Ok
		stream.sendData("123".getBytes());

		// Ok
		stream.sendData("1234".getBytes());

		// Generating fault
		stream.sendData("12345".getBytes());

		// Check fault
		try {
			promise.waitFor(5000);
			throw new Exception("Invalid position!");
		} catch (MoleculerError e) {

			// Ok
			assertTrue(e.getStack().contains("xyz"));
		}

		// Already faulty
		try {
			stream.sendData("123456".getBytes());
			throw new Exception("Invalid position!");
		} catch (MoleculerError e) {

			// Ok
			assertTrue(e.getStack().contains("xyz"));
		}

		// Already closed
		boolean canClose = stream.sendClose();
		assertFalse(canClose);

		// --- TEST 11 (SEND ERROR) ---

		br2.createService(new ErrorReceiver());
		br1.waitForServices("error-receiver").waitFor(5000);

		stream = br1.createStream();
		promise = br1.call("error-receiver.receive", stream);

		stream.sendData("123".getBytes());

		stream.sendError(new Exception("abc"));

		String msg = promise.waitFor(5000).asString();
		assertEquals("abc", msg);
	}

	// --- SAMPLES ---

	@Name("error-receiver")
	protected static final class ErrorReceiver extends Service {

		public Action receive = ctx -> {
			return new Promise(res -> {
				ctx.stream.onPacket((bytes, cause, close) -> {
					if (cause != null) {
						res.resolve(cause.getMessage());
					}
					if (close) {
						res.resolve();
					}
				});
			});
		};

	}

	@Name("faulty-receiver")
	protected static final class FaultyService extends Service {

		public Action receive = ctx -> {
			return new Promise(res -> {
				ctx.stream.onPacket((bytes, cause, close) -> {
					if (bytes != null && bytes.length == 5) {
						throw new IOException("xyz");
					}
					if (close) {
						res.resolve();
					}
				});
			});
		};

	}

	@Name("promise-producer")
	protected static final class PromiseProducerService extends Service {

		@Name("sum")
		public Action action = ctx -> {
			return new Promise(res -> {

				final AtomicInteger sum = new AtomicInteger();

				ctx.stream.onPacket((bytes, cause, close) -> {
					if (bytes != null) {
						for (byte b : bytes) {
							sum.addAndGet(b);
						}
					}
					if (cause != null) {
						res.resolve("ERR");
					}
					if (close) {
						res.resolve(sum.get());
					}
				});
			});
		};

	}

	@Name("stream-receiver")
	protected static final class StreamReceiverService extends Service {

		protected ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		protected Throwable error;
		protected boolean closed;

		protected volatile int packetCounter;
		protected Object waitLock = new Object();

		public Action receive = ctx -> {
			assertNotNull(ctx.stream);
			ctx.stream.onPacket((bytes, cause, close) -> {
				if (bytes != null) {
					buffer.write(bytes);
				}
				if (cause != null) {
					error = cause;
				}
				if (close) {
					closed = true;
				}
				synchronized (waitLock) {
					packetCounter++;
					waitLock.notifyAll();
				}
			});
			return 123;
		};

		public StreamReceiverService waitForPackets(int numberOfPackets) throws InterruptedException {
			synchronized (waitLock) {
				if (packetCounter == numberOfPackets) {
					return this;
				}
				waitLock.wait(500);
				for (int i = 0; i < numberOfPackets; i++) {
					if (packetCounter == numberOfPackets) {
						return this;
					}
					waitLock.wait(1000);
				}
				assertEquals(numberOfPackets, packetCounter);
			}
			return this;
		}

		public void reset() {
			buffer.reset();
			error = null;
			closed = false;
			packetCounter = 0;
		}

		public byte[] getBytes() {
			return buffer.toByteArray();
		}

		public void assertOpened() {
			assertFalse(closed);
		}

		public void assertClosed() {
			assertTrue(closed);
		}

		public void assertDataEquals(byte[] bytes) {
			assertTrue(Arrays.equals(bytes, buffer.toByteArray()));
		}

		public void assertFaulty() {
			assertNotNull(error);
		}

		public void assertNotFaulty() {
			assertNull(error);
		}

		public void assertEmpty() {
			assertEquals(0, buffer.size());
		}

	}

	// --- UTILITIES ---

	public void save(File file, byte[] bytes) throws Exception {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file, false);
			out.write(bytes);
		} catch (Exception e) {
			if (file != null) {
				file.delete();
			}
			throw e;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	public byte[] randomBytes(int len) {
		byte[] bytes = new byte[len];
		rnd.nextBytes(bytes);
		return bytes;
	}

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

		// Install service
		br2.createService(new StreamReceiverService());

		// Start brokers
		br1.start();
		br2.start();

		// Wait for "stream-receiver" service
		br1.waitForServices("stream-receiver").waitFor(10000);
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