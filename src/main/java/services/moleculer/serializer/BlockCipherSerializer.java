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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Serializer with Symmetric Key Encryption. This enables message-level
 * encryption. Sample of usage:<br>
 * 
 * <pre>
 * BlockCipherSerializer serializer = new BlockCipherSerializer();
 * serializer.setAlgorithm("AES/CBC/PKCS5Padding");
 * serializer.setPassword("12345678901234567890123456789012");
 * serializer.setIv("1234567890123456");
 * Transporter trans = new NatsTransporter("localhost");
 * trans.setSerializer(serializer);
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(trans)
 *                                     .build();
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

	// --- DEFAULT CIPHER ---

	/**
	 * Same as "aes-256-cbc" in Node.js.
	 */
	public static final String DEFAULT_ALGORITHM = "AES/CBC/PKCS5Padding";

	/**
	 * Empty IV block.
	 */
	public static final byte[] EMPTY_IV = new byte[16];

	// --- PROPERTIES ---

	/**
	 * Type of the block Cipher. Possible values include:
	 * <ul>
	 * <li>AES/CTR/NoPadding
	 * <li>AES/CBC/PKCS5Padding
	 * <li>AES
	 * <li>DES
	 * <li>DESede
	 * <li>RC2
	 * <li>Blowfish
	 * <li>ARCFOUR
	 * <li>etc.
	 * </ul>
	 */
	protected String algorithm = DEFAULT_ALGORITHM;

	/**
	 * Password for Symmetric Key Encryption. Using this hard-coded, default
	 * password is not secure. Use a custom password instead of this. The
	 * required password length is 32, when using "AES/CBC/PKCS5Padding"!
	 */
	protected String password = "k@#QMw!93mcDg%2yo39dmw2p397dhtzI";

	/**
	 * Algorithm parameters (IV). Can be "null".
	 */
	protected byte[] iv = EMPTY_IV;

	// --- SECRET KEY ---

	/**
	 * SecretKeySpec, can be specified externally. If not specified, it is
	 * calculated from the "password".
	 */
	protected SecretKeySpec secretKey;

	// --- CHIPHERS OF THE THREAD ---

	protected ThreadLocal<Cipher> encriptors = new ThreadLocal<>();
	protected ThreadLocal<Cipher> decriptors = new ThreadLocal<>();

	// --- CONSTRUCTORS ---

	/**
	 * Creates a JSON Serializer that uses AES encryption algorithm with the
	 * default password (using the hard-coded, default password is not very
	 * secure).
	 */
	public BlockCipherSerializer() {
		this(null, null, null, EMPTY_IV);
	}

	/**
	 * Creates a JSON Serializer that uses AES encryption algorithm to
	 * encrypt/decrypt messages.
	 * 
	 * @param password
	 *            password for Symmetric Key Encryption
	 */
	public BlockCipherSerializer(String password) {
		this(null, password, null, EMPTY_IV);
	}

	/**
	 * Creates a Serializer that uses a symmetric encryption algorithm to
	 * encrypt/decrypt messages.
	 *
	 * @param parent
	 *            parent Serializer (eg. a JsonSerializer)
	 */
	public BlockCipherSerializer(Serializer parent) {
		this(parent, null, null, EMPTY_IV);
	}
	
	/**
	 * Creates a Serializer that uses a symmetric encryption algorithm to
	 * encrypt/decrypt messages.
	 *
	 * @param parent
	 *            parent Serializer (eg. a JsonSerializer)
	 * @param password
	 *            password for Symmetric Key Encryption
	 */
	public BlockCipherSerializer(Serializer parent, String password) {
		this(parent, password, null, EMPTY_IV);
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
	 */
	public BlockCipherSerializer(Serializer parent, String password, String algorithm) {
		this(parent, password, algorithm, (byte[]) null);
	}

	/**
	 * Creates a Serializer that uses a symmetric encryption algorithm to
	 * encrypt/decrypt messages.
	 *
	 * @param password
	 *            password for Symmetric Key Encryption
	 * @param algorithm
	 *            block Cipher type (eg. "AES", "DES", "DESede", "Blowfish")
	 * @param iv
	 *            IV parameter (can be null)
	 */
	public BlockCipherSerializer(String password, String algorithm, byte[] iv) {
		this(null, password, algorithm, iv);
	}

	/**
	 * Creates a Serializer that uses a symmetric encryption algorithm to
	 * encrypt/decrypt messages.
	 *
	 * @param password
	 *            password for Symmetric Key Encryption
	 * @param algorithm
	 *            block Cipher type (eg. "AES", "DES", "DESede", "Blowfish")
	 * @param iv
	 *            IV parameter (can be null)
	 */
	public BlockCipherSerializer(String password, String algorithm, String iv) {
		this(null, password, algorithm, iv);
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
	 * @param iv
	 *            IV parameter (can be null)
	 */
	public BlockCipherSerializer(Serializer parent, String password, String algorithm, String iv) {
		this(parent, password, algorithm, iv == null ? (byte[]) null : iv.getBytes(StandardCharsets.UTF_8));
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
	 * @param iv
	 *            IV parameter (can be null)
	 */
	public BlockCipherSerializer(Serializer parent, String password, String algorithm, byte[] iv) {
		super(parent == null ? new JsonSerializer() : parent);
		if (password != null) {
			setPassword(password);
		}
		if (algorithm != null) {
			setAlgorithm(algorithm);
		}
		setIv(iv);
	}
	
	// --- INIT ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Create SecretKey
		if (secretKey == null) {
			String shortName = algorithm;
			int i = shortName.indexOf('/');
			if (i > -1) {
				shortName = shortName.substring(0, i);
			}
			byte[] key = password.getBytes(StandardCharsets.UTF_8);
			secretKey = new SecretKeySpec(key, shortName);
			if (debug) {
				logger.info(algorithm + " secret key created (" + secretKey + ").");
			}
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
			if (iv == null || iv.length == 0) {
				encriptor.init(Cipher.ENCRYPT_MODE, secretKey);
			} else {
				encriptor.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
			}
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
			if (iv == null || iv.length == 0) {
				decriptor.init(Cipher.DECRYPT_MODE, secretKey);
			} else {
				decriptor.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
			}
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
		this.secretKey = null;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Null or empty password!");
		}
		this.password = password;
		this.secretKey = null;
	}

	public byte[] getIv() {
		return iv;
	}

	public void setIv(String iv) {
		if (iv == null) {
			this.iv = null;
		} else {
			this.iv = iv.getBytes(StandardCharsets.UTF_8);
		}
		this.secretKey = null;
	}

	public void setIv(byte[] iv) {
		this.iv = iv;
		this.secretKey = null;		
	}

	public SecretKeySpec getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKeySpec secretKey) {
		this.secretKey = secretKey;
	}

}