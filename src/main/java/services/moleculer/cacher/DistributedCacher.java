package services.moleculer.cacher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.datatree.Tree;
import io.datatree.dom.BASE64;
import services.moleculer.ServiceBroker;

public abstract class DistributedCacher extends Cacher {

	// --- CONTENT CONTAINER NAME ---

	protected static final String CONTENT = "_";
	
	// --- PROPERTIES ---

	protected int maxKeyLength;

	// --- KEY HASHERS ---
	
	protected final Queue<MessageDigest> hashers = new ConcurrentLinkedQueue<>();
	
	// --- CONSTRUCTORS ---
	
	public DistributedCacher() {
	}
	
	public DistributedCacher(int maxKeyLength) {
		setMaxKeyLength(maxKeyLength);
	}
	
	// --- START CACHER ---

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {
		
		// Process config 
		setMaxKeyLength(config.get("maxKeyLength", maxKeyLength));	
	}
	
	// --- GENERATE CACHE KEY ---
	
	/**
	 * Creates a cacher-specific key by name and params. Concatenates the name
	 * and params.
	 * 
	 * @param name
	 * @param params
	 * @param keys
	 * @return
	 */
	public String getCacheKey(String name, Tree params, String... keys) {
		String key = super.getCacheKey(name, params, keys);
		int keyLength = key.length();
		if (maxKeyLength < 44 || keyLength <= maxKeyLength) {
			
			// Hashing is disabled
			return key;
		}
		
		// Length of unhashed part (begining of the original key)
		int prefixLength = maxKeyLength - 44;
		
		// Create SHA-256 hash from the entire key
		byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
		MessageDigest hasher = hashers.poll();
		if (hasher == null) {
			try {
				hasher = MessageDigest.getInstance("SHA-256");				
			} catch (Exception cause) {
				logger.warn("Unable to get SHA-256 hasher!", cause);
				return key;
			}
		} else {
			hasher.reset();
		}
		bytes = hasher.digest(bytes);
		hashers.add(hasher);
		
		// Concatenate key and the 44 character long hash
		String basee64 = BASE64.encode(bytes); 
		if (prefixLength < 1) {
			return basee64;
		}
		return key.substring(0, prefixLength) + basee64;
	}
	
	@Override
	protected void appendToKey(StringBuilder key, Object object) {
		if (object != null) {
			if (object instanceof Tree) {
				Tree tree = (Tree) object;
				if (tree.isPrimitive()) {
					key.append(tree.asObject());
				} else {
					String json = tree.toString(null, false, true);

					// Create cross-platform, simplified JSON without
					// formatting characters and quotation marks
					for (char c : json.toCharArray()) {
						if (c < 33 || c == '\"' || c == '\'') {
							continue;
						}
						key.append(c);
					}
				}
			} else {
				key.append(object);
			}
		} else {
			key.append("null");
		}
	}

	// --- GETTERS / SETTERS ---
	
	public int getMaxKeyLength() {
		return maxKeyLength;
	}

	public void setMaxKeyLength(int maxKeyLength) {
		if (maxKeyLength > 0 && maxKeyLength < 44) {
			logger.warn("The minimum value of \"maxKeyLength\" parameter is 44!");
			maxKeyLength = 44;
		}
		this.maxKeyLength = maxKeyLength;
	}
	
}
