/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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

import services.moleculer.ServiceBroker;

/**
 * Superclass of chainable Serializers. Sample of usage (serialize then compress
 * then encrypt packets):
 * 
 * <pre>
 * Transporter trans = new NatsTransporter("localhost");
 * 
 * MsgPackSerializer msgPack = new MsgPackSerializer();
 * DeflaterSerializer deflater = new DeflaterSerializer(msgPack);
 * BlockCipherSerializer cipher = new BlockCipherSerializer(deflater);
 * 
 * trans.setSerializer(cipher);
 * </pre>
 */
public abstract class ChainedSerializer extends Serializer {

	protected final Serializer parent;

	protected ChainedSerializer(Serializer parent) {
		super("json");
		this.parent = parent;
	}

	// --- INSTANCE STARTED ---

	@Override
	public void started(ServiceBroker broker) throws Exception {
		this.broker = broker;
		parent.started(broker);
	}

	// --- INSTANCE STOPPED ---

	@Override
	public void stopped() {
		parent.stopped();
	}

	// --- GET FORMAT NAME ---

	@Override
	public String getFormat() {
		return parent.getFormat();
	}
	
	// --- DEBUG MODE ---
	
	@Override
	public void setDebug(boolean debug) {
		super.setDebug(debug);
		parent.setDebug(debug);
	}	

}