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
package services.moleculer.transporter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Filesystem-based transporter. It is primarily not for production use (its
 * much slower than other Transporters). Rather it can be considered as a
 * reference implementation or a sample. With this Transporter multiple Service
 * Brokers can communicate with each other through a common directory structure.
 * Usage:
 * <pre>
 * ServiceBroker broker = ServiceBroker.builder().nodeID("node1")
 * .transporter(new FileSystemTransporter("/temp")).build();
 * </pre>
 * 
 * @see AmqpTransporter
 * @see TcpTransporter
 * @see RedisTransporter
 * @see NatsTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see GoogleTransporter
 * @see KafkaTransporter
 */
@Name("File System based Transporter")
public class FileSystemTransporter extends Transporter {

	// --- PROPERTIES ---

	/**
	 * Root directory of message files (eg. "/moleculer")
	 */
	protected String directory;

	/**
	 * Poll period in MILLISECONDS.
	 */
	protected long pollingDelay = 1000;

	/**
	 * Read timeout of request in MILLISECONDS (deleted request files after this
	 * limit).
	 */
	protected long fileTimeout = 10000;

	/**
	 * Max stored file names (per channel).
	 */
	protected int storedFileNames = 1024;

	// --- CHANNEL/DIRECTORY MAPS ---

	protected HashMap<String, DirectoryHandler> inputDirectories = new HashMap<>();

	protected HashMap<String, DirectoryHandler> outputDirectories = new HashMap<>();

	// --- TIMERS ---

	/**
	 * Cancelable timer of poller/cleanup process
	 */
	protected volatile ScheduledFuture<?> pollerProcess;

	/**
	 * Cancelable timer of poller/cleanup process
	 */
	protected volatile ScheduledFuture<?> timeoutProcess;

	// --- CONSTRUCTORS ---

	public FileSystemTransporter() {
	}

	public FileSystemTransporter(String directory) {
		setDirectory(directory);
	}

	public FileSystemTransporter(String directory, long pollingDelay) {
		setDirectory(directory);

	}

	// --- START TRANSPORTER ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);

		// Check timeouts
		if (pollingDelay < 500) {
			pollingDelay = 500;
		}
		fileTimeout = Math.max(fileTimeout, pollingDelay * 3);

