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
package services.moleculer.metrics;

import java.util.Objects;
import java.util.Properties;

public abstract class AbstractMetric {

	// --- PROPERTIES ---

	protected final String name;
	protected final String description;
	protected final String[] tagValuePairs;

	protected Properties cachedProps;

	// --- CONSTRUCTOR ---

	public AbstractMetric(String name, String description, String[] tagValuePairs) {
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.tagValuePairs = tagValuePairs;
	}

	// --- COUNTER METHODS ---

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String[] getTagsAsArray() {
		return tagValuePairs;
	}

	public Properties getTagsAsProperties() {
		if (cachedProps == null) {
			if (tagValuePairs != null && tagValuePairs.length > 1) {
				for (int i = 0; i < tagValuePairs.length; i += 2) {
					cachedProps.setProperty(tagValuePairs[i], tagValuePairs[i + 1]);
				}
			}
		}
		return cachedProps;
	}

	public String getTagValue(String tagName) {
		return getTagsAsProperties().getProperty(tagName);
	}

	public boolean hasTag(String tagName) {
		return getTagValue(tagName) != null;
	}

	public boolean hasTag(String tagName, String tagValue) {
		String value = getTagValue(tagName);
		if (tagValue == null) {
			return value == null;
		}
		if (value == null) {
			return false;
		}
		return tagValue.equals(value);
	}
	
}