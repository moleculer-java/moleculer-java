/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2020 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted; free of charge; to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"); to deal in the Software without restriction; including
 * without limitation the rights to use; copy; modify; merge; publish;
 * distribute; sublicense; and/or sell copies of the Software; and to
 * permit persons to whom the Software is furnished to do so; subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS"; WITHOUT WARRANTY OF ANY KIND;
 * EXPRESS OR IMPLIED; INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY; FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM; DAMAGES OR OTHER LIABILITY; WHETHER IN AN ACTION
 * OF CONTRACT; TORT OR OTHERWISE; ARISING FROM; OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.metrics;

public interface MetricConstants {

	// --- MOLECULER REQUEST METRICS ---

	public static final String MOLECULER_REQUEST_TOTAL = "moleculer.request.total";
	public static final String MOLECULER_REQUEST_ACTIVE = "moleculer.request.active";
	public static final String MOLECULER_REQUEST_ERROR_TOTAL = "moleculer.request.error.total";
	public static final String MOLECULER_REQUEST_TIME = "moleculer.request.time";
	public static final String MOLECULER_REQUEST_LEVELS = "moleculer.request.levels";
	
	// --- MOLECULER EVENTS METRICS ---
	
	public static final String MOLECULER_EVENT_EMIT_TOTAL = "moleculer.event.emit.total";
	public static final String MOLECULER_EVENT_BROADCAST_TOTAL = "moleculer.event.broadcast.total";
	public static final String MOLECULER_EVENT_BROADCASTLOCAL_TOTAL = "moleculer.event.broadcast-local.total";
	
	public static final String MOLECULER_EVENT_RECEIVED_TOTAL = "moleculer.event.received.total";
	public static final String MOLECULER_EVENT_RECEIVED_ACTIVE = "moleculer.event.received.active";
	public static final String MOLECULER_EVENT_RECEIVED_ERROR_TOTAL = "moleculer.event.received.error.total";
	public static final String MOLECULER_EVENT_RECEIVED_TIME = "moleculer.event.received.time";
	
	// --- MOLECULER TRANSIT METRICS ---

	public static final String MOLECULER_TRANSIT_PUBLISH_TOTAL = "moleculer.transit.publish.total";
	public static final String MOLECULER_TRANSIT_RECEIVE_TOTAL = "moleculer.transit.receive.total";

	public static final String MOLECULER_TRANSIT_REQUESTS_ACTIVE = "moleculer.transit.requests.active";
	public static final String MOLECULER_TRANSIT_STREAMS_SEND_ACTIVE = "moleculer.transit.streams.send.active";
	public static final String MOLECULER_TRANSIT_READY = "moleculer.transit.ready";
	public static final String MOLECULER_TRANSIT_CONNECTED = "moleculer.transit.connected";

	public static final String MOLECULER_TRANSIT_PONG_TIME = "moleculer.transit.pong.time";
	public static final String MOLECULER_TRANSIT_PONG_SYSTIME_DIFF = "moleculer.transit.pong.systime-diff";

	public static final String MOLECULER_TRANSIT_ORPHAN_RESPONSE_TOTAL = "moleculer.transit.orphan.response.total";

	// --- MOLECULER TRANSPORTER METRICS ---

	public static final String MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL = "moleculer.transporter.packets.sent.total";
	public static final String MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES = "moleculer.transporter.packets.sent.bytes";
	public static final String MOLECULER_TRANSPORTER_PACKETS_RECEIVED_TOTAL = "moleculer.transporter.packets.received.total";
	public static final String MOLECULER_TRANSPORTER_PACKETS_RECEIVED_BYTES = "moleculer.transporter.packets.received.bytes";

	// --- MOLECULER CIRCUIT BREAKER METRICS ---

	public static final String MOLECULER_CIRCUIT_BREAKER_OPENED_ACTIVE = "moleculer.circuit-breaker.opened.active";
	public static final String MOLECULER_CIRCUIT_BREAKER_OPENED_TOTAL = "moleculer.circuit-breaker.opened.total";
	public static final String MOLECULER_CIRCUIT_BREAKER_HALF_OPENED_ACTIVE = "moleculer.circuit-breaker.half-opened.active"; 
	
	// --- MOLECULER RETRY METRICS ---

	public static final String MOLECULER_REQUEST_RETRY_ATTEMPTS_TOTAL = "moleculer.request.retry.attempts.total";
	
	// --- MOLECULER TIMEOUT METRICS ---

	public static final String MOLECULER_REQUEST_TIMEOUT_TOTAL = "moleculer.request.timeout.total";

	// --- MOLECULER CACHER METRICS ---

	public static final String MOLECULER_CACHER_GET_TOTAL = "moleculer.cacher.get.total";
	public static final String MOLECULER_CACHER_GET_TIME = "moleculer.cacher.get.time";
	public static final String MOLECULER_CACHER_FOUND_TOTAL = "moleculer.cacher.found.total";
	public static final String MOLECULER_CACHER_SET_TOTAL = "moleculer.cacher.set.total";
	public static final String MOLECULER_CACHER_SET_TIME = "moleculer.cacher.set.time";
	public static final String MOLECULER_CACHER_DEL_TOTAL = "moleculer.cacher.del.total";
	public static final String MOLECULER_CACHER_DEL_TIME = "moleculer.cacher.del.time";
	public static final String MOLECULER_CACHER_CLEAN_TOTAL = "moleculer.cacher.clean.total";
	public static final String MOLECULER_CACHER_CLEAN_TIME = "moleculer.cacher.clean.time";
	public static final String MOLECULER_CACHER_EXPIRED_TOTAL = "moleculer.cacher.expired.total";
	
}
