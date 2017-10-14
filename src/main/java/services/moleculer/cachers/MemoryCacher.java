package services.moleculer.cachers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import io.datatree.dom.TreeReaderRegistry;
import services.moleculer.Promise;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.GlobMatcher;
import services.moleculer.services.Name;

/**
 * On-heap cache implementation.<br>
 * Configuration properties:
 * <ul>
 * <li>initialCapacityPerPartition: Initial capacity per partition
 * <li>maximumCapacityPerPartition: Maximum capacity per partition
 * <li>ttl: Expire time of entries in memory, in seconds (default: 0 = never expires)
 * <li>cleanup: Cleanup period, in seconds (default value is 5 seconds)
 * <li>overflowToDisk: If true, memory cacher writes old entries to disk (disabled by default)
 * <li>diskTTL: Disk entry timeout in seconds (default value is 1 hour)
 * <li>directory: Directory of disk entries (default is "java.io.tmpdir")
 * <li>format: File format of disk entries (json, smile, etc.)
 * </ul>
 * Performance: 10 000 000 gets within 1406 milliseconds
 */
@Name("On-heap Memory Cacher")
public final class MemoryCacher extends Cacher implements Runnable {

	// --- PROPERTIES ---

	/**
	 * Initial capacity per partition
	 */
	private int initialCapacityPerPartition;
	
	/**
	 * Maximum capacity per partition
	 */
	private int maximumCapacityPerPartition;

	/**
	 * Expire time, in seconds (0 = never expires)
	 */
	private int ttl;

	/**
	 * Cleanup period, in seconds
	 */
	private int cleanup = 5;

	/**
	 * If true, memory cacher writes old entries to disk (disabled by default)
	 */
	private boolean overflowToDisk;

	/**
	 * Directory of disk entries (default is "java.io.tmpdir")
	 */
	private File directory;

	/**
	 * Disk entry timeout in seconds (1 hour)
	 */
	private int diskTTL = 3600;

	/**
	 * File format of disk entries (json, smile, etc.)
	 */
	private String format;

	/**
	 * NodeID
	 */
	private String nodeID;

	// --- LOCKS ---

	private final Lock readerLock;
	private final Lock writerLock;

	// --- PARTITIONS / CACHE REGIONS ---

	private final HashMap<String, MemoryPartition> partitions = new HashMap<>();

	// --- CONSTUCTORS ---

	public MemoryCacher() {
		this(2048, 0);
	}

	public MemoryCacher(int maximumCapacityPerPartition, int ttl) {
		this(Math.min(512, maximumCapacityPerPartition), maximumCapacityPerPartition, ttl, 5, false, null, 3600);
	}

	public MemoryCacher(int initialCapacityPerPartition, int maximumCapacityPerPartition, int ttl, int cleanup,
			boolean overflowToDisk, File directory, int diskTTL) {

		// Check variables
		if (initialCapacityPerPartition < 1) {
			throw new IllegalArgumentException(
					"Zero or negative initialCapacityPerPartition property (" + initialCapacityPerPartition + ")!");
		}
		if (maximumCapacityPerPartition < 1) {
			throw new IllegalArgumentException(
					"Zero or negative maximumCapacityPerPartition property (" + maximumCapacityPerPartition + ")!");
		}
		if (initialCapacityPerPartition > maximumCapacityPerPartition) {
			int tmp = initialCapacityPerPartition;
			initialCapacityPerPartition = maximumCapacityPerPartition;
			maximumCapacityPerPartition = tmp;
		}

		// Set properties
		this.initialCapacityPerPartition = initialCapacityPerPartition;
		this.maximumCapacityPerPartition = maximumCapacityPerPartition;
		this.ttl = ttl;
		this.cleanup = cleanup;

		this.overflowToDisk = overflowToDisk;
		this.directory = directory;
		this.diskTTL = diskTTL;

		// Init locks
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
		readerLock = lock.readLock();
		writerLock = lock.writeLock();
	}

	// --- START CACHER ---

