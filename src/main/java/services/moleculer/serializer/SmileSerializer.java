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

import services.moleculer.service.Name;

/**
 * <b>Smile Serializer</b><br>
 * <br>
 * Smile is a computer data interchange format based on JSON. It can also be
 * considered as a binary serialization of generic JSON data model, which means
 * that tools that operate on JSON may be used with Smile as well, as long as
 * proper encoder/decoder exists for tool to use. Compared to JSON, Smile is
 * both more compact and more efficient to process (both to read and write).<br>
 * <br>
 * It is the FASTEST serializer, but it is NOT compatible with the
 * JavaScript/Node version of Moleculer. Sample of usage:
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * trans.setSerializer(new SmileSerializer());
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(trans)
 *                                     .build();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/
 * jackson-dataformat-smile<br>
 * compile group: 'com.fasterxml.jackson.dataformat', name:
 * 'jackson-dataformat-smile', version: '2.10.0'
 *
 * @see JsonSerializer
 * @see MsgPackSerializer
 */
@Name("SMILE Serializer")
public class SmileSerializer extends Serializer {

	// --- CONSTRUCTOR ---

	public SmileSerializer() {
		super("smile");
	}

}