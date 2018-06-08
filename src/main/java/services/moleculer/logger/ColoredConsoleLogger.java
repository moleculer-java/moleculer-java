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

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

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

	// --- ANSI CONSOLE ---

	protected final ColoredPrinter coloredPrinter;

	// --- CONSTRUCTOR ---

	public ColoredConsoleLogger() {
		coloredPrinter = new ColoredPrinter.Builder(1, false).build();
	}

	// --- LOGGER ---

	@Override
	public synchronized void log(LinkedList<LogRecord> records, StringBuilder lines) {
		Throwable cause;
		String msg;
		for (LogRecord record : records) {
			final Level l = record.getLevel();
			if (l == Level.SEVERE) {

				coloredPrinter.print(SEVERE, Attribute.LIGHT, FColor.RED, BColor.NONE);

			} else if (l == Level.WARNING) {

				coloredPrinter.print(WARNING, Attribute.LIGHT, FColor.YELLOW, BColor.NONE);

			} else if (l == Level.INFO) {

				coloredPrinter.print(INFO, Attribute.LIGHT, FColor.GREEN, BColor.NONE);

			} else if (l == Level.CONFIG) {

				coloredPrinter.print(CONFIG, Attribute.CLEAR, FColor.CYAN, BColor.NONE);

			} else if (l == Level.FINE) {

				coloredPrinter.print(FINE, Attribute.CLEAR, FColor.MAGENTA, BColor.NONE);

			} else if (l == Level.FINER) {

				coloredPrinter.print(FINER, Attribute.CLEAR, FColor.BLUE, BColor.NONE);

			} else {

				coloredPrinter.print(FINEST, Attribute.CLEAR, FColor.RED, BColor.NONE);

			}
			msg = record.getMessage();
			if (msg == null) {
				msg = "<null>";
			} else {
				msg = msg.trim();
			}
			coloredPrinter.println(msg, Attribute.LIGHT, FColor.WHITE, BColor.NONE);
			coloredPrinter.clear();

			cause = record.getThrown();
			if (cause != null) {
				cause.printStackTrace();
			}
		}

	}

}