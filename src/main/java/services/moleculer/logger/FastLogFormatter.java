package services.moleculer.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Fast, single-threaded log formatter for AsyncFileLogger.
 */
public final class FastLogFormatter extends Formatter {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private static final char[] SEVERE = " | SEVERE  | ".toCharArray();
	private static final char[] WARNING = " | WARNING | ".toCharArray();
	private static final char[] INFO = " | INFO    | ".toCharArray();
	private static final char[] CONFIG = " | CONFIG  | ".toCharArray();
	private static final char[] FINE = " | FINE    | ".toCharArray();
	private static final char[] FINER = " | FINER   | ".toCharArray();
	private static final char[] FINEST = " | FINEST  | ".toCharArray();

	private static final char[] TUBE = " | ".toCharArray();
	private static final char[] BREAK = System.getProperty("line.separator", "\r\n").toCharArray();

	private static final char[] ERROR_AT_LINE = " at line ".toCharArray();
	private static final char[] ERROR_IN = " in ".toCharArray();
	private static final char[] ERROR_BRACKETS = "()".toCharArray();
	private static final char[] ERROR_LINE = new char[120];
	
	static {
		for (int i = 0; i < ERROR_LINE.length; i++) {
			ERROR_LINE[i] = '-';
		}
		ERROR_LINE[24] = '+';
		ERROR_LINE[34] = '+';
		ERROR_LINE[87] = '+';
	}
	
	private final StringBuilder line = new StringBuilder(512);

	public final String format(LogRecord record) {
		line.setLength(0);
		line.append(DATE_FORMAT.format(new Date(record.getMillis())));
		Level l = record.getLevel();
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
		if (className == null) {
			className = "unknown";
		} else {
			int i = className.lastIndexOf('$');
			if (i > -1) {
				className = className.substring(0, i);
			}
		}
		line.append(className);
		if (line.length() < 86) {
			int spaces = 86 - line.length();
			for (int i = 0; i < spaces; i++) {
				line.append(' ');
			}
		}
		line.append(TUBE);
		line.append(formatMessage(record));
		line.append(BREAK);
		Throwable t = record.getThrown();
		if (t != null) {
			dump(t);
		}
		return line.toString();
	}
	
	private final void dump(Throwable t) {
		line.append(ERROR_LINE);
		line.append(BREAK);
		String msg = t.getMessage();
		if (msg == null || msg.isEmpty()) {
			msg = t.toString();
		}
		line.append(msg.trim());
		line.append(BREAK);
		line.append(BREAK);
		StackTraceElement[] elements = t.getStackTrace();
		for (StackTraceElement element: elements) {
			line.append(ERROR_AT_LINE);
			int num = element.getLineNumber();
			line.append(num);
			line.append('.');
			if (num < 10) {
				line.append(' ');
			}
			if (num < 100) {
				line.append(' ');
			}
			if (num < 1000) {
				line.append(' ');
			}
			line.append(ERROR_IN);
			line.append(element.getClassName());
			line.append('.');
			line.append(element.getMethodName());
			line.append(ERROR_BRACKETS);
			line.append(BREAK);
		}
		line.append(ERROR_LINE);
		line.append(BREAK);
	}
	
}