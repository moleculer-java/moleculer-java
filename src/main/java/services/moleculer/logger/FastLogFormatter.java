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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Fast log formatter for AsyncFileLogger. Generates nice, human-readable logs.
 * The lines of the generated log file are readable by humans and machines.
 */
public class FastLogFormatter extends Formatter {

	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	protected static final char[] SEVERE = " | SEVERE  | ".toCharArray();
	protected static final char[] WARNING = " | WARNING | ".toCharArray();
	protected static final char[] INFO = " | INFO    | ".toCharArray();
	protected static final char[] CONFIG = " | CONFIG  | ".toCharArray();
	protected static final char[] FINE = " | FINE    | ".toCharArray();
	protected static final char[] FINER = " | FINER   | ".toCharArray();
	protected static final char[] FINEST = " | FINEST  | ".toCharArray();

	protected static final char[] TUBE = " | ".toCharArray();
	protected static final char[] BREAK = System.getProperty("line.separator", "\r\n").toCharArray();
	protected static final char[] AT = " at ".toCharArray();
	protected static final char[] JAVA = ".java:".toCharArray();

	protected final StringBuilder line = new StringBuilder(512);

	protected volatile int position = 83;

	public String format(LogRecord record) {
		line.setLength(0);
		line.append(DATE_FORMAT.format(new Date(record.getMillis())));

		final Level l = record.getLevel();
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
		n = position - n;
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				line.append(' ');
			}
		}
		line.append(TUBE);
		line.append(formatMessage(record));

		final Throwable cause = record.getThrown();
		if (cause != null) {
			n = line.length();
			line.append(BREAK);
			dump(cause, 0, n);
		} else {
			line.append(BREAK);
		}
		return line.toString();
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