		// Start timers
		pollerProcess = scheduler.scheduleWithFixedDelay(this::pollerProcess, pollingDelay, pollingDelay,
				TimeUnit.MILLISECONDS);
		timeoutProcess = scheduler.scheduleWithFixedDelay(this::timeoutProcess, fileTimeout, fileTimeout,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void stopped() {

		// Stop timers
		if (pollerProcess != null) {
			pollerProcess.cancel(false);
			pollerProcess = null;
		}
		if (timeoutProcess != null) {
			timeoutProcess.cancel(false);
			timeoutProcess = null;
		}
		super.stopped();
		synchronized (outputDirectories) {
			for (DirectoryHandler handler : outputDirectories.values()) {
				handler.removeAllSavedFiles();
			}
			outputDirectories.clear();
		}
		synchronized (inputDirectories) {
			for (DirectoryHandler handler : inputDirectories.values()) {
				handler.removeAllFiles();
				if (!handler.shared) {
					if (handler.directory.delete() && debug) {
						logger.info("Directory removed: " + directory);
					}
				}
			}
		}
	}

	// --- FILE LISTENER AND CLEANUP PROCESSES ---

	protected void pollerProcess() {
		synchronized (inputDirectories) {
			for (Map.Entry<String, DirectoryHandler> entry : inputDirectories.entrySet()) {
				LinkedList<byte[]> byteList = entry.getValue().readAndRemoveNextFiles();
				if (byteList == null) {
					continue;
				}
				for (byte[] bytes : byteList) {
					received(entry.getKey(), bytes);
				}
			}
		}
	}

	protected void timeoutProcess() {
		synchronized (outputDirectories) {
			for (DirectoryHandler handler : outputDirectories.values()) {
				handler.removeTimeoutedFiles();
			}
		}
	}

	// --- CONNECT ---

	@Override
	public void connect() {
		connected(false);
	}

	@Override
	public void publish(String channel, Tree message) {
		byte[] bytes;
		try {
			bytes = serializer.write(message);
		} catch (Exception cause) {
			logger.error("Unable to serialize message!", cause);
			return;
		}
		DirectoryHandler handler;
		synchronized (outputDirectories) {
			handler = outputDirectories.get(channel);
			if (handler == null) {
				boolean shared = !channel.endsWith('.' + nodeID);
				File directory = getChannelDirectory(channel);
				handler = new DirectoryHandler(directory, fileTimeout, shared, false, debug, storedFileNames);
				outputDirectories.put(channel, handler);
			}
		}
		handler.saveTempFile(bytes);
	}

	@Override
	public Promise subscribe(String channel) {
		try {
			DirectoryHandler handler;
			synchronized (inputDirectories) {
				handler = inputDirectories.get(channel);
				if (handler == null) {
					boolean shared = !channel.endsWith('.' + nodeID);
					File directory = getChannelDirectory(channel);
					handler = new DirectoryHandler(directory, fileTimeout, shared, true, debug, storedFileNames);
					inputDirectories.put(channel, handler);
				}
			}
		} catch (Exception cause) {
			return Promise.reject(cause);
		}
		return Promise.resolve();
	}

	protected File getChannelDirectory(String channel) {
		String root = directory;
		if (root == null) {
			root = System.getProperty("java.io.tmpdir");
			if (root == null) {
				root = System.getProperty("user.home");
				if (root == null) {
					root = "";
				}
			}
		}
		File rootDir = new File(root);
		rootDir.mkdirs();
		File channelDir = new File(new File(root), channel);
		channelDir.mkdirs();
		return channelDir;
	}

	// --- DIRECTORY HANDLER ---

	protected static class DirectoryHandler {

		// --- LOGGER ---

		protected final Logger logger = LoggerFactory.getLogger(getClass());

		// --- PROPERTIES ---

		protected final File directory;
		protected final long fileTimeout;
		protected final boolean shared;
		protected final boolean polled;
		protected final boolean debug;

		protected final HashSet<RemovableFile> removableFiles = new HashSet<>();

		protected LinkedHashMap<String, Long> loadedFiles;

		protected volatile long lastChecked;
		protected volatile long lastModified;

		// --- CONSTRUCTOR ---

		protected DirectoryHandler(File directory, long fileTimeout, boolean shared, boolean polled, boolean debug,
				int storedFileNames) {
			this.directory = directory;
			this.fileTimeout = fileTimeout;
			this.shared = shared;
			this.polled = polled;
			this.debug = debug;
			if (directory.isDirectory()) {
				if (!shared) {
					removeAllFiles();
				}
			} else {
				String action = directory.mkdirs() ? "created" : "opened";
				if (debug) {
					StringBuilder msg = new StringBuilder(128);
					if (shared) {
						msg.append("Shared directory ");
					} else {
						msg.append("Directory ");
					}
					msg.append(action);
					msg.append(" for ");
					if (polled) {
						msg.append("incoming");
					} else {
						msg.append("outgoing");
					}
					msg.append(" messages: ");
					msg.append(directory.toString());
					logger.info(msg.toString());
				}
			}
			if (polled) {
				loadedFiles = new LinkedHashMap<String, Long>() {

					private static final long serialVersionUID = 1L;

					@Override
					protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
						return size() > storedFileNames;
					}

				};
			}
		}

		protected LinkedList<byte[]> readAndRemoveNextFiles() {
			if (!polled) {
				return null;
			}

			long now = System.currentTimeMillis();
			long currentModified = directory.lastModified();
			if (currentModified == lastModified && now - lastChecked < fileTimeout) {
				return null;
			}
			lastChecked = now;
			lastModified = currentModified;

			File[] files = listFiles(now);
			if (files == null || files.length == 0) {
				return null;
			}

			LinkedList<byte[]> list = new LinkedList<>();
			for (File file : files) {
				RandomAccessFile in = null;
				try {
					in = new RandomAccessFile(file, "r");
					byte[] bytes = new byte[(int) file.length()];
					in.readFully(bytes);
					in.close();
					in = null;
					boolean deleted = false;
					if (!shared) {
						deleted = file.delete();
						if (deleted && debug) {
							logger.info(bytes.length + " bytes long file loaded and removed: " + file);
						}
					} else {
						if (debug) {
							logger.info(bytes.length + " bytes long file loaded: " + file);
						}
					}
					if (!deleted) {
						synchronized (loadedFiles) {
							loadedFiles.put(file.getName(), file.lastModified());
						}
					}
					list.addLast(bytes);
				} catch (Exception ignored) {
					file.delete();
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (Exception ignored) {

							// Do nothind
						}
					}
				}
			}
			return list.isEmpty() ? null : list;
		}

