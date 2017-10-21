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
package services.moleculer.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * To set up, add the following line to the JVM properties:<br> 
 * <br>
 * -Djava.util.logging.config.file=./config/logging.properties<br>
 * <br>
 * ...where the content of the "logging.properties" is...<br>
 * <br>
 * handlers = services.moleculer.logger.AsyncFileLogger<br>
 * services.moleculer.logger.AsyncFileLogger.directory = logs<br>
 * services.moleculer.logger.AsyncFileLogger.prefix = moleculer-<br>
 * services.moleculer.logger.AsyncFileLogger.encoding = UTF8<br>
 * services.moleculer.logger.AsyncFileLogger.compressAfter = 30 days<br>
 * services.moleculer.logger.AsyncFileLogger.deleteAfter = 365 days<br>
 * services.moleculer.logger.AsyncFileLogger.logToConsole = true<br>
 * services.moleculer.logger.AsyncFileLogger.level = INFO<br>
 * .level = INFO
 */
public final class AsyncFileLogger extends Handler implements Runnable {

	// --- FILE NAME FORMATTER ---

	private DateFormat FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	// --- PROPERTIES ---

	private String prefix;
	private String directory;
	private int compressAfterDays;
	private int deleteAfterDays;
	private boolean logToConsole;
	private String fileEncoding;

	private static final long DAY = 1000L * 60 * 60 * 24;

	// --- LOG EVENT QUEUE ---

	private final LinkedList<LogRecord> messages = new LinkedList<>();

	// --- LOG DIRECTORY ---

	private File logDirectory;

	// --- CONSTRUCTOR ---

	private ExecutorService executor;

