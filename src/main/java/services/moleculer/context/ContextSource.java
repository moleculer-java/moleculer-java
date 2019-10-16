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

import static services.moleculer.util.CommonUtils.extractStream;
import static services.moleculer.util.CommonUtils.parseParams;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.config.ServiceBrokerConfig;
import services.moleculer.eventbus.Eventbus;
import services.moleculer.eventbus.Groups;
import services.moleculer.service.ServiceInvoker;
import services.moleculer.strategy.Strategy;
import services.moleculer.stream.PacketStream;
import services.moleculer.uid.UidGenerator;
import services.moleculer.util.ParseResult;

/**
 * Base superclass of all objects that can send events (eg. ServiceBroker or
 * Context).
 */
public abstract class ContextSource {

	// --- COMPONENTS ---

	/**
	 * Eventbus of the ServiceBroker or Context.
	 */
	protected final Eventbus eventbus;

	/**
	 * Service Invoker of the ServiceBroker or Context.
	 */
	protected final ServiceInvoker serviceInvoker;

	/**
	 * UID Generator of the ServiceBroker or Context.
	 */
	protected final UidGenerator uidGenerator;

	/**
	 * Source nodeID
	 */
	protected final String nodeID;
	
	// --- CONSTRUCTOR ---

	protected ContextSource(ServiceInvoker serviceInvoker, Eventbus eventbus, UidGenerator uidGenerator, String nodeID) {
		this.serviceInvoker = serviceInvoker;
		this.eventbus = eventbus;
		this.uidGenerator = uidGenerator;
		this.nodeID = nodeID;
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
		emit(name, res.data, res.groups, res.stream, res.opts);
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
		ParseResult res = extractStream(payload);
		emit(name, res.data, groups, res.stream, null);
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
		ParseResult res = extractStream(payload);
		emit(name, res.data, null, res.stream, null);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from ALL event groups, who are
	 * listening this event. The service broker uses the default {@link Strategy
	 * strategy} of the broker for event redirection and node selection.
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param stream
	 *            streamed data
	 */
	public void emit(String name, PacketStream stream) {
		emit(name, null, null, stream, null);
	}

	/**
	 * Emits an event to <b>ONE</b> listener from ALL event groups, who are
	 * listening this event. The service broker uses the default {@link Strategy
	 * strategy} of the broker for event redirection and node selection.
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param stream
	 *            streamed data
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void emit(String name, PacketStream stream, Groups groups) {
		emit(name, null, groups, stream, null);
	}

	// --- BROADCAST EVENT TO ALL LISTENERS ---

	/**
	 * Sends an event to <b>ALL</b> listeners from ALL (or the specified) event
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
		broadcast(name, res.data, res.groups, res.stream, res.opts, false);
	}

	/**
	 * Sends an event to <b>ALL</b> listeners from the specified event group(s),
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
		ParseResult res = extractStream(payload);
		broadcast(name, res.data, groups, res.stream, null, false);
	}

	/**
	 * Sends an event to <b>ALL</b> listeners from ALL event groups, who are
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
		ParseResult res = extractStream(payload);
		broadcast(name, res.data, null, res.stream, null, false);
	}

	/**
	 * Sends an event to <b>ALL</b> listeners from ALL event groups, who are
	 * listening this event.
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param stream
	 *            streamed data
	 */
	public void broadcast(String name, PacketStream stream) {
		broadcast(name, null, null, stream, null, false);
	}

	/**
	 * Sends an event to <b>ALL</b> listeners from ALL event groups, who are
	 * listening this event.
	 * 
	 * @param name
	 *            name of event (eg. "user.created")
	 * @param stream
	 *            streamed data
	 * @param groups
	 *            {@link Groups event group} container
	 */
	public void broadcast(String name, PacketStream stream, Groups groups) {
		broadcast(name, null, groups, stream, null, false);
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
		broadcast(name, res.data, res.groups, res.stream, res.opts, true);
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
		ParseResult res = extractStream(payload);
		broadcast(name, res.data, groups, res.stream, null, true);
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
		ParseResult res = extractStream(payload);
		broadcast(name, res.data, null, res.stream, null, true);
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
		ParseResult res = extractStream(params);
		return call(name, res.data, null, res.stream);
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
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, Tree params, CallOptions.Options opts) {
		ParseResult res = extractStream(params);
		return call(name, res.data, opts, res.stream);
	}

	/**
	 * Calls an action (local or remote).
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param stream
	 *            streamed data (optional)
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, PacketStream stream) {
		return call(name, null, null, stream);
	}

	/**
	 * Calls an action (local or remote).
	 * 
	 * @param name
	 *            action name (eg. "math.add" in "service.action" syntax)
	 * @param stream
	 *            streamed data (optional)
	 * @param opts
	 *            calling options (target nodeID, call timeout, number of
	 *            retries)
	 * 
	 * @return response Promise
	 */
	public Promise call(String name, PacketStream stream, CallOptions.Options opts) {
		return call(name, null, opts, stream);
	}

	// --- STREAMED REQUEST OR RESPONSE ---

	/**
	 * Creates a stream what is suitable for transferring large files (or other
	 * "unlimited" media content) between Moleculer Nodes. Sample:<br>
	 * 
	 * <pre>
	 * public Action send = ctx -&gt; {
	 *   PacketStream reqStream = ctx.createStream();
	 *   
	 *   ctx.call("service.action", reqStream).then(rsp -&gt; {
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
		ServiceBrokerConfig config = eventbus.getBroker().getConfig();
		return new PacketStream(config.getNodeID(), config.getScheduler());
	}

	// --- PROTECTED METHODS ---

	protected void emit(String name, Tree payload, Groups groups, PacketStream stream, CallOptions.Options opts) {
		eventbus.emit(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name, payload, 1,
				null, null, stream, opts, nodeID), groups, false);
	}

	protected void broadcast(String name, Tree payload, Groups groups, PacketStream stream, CallOptions.Options opts,
			boolean local) {
		eventbus.broadcast(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name, payload, 1,
				null, null, stream, opts, nodeID), groups, local);
	}

	protected Promise call(String name, Tree params, CallOptions.Options opts, PacketStream stream) {
		return serviceInvoker.call(new Context(serviceInvoker, eventbus, uidGenerator, uidGenerator.nextUID(), name,
				params, 1, null, null, stream, opts, nodeID));
	}

}