		protected File[] listFiles(long now) {
			return directory.listFiles(new FileFilter() {

				@Override
				public boolean accept(File file) {
					try {
						if (!file.isFile()) {
							return false;
						}
						String name = file.getName();
						if (!name.startsWith("msg")) {
							if (file.delete() && debug) {
								logger.info("File deleted with invalid prefix: " + file);
							}
							return false;
						}
						int i = name.indexOf('.');
						boolean closed = name.endsWith(".closed");
						if (i > -1 && !closed) {
							if (file.delete() && debug) {
								logger.info("File deleted with invalid extension: " + file);
							}
							return false;
						}
						if (now - file.lastModified() >= fileTimeout * 2) {
							if (file.delete() && debug) {
								logger.info("Timeouted file deleted: " + file);
							}
							return false;
						}
						synchronized (loadedFiles) {
							if (loadedFiles.containsKey(name)) {

								// Has been processed, skipping to the next file
								return false;
							}
						}
						return closed;
					} catch (Exception ignored) {

						// Ignored (eg. file deleted)
					}
					return false;
				}

			});
		}

		protected void saveTempFile(byte[] bytes) {
			if (polled) {
				return;
			}
			FileOutputStream out = null;
			try {
				if (!directory.isDirectory()) {
					if (directory.mkdirs() && debug) {
						logger.info("Directory recreated: " + directory);
					}
				}
				File file = File.createTempFile("msg", "", directory);
				long timeoutAt = System.currentTimeMillis() + fileTimeout;
				RemovableFile removable = new RemovableFile(file, timeoutAt);
				synchronized (removableFiles) {
					removableFiles.add(removable);
				}
				out = new FileOutputStream(file);
				out.write(bytes);
				out.flush();
				out.close();
				out = null;
				File finishedFile = new File(directory, file.getName() + ".closed");
				file.renameTo(finishedFile);
				synchronized (removableFiles) {
					removableFiles.remove(removable);
					removableFiles.add(new RemovableFile(finishedFile, timeoutAt));
				}
				if (debug) {
					logger.info(bytes.length + " bytes long file created: " + file);
				}
			} catch (Exception cause) {
				logger.warn("Unable to save file!", cause);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignored) {

						// Do nothind
					}
				}
			}
		}

		protected void removeTimeoutedFiles() {
			int size;
			synchronized (removableFiles) {
				size = removableFiles.size();
			}
			if (size == 0) {
				return;
			}
			ArrayList<RemovableFile> removableCopy = new ArrayList<>(size);
			synchronized (removableFiles) {
				removableCopy.addAll(removableFiles);
			}
			ArrayList<RemovableFile> removedFiles = new ArrayList<>(size);
			long now = System.currentTimeMillis();
			for (RemovableFile removable : removableCopy) {
				if (now >= removable.removeAt) {
					removedFiles.add(removable);
					if (removable.file.delete() && debug) {
						logger.info("Timeouted output file removed: " + removable.file);
					}
					synchronized (loadedFiles) {
						loadedFiles.remove(removable.file.getName());
					}
				}
			}
			synchronized (removableFiles) {
				removableFiles.removeAll(removedFiles);
			}
			synchronized (loadedFiles) {
				Iterator<Long> timestamps = loadedFiles.values().iterator();
				Long timestamp;
				while (timestamps.hasNext()) {
					timestamp = timestamps.next();
					if (timestamp != null && now - timestamp >= fileTimeout * 10) {
						timestamps.remove();
					}
				}
			}
		}

		protected void removeAllFiles() {
			if (shared) {
				return;
			}
			for (File file : directory.listFiles()) {
				if (file.delete() && debug) {
					logger.info("File removed: " + file);
				}
			}
		}

		protected void removeAllSavedFiles() {
			synchronized (removableFiles) {
				for (RemovableFile removable : removableFiles) {
					if (removable.file.delete() && debug) {
						logger.info("Output file removed: " + removable.file);
					}
				}
				removableFiles.clear();
			}
		}

	}

	protected static class RemovableFile {

		protected final File file;
		protected final long removeAt;

		protected RemovableFile(File file, long removeAt) {
			this.file = file;
			this.removeAt = removeAt;
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			RemovableFile other = (RemovableFile) obj;
			return file.equals(other.file);
		}

	}

	// --- GETTERS / SETTERS ---

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public long getPollingDelay() {
		return pollingDelay;
	}

	public void setPollingDelay(long pollingDelay) {
		this.pollingDelay = pollingDelay;
	}

	public long getFileTimeout() {
		return fileTimeout;
	}

	public void setFileTimeout(long fileTimeout) {
		this.fileTimeout = fileTimeout;
	}

	public int getStoredFileNames() {
		return storedFileNames;
	}

	public void setStoredFileNames(int storedFileNames) {
		this.storedFileNames = storedFileNames;
	}

}