	public AsyncFileLogger() {
		configure();

		// IBM WebSphere-kompatibilis szálkezelő létrehozása
		ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable, "Asynchronous Log Writer");
				thread.setDaemon(true);
				return thread;
			}
		};

		// Start log writer thread
		executor = Executors.newSingleThreadExecutor(threadFactory);
		executor.execute(this);
	}

	// --- ADD TO QUEUE ---

	@Override
	public final void publish(LogRecord record) {
		if (!isLoggable(record)) {
			return;
		}
		synchronized (messages) {
			messages.addLast(record);
			messages.notifyAll();
		}
	}

	public final void run() {
		LinkedList<LogRecord> records = new LinkedList<>();
		try {

			// String message = getFormatter().format(record);
			Formatter formatter = getFormatter();
			StringBuilder lines = new StringBuilder(512);

			while (true) {

				// Get next records
				synchronized (messages) {
					while (messages.isEmpty()) {
						messages.wait(15000);
					}
					records.addAll(messages);
					messages.clear();
				}

				// Write records to console and/or file
				writeLines(records, lines, formatter);

				// Waiting for other log records
				Thread.sleep(400);
			}
		} catch (InterruptedException interrupt) {
			return;
		} catch (Exception e) {
			records.clear();
			e.printStackTrace();
		}
	}

	private final void writeLines(LinkedList<LogRecord> records, StringBuilder lines, Formatter formatter)
			throws Exception {
		lines.setLength(0);
		for (LogRecord record : records) {
			lines.append(formatter.format(record));
		}
		records.clear();
		String packet = lines.toString();

		// Write records to log file
		if (logDirectory != null) {
			String date = FILE_FORMAT.format(new Date());
			appendToFile(prefix + date + ".log", packet.getBytes(fileEncoding));
		}

		// Write records to console
		if (logToConsole) {
			System.out.println(packet.trim());
		}
	}

	private String openedFile = "";
	private FileOutputStream openedStream;

	private final void appendToFile(String fileName, byte[] bytes) {
		try {
			if (!openedFile.equals(fileName)) {
				closeStream();
				File file = new File(logDirectory, fileName);
				openedStream = new FileOutputStream(file, true);
				openedFile = fileName;
				boolean cleanup = !file.isFile();
				if (cleanup) {
					compressOrDeleteOldFiles();
				}
			}
			openedStream.write(bytes);
			openedStream.flush();
		} catch (Exception e) {
			closeStream();
			e.printStackTrace();
		}
	}

	private final void closeStream() {
		if (openedStream != null) {
			openedFile = "";
			try {
				openedStream.close();
			} catch (Exception ignored) {
			}
		}
	}

	private final void compressOrDeleteOldFiles() {
		File[] files = logDirectory.listFiles(new FilenameFilter() {

			@Override
			public final boolean accept(File dir, String name) {
				name = name.toLowerCase();
				return name.endsWith(".log") || name.endsWith(".zip");
			}
		});
		long now = System.currentTimeMillis();
		byte[] buffer = new byte[100 * 1024];
		int count = 0;
		for (File file : files) {
			boolean isLog = file.getName().toLowerCase().endsWith(".log");
			long time = file.lastModified();
			long days = (now - time) / DAY;

			// Delete too old files
			if (days > deleteAfterDays) {
				file.delete();
				continue;
			}
			if (isLog) {

				// Compress old files
				if (days > compressAfterDays) {
					ZipOutputStream zos = null;
					FileInputStream in = null;
					File zf = null;
					try {
						zf = new File(logDirectory, file.getName() + ".zip");
						FileOutputStream fos = new FileOutputStream(zf);
						zos = new ZipOutputStream(fos);
						zos.setLevel(Deflater.BEST_SPEED);
						ZipEntry ze = new ZipEntry(file.getName());
						ze.setTime(time);
						zos.putNextEntry(ze);
						in = new FileInputStream(file);
						int len;
						while ((len = in.read(buffer)) > 0) {
							zos.write(buffer, 0, len);
						}
						in.close();
						in = null;
						zos.closeEntry();
						zos.close();
						zos = null;
						zf.setLastModified(time);
						file.delete();

						count++;
						if (count > 100) {
							break;
						}
					} catch (Exception e) {
						if (zf != null) {
							zf.delete();
						}
						continue;
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (Exception ignored) {
							}
						}
						if (zos != null) {
							try {
								zos.close();
							} catch (Exception ignored) {
							}
						}
					}
				}
			}
		}
	}

	// --- FLUSH / CLOSE ---

	@Override
	public final void flush() {
	}

	@Override
	public final void close() throws SecurityException {

		// Stop executor
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}

		// Write rest of the log
		LinkedList<LogRecord> records = new LinkedList<>();
		synchronized (messages) {
			if (!messages.isEmpty()) {
				records.addAll(messages);
				messages.clear();
			}
		}
		if (!records.isEmpty()) {
			try {
				writeLines(records, new StringBuilder(512), getFormatter());
			} catch (Exception ignored) {
			}
		}

		// Close stream
		closeStream();
	}

	// --- CONFIGURATION ---

	private final void configure() {
		String className = AsyncFileLogger.class.getName();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		// Get configured log directory
		directory = getProperty(className + ".directory", "");
		if (directory.isEmpty()) {
			directory = getProperty("moleculer.log.directory", "");
		}

		// Automatic directory selection
		// services.moleculer.logger.AsyncFileLogger.directory = #AUTO
		if (directory.startsWith("#")) {
			String found = null;

			// Detect by "catalina.home" System Property
			String path = System.getProperty("catalina.home");
			if (path != null && path.length() > 0) {
				File test = new File(path, "logs");
				if (test.isDirectory()) {
					found = test.getAbsolutePath();
				}
			}

			// Detect by classpath
			if (found == null) {

				// Lehetséges útvonalak
				HashSet<String> paths = new HashSet<String>();
				paths.add("/moleculer");
				paths.add("/Program Files/moleculer");
				paths.add("/Program Files (x86)/moleculer");
				paths.add("/opt/moleculer");

				String cp = System.getProperty("java.class.path");
				if (paths != null) {
					String pathSeparator = System.getProperty("path.separator");
					if (pathSeparator == null || pathSeparator.length() == 0) {
						pathSeparator = ";:";
					}
					StringTokenizer st = new StringTokenizer(cp, pathSeparator);
					while (st.hasMoreTokens()) {
						path = st.nextToken().trim();
						if (path.length() > 0) {
							path = path.replace('\\', '/');
							int index = path.lastIndexOf('/');
							if (index > -1) {
								path = path.substring(0, index);
							}
							paths.add(path);
						}
					}
				}
				for (String test : paths) {
					found = findLogDirectory(new File(test));
					if (found != null) {
						found = found.replace('\\', '/');
						break;
					}
				}
			}
			if (found == null) {
				found = System.getProperty("user.home", "") + "/logs";
			}
			directory = found;
		}

		if (directory.isEmpty()) {
			logDirectory = null;
		} else {
			logDirectory = new File(directory);
			if (logDirectory.isDirectory()) {
				logDirectory.mkdirs();
			}
		}

		if (prefix == null) {
			prefix = getProperty(className + ".prefix", "moleculer-");
		}

		// Set 'compress after days' property
		String day = getProperty(className + ".compressAfter", "");
		if (!day.isEmpty()) {
			if (!day.endsWith("days")) {
				throw new IllegalArgumentException("The \"compressAfter\" must be in days (eg. \"14 days\")!");
			}
			compressAfterDays = Integer.parseInt(day.substring(0, day.length() - 4).trim());
		}

		// Set "delete after days" property
		day = getProperty(className + ".deleteAfter", "");
		if (!day.isEmpty()) {
			if (!day.endsWith("days")) {
				throw new IllegalArgumentException("The \"deleteAfter\" must be in days (eg. \"14 days\")!");
			}
			deleteAfterDays = Integer.parseInt(day.substring(0, day.length() - 4).trim());
			if (deleteAfterDays < compressAfterDays || compressAfterDays < 2 || deleteAfterDays < 2) {
				throw new IllegalArgumentException("Invalid values (delete logs after " + deleteAfterDays
						+ " days, compress logs after " + compressAfterDays + " days)!");
			}
		}

		// Set formatter
		String formatterName = getProperty(className + ".formatter", FastLogFormatter.class.getName());
		Formatter f = null;
		try {
			f = (Formatter) cl.loadClass(formatterName).newInstance();
			setFormatter(f);
		} catch (Exception ignored) {
			f = new SimpleFormatter();
			setFormatter(f);
		}

		// Set file encoding
		fileEncoding = getProperty(className + ".encoding", "UTF8");

		// Log to console
		logToConsole = Boolean.parseBoolean(getProperty(className + ".logToConsole", "false"));

		// Set level
		setLevel(Level.parse(getProperty(className + ".level", Level.INFO.toString())));

		// Print log directory
		if (logDirectory != null) {
			try {
				LogRecord record = new LogRecord(Level.INFO,
						"Directory of log files: " + logDirectory.getCanonicalPath());
				record.setSourceClassName(AsyncFileLogger.class.getName());
				publish(record);
			} catch (Exception cause) {
				cause.printStackTrace();
			}
		}
	}

	private static final String getProperty(String name, String defaultValue) {
		String value = LogManager.getLogManager().getProperty(name);
		if (value == null) {
			value = defaultValue;
		} else {
			value = value.trim();
		}
		return value;
	}

	private static final String findLogDirectory(File dir) {
		if (dir == null || !dir.exists()) {
			return null;
		}
		File test = new File(dir, "logs");
		if (test.isDirectory()) {
			return test.getAbsolutePath();
		}
		File parent = dir.getParentFile();
		if (parent != null) {
			String found = findLogDirectory(parent);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

}