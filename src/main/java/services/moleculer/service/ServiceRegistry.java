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
package services.moleculer.service;

import java.util.Collection;

import io.datatree.Promise;
import io.datatree.Tree;

/**
 * Base superclass of all Service Registry implementations.
 *
 * @see DefaultServiceRegistry
 */
@Name("Service Registry")
public abstract class ServiceRegistry extends MoleculerComponent {

	// --- RECEIVE REQUEST FROM REMOTE SERVICE ---

	public abstract void receiveRequest(Tree message);

	// --- RECEIVE PING-PONG RESPONSE ---

	public abstract void receivePong(Tree message);

	// --- RECEIVE RESPONSE FROM REMOTE SERVICE ---

	public abstract void receiveResponse(Tree message);

	// --- ADD MIDDLEWARES ---

	public abstract void use(Collection<Middleware> middlewares);

	// --- ADD ACTION OF A LOCAL SERVICE ---

	public abstract Promise addActions(String name, Service service);

	// --- ADD ACTIONS OF A REMOTE SERVICE ---

	public abstract void addActions(String nodeID, Tree config);

	// --- REMOVE ALL ACTIONS OF A NODE ---

	public abstract void removeActions(String nodeID);

	// --- GET LOCAL SERVICE ---

	public abstract Service getService(String name);

	// --- GET LOCAL OR REMOTE ACTION CONTAINER ---

	public abstract Action getAction(String name, String nodeID);

	// --- WAIT FOR SERVICE(S) ---

	public abstract Promise waitForServices(long timeoutMillis, Collection<String> services);

	// --- PING / PONG HANDLING ---

	public abstract Promise ping(long timeoutMillis, String nodeID);

	// --- GENERATE SERVICE DESCRIPTOR ---

	public abstract Tree getDescriptor();

	// --- TIMESTAMP OF SERVICE DESCRIPTOR ---

	public abstract long getTimestamp();

}