	/**
	 * Cancelable timer
	 */
	private volatile ScheduledFuture<?> timer;

	/**
	 * Initializes cacher instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		initialCapacityPerPartition = config.get("initialCapacityPerPartition", initialCapacityPerPartition);
		maximumCapacityPerPartition = config.get("maximumCapacityPerPartition", maximumCapacityPerPartition);
		ttl = config.get("ttl", ttl);
		cleanup = config.get("cleanup", cleanup);
		overflowToDisk = config.get("overflowToDisk", overflowToDisk);
		diskTTL = config.get("diskTTL", diskTTL);
		format = config.get("format", format);

		if (overflowToDisk) {
			String dir = config.get("directory", "");
			if (dir.isEmpty()) {
				if (directory == null) {
					dir = System.getProperty("java.io.tmpdir", "");
					if (dir.isEmpty()) {
						dir = System.getProperty("user.home", "/");
					}
				} else {
					dir = directory.getAbsolutePath();
				}
			}
			File test = new File(dir);
			if (!test.isDirectory()) {
				test.mkdirs();
			}
			if (!test.isDirectory()) {
				throw new IllegalArgumentException("Unable to create cache directory (" + test + ")!");
			}
			directory = test;

			// Autodetect best format
			if (format == null) {
				try {
					TreeReaderRegistry.getReader("smile");
					format = "smile";
				} catch (Throwable notFound) {
					format = "json";
				}
			}
			String formatName = format == null ? "JSON" : format.toUpperCase();
			logger.info("Cacher will use \"" + directory + "\" directory to save entries to disk in " + formatName
					+ " format.");
			logger.info("Entries on disk expire after " + diskTTL + " seconds.");
		}

		// Store nodeID
		nodeID = broker.nodeID();

		// Start timer
		if (ttl > 0) {
			if (cleanup < 1) {
				cleanup = 1;
			}
			timer = broker.components().scheduler().scheduleWithFixedDelay(this, cleanup, cleanup, TimeUnit.SECONDS);
			logger.info("Entries in memory expire after " + ttl + " seconds.");
		}
	}

	// --- REMOVE OLD ENTRIES ---

	private final AtomicLong diskCleanupCounter = new AtomicLong();

	@Override
	public final void run() {
		readerLock.lock();
		try {
			for (MemoryPartition partition : partitions.values()) {
				partition.removeOldEntries();
			}
		} finally {
			readerLock.unlock();
		}

		// Remove old entries from disk
		if (overflowToDisk) {
			if (diskCleanupCounter.incrementAndGet() % 10 != 0) {
				return;
			}
			try {
				final long now = System.currentTimeMillis();
				final String prefix = "cache." + nodeID + '.';
				File[] files = directory.listFiles(new FilenameFilter() {

					@Override
					public final boolean accept(File dir, String name) {
						return name.startsWith(prefix) && name.endsWith(".tmp");
					}

				});
				for (File file : files) {
					if (now - file.lastModified() >= diskTTL) {
						file.delete();
						if (logger.isDebugEnabled()) {
							logger.debug("Disk entry deleted (" + file + ").");
						}
					}
				}
			} catch (Exception cause) {
				logger.error("Unable to run cleanup process!", cause);
			}
		}
	}

	// --- STOP CACHER ---

	@Override
	public final void stop() {

		// Stop timer
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}

		// Clear partitions
		writerLock.lock();
		try {
			partitions.clear();
		} finally {
			writerLock.unlock();
		}
	}

	// --- CACHE METHODS ---

	@Override
	public final Promise get(String key) {
		try {
			int pos = partitionPosition(key, true);
			String prefix = key.substring(0, pos);
			MemoryPartition partition;
			readerLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readerLock.unlock();
			}
			if (partition == null) {
				return null;
			}
			Tree value = partition.get(key.substring(pos + 1));
			if (value == null) {
				return null;
			}
			return Promise.resolve(value);
		} catch (Throwable cause) {
			logger.warn("Unable to get data from MemoryCacher!", cause);
		}
		return null;
	}

	@Override
	public final void set(String key, Tree value) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		writerLock.lock();
		try {
			partition = partitions.get(prefix);
			if (partition == null) {
				partition = new MemoryPartition(initialCapacityPerPartition, maximumCapacityPerPartition, ttl,
						overflowToDisk, directory, format, nodeID);
				partitions.put(prefix, partition);
			}
		} finally {
			writerLock.unlock();
		}
		partition.set(key.substring(pos + 1), value);
	}

	@Override
	public final void del(String key) {
		int pos = partitionPosition(key, true);
		String prefix = key.substring(0, pos);
		MemoryPartition partition;
		readerLock.lock();
		try {
			partition = partitions.get(prefix);
		} finally {
			readerLock.unlock();
		}
		if (partition != null) {
			partition.del(key.substring(pos + 1));
		}
	}

	@Override
	public final void clean(String match) {
		int pos = partitionPosition(match, false);
		if (pos > 0) {

			// Remove items in partitions
			String prefix = match.substring(0, pos);
			MemoryPartition partition;
			readerLock.lock();
			try {
				partition = partitions.get(prefix);
			} finally {
				readerLock.unlock();
			}
			if (partition != null) {
				partition.clean(match.substring(pos + 1));
			}

		} else {

			// Remove entire partitions
			writerLock.lock();
			try {
				if (match.isEmpty() || match.startsWith("*")) {
					partitions.clear();
				} else if (match.indexOf('*') == -1) {
					partitions.remove(match);
				} else {
					Iterator<String> i = partitions.keySet().iterator();
					String key;
					while (i.hasNext()) {
						key = i.next();
						if (GlobMatcher.matches(key, match)) {
							i.remove();
						}
					}
				}
			} finally {
				writerLock.unlock();
			}
		}
	}

	private static final int partitionPosition(String key, boolean throwErrorIfMissing) {
		int i = key.indexOf(':');
		if (i == -1) {
			i = key.lastIndexOf('.');
		} else {
			i = key.lastIndexOf('.', i);
		}
		if (i == -1 && throwErrorIfMissing) {
			throw new IllegalArgumentException("Invalid cache key, a point is missing from the key (" + key + ")!");
		}
		return i;
	}

	// --- MEMORY PARTITION ---

	private static final class MemoryPartition {

		// --- LOGGER ---

		private static final Logger logger = LoggerFactory.getLogger(MemoryPartition.class);

		// --- LOCKS ---

		private final Lock readerLock;
		private final Lock writerLock;

		/**
		 * Expire time, in seconds (0 = never expires)
		 */
		private final int ttl;

