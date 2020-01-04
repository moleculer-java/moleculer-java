/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.crypto.Cipher;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;
import services.moleculer.util.CommonUtils;

/**
 * XOR algorithm based encryption. It works based on a key file, starting from a
 * random position. The random position is at the beginning of the data packet
 * with AES encryption. The algorithm generates different random positions for
 * each data packet. If the key file is large (>100 MByte), this encryption can
 * be considered quite secure. The XorCipherSerializer holds the key file in
 * memory, so use only the key file that fits in the heap. Sample of usage:<br>
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * File keyFile = new File("/private.text");
 * trans.setSerializer(new XorCipherSerializer(keyFile, "secret123"));
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1").transporter(trans).build();
 * </pre>
 * 
 * Chaining Serializers (serialize then compress then encrypt packets):
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * MsgPackSerializer msgPack = new MsgPackSerializer();
 * DeflaterSerializer deflater = new DeflaterSerializer(msgPack);
 * XorCipherSerializer cipher = new XorCipherSerializer(deflater, keyFile, "secret123");
 * trans.setSerializer(cipher);
 * </pre>
 */
@Name("XOR Cipher Serializer")
public class XorCipherSerializer extends BlockCipherSerializer {

	// --- PROPERTIES ---

	/**
	 * Header length (minimum is 16 bytes).
	 */
	protected int headerLength;

	/**
	 * Key bytes (recommended length is about 10...100 MBytes).
	 */
	protected byte[] keyBytes;

	// --- RAN_DOM ---

	protected ThreadLocal<Random> randoms = ThreadLocal.withInitial(Random::new);

	// --- CONSTRUCTORS ---

	public XorCipherSerializer(File keyFile, String password) throws IOException {
		this(new JsonSerializer(), keyFile, password);
	}

	public XorCipherSerializer(Serializer parent, File keyFile, String password) throws IOException {
		super(parent, password);
		if (keyFile != null) {
			loadKey(keyFile);
		}
	}

	// --- LOAD KEY FROM FILE OR STREAM ---

	public void loadKey(File key) throws FileNotFoundException, IOException {
		loadKey(new FileInputStream(key));
	}

	public void loadKey(InputStream key) throws IOException {
		setKeyBytes(CommonUtils.readFully(key));
	}
	
	// --- INIT ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		if (headerLength < 1) {
			headerLength = getEncriptor().getBlockSize();
		}
		if (debug) {
			logger.info("Header length is " + headerLength + " bytes.");
		}
	}

	// --- ENCRYPT ---

	@Override
	protected byte[] encrypt(Cipher cipher, byte[] bytes) throws Exception {

		// Get random generator
		Random rnd = randoms.get();

		// Convert to random position
		int pos = rnd.nextInt(keyBytes.length);

		// Create header (position + random)
		byte[] header = new byte[headerLength];

		// Write position
		header[0] = (byte) (pos >>> 24);
		header[1] = (byte) (pos >>> 16);
		header[2] = (byte) (pos >>> 8);
		header[3] = (byte) pos;

		// Write random bytes
		for (int i = 4; i < header.length;) {
			for (int r = rnd.nextInt(), n = Math.min(header.length - i,
					Integer.SIZE / Byte.SIZE); n-- > 0; r >>= Byte.SIZE) {
				header[i++] = (byte) r;
			}
		}
		
		// Encrypt header
		header = cipher.doFinal(header);

		// XOR (from random position)
		for (int i = 0; i < bytes.length; i++) {
			if (pos > keyBytes.length - 1) {
				pos = 0;
			}
			bytes[i] ^= keyBytes[pos++];
		}

		// Concatenate (header length + header + data)
		byte[] union = new byte[header.length + bytes.length + 4];
		union[0] = (byte) (header.length >>> 24);
		union[1] = (byte) (header.length >>> 16);
		union[2] = (byte) (header.length >>> 8);
		union[3] = (byte) header.length;
		System.arraycopy(header, 0, union, 4, header.length);
		System.arraycopy(bytes, 0, union, header.length + 4, bytes.length);
		return union;
	}

	// --- DECRYPT ---

	@Override
	protected byte[] decrypt(Cipher cipher, byte[] bytes) throws Exception {

		// Decrypt header (position + random)
		int length = ((0xFF & bytes[0]) << 24) | ((0xFF & bytes[1]) << 16) | ((0xFF & bytes[2]) << 8)
				| (0xFF & bytes[3]);
		byte[] header = new byte[length];
		System.arraycopy(bytes, 4, header, 0, header.length);
		header = cipher.doFinal(header);

		// Read position (and ignore the other random bytes in header)
		int pos = ((0xFF & header[0]) << 24) | ((0xFF & header[1]) << 16) | ((0xFF & header[2]) << 8)
				| (0xFF & header[3]);

		// XOR (from the specified position)
		for (int i = length + 4; i < bytes.length; i++) {
			if (pos > keyBytes.length - 1) {
				pos = 0;
			}
			bytes[i] ^= keyBytes[pos++];
		}

		// Copy the encrypted data block
		byte[] data = new byte[bytes.length - length - 4];
		System.arraycopy(bytes, length + 4, data, 0, data.length);
		return data;
	}

	// --- DEBUG INFO ---

	@Override
	protected String getAlgorithmName() {
		return algorithm + "+XOR";
	}

	// --- GETTERS / SETTERS ---

	public byte[] getKeyBytes() {
		return keyBytes;
	}

	public void setKeyBytes(byte[] keyBytes) {
		if (keyBytes == null || keyBytes.length == 0) {
			throw new IllegalArgumentException("Null or empty key array!");
		}
		this.keyBytes = keyBytes;
	}

	public int getHeaderLength() {
		return headerLength;
	}

	public void setHeaderLength(int headerLength) {
		this.headerLength = headerLength;
	}

}