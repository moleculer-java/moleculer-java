/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
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
package services.moleculer.repl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Creates and formats a text table, what contains two coloumns with headers.
 */
public class StringTable {

	// --- PROPERTIES ---

	protected LinkedList<String[]> rows = new LinkedList<String[]>();
	protected String caption;
	protected String left;
	protected String right;

	// --- CONSTRUCTOR ---

	public StringTable(String tablCaption, String leftHeader, String rightHeader) {
		caption = tablCaption;
		left = leftHeader;
		right = rightHeader;
	}

	// --- TABLE METHODS ---

	public void addRow(String left, String right) {
		String[] row = new String[2];
		row[0] = String.valueOf(left);
		row[1] = String.valueOf(right);
		rows.addLast(row);
	}

	public boolean isEmpty() {
		return rows.isEmpty();
	}

	@Override
	public String toString() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(baos, true, "UTF8");
			printTable(out);
			return new String(baos.toByteArray(), "UTF8");
		} catch (Exception e) {

			// Never happens
			e.printStackTrace();
		}
		return super.toString();
	}

	public void printTable(PrintStream out) {
		out.print(caption);
		out.println(':');
		out.println();

		// Compute width
		int[] lengths = new int[2];
		int fullLength = 0;
		Iterator<String[]> r;
		for (int c = 0; c < 2; c++) {
			r = rows.iterator();
			int maxLength = c == 0 ? left.length() : right.length();
			while (r.hasNext()) {
				String[] row = (String[]) r.next();
				int len = String.valueOf(row[c]).length();
				if (len > maxLength) {
					maxLength = len;
				}
			}
			fullLength = fullLength + maxLength;
			if (c == 0) {
				fullLength += 3;
			}
			if (fullLength > 75) {
				maxLength = maxLength - fullLength + 75;
				if (maxLength < 10) {
					maxLength = 10;
				}
			}
			lengths[c] = maxLength;
		}

		// Headers
		out.print("  ");
		appendString(out, left, lengths[0], 0);
		out.print("   ");
		out.println(right);
		out.print("  ");
		for (int l0 = 0; l0 < lengths[0]; l0++) {
			out.print('-');
		}
		out.print("   ");
		for (int l1 = 0; l1 < lengths[1]; l1++) {
			out.print('-');
		}
		out.println();

		// Rows
		r = rows.iterator();
		while (r.hasNext()) {
			String[] row = (String[]) r.next();
			out.print("  ");
			appendString(out, row[0], lengths[0], fullLength);
			fullLength = lengths[0] + 3;
			out.print(" - ");
			if (row[1].length() < lengths[1]) {
				out.print(row[1]);
			} else {
				appendString(out, row[1], lengths[1], fullLength);
			}
			out.println();
		}
	}

	protected void appendString(PrintStream out, String originalString, int newLength, int leaderSpaces) {
		if (originalString.indexOf('\n') == -1) {
			int originalLength = originalString.length();
			if (originalLength == newLength) {
				out.print(originalString);
			} else {
				if (originalLength > newLength) {
					out.println(originalString.substring(0, newLength));
					String nextRow = originalString.substring(newLength);
					for (;;) {
						for (int l = 0; l < leaderSpaces + 2; l++) {
							out.print(' ');
						}
						String currentRow;
						if (nextRow.length() > newLength) {
							currentRow = nextRow.substring(0, newLength);
						} else {
							currentRow = nextRow;
						}
						out.print(currentRow);
						if (nextRow.length() > newLength) {
							nextRow = nextRow.substring(newLength);
							out.println();
						} else {
							break;
						}
					}
				} else {
					out.print(originalString);
					int missingLength = newLength - originalLength;
					for (int l = 0; l < missingLength; l++) {
						out.print(' ');
					}
				}
			}
		} else {
			out.println();
			out.print(originalString);
		}
	}

}