		/**
		 * If true, memory cacher writes old entries to disk
		 */
		private boolean overflowToDisk;

		/**
		 * Directory of disk entries
		 */
		private File directory;

		/**
		 * File format of disk entries (json, smile, etc.)
		 */
		private String format;

		/**
		 * NodeID
		 */
		private String nodeID;

		// --- MEMORY CACHE PARTITION ---

		private final LinkedHashMap<String, PartitionEntry> cache;

		// --- CONSTUCTORS ---

		private MemoryPartition(int initialCapacityPerPartition, int maximumCapacityPerPartition, int ttl,
				boolean overflowToDisk, File directory, String format, String nodeID) {

			// Set properties
			this.ttl = ttl;
			this.overflowToDisk = overflowToDisk && directory != null;
			this.directory = directory;
			this.format = format;
			this.nodeID = nodeID;

			// Create lockers
			ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
			readerLock = lock.readLock();
			writerLock = lock.writeLock();

			// Create cache partition
			cache = new LinkedHashMap<String, PartitionEntry>(initialCapacityPerPartition, 1.0f, true) {

				private static final long serialVersionUID = 5994447707758047152L;

				protected final boolean removeEldestEntry(Map.Entry<String, PartitionEntry> entry) {
					if (this.size() > maximumCapacityPerPartition) {
						if (overflowToDisk) {
							saveEntry(entry);
						}
						return true;
					}
					return false;
				};
			};
		}

