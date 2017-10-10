package services.moleculer.context;

import io.datatree.Tree;
import io.datatree.dom.Cache;

public class CallingOptions {

	// --- CONSTANTS ---

	public static final int DEFAULT_TIMEOUT = 0;
	public static final int DEFAULT_RETRY_COUNT = 0;

	// --- VARIABLES ---

	private final String nodeID;
	private final long timeout;
	private final int retryCount;
	private final Tree meta;
	private final Context parentContext;

	// --- CONSTRUTORS ---

	public CallingOptions(String nodeID, long timeout, int retryCount, Tree meta, Context parentContext) {
		this.nodeID = nodeID;
		this.timeout = timeout;
		this.retryCount = retryCount;
		this.meta = meta;
		this.parentContext = parentContext;
	}

	// --- STATIC CONSTRUCTORS ---

	private static final Cache<String, CallingOptions> cache = new Cache<>(1024, true);

	public static final CallingOptions get(long timeout) {
		return get(null, timeout, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(long timeout, int retryCount) {
		return get(null, timeout, retryCount);
	}

	public static final CallingOptions get(String nodeID) {
		return get(nodeID, DEFAULT_TIMEOUT, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(String nodeID, long timeout) {
		return get(nodeID, timeout, DEFAULT_RETRY_COUNT);
	}

	public static final CallingOptions get(String nodeID, long timeout, int retryCount) {
		String key = nodeID + '.' + timeout + '.' + retryCount;
		CallingOptions opts = cache.get(key);
		if (opts == null) {
			opts = new CallingOptions(nodeID, timeout, retryCount, null, null);
			cache.put(key, opts);
		}
		return opts;
	}

	public static final CallingOptions get(String nodeID, long timeout, int retryCount, Tree meta) {
		return new CallingOptions(nodeID, timeout, retryCount, meta, null);
	}

	public static final CallingOptions get(String nodeID, long timeout, int retryCount, Tree meta,
			Context parentContext) {
		return new CallingOptions(nodeID, timeout, retryCount, meta, parentContext);
	}

	// --- VARIABLE GETTERS ---

	public final String nodeID() {
		return nodeID;
	}

	public final long timeout() {
		return timeout;
	}

	public final int retryCount() {
		return retryCount;
	}

	public final Tree meta() {
		return meta;
	}

	public final Context parentContext() {
		return parentContext;
	}

}