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
package services.moleculer.serializer;

import services.moleculer.service.Name;

/**
 * <b>MessagePack serializer</b><br>
 * <br>
 * MessagePack is an efficient binary serialization format. It lets you exchange
 * data among multiple languages like JSON. But it's faster and smaller. Small
 * integers are encoded into a single byte, and typical short strings require
 * only one extra byte in addition to the strings themselves. This serializer is
 * COMPATIBLE with the JavaScript/Node version of Moleculer.<br>
 * <br>
 * <b>Required dependency:</b><br>
 * <br>
 * https://mvnrepository.com/artifact/org.msgpack/msgpack<br>
 * compile group: 'org.msgpack', name: 'msgpack', version: '0.6.12'
 * 
 * @see JsonSerializer
 */
@Name("MessagePack Serializer")
public class MsgPackSerializer extends Serializer {

	// --- CONSTRUCTOR ---

	public MsgPackSerializer() {
		super("msgpack");
	}

}