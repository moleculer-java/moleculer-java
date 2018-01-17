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
package services.moleculer.internal;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

/**
 * The broker contains some internal services to check the health of node or get
 * broker statistics. You can disable it with the internalServices: false broker
 * option within the constructor.
 */
@Name("$node")
public class InternalService extends Service {

	// --- ACTIONS ---

	/**
	 * This actions lists all connected nodes.
	 */
	public Action list = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

	/**
	 * This action lists all registered services (local & remote).
	 */
	public Action services = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

	/**
	 * This action lists all registered actions.
	 */
	public Action actions = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

	/**
	 * This action lists all event subscriptions.
	 */
	public Action events = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

	/**
	 * This action returns the health info of process & OS.
	 */
	public Action health = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

	/**
	 * This action returns the request statistics if the statistics is enabled
	 * in options.
	 */
	public Action stats = (ctx) -> {
		Tree message = new Tree();

		return message;
	};

}