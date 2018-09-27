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
package services.moleculer.context;

import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.error.RequestRejectedError;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.strategy.Strategy;
import services.moleculer.stream.PacketStream;
import services.moleculer.util.ParseResult;

public class Context {

	// --- PROPERTIES ---

	/**
	 * Unique context ID
	 */
	public final String id;

	/**
	 * Action name
	 */
	public final String name;

	/**
	 * Request parameters (including {@link io.datatree.Tree#getMeta() meta})
	 */
	public final Tree params;

	/**
	 * Request level (in nested-calls) - the first level is 1
	 */
	public final int level;

	/**
	 * Parent context ID (in nested-calls)
	 */
	public final String parentID;

	/**
	 * Request ID (= first context ID)
	 */
	public final String requestID;

	/**
	 * Calling options
	 */
	public final CallOptions.Options opts;

	/**
	 * Context creation time
	 */
	public final long startTime;

	// --- STREAM ---

	/**
	 * Streamed content
	 */
	public PacketStream stream;
	
	// --- COMPONENTS ---

	protected final ServiceInvoker serviceInvoker;
	protected final Eventbus eventbus;

	// --- CONSTRUCTORS ---

	public Context(ServiceInvoker serviceInvoker, Eventbus eventbus, String id, String name, Tree params,
			CallOptions.Options opts, PacketStream stream) {

		// Set components
		this.serviceInvoker = serviceInvoker;
		this.eventbus = eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = 1;
		this.parentID = null;
		this.opts = opts;
		this.stream = stream;
		
		// Set the first ID
		this.requestID = id;

		// Start time
		if (opts != null && opts.timeout > 0) {
			this.startTime = System.currentTimeMillis();
		} else {
			this.startTime = 0;
		}
	}

	public Context(String id, String name, Tree params, CallOptions.Options opts, PacketStream stream, Context parent) {

		// Set components
		this.serviceInvoker = parent.serviceInvoker;
		this.eventbus = parent.eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = parent.level + 1;
		this.parentID = parent.id;
		this.opts = opts;
		this.stream = stream;

		// Get the request ID from parent
		this.requestID = parent.requestID;

		// Start time
		if (opts != null && opts.timeout > 0) {
			this.startTime = System.currentTimeMillis();
		} else {
			this.startTime = 0;
		}
	}

	public Context(ServiceInvoker serviceInvoker, Eventbus eventbus, String id, String name, Tree params,
			CallOptions.Options opts, PacketStream stream, int level, String requestID, String parentID) {

		// Set components
		this.serviceInvoker = serviceInvoker;
		this.eventbus = eventbus;

		// Set properties
		this.id = id;
		this.name = name;
		this.params = params;
		this.level = level;
		this.parentID = parentID;
		this.opts = opts;
		this.stream = stream;
		this.requestID = requestID;

		// Start time
		if (opts != null && opts.timeout > 0) {
			this.startTime = System.currentTimeMillis();
		} else {
			this.startTime = 0;
		}
	}

	// --- INVOKE LOCAL OR REMOTE ACTION ---

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * broker.call("service.action").then(ctx -&gt; {<br>
	 * <br>
	 * // Nested call:<br>
	 * return ctx.call("math.add", "a", 1, "b", 2);<br>
	 * <br>
	 * });<br>
	 * <br>
	 * ...or with CallOptions:<br>
	 * <br>
	 * return ctx.call("math.add", "a", 1, "b", 2, CallOptions.nodeID("node2"));
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            list of parameter name-value pairs and an optional CallOptions
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Object... params) {
		ParseResult res = parseParams(params);
		return call(name, res.data, res.opts, res.stream);
	}

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * broker.call("service.action").then(ctx -&gt; {<br>
	 * <br>
	 * // Nested call:<br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * rerturn ctx.call("math.add", params);<br>
	 * <br>
	 * });
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Tree params) {
		return call(name, params, null, null);
	}

	/**
	 * Calls an action (local or remote). Sample code:<br>
	 * <br>
	 * broker.call("service.action").then(ctx -&gt; {<br>
	 * <br>
	 * // Nested call:<br>
	 * Tree params = new Tree();<br>
	 * params.put("a", true);<br>
	 * params.putList("b").add(1).add(2).add(3);<br>
	 * return ctx.call("math.add", params, CallOptions.nodeID("node2"));<br>
	 * <br>
	 * });
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * @param opts
	 *            calling options (target nodeID, call timeout, number of
	 *            retries)
	 * 
	 * @return response Promise
	 */
	protected Promise call(String name, Tree params, CallOptions.Options opts) {
		return call(name, params, opts, null);
	}

	/**
	 * Calls an action (local or remote).
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param params
	 *            {@link Tree} structure (input parameters of the method call)
	 * @param opts
	 *            calling options (target nodeID, call timeout, number of
	 *            retries)
	 * @param stream
	 *            streamed data (optional)
	 * 
	 * @return response Promise
	 */
	protected Promise call(String name, Tree params, CallOptions.Options opts, PacketStream stream) {

		// Recalculate distributed timeout
		if (startTime > 0) {

			// Distributed timeout handling. Decrementing the timeout value with
			// the elapsed time.
			// If the timeout below 0, skip the call.
			final long duration = System.currentTimeMillis() - startTime;
			final long distTimeout = this.opts.timeout - duration;

			if (distTimeout <= 0) {
				return Promise.reject(new RequestRejectedError(serviceInvoker.getBroker().getNodeID(), name));
			}

			if (opts == null) {
				opts = CallOptions.timeout(distTimeout);
			} else if (opts.timeout < 1 || distTimeout < opts.timeout) {
				opts = opts.timeout(distTimeout);
			}
		}
		return serviceInvoker.call(name, params, opts, stream, this);
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

	// --- STREAMED REQUEST OR RESPONSE ---

	/**
	 * Creates a stream what is suitable for transferring large files (or other
	 * "unlimited" media content) between Moleculer Nodes. Sample:<br>
	 * 
	 * <pre>
	 * public Action send = ctx -> {
	 *   PacketStream reqStream = ctx.createStream();
	 *   
	 *   ctx.call("service.action", reqStream).then(rsp -> {
	 *   
	 *     // Receive bytes into file
	 *     PacketStream rspStream = (PacketStream) rsp.asObject();
	 *     rspStream.transferTo(new File("out"));
	 *   }
	 *   
	 *   // Send bytes from file
	 *   reqStream.transferFrom(new File("in"));
	 * }
	 * </pre>
	 * 
	 * @return new stream
	 */
	public PacketStream createStream() {
		return new PacketStream(eventbus.getBroker().getConfig().getScheduler());
	}
	
}