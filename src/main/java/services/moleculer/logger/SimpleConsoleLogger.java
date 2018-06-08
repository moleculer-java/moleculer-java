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

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Simple, System.out-based logger.
 */
public class SimpleConsoleLogger implements ConsoleLogger {

	// --- CONSTANTS ---

	protected static final char[] SEVERE = "SEVERE  ".toCharArray();
	protected static final char[] WARNING = "WARNING ".toCharArray();
	protected static final char[] INFO = "INFO    ".toCharArray();
	protected static final char[] CONFIG = "CONFIG  ".toCharArray();
	protected static final char[] FINE = "FINE    ".toCharArray();
	protected static final char[] FINER = "FINER   ".toCharArray();
	protected static final char[] FINEST = "FINEST  ".toCharArray();
	
	// --- LOGGER ---
	
	@Override
	public void log(LinkedList<LogRecord> records, StringBuilder lines) {
		Throwable cause;
		String msg;
		for (LogRecord record : records) {
			lines.setLength(0);
			final Level l = record.getLevel();
			if (l == Level.SEVERE) {
				lines.append(SEVERE);
			} else if (l == Level.WARNING) {
				lines.append(WARNING);
			} else if (l == Level.INFO) {
				lines.append(INFO);
			} else if (l == Level.CONFIG) {
				lines.append(CONFIG);
			} else if (l == Level.FINE) {
				lines.append(FINE);
			} else if (l == Level.FINER) {
				lines.append(FINER);
			} else {
				lines.append(FINEST);
			}
			msg = record.getMessage();
			if (msg == null) {
				msg = "<null>";
			} else {
				msg = msg.trim();
			}
			System.out.println(lines.append(msg).toString());
			cause = record.getThrown();
			if (cause != null) {
				cause.printStackTrace();
			}
		}
	}
	
}