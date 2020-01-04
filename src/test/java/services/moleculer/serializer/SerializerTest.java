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
package services.moleculer.serializer;

import io.datatree.Tree;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import io.datatree.dom.adapters.JsonIon;
import services.moleculer.cacher.KeyTest;

public abstract class SerializerTest extends KeyTest {

	protected final Serializer serializer;

	public SerializerTest() {
		serializer = createSerializer();
		serializer.setDebug(true);
	}

	protected abstract Serializer createSerializer();

	@Override
	protected void check(String json, String key, String[] keys, int maxKeyLength) throws Exception {
		json = json.replace('\'', '\"');
		Tree v1 = new Tree(json, "JsonBuiltin");
		byte[] bytes = serializer.write(v1);
		Tree v2 = serializer.read(bytes);
		String json2 = v2.toString(false);
		assertJsonEquals(json, json2);
	}

	private static final void assertJsonEquals(String s1, String s2) {
		if (s1 != null) {
			s1 = removeFormatting(s1);
		}
		if (s2 != null) {
			s2 = removeFormatting(s2);
		}
		assertEquals(s1, s2);
	}

	private static final String removeFormatting(String txt) {
		Class<? extends TreeWriter> writerClass = TreeWriterRegistry.getWriter(TreeWriterRegistry.JSON).getClass();
		if (writerClass == JsonIon.class) {
			txt = txt.replace("\"", "");
			txt = txt.replace("e0,", ",");
		}
		return txt.replace("\t", " ").replace("\r", " ").replace("\n", " ").replace(" ", "").replace(".0", "");
	}

}