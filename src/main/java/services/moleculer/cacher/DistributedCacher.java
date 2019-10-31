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
package services.moleculer.cacher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import io.datatree.Tree;
import io.datatree.dom.BASE64;

/**
 * Abstract class of distributed cachers. Currently the {@link RedisCacher} and
 * the certain types of the {@link JCacheCacher} can be distributed (without
 * special development).
 *
 * @see RedisCacher
 * @see JCacheCacher
 */
public abstract class DistributedCacher extends Cacher {

	// --- CONTENT CONTAINER NAME ---

	protected static final String CONTENT = "_";

	// --- PROPERTIES ---

	protected int maxParamsLength;

	// --- KEY HASHERS ---

	protected final ThreadLocal<MessageDigest> hashers = new ThreadLocal<>();

	// --- CONSTRUCTORS ---

	public DistributedCacher() {
	}

	public DistributedCacher(int maxKeyLength) {
		setMaxParamsLength(maxKeyLength);
	}

	// --- GENERATE CACHE KEY ---

	/**
	 * Creates a cacher-specific key by name and params. Concatenates the name
	 * and params.
	 *
	 * @param name
	 *            action name
	 * @param params
	 *            input (JSON) structure
	 * @param keys
	 *            keys in the "params" structure (optional)
	 *
	 * @return generated cache key String
	 */
	@Override
	public String getCacheKey(String name, Tree params, String... keys) {
		if (params == null) {
			return name;
		}
		StringBuilder buffer = new StringBuilder(128);
		serializeKey(buffer, params, keys);
		String serializedParams = buffer.toString();
		int paramsLength = serializedParams.length();
		if (maxParamsLength < 44 || paramsLength <= maxParamsLength) {

			// Key = action name : serialized key
			return name + ':' + serializedParams;
		}

		// Length of unhashed part (begining of the serialized params)
		int prefixLength = maxParamsLength - 44;

		// Create SHA-256 hash from the entire key
		byte[] bytes = serializedParams.getBytes(StandardCharsets.UTF_8);
		MessageDigest hasher = hashers.get();
		if (hasher == null) {
			try {
				hasher = MessageDigest.getInstance("SHA-256");
				hashers.set(hasher);
			} catch (Exception cause) {
				logger.warn("Unable to get SHA-256 hasher!", cause);
				return name + ':' + serializedParams;
			}
		}
		bytes = hasher.digest(bytes);

		// Concatenate key and the 44 character long hash
		String base64 = BASE64.encode(bytes);
		if (prefixLength < 1) {

			// Fully hashed key = action name : hash code
			return name + ':' + base64;
		}

		// Partly hashed key = action name : beginig of the prefix + hash code
		return name + ':' + serializedParams.substring(0, prefixLength) + base64;
	}

	// --- GETTERS / SETTERS ---

	public int getMaxParamsLength() {
		return maxParamsLength;
	}

	public void setMaxParamsLength(int maxParamsLength) {
		if (maxParamsLength > 0 && maxParamsLength < 44) {
			logger.warn("The minimum value of \"maxParamsLength\" parameter is 44!");
			this.maxParamsLength = 44;
		} else {
			this.maxParamsLength = maxParamsLength;
		}
	}

}