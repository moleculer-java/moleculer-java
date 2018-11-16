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
package services.moleculer.eventbus;

import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Tree;
import services.moleculer.strategy.Strategy;
import services.moleculer.util.ParseResult;

/**
 * Base superclass of all objects that can send events (eg. ServiceBroker or
 * Context).
 */
public abstract class EventEmitter {

	// --- COMPONENTS ---

	/**
	 * Eventbus of the parent ServiceBroker.
	 */
	protected final Eventbus eventbus;

	// --- CONSTRUCTOR ---

	protected EventEmitter(Eventbus eventbus) {
		this.eventbus = eventbus;
	}

	// --- EMIT EVENT TO EVENT GROUP ---

	/**
	 * Emits an event to <b>ONE</b> listener from ALL (or the specified) event
	 * group(s), who are listening this event. The service broker uses the
	 * default {@link Strategy strategy} of the broker for event redirection and
	 * node selection. Sample code:<br>
	 * <br>
	 * ctx.emit("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) listener group(s):<br>
	 * <br>
	 * ctx.emit("user.deleted", "a", 1, "b", 2, Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void emit(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.emit(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from the specified event group(s),
	 * who are listening this event. The service broker uses the default
	 * {@link Strategy strategy} of the broker for event redirection and node
	 * selection. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.emit("user.created", params, Groups.of("group1", "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void emit(String name, Tree payload, Groups groups) {
		eventbus.emit(name, payload, groups, false);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from ALL event groups, who are
	 * listening this event. The service broker uses the default {@link Strategy
	 * strategy} of the broker for event redirection and node selection. Sample
	 * code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.emit("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void emit(String name, Tree payload) {
		eventbus.emit(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO ALL LISTENERS ---

	/**
	 * Emits an event to <b>ALL</b> listeners from ALL (or the specified) event
	 * group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * ctx.broadcast("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) listener group(s):<br>
	 * <br>
	 * ctx.broadcast("user.deleted", "a", 1, "b", 2, Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void broadcast(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, false);
	}

	/**
	 * Emits an event to <b>ALL</b> listeners from the specified event group(s),
	 * who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.broadcast("user.created", params, Groups.of("group1", "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void broadcast(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, false);
	}

	/**
	 * Emits an event to <b>ALL</b> listeners from ALL event groups, who are
	 * listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.broadcast("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void broadcast(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, false);
	}

	// --- BROADCAST EVENT TO LOCAL LISTENERS ---

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from ALL (or the
	 * specified) event group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * ctx.broadcastLocal("user.deleted", "a", 1, "b", 2);<br>
	 * <br>
	 * ...or send event to (one or more) local listener group(s):<br>
	 * <br>
	 * ctx.broadcastLocal("user.deleted", "a", 1, "b", 2, Groups.of("logger"));
	 * 
	 * @param name
	 *            name of event (eg. "user.deleted")
	 * @param params
	 *            list of parameter name-value pairs and an optional
	 *            {@link Groups event group} container
	 */
	public void broadcastLocal(String name, Object... params) {
		ParseResult res = parseParams(params);
		eventbus.broadcast(name, res.data, res.groups, true);
	}

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from the specified
	 * event group(s), who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.broadcastLocal("user.created", params, Groups.of("group1",
	 * "group2"));
	 * 
	 * @param name
	 *            name of event (eg. "user.modified")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void broadcastLocal(String name, Tree payload, Groups groups) {
		eventbus.broadcast(name, payload, groups, true);
	}

	/**
	 * Emits a <b>LOCAL</b> event to <b>ALL</b> listeners from ALL event groups,
	 * who are listening this event. Sample code:<br>
	 * <br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * ctx.broadcastLocal("user.modified", params);
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param payload
	 *            {@link Tree} structure (payload of the event)
	 */
	public void broadcastLocal(String name, Tree payload) {
		eventbus.broadcast(name, payload, null, true);
	}

}