/**
 * This software is licensed under MIT license.<br>
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
package services.moleculer.context;

import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.eventbus.EventBus;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.ServiceRegistry;
import services.moleculer.util.ParseResult;

/**
 * Invocation context of Actions.
 */
public final class Context {

	// --- PROPERTIES ---

	private final ServiceRegistry registry;
	private final EventBus eventbus;
	private final String id;
	private final String name;
	private final Tree params;
	private final CallingOptions.Options opts;

	// --- CONSTRUCTOR ---

	public Context(ServiceRegistry registry, EventBus eventbus, String id, String name, Tree params,
			CallingOptions.Options opts) {
		this.registry = registry;
		this.eventbus = eventbus;
		this.id = id;
		this.name = name;
		this.params = params;
		this.opts = opts;
	}

	// --- VARIABLE GETTERS ---

	public final String id() {
		return id;
	}

	public final String name() {
		return name;
	}

	public final Tree params() {
		return params;
	}

	public final CallingOptions.Options opts() {
		return opts;
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote)
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		CallingOptions.Options opts = res.opts();
		String targetID = opts == null ? null : opts.nodeID();
		return registry.getAction(name, targetID).call(res.data(), opts, res.parent());
	}

	public Promise call(String name, Tree params) {
		return registry.getAction(name, null).call(params, (CallingOptions.Options) null, this);
	}

	public Promise call(String name, Tree params, CallingOptions.Options opts) {
		String targetID = opts == null ? null : opts.nodeID();
		return registry.getAction(name, targetID).call(params, opts, this);
	}

	// --- EMIT EVENT TO EVENT GROUP ---

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.emit(name, res.data(), res.groups());
	}

	/**
	 * Emits an event (grouped & balanced global event)
	 */
	public void emit(String name, Tree payload, Groups groups) {
		eventbus.emit(name, payload, groups);
	}

	// --- BROADCAST EVENT TO ALL LISTENERS ---

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data(), res.groups());
	}

	/**
	 * Emits an event for all local & remote services
	 */
	public void broadcast(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups);
	}

	// --- BROADCAST EVENT TO LOCAL LISTENERS ---

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcastLocal(name, res.data(), res.groups());
	}

	/**
	 * Emits an event for all local services.
	 */
	public void broadcastLocal(String name, Tree payload, Groups groups) {
		eventbus.broadcastLocal(name, payload, groups);
	}

}