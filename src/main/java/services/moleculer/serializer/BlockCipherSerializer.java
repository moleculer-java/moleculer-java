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

import static services.moleculer.util.CommonUtils.formatNamoSec;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Serializer with Symmetric Key Encryption. This enables message-level
 * encryption. Sample of usage:<br>
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * trans.setSerializer(new BlockCipherSerializer("secret123"));
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1").transporter(trans).build();
 * </pre>
 * 
 * Chaining Serializers (serialize then compress then encrypt packets):
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * MsgPackSerializer msgPack = new MsgPackSerializer();
 * DeflaterSerializer deflater = new DeflaterSerializer(msgPack);
 * BlockCipherSerializer cipher = new BlockCipherSerializer(deflater);
 * trans.setSerializer(cipher);
 * </pre>
 */
@Name("Block Cipher Serializer")
public class BlockCipherSerializer extends ChainedSerializer {

	// --- PROPERTIES ---

	/**
	 * Type of the block Cipher. Possible values include:
	 * <ul>
	 * <li>AES
	 * <li>DES
	 * <li>DESede
	 * <li>RC2
	 * <li>Blowfish
	 * <li>ARCFOUR
	 * <li>etc.
	 * </ul>
	 */
	protected String algorithm = "AES";

	/**
	 * Password for Symmetric Key Encryption. Using this hard-coded, default
	 * password is not secure. Use a custom password instead of this.
	 */
	protected String password = "k@#QMw!93mcDg%2y";

	/**
	 * Key length (in bytes, or -1 = try to autodetect key length).
	 */
	protected int requiredKeyLength = -1;

	// --- SECRET KEY ---

	protected SecretKeySpec secretKey;

	// --- CHIPHERS ---

	protected ThreadLocal<Cipher> encriptors = new ThreadLocal<>();
	protected ThreadLocal<Cipher> decriptors = new ThreadLocal<>();

	// --- CONSTRUCTORS ---

	/**
	 * Creates a JSON Serializer that uses AES encryption algorithm with the
	 * default password (using the hard-coded, default password is not very
	 * secure).
	 */
	public BlockCipherSerializer() {
		super(new JsonSerializer());
	}

	/**
	 * Creates a JSON Serializer that uses AES encryption algorithm to
	 * encrypt/decrypt messages.
	 * 
	 * @param password
	 *            password for Symmetric Key Encryption
	 */
	public BlockCipherSerializer(String password) {
		this(new JsonSerializer(), password);
	}

	/**
	 * Creates a custom Serializer that uses AES encryption algorithm to
	 * encrypt/decrypt messages.
	 * 
	 * @param parent
	 *            parent Serializer (eg. a JsonSerializer)
	 * @param password
	 *            password for Symmetric Key Encryption
	 */
	public BlockCipherSerializer(Serializer parent, String password) {
		this(parent, password, "AES", 16);
	}

	/**
	 * Creates a Serializer that uses a symmetric encryption algorithm to
	 * encrypt/decrypt messages.
	 *
	 * @param parent
	 *            parent Serializer (eg. a JsonSerializer)
	 * @param password
	 *            password for Symmetric Key Encryption
	 * @param algorithm
	 *            block Cipher type (eg. "AES", "DES", "DESede", "Blowfish")
	 * @param requiredKeyLength
	 *            required key length (-1 = try to autodetect)
	 */
	public BlockCipherSerializer(Serializer parent, String password, String algorithm, int requiredKeyLength) {
		super(parent);
		setPassword(password);
		setAlgorithm(algorithm);
		setRequiredKeyLength(requiredKeyLength);
	}

	// --- INIT ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Create SecretKey
		int keyLength;
		if (requiredKeyLength < 1) {
			try {
				KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
				SecretKey sample = keyGen.generateKey();
				keyLength = sample.getEncoded().length;
			} catch (Exception e) {
				keyLength = 16;
			}
		} else {
			keyLength = requiredKeyLength;
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(keyLength * 2);
		byte[] key = password.getBytes(StandardCharsets.UTF_8);
		while (buffer.size() < keyLength) {
			buffer.write(key);
		}
		key = buffer.toByteArray();
		if (key.length > keyLength) {
			key = Arrays.copyOf(key, keyLength);
		}
		secretKey = new SecretKeySpec(key, algorithm);
		if (debug) {
			logger.info(algorithm + " secret key created (" + secretKey + ").");
		}
	}
	
	// --- DEBUG INFO ---

	protected String getAlgorithmName() {
		return algorithm;
	}

	// --- SERIALIZE AND ENCRYPT TREE TO BYTE ARRAY ---

	@Override
	public byte[] write(Tree value) throws Exception {
		if (debug) {
			byte[] original = parent.write(value);
			long start = System.nanoTime();
			byte[] encrypted = encrypt(getEncriptor(), original);
			long duration = System.nanoTime() - start;
			logger.info(getAlgorithmName() + " encryption of " + original.length + " bytes finished in "
					+ formatNamoSec(duration) + ".");
			return encrypted;
		} else {
			return encrypt(getEncriptor(), parent.write(value));
		}
	}

	protected byte[] encrypt(Cipher cipher, byte[] bytes) throws Exception {
		return cipher.doFinal(bytes);
	}

	// --- DECRYPT AND DESERIALIZE BYTE ARRAY TO TREE ---

	@Override
	public Tree read(byte[] source) throws Exception {
		if (debug) {
			long start = System.nanoTime();
			byte[] decrypted = decrypt(getDecryptor(), source);
			long duration = System.nanoTime() - start;
			logger.info(getAlgorithmName() + " decryption of " + source.length + " bytes finished in "
					+ formatNamoSec(duration) + ".");
			return parent.read(decrypted);
		} else {
			return parent.read(decrypt(getDecryptor(), source));
		}
	}

	protected byte[] decrypt(Cipher cipher, byte[] bytes) throws Exception {
		return cipher.doFinal(bytes);
	}

	// --- ENCRYPTOR HANDLER ---

	protected Cipher getEncriptor() throws Exception {
		Cipher encriptor = encriptors.get();
		if (encriptor == null) {
			encriptor = Cipher.getInstance(algorithm);
			encriptor.init(Cipher.ENCRYPT_MODE, secretKey);
			encriptors.set(encriptor);
			if (debug) {
				logger.info(algorithm + " encryptor created for Thread \"" + Thread.currentThread().getName() + "\".");
			}
		}
		return encriptor;
	}

	protected Cipher getDecryptor() throws Exception {
		Cipher decriptor = decriptors.get();
		if (decriptor == null) {
			decriptor = Cipher.getInstance(algorithm);
			decriptor.init(Cipher.DECRYPT_MODE, secretKey);
			decriptors.set(decriptor);
			if (debug) {
				logger.info(algorithm + " decryptor created for Thread \"" + Thread.currentThread().getName() + "\".");
			}
		}
		return decriptor;
	}

	// --- GETTERS / SETTERS ---

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = Objects.requireNonNull(algorithm);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Null or empty password!");
		}
		this.password = password;
	}

	public int getRequiredKeyLength() {
		return requiredKeyLength;
	}

	public void setRequiredKeyLength(int requiredKeyLength) {
		this.requiredKeyLength = requiredKeyLength;
	}

}
