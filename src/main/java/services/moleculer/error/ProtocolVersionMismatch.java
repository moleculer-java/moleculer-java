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
package services.moleculer.error;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

/**
 * Throw it if an old nodeID connected with older protocol version (error code:
 * 500, retriable: false).
 */
public class ProtocolVersionMismatch extends MoleculerException {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -8517319145071164437L;

	// --- PROPERTIES ---

	protected final String nodeID;
	protected final String actual = ServiceBroker.PROTOCOL_VERSION;
	protected final String received;

	// --- CONSTRUCTORS ---

	public ProtocolVersionMismatch(String type, Tree data, String nodeID, String received) {
		super(false, "Invalid protocol version (" + received + ")!", 500, type, data);
		this.nodeID = nodeID;
		this.received = received;
	}

	// --- PROPERTY GETTERS ---

	public String getNodeID() {
		return nodeID;
	}

	public String getActual() {
		return actual;
	}

	public String getReceived() {
		return received;
	}

}