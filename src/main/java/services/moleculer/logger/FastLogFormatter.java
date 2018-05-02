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
package services.moleculer.logger;

import static services.moleculer.logger.AsyncFileLogger.CONFIG;
import static services.moleculer.logger.AsyncFileLogger.FINE;
import static services.moleculer.logger.AsyncFileLogger.FINER;
import static services.moleculer.logger.AsyncFileLogger.FINEST;
import static services.moleculer.logger.AsyncFileLogger.INFO;
import static services.moleculer.logger.AsyncFileLogger.SEVERE;
import static services.moleculer.logger.AsyncFileLogger.WARNING;

import java.time.Instant;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Fast log formatter for AsyncFileLogger. Generates nice, human-readable logs.
 * The lines of the generated log file are readable by humans and machines.
 */
public class FastLogFormatter extends Formatter {

	// --- CONSTANTS ---

	protected static final char[] BREAK = System.getProperty("line.separator", "\r\n").toCharArray();
	protected static final String BREAK_STRING = new String(BREAK);
	protected static final char[] AT = " at ".toCharArray();
	protected static final char[] JAVA = ".java:".toCharArray();

	// --- PROPERTIES ---

	protected final StringBuilder line = new StringBuilder(512);

	protected volatile int position = 83;

	// --- FORMATTER ---

	public String format(LogRecord record) {
		line.setLength(0);
		line.append('[');
		line.append(Instant.ofEpochMilli(record.getMillis()).toString());

		final Level l = record.getLevel();
		line.append("] ");
		if (l == Level.SEVERE) {
			line.append(SEVERE);
		} else if (l == Level.WARNING) {
			line.append(WARNING);
		} else if (l == Level.INFO) {
			line.append(INFO);
		} else if (l == Level.CONFIG) {
			line.append(CONFIG);
		} else if (l == Level.FINE) {
			line.append(FINE);
		} else if (l == Level.FINER) {
			line.append(FINER);
		} else {
			line.append(FINEST);
		}

		String className = record.getSourceClassName();
		int n;
		if (className == null) {
			className = "unknown";
		} else {
			n = className.lastIndexOf('$');
			if (n > -1) {
				className = className.substring(0, n);
			}
		}
		line.append(className);
		n = line.length();
		if (n > position || position - n > 30) {
			position = n;
		}
		n = 1 + position - n;
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				line.append(' ');
			}
		}
		line.append(formatMessage(record));
		line.append(BREAK);

		// Dump error
		final Throwable cause = record.getThrown();
		if (cause != null) {
			StringBuilder errors = new StringBuilder(256);
			n = appendMessages(errors, cause);
			String trace = errors.toString().trim();
			if (!trace.isEmpty()) {
				for (int i = 0; i < n; i++) {
					line.append('-');
				}
				line.append(BREAK);
				line.append(trace);
				line.append(BREAK);
				for (int i = 0; i < n; i++) {
					line.append('-');
				}
				line.append(BREAK);
			}
			line.append("Trace: ");
			line.append(cause.toString());
			line.append(BREAK);
			StackTraceElement[] array = cause.getStackTrace();
			if (array != null && array.length > 0) {
				for (n = 0; n < array.length; n++) {
					line.append("-> [");
					line.append(n);
					line.append("] ");
					line.append(array[n]);
					line.append(BREAK);
				}
			}
		}
		return line.toString();
	}

	protected int appendMessages(StringBuilder errors, Throwable t) {
		int max = 0;
		try {
			LinkedList<String> list = new LinkedList<>();
			appendMessages(list, t, 0);
			int n;
			for (String line : list) {
				errors.append(line);
				n = line.length();
				if (n > max) {
					max = n;
				}
				errors.append(BREAK);
			}
		} catch (Exception ignored) {
		}
		return max;
	}

	protected void appendMessages(LinkedList<String> list, Throwable t, int level) throws Exception {
		if (level > 10) {
			return;
		}
		String msg = t.getMessage();
		if (msg != null) {
			msg = msg.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
			if (!msg.isEmpty()) {
				msg = msg.toUpperCase();
				msg = msg.replace("NESTED EXCEPTION IS", BREAK_STRING);
				StringTokenizer st = new StringTokenizer(msg, BREAK_STRING);
				while (st.hasMoreTokens()) {
					String line = st.nextToken();
					line = normalizeLine(line);
					if (!line.isEmpty()) {
						boolean found = false;
						for (String test : list) {
							if (test.contains(line)) {
								found = true;
								break;
							}
						}
						if (!found) {
							list.add(line);
						}
					}
				}
			}
		}
		Throwable cause = t.getCause();
		if (cause != null) {
			appendMessages(list, cause, level + 1);
		}
	}

	protected String normalizeLine(String line) throws Exception {
		StringBuilder tmp = new StringBuilder(line.length());
		line = line.trim();
		if (line.endsWith(";") || line.endsWith(":")) {
			line = line.substring(0, line.length() - 1);
		}
		StringTokenizer st = new StringTokenizer(line);
		String previous = "";
		while (st.hasMoreTokens()) {
			String word = normalizeWord(st.nextToken().trim());
			if (!word.isEmpty()) {
				if (previous.equals(word)) {
					continue;
				}
				previous = word;
				if (tmp.length() > 0) {
					tmp.append(' ');
				}
				tmp.append(word);
			}
		}
		return tmp.toString().trim();
	}

	protected String normalizeWord(String word) throws Exception {
		if (word.startsWith("ORG.") || word.startsWith("COM.") || word.startsWith("JAVA.")
				|| word.startsWith("JAVAX.")) {
			int i = word.lastIndexOf('.');
			word = word.substring(i + 1);
			i = word.indexOf("EXCEPTION");
			if (i > 1) {
				word = word.substring(0, i) + ' ' + word.substring(i);
			}
			return word;
		}
		if (word.startsWith("[JAR:") && word.endsWith("]:")) {
			int i = word.lastIndexOf('/');
			if (i > 1) {
				return "[JAR:..." + word.substring(i);
			}
			return "[JAR:...]";
		}
		return word;
	}

	protected void dump(Throwable cause, int level, int lineLength) {
		if (level == 0) {
			for (int i = 0; i < lineLength; i++) {
				line.append('-');
			}
			line.append(BREAK);
		}
		String msg = cause.getMessage();
		if (msg == null || msg.isEmpty()) {
			msg = cause.toString();
		}
		line.append(msg.trim());
		line.append(BREAK);
		line.append(BREAK);
		StackTraceElement[] elements = cause.getStackTrace();
		String className;
		int n;
		for (StackTraceElement element : elements) {
			line.append(AT);
			className = element.getClassName();
			line.append(className);
			line.append('.');
			line.append(element.getMethodName());
			line.append('(');
			n = className.lastIndexOf('.');
			if (n > -1) {
				line.append(className.substring(n + 1));
			} else {
				line.append(className);
			}
			line.append(JAVA);
			line.append(element.getLineNumber());
			line.append(')');
			line.append(BREAK);
		}
		cause = cause.getCause();
		if (level < 5 && cause != null) {
			line.append(BREAK);
			dump(cause, ++level, lineLength);
		} else {
			for (int i = 0; i < lineLength; i++) {
				line.append('-');
			}
			line.append(BREAK);
		}
	}

}