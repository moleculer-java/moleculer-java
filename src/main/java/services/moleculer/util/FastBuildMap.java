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
package services.moleculer.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This Map is optimized for fast building and serialization.
 */
public class FastBuildMap extends AbstractMap<String, Object> {

	// --- PROPERTIES ---
	
	protected int size;
	
	protected final String[] keys;
	protected final Object[] values;

	// --- CONSTRUCTORS ---
	
	protected FastBuildMap() {
		this(16);
	}

	protected FastBuildMap(int size) {
		keys = new String[size];
		values = new Object[size];
	}

	// --- MAP FUNCTIONS ---
	
	/**
	 * WARNING: All key must be unique!
	 */
	public Object put(String key, Object value) {
		if (size == keys.length) {
			throw new ArrayIndexOutOfBoundsException(size);
		}
		keys[size] = key;
		values[size] = value;
		size++;
		return value;
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return new FastBuildEntrySet(size, keys, values);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i = 0; i < size; i++) {
			if (value.equals(values[i])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		for (int i = 0; i < size; i++) {
			if (key.equals(keys[i])) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object get(Object key) {
		for (int i = 0; i < size; i++) {
			if (key.equals(keys[i])) {
				return values[i];
			}
		}
		return null;
	}

	@Override
	public Object remove(Object key) {
		
		// Ignored
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void putAll(Map m) {
		Set<Map.Entry> entries = m.entrySet();
		for (Map.Entry entry : entries) {
			put(String.valueOf(entry.getKey()), entry.getValue());
		}
	}

	@Override
	public void clear() {
		size = 0;
	}

	@Override
	public Set<String> keySet() {
		LinkedHashSet<String> set = new LinkedHashSet<>(values.length * 2);
		for (int i = 0; i < size; i++) {
			set.add(keys[i]);
		}
		return set;
	}

	@Override
	public Collection<Object> values() {
		ArrayList<Object> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(keys[i]);
		}
		return list;
	}

	public static final class FastBuildEntrySet extends AbstractSet<Entry<String, Object>> {

		private final int size;
		private final String[] keys;
		private final Object[] values;

		public FastBuildEntrySet(int size, String[] keys, Object[] values) {
			this.size = size;
			this.keys = keys;
			this.values = values;
		}

		@Override
		public final Iterator<Entry<String, Object>> iterator() {
			return new Iterator<Entry<String, Object>>() {

				private int pos = -1;

				@Override
				public final boolean hasNext() {
					return pos < size - 1;
				}

				@Override
				public final Entry<String, Object> next() {
					pos++;
					return new Entry<String, Object>() {

						@Override
						public String getKey() {
							return keys[pos];
						}

						@Override
						public Object getValue() {
							return values[pos];
						}

						@Override
						public Object setValue(Object value) {
							Object prev = values[pos];
							values[pos] = value;
							return prev;
						}

					};
				}

			};
		}

		@Override
		public final int size() {
			return size;
		}

	}

}