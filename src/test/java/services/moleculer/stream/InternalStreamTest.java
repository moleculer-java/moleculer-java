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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Test;

import junit.framework.TestCase;

public class InternalStreamTest extends TestCase {

	// --- VARIABLES ---

	protected ScheduledExecutorService scheduler;
	protected Random rnd = new Random();

	// --- SET UP ---

	@Override
	protected void setUp() throws Exception {
		scheduler = Executors.newSingleThreadScheduledExecutor();
	}

	// --- COMMON TESTS ---

	@Test
	public void testStreams() throws Exception {

		// Create test listener
		TestListener listener = new TestListener();

		// --- TEST 1 ---

		PacketStream stream = newStream(listener);

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		byte[] bytes1 = randomBytes(300);
		assertTrue(stream.sendData(bytes1));

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertDataEquals(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		byte[] bytes2 = randomBytes(300);
		assertTrue(stream.sendData(bytes2));

		listener.assertOpened();
		listener.assertNotFaulty();
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		byte[] bytes3 = randomBytes(300);
		assertTrue(stream.sendData(bytes3));

		listener.assertOpened();
		listener.assertNotFaulty();
		assertEquals(bytes1.length + bytes2.length + bytes3.length, stream.getTransferedBytes());

		byte[] all = new byte[bytes1.length + bytes2.length + bytes3.length];
		System.arraycopy(bytes1, 0, all, 0, bytes1.length);
		System.arraycopy(bytes2, 0, all, bytes1.length, bytes2.length);
		System.arraycopy(bytes3, 0, all, bytes1.length + bytes2.length, bytes3.length);

		listener.assertDataEquals(all);

		assertTrue(stream.sendClose());
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
		stream = newStream(listener);

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		assertTrue(stream.sendData(bytes1));

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertDataEquals(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());
		listener.assertDataEquals(bytes1);

		assertTrue(stream.sendError(new Exception()));

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
		stream = newStream(listener);

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

		assertEquals(all.length, stream.getTransferedBytes());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 4 (BUFFERED CHANNEL) ---

		listener.reset();
		stream = newStream(listener);

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

		assertEquals(all.length, stream.getTransferedBytes());
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 5 (UNBUFFERED STREAM) ---

		listener.reset();
		stream = newStream(listener);
		stream.setPacketSize(0);

		out = stream.asOutputStream();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		out.write(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		out.write(bytes2);
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		out.write(bytes3);
		assertEquals(all.length, stream.getTransferedBytes());

		out.close();

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 6 (UNBUFFERED CHANNEL) ---

		listener.reset();
		stream = newStream(listener);
		stream.setPacketSize(0);

		channel = stream.asWritableByteChannel();

		listener.assertOpened();
		listener.assertNotFaulty();
		listener.assertEmpty();
		assertEquals(0, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes1));
		assertEquals(bytes1.length, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes2));
		assertEquals(bytes1.length + bytes2.length, stream.getTransferedBytes());

		channel.write(ByteBuffer.wrap(bytes3));
		assertEquals(all.length, stream.getTransferedBytes());

		channel.close();

		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 7 (BUFFER SIZE = 10 BYTES) ---

		listener.reset();
		stream = newStream(listener);
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
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 8 (LOAD FROM FILE) ---

		listener.reset();
		stream = newStream(listener);

		File f1 = File.createTempFile("MoleculerStreamTest", ".tmp");
		save(f1, all);
		
		stream.transferFrom(f1).waitFor(5000);
		
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 9 (LOAD FROM FILE - SMALL PACKETS) ---

		listener.reset();
		stream = newStream(listener);
		stream.setPacketSize(10);
		stream.setPacketDelay(1);
		
		stream.transferFrom(f1).waitFor(5000);
		
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 10 (PUSH FROM CHANNEL) ---

		listener.reset();
		stream = newStream(listener);
		
		FileInputStream in1 = new FileInputStream(f1);
		FileChannel fc1 = in1.getChannel();
		WritableByteChannel c1 = stream.asWritableByteChannel();
		fc1.transferTo(0, f1.length(), c1);
		c1.close();
		assertTrue(fc1.isOpen());
		fc1.close();
		in1.close();
		
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		
		// --- TEST 11 (LOAD FROM CHANNEL) ---

		listener.reset();
		stream = newStream(listener);
		
		FileInputStream in2 = new FileInputStream(f1);
		FileChannel fc2 = in2.getChannel();
		stream.transferFrom(fc2).waitFor(5000);
		assertFalse(fc2.isOpen());
		f1.delete();
		in2.close();
		
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();
		
		// --- TEST 12 (LOAD FROM STREAM) ---

		listener.reset();
		stream = newStream(listener);
		
		stream.transferFrom(new ByteArrayInputStream(all)).waitFor(1000);
		
		listener.assertDataEquals(all);
		listener.assertClosed();
		listener.assertNotFaulty();

		// --- TEST 13 (EMPTY BLOCKS) ---
		
		listener.reset();
		stream = newStream(listener);

		stream.sendData(bytes1);
		assertEquals(bytes1.length, stream.getTransferedBytes());
		
		stream.sendData(new byte[0]);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		stream.sendData(null);
		assertEquals(bytes1.length, stream.getTransferedBytes());

		// --- TEST 14 (MISSING CAUSE) ---

		try {
			stream.sendError(null);
			throw new Exception("Invalid position!");
		} catch (NullPointerException e) {
			// Ok!
		}

	}

	// --- UTILITIES ---

	public byte[] load(File file) throws Exception {
		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile(file, "r");
			byte[] bytes = new byte[(int) f.length()];
			f.readFully(bytes);
			return bytes;
		} catch (Exception e) {
			if (file != null) {
				file.delete();
			}
			throw e;
		} finally {
			if (f != null) {
				try {
					f.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

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

	public PacketStream newStream(TestListener listener) throws Exception {
		PacketStream s = new PacketStream(scheduler);
		assertTrue(s.onPacket(listener));
		assertFalse(s.onPacket(listener));
		return s;
	}

	public static class TestListener implements PacketListener {

		protected ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		protected Throwable error;
		protected boolean closed;

		@Override
		public void onPacket(byte[] bytes, Throwable cause, boolean close) throws IOException {
			if (bytes != null) {
				buffer.write(bytes);
			}
			if (cause != null) {
				error = cause;
			}
			if (close) {
				closed = true;
			}
		}

		public void reset() {
			buffer.reset();
			error = null;
			closed = false;
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

	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

}