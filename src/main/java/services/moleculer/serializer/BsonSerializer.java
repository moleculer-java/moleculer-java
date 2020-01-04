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
 * <b>BSON Serializer</b><br>
 * <br>
 * Binary BSON reader and writer. This serializer is NOT compatible with the
 * JavaScript/Node version of Moleculer. Sample of usage:<br>
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * trans.setSerializer(new BsonSerializer());
 * ServiceBroker broker = ServiceBroker.builder()
 *                                     .nodeID("node1")
 *                                     .transporter(trans)
 *                                     .build();
 * </pre>
 * 
 * <b>Required dependency:</b><br>
 * <br>
 * https://mvnrepository.com/artifact/de.undercouch/bson4jackson<br>
 * compile group: 'de.undercouch', name: 'bson4jackson', version: '2.9.2'
 * 
 * @see JsonSerializer
 * @see MsgPackSerializer
 */
@Name("BSON Serializer")
public class BsonSerializer extends Serializer {

	// --- CONSTRUCTOR ---

	public BsonSerializer() {
		super("bson");
	}

}