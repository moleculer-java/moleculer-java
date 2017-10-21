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
package services.moleculer.eventbus;

/**
 * 
 */
public final class GlobMatcher {

	public static final boolean matches(String text, String pattern) {
		String rest = null;
		int pos = pattern.indexOf('*');
		if (pos != -1) {
			rest = pattern.substring(pos + 1);
			pattern = pattern.substring(0, pos);
		}
		if (pattern.length() > text.length()) {
			return false;
		}

		// Handle the part up to the first *
		for (int i = 0; i < pattern.length(); i++) {
			if (pattern.charAt(i) != '?' && !pattern.substring(i, i + 1).equalsIgnoreCase(text.substring(i, i + 1))) {
				return false;
			}
		}

		// Recurse for the part after the first *, if any
		if (rest == null) {
			return pattern.length() == text.length();
		} else {
			for (int i = pattern.length(); i <= text.length(); i++) {
				if (matches(text.substring(i), rest)) {
					return true;
				}
			}
			return false;
		}
	}

}
