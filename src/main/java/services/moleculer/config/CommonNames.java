/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer.config;

/**
 * Common property names in JSON Configurations and Service Descriptors.
 */
public interface CommonNames {

	/**
	 * Name of a component in the configuration.
	 */
	public static final String NAME = "name";

	/**
	 * Type of a component (or full name of the Class).
	 */
	public static final String TYPE = "type";

	/**
	 * Options block (component's custom configuration).
	 */
	public static final String OPTS = "opts";

	/**
	 * Name (or list) of a Java Package(s), to scan for MoleculerComponents or
	 * Services.
	 */
	public static final String PACKAGES_TO_SCAN = "packagesToScan";

	/**
	 * Name of the current / target node (server instance).
	 */
	public static final String NODE_ID = "nodeID";

	/**
	 * Enables (="true) or disables (="false") caching in action configuration.
	 */
	public static final String CACHE = "cache";

	/**
	 * Cache keys in JSON structure.
	 */
	public static final String CACHE_KEYS = "cacheKeys";

	/**
	 * Cleanup period of a Cache, Pool, etc. (in seconds).
	 */
	public static final String CLEANUP = "cleanup";

	/**
	 * Time To Live (TTL) property of a cached entry (in seconds).
	 */
	public static final String TTL = "ttl";

	/**
	 * List of actions in a Service Descriptor JSON.
	 */
	public static final String ACTIONS = "actions";

	/**
	 * Default timeout (in seconds) of the action invocations.
	 */
	public static final String DEFAULT_TIMEOUT = "defaultTimeout";

	/**
	 * Name (or Java class name) of an invocation strategy (eg. "round-robin" or
	 * "random", or full class name).
	 */
	public static final String STRATEGY = "strategy";

	/**
	 * Invoke all local services via Thread pool (true) or directly (false).
	 * It's a Java-specific property.
	 */
	public static final String ASYNC_LOCAL_INVOCATION = "asyncLocalInvocation";

	/**
	 * Always invokes local Services if possible (eg. ignores "round-robin",
	 * etc. strategy).
	 */
	public static final String PREFER_LOCAL = "preferLocal";

	/**
	 * NodeID of the Sender Node.
	 */
	public static final String SENDER = "sender";

	/**
	 * Short version property name.
	 */
	public static final String VER = "ver";

	/**
	 * List of services in the Service Descriptor JSON.
	 */
	public static final String SERVICES = "services";

	/**
	 * Settings block in the Service Descriptor JSON.
	 */
	public static final String SETTINGS = "settings";

	/**
	 * Metadata block in the Service Descriptor JSON.
	 */
	public static final String METADATA = "metadata";

	/**
	 * Params block in the Service Descriptor JSON.
	 */
	public static final String PARAMS = "params";

	/**
	 * IP address block in the Service Descriptor JSON.
	 */
	public static final String IP_LIST = "ipList";

	/**
	 * Client block in the Service Descriptor JSON.
	 */
	public static final String CLIENT = "client";

	/**
	 * Version property in the Service Descriptor JSON.
	 */
	public static final String VERSION = "version";

	/**
	 * Lang version property in the Service Descriptor JSON.
	 */
	public static final String LANG_VERSION = "langVersion";

	/**
	 * Port property in the Service Descriptor JSON.
	 */
	public static final String PORT = "port";

	/**
	 * Config block in the Service Descriptor JSON.
	 */
	public static final String CONFIG = "config";

	/**
	 * URL (or list of URLs) of remote service(s) (eg. http, nats, redis, etc.)
	 */
	public static final String URL = "url";

	/**
	 * Password of a remote service (eg. Redis).
	 */
	public static final String PASSWORD = "password";

	/**
	 * Use SSL/TLS ("true") or not ("false") when connects to a remote service
	 * (eg. to Redis / NATS servers).
	 */
	public static final String SECURE = "secure";

	/**
	 * Prefix of communication channel names (used by Transporters), or UIDs
	 * (used by UID Generators).
	 */
	public static final String PREFIX = "prefix";

	/**
	 * Name (or Java class name) of an data serializer (eg. "json" or "msgpack",
	 * or full class name).
	 */
	public static final String SERIALIZER = "serializer";

	/**
	 * CPU usage in HeartBeat packages. Used by the CPU-based load-balancer.
	 */
	public static final String CPU = "cpu";

	/**
	 * Capacity (the maximum number of elements) of a cache or queue.
	 */
	public static final String CAPACITY = "capacity";

	/**
	 * The stage we're running in ("tool", "development" or "production").
	 */
	public static final String STAGE = "stage";

	/**
	 * Optional Guice configurator module (class name).
	 */
	public static final String MODULE = "module";

	/**
	 * Should shut down Executor and Scheduler Services on stop?
	 */
	public static final String SHUT_DOWN_THREAD_POOLS = "shutDownThreadPools";

	/**
	 * Namespace of ServiceBroker (it's a kind of prefix). Default namespace is
	 * an empty String.
	 */
	public static final String NAMESPACE = "namespace";

	/**
	 * Number of segments in a cache. Currently used by OHCacher.
	 */
	public static final String SEGMENT_COUNT = "segmentCount";

	/**
	 * Initial size of each segment's hash table in OHCacher.
	 */
	public static final String HASH_TABLE_SIZE = "hashTableSize";

	/**
	 * Cache or transporter compresses content above this size (specified in
	 * bytes).
	 */
	public static final String COMPRESS_ABOVE = "compressAbove";

	/**
	 * Heartbeat interval of Transporters (default is 5 seconds).
	 */
	public static final String HEARTBEAT_INTERVAL = "heartbeatInterval";

	/**
	 * Heartbeat timeout limit of Transporters (default is 30 seconds).
	 */
	public static final String HEARTBEAT_TIMEOUT = "heartbeatTimeout";

}