		private final void saveEntry(Map.Entry<String, PartitionEntry> entry) {
			FileOutputStream out = null;
			try {
				String key = entry.getKey();
				File file = new File(directory, "cache." + nodeID + '.' + key + ".part");
				out = new FileOutputStream(file);
				out.write(entry.getValue().value.toBinary(format, true));
				out.flush();
				out.close();
				out = null;
				File finalFile = new File(directory, "cache." + nodeID + '.' + key + ".tmp");
				file.renameTo(finalFile);
				if (logger.isDebugEnabled()) {
					logger.debug("Disk entry saved (" + finalFile + ").");
				}
			} catch (Exception cause) {
				logger.warn("Unable to save entry to disk!", cause);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignored) {
					}
				}
			}
		}

		// --- REMOVE OLD ENTRIES ---

		private final void removeOldEntries() {
			writerLock.lock();
			try {
				Iterator<Map.Entry<String, PartitionEntry>> i = cache.entrySet().iterator();
				Map.Entry<String, PartitionEntry> mEntry;
				PartitionEntry pEntry;
				long limit = System.currentTimeMillis() - (1000L * ttl);
				while (i.hasNext()) {
					mEntry = i.next();
					pEntry = mEntry.getValue();
					if (pEntry.timestamp <= limit) {
						i.remove();
						if (overflowToDisk) {
							saveEntry(mEntry);
						}
					}
				}
			} finally {
				writerLock.unlock();
			}
		}

		// --- CACHE METHODS ---

		private final Tree get(String key) throws Exception {
			PartitionEntry entry;
			readerLock.lock();
			try {
				entry = cache.get(key);
			} finally {
				readerLock.unlock();
			}
			if (entry == null) {
				if (overflowToDisk) {
					entry = tryToLoadFromDisk(key);
					if (entry == null) {
						return null;
					} else {
						writerLock.lock();
						try {
							cache.put(key, entry);
						} finally {
							writerLock.unlock();
						}
					}
				} else {
					return null;
				}
			}
			return entry.value;
		}

		private final PartitionEntry tryToLoadFromDisk(String key) {
			RandomAccessFile raf = null;
			try {
				File file = new File(directory, "cache." + nodeID + '.' + key + ".tmp");
				if (file.isFile()) {
					raf = new RandomAccessFile(file, "r");
					byte[] bytes = new byte[(int) file.length()];
					raf.readFully(bytes);
					if (logger.isDebugEnabled()) {
						logger.debug("Disk entry loaded (" + file + ").");
					}
					return new PartitionEntry(new Tree(bytes, format));
				}
			} catch (Exception cause) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to load entry from disk!", cause);
				}
			} finally {
				if (raf != null) {
					try {
						raf.close();
					} catch (Exception ignored) {
					}
				}
			}
			return null;
		}

		private final void set(String key, Tree value) {
			writerLock.lock();
			try {
				if (value == null) {
					cache.remove(key);
				} else {
					cache.put(key, new PartitionEntry(value));
				}
			} finally {
				writerLock.unlock();
			}
		}

		private final void del(String key) {
			writerLock.lock();
			try {
				cache.remove(key);
			} finally {
				writerLock.unlock();
			}
		}

		private final void clean(String match) {
			writerLock.lock();
			try {
				if (match.isEmpty() || match.startsWith("*")) {
					cache.clear();
				} else if (match.indexOf('*') == -1) {
					cache.remove(match);
				} else {
					Iterator<String> i = cache.keySet().iterator();
					String key;
					while (i.hasNext()) {
						key = i.next();
						if (GlobMatcher.matches(key, match)) {
							i.remove();
						}
					}
				}
			} finally {
				writerLock.unlock();
			}
		}

	}

	private static final class PartitionEntry {

		private final Tree value;
		private final long timestamp = System.currentTimeMillis();

		private PartitionEntry(Tree value) {
			this.value = value;
		}

	}

}