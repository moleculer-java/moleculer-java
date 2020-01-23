/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.uid;

import services.moleculer.service.Name;

/**
 * Time-based standard UUID generator. The generated UUIDs can be sorted in
 * alphabetical order, which corresponds to a time-based order. If no such
 * sorting is required, use the faster faster {@link IncrementalUidGenerator} in
 * production mode.
 *
 * @see IncrementalUidGenerator
 * @see XorShiftRandomUidGenerator
 */
@Name("Time-based UUID Generator")
public class TimeBasedUidGenerator extends XorShiftRandomUidGenerator {

	// --- GENERATE UID ---

	@Override
	public String nextUID() {
		StringBuilder tmp = new StringBuilder(45);
		tmp.append(Long.toHexString(System.currentTimeMillis()));
		while (tmp.length() < 32) {
			tmp.append(Long.toHexString(nextLong()));
		}
		char[] chars = new char[36];
		tmp.getChars(0, 32, chars, 0);
		System.arraycopy(chars, 8, chars, 9, 35 - 8);
		chars[8] = '-';
		System.arraycopy(chars, 13, chars, 14, 35 - 13);
		chars[13] = '-';
		System.arraycopy(chars, 18, chars, 19, 35 - 18);
		chars[18] = '-';
		System.arraycopy(chars, 23, chars, 24, 35 - 23);
		chars[23] = '-';
		return new String(chars);
	}

}