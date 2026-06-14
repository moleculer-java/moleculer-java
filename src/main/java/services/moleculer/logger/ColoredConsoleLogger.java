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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

/**
 * "Colorized" ANSI logger.
 */
public class ColoredConsoleLogger implements ConsoleLogger {

	// --- CONSTANTS ---

	protected static final String SEVERE = "SEVERE  ";
	protected static final String WARNING = "WARNING ";
	protected static final String INFO = "INFO    ";
	protected static final String CONFIG = "CONFIG  ";
	protected static final String FINE = "FINE    ";
	protected static final String FINER = "FINER   ";
	protected static final String FINEST = "FINEST  ";

	// --- MESSAGE COLOR ---

	protected static final Attribute MESSAGE_COLOR = Attribute.BRIGHT_WHITE_TEXT();

	// --- CONSTRUCTOR ---

	public ColoredConsoleLogger() {
	}

	// --- LOGGER ---

	@Override
	public synchronized void log(LinkedList<LogRecord> records, StringBuilder lines) {
		Throwable cause;
		String msg;
		for (LogRecord record : records) {
			final Level l = record.getLevel();
			final String label;
			final Attribute labelColor;
			if (l == Level.SEVERE) {
				label = SEVERE;
				labelColor = Attribute.BRIGHT_RED_TEXT();
			} else if (l == Level.WARNING) {
				label = WARNING;
				labelColor = Attribute.BRIGHT_YELLOW_TEXT();
			} else if (l == Level.INFO) {
				label = INFO;
				labelColor = Attribute.BRIGHT_GREEN_TEXT();
			} else if (l == Level.CONFIG) {
				label = CONFIG;
				labelColor = Attribute.CYAN_TEXT();
			} else if (l == Level.FINE) {
				label = FINE;
				labelColor = Attribute.MAGENTA_TEXT();
			} else if (l == Level.FINER) {
				label = FINER;
				labelColor = Attribute.BLUE_TEXT();
			} else {
				label = FINEST;
				labelColor = Attribute.RED_TEXT();
			}
			msg = record.getMessage();
			if (msg != null) {
				msg = msg.trim();
			}
			if (msg == null || msg.isEmpty()) {
				msg = "<null>";
			}
			System.out.println(Ansi.colorize(label, labelColor) + Ansi.colorize(msg, MESSAGE_COLOR));

			cause = record.getThrown();
			if (cause != null) {
				StringWriter sw = new StringWriter();
				cause.printStackTrace(new PrintWriter(sw));
				System.out.print(sw.toString());
				System.out.flush();
			}
		}
	}

}
