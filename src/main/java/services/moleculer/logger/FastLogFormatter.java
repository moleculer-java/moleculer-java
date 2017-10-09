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

	private static final char[] SEVERE  = " | SEVERE  | ".toCharArray();
	private static final char[] WARNING = " | WARNING | ".toCharArray();
	private static final char[] INFO    = " | INFO    | ".toCharArray();
	private static final char[] CONFIG  = " | CONFIG  | ".toCharArray();
	private static final char[] FINE    = " | FINE    | ".toCharArray();
	private static final char[] FINER   = " | FINER   | ".toCharArray();
	private static final char[] FINEST  = " | FINEST  | ".toCharArray();
	
	private static final char[] SEPARATOR = " | ".toCharArray();
	
	private static final char[] BREAK = System.getProperty("line.separator", "\r\n").toCharArray();
	
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
		line.append(SEPARATOR);
		line.append(formatMessage(record));
		line.append(BREAK);
		return line.toString();
	}

}