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
package services.moleculer.eventbus;

import java.util.regex.Pattern;

import io.datatree.dom.Cache;
import io.datatree.dom.Config;

/**
 * Event mask matcher (eg. "service.event.**").
 */
public final class Matcher {

	// --- CACHE OF COMPILED PATTERNS ---

	private static final Cache<String, Pattern> regexCache = new Cache<>(Config.CACHE_SIZE);

	// --- MATCHER ---

	public static final boolean matches(String text, String pattern) {

		// Simple patterns
		if (pattern.indexOf('?') == -1) {

			// Exact match (eg. "prefix.event")
			final int firstStarPosition = pattern.indexOf('*');
			if (firstStarPosition == -1) {
				return pattern.equals(text);
			}

			// Eg. "prefix**"
			final int len = pattern.length();
			if (len > 2 && pattern.endsWith("**") && firstStarPosition > len - 3) {
				return text.startsWith(pattern.substring(0, len - 2));
			}

			// Eg. "prefix*"
			if (len > 1 && pattern.endsWith("*") && firstStarPosition > len - 2) {
				if (text.startsWith(pattern.substring(0, len - 1))) {
					return text.indexOf('.', len) == -1;
				}
				return false;
			}

			// Accept simple text, without point character (*)
			if (len == 1 && firstStarPosition == 0) {
				return text.indexOf('.') == -1;
			}

			// Accept all inputs (**)
			if (len == 2 && firstStarPosition == 0 && pattern.lastIndexOf('*') == 1) {
				return true;
			}
		}

		// Regex (eg. "prefix.ab?cd.*.foo")
		Pattern regex = regexCache.get(pattern);
		if (regex == null) {
			String regexPattern = pattern;
			if (regexPattern.startsWith("$")) {
				regexPattern = '\\' + regexPattern;
			}
			regexPattern = regexPattern.replace("?", ".");
			regexPattern = regexPattern.replace("**", "§§§");
			regexPattern = regexPattern.replace("*", "[^\\.]*");
			regexPattern = regexPattern.replace("§§§", ".*");
			regex = Pattern.compile("^" + regexPattern + "$");
			regexCache.put(regexPattern, regex);
		}
		return regex.matcher(text).matches();
	}

}
