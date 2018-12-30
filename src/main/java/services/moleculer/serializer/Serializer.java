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
import io.datatree.dom.TreeReader;
import io.datatree.dom.TreeReaderRegistry;
import io.datatree.dom.TreeWriter;
import io.datatree.dom.TreeWriterRegistry;
import services.moleculer.ServiceBroker;
import services.moleculer.service.MoleculerComponent;
import services.moleculer.util.CheckedTree;

/**
 * Base superclass of all data serializer implementations.
 *
 * @see JsonSerializer
 * @see MsgPackSerializer
 */
public abstract class Serializer extends MoleculerComponent {

	// --- PROPERTIES ---

	/**
	 * Name of the format (eg. "json").
	 */
	protected final String format;

	/**
	 * Data serializer (eg. Tree -> JSON, comes from the "datatree-adapters"
	 * pack).
	 */
	protected TreeWriter writer;

	/**
	 * Data deserializer (eg. JSON -> Tree, comes from the "datatree-adapters"
	 * pack).
	 */
	protected TreeReader reader;

	// --- CONSTRUCTOR ---

	protected Serializer(String format) {
		this.format = format;

		// Init implementations
		this.writer = TreeWriterRegistry.getWriter(format);
		this.reader = TreeReaderRegistry.getReader(format);
	}

	// --- INSTANCE STARTED ---

	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		
		// Upgrade implementations (maybe changed)
		this.writer = TreeWriterRegistry.getWriter(format);
		this.reader = TreeReaderRegistry.getReader(format);
	}

	// --- SERIALIZE TREE TO BYTE ARRAY ---

	public byte[] write(Tree value) throws Exception {
		return writer.toBinary(value.asObject(), null, true);
	}

	// --- DESERIALIZE BYTE ARRAY TO TREE ---

	public Tree read(byte[] source) throws Exception {
		return new CheckedTree(reader.parse(source));
	}

	// --- GET FORMAT NAME ---

	public String getFormat() {
		return format;
	}

}