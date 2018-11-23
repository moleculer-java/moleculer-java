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

import org.junit.Test;

import io.datatree.Tree;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.transporter.TcpTransporter;
import services.moleculer.transporter.Transporter;

public class ErrorTest extends TestCase {

	// --- BUILT-IN / STANDARD ERROR TYPES ---

	public static final String MOLECULER_ERROR = "MoleculerError";
	public static final String MOLECULER_RETRYABLE_ERROR = "MoleculerRetryableError";
	public static final String MOLECULER_SERVER_ERROR = "MoleculerServerError";
	public static final String MOLECULER_CLIENT_ERROR = "MoleculerClientError";

	public static final String SERVICE_NOT_FOUND_ERROR = "ServiceNotFoundError";
	public static final String SERVICE_NOT_AVAILABLE_ERROR = "ServiceNotAvailableError";

	public static final String VALIDATION_ERROR = "ValidationError";
	public static final String REQUEST_TIMEOUT_ERROR = "RequestTimeoutError";
	public static final String REQUEST_SKIPPED_ERROR = "RequestSkippedError";
	public static final String REQUEST_REJECTED_ERROR = "RequestRejectedError";
	public static final String QUEUE_IS_FULL_ERROR = "QueueIsFullError";
	public static final String MAX_CALL_LEVEL_ERROR = "MaxCallLevelError";

	public static final String SERVICE_SCHEMA_ERROR = "ServiceSchemaError";
	public static final String BROKER_OPTIONS_ERROR = "BrokerOptionsError";
	public static final String GRACEFUL_STOP_TIMEOUT_ERROR = "GracefulStopTimeoutError";

	public static final String PROTOCOL_VERSION_MISMATCH_ERROR = "ProtocolVersionMismatchError";
	public static final String INVALID_PACKET_DATA_ERROR = "InvalidPacketDataError";

	// --- VARIABLES ---

	protected Transporter tr1;
	protected ServiceBroker br1;

	protected Transporter tr2;
	protected ServiceBroker br2;

	// --- TESTS ---

	@Test
	public void testErrors() throws Exception {

		// --- CONTENT TEST ---

		Tree t = createMoleculerError2().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerError3().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerError4().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("MOLECULER_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerError5().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("MOLECULER_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerError6().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("MOLECULER_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerError().toTree();
		assertEquals("MoleculerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerRetryableError2().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerRetryableError3().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerRetryableError4().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("RETRYABLE_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerRetryableError5().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("RETRYABLE_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerRetryableError6().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("RETRYABLE_ERROR", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerRetryableError().toTree();
		assertEquals("MoleculerRetryableError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerServerError2().toTree();
		assertEquals("MoleculerServerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerServerError3().toTree();
		assertEquals("MoleculerServerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerServerError().toTree();
		assertEquals("MoleculerServerError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerClientError2().toTree();
		assertEquals("MoleculerClientError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerClientError3().toTree();
		assertEquals("MoleculerClientError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("400", t.get("code", "?"));
		assertEquals("CLIENT_ERROR", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.1", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createMoleculerClientError4().toTree();
		assertEquals("MoleculerClientError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("400", t.get("code", "?"));
		assertEquals("CLIENT_ERROR", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertNull(t.get("data", "?"));

		t = createMoleculerClientError().toTree();
		assertEquals("MoleculerClientError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("123", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createServiceNotFoundError().toTree();
		assertEquals("ServiceNotFoundError", t.get("name", "?"));
		assertEquals("Service 'action1' is not found on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("404", t.get("code", "?"));
		assertEquals("SERVICE_NOT_FOUND", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createServiceNotAvailableError().toTree();
		assertEquals("ServiceNotAvailableError", t.get("name", "?"));
		assertEquals("Service 'action1' is not available on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("404", t.get("code", "?"));
		assertEquals("SERVICE_NOT_AVAILABLE", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createValidationError().toTree();
		assertEquals("ValidationError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("422", t.get("code", "?"));
		assertEquals("TEST_1", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createRequestTimeoutError().toTree();
		assertEquals("RequestTimeoutError", t.get("name", "?"));
		assertEquals("Request is timed out when call 'action1' action on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("504", t.get("code", "?"));
		assertEquals("REQUEST_TIMEOUT", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createRequestSkippedError().toTree();
		assertEquals("RequestSkippedError", t.get("name", "?"));
		assertEquals("Calling 'action1' is skipped because timeout reached on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("514", t.get("code", "?"));
		assertEquals("REQUEST_SKIPPED", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createRequestRejectedError().toTree();
		assertEquals("RequestRejectedError", t.get("name", "?"));
		assertEquals("Request is rejected when call 'action1' action on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("503", t.get("code", "?"));
		assertEquals("REQUEST_REJECTED", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createQueueIsFullError().toTree();
		assertEquals("QueueIsFullError", t.get("name", "?"));
		assertEquals("Queue is full. Request 'action1' action on 'node1' node is rejected.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("429", t.get("code", "?"));
		assertEquals("QUEUE_FULL", t.get("type", "?"));
		assertEquals("true", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("action1", t.get("data.action", "?"));

		t = createMaxCallLevelError().toTree();
		assertEquals("MaxCallLevelError", t.get("name", "?"));
		assertEquals("Request level is reached, the limit is '123' on 'node1' node.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("MAX_CALL_LEVEL", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("123", t.get("data.level", "?"));

		t = createServiceSchemaError().toTree();
		assertEquals("ServiceSchemaError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("SERVICE_SCHEMA_ERROR", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createBrokerOptionsError().toTree();
		assertEquals("BrokerOptionsError", t.get("name", "?"));
		assertEquals("message-1", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("BROKER_OPTIONS_ERROR", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createGracefulStopTimeoutError().toTree();
		assertEquals("GracefulStopTimeoutError", t.get("name", "?"));
		assertEquals("Unable to stop service gracefully.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("GRACEFUL_STOP_TIMEOUT", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		t = createProtocolVersionMismatchError().toTree();
		assertEquals("ProtocolVersionMismatchError", t.get("name", "?"));
		assertEquals("Protocol version mismatch.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("PROTOCOL_VERSION_MISMATCH", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("123", t.get("data.actual", "?"));
		assertEquals("456", t.get("data.received", "?"));

		t = createInvalidPacketDataError().toTree();
		assertEquals("InvalidPacketDataError", t.get("name", "?"));
		assertEquals("Invalid packet data.", t.get("message", "?"));
		assertEquals("node1", t.get("nodeID", "?"));
		assertEquals("500", t.get("code", "?"));
		assertEquals("INVALID_PACKET_DATA", t.get("type", "?"));
		assertEquals("false", t.get("retryable", "?"));
		assertTrue(t.get("stack", "?").length() > 30);
		assertEquals("3", t.get("data.a", "?"));
		assertEquals("true", t.get("data.b", "?"));

		// --- JSON TO EXCEPTION CONVERSIONS ---

		checkConvert(createMoleculerError2());
		checkConvert(createMoleculerError3());
		checkConvert(createMoleculerError4());
		checkConvert(createMoleculerError5());
		checkConvert(createMoleculerError6());
		checkConvert(createMoleculerError());

		checkConvert(createMoleculerRetryableError2());
		checkConvert(createMoleculerRetryableError3());
		checkConvert(createMoleculerRetryableError4());
		checkConvert(createMoleculerRetryableError5());
		checkConvert(createMoleculerRetryableError6());
		checkConvert(createMoleculerRetryableError());

		checkConvert(createMoleculerServerError2());
		checkConvert(createMoleculerServerError3());
		checkConvert(createMoleculerServerError());

		checkConvert(createMoleculerClientError2());
		checkConvert(createMoleculerClientError3());
		checkConvert(createMoleculerClientError4());
		checkConvert(createMoleculerClientError());

		checkConvert(createServiceNotFoundError());

		checkConvert(createServiceNotAvailableError());

		checkConvert(createValidationError());

		checkConvert(createRequestTimeoutError());

		checkConvert(createRequestSkippedError());

		checkConvert(createRequestRejectedError());

		checkConvert(createQueueIsFullError());

		checkConvert(createMaxCallLevelError());

		checkConvert(createServiceSchemaError());

		checkConvert(createBrokerOptionsError());

		checkConvert(createGracefulStopTimeoutError());

		checkConvert(createProtocolVersionMismatchError());

		checkConvert(createInvalidPacketDataError());

		checkConvert(new CustomError("Custom message", "node345"));

		// --- CHECK ERROR TRANSFER BETWEEN NODES ---

		assertEquals(createMoleculerError2(), invoke(MOLECULER_ERROR, 2));
		assertEquals(createMoleculerError3(), invoke(MOLECULER_ERROR, 3));
		assertEquals(createMoleculerError4(), invoke(MOLECULER_ERROR, 4));
		assertEquals(createMoleculerError5(), invoke(MOLECULER_ERROR, 5));
		assertEquals(createMoleculerError6(), invoke(MOLECULER_ERROR, 6));
		assertEquals(createMoleculerError(), invoke(MOLECULER_ERROR));

		assertEquals(createMoleculerRetryableError2(), invoke(MOLECULER_RETRYABLE_ERROR, 2));
		assertEquals(createMoleculerRetryableError3(), invoke(MOLECULER_RETRYABLE_ERROR, 3));
		assertEquals(createMoleculerRetryableError4(), invoke(MOLECULER_RETRYABLE_ERROR, 4));
		assertEquals(createMoleculerRetryableError5(), invoke(MOLECULER_RETRYABLE_ERROR, 5));
		assertEquals(createMoleculerRetryableError6(), invoke(MOLECULER_RETRYABLE_ERROR, 6));
		assertEquals(createMoleculerRetryableError(), invoke(MOLECULER_RETRYABLE_ERROR));

		assertEquals(createMoleculerServerError2(), invoke(MOLECULER_SERVER_ERROR, 2));
		assertEquals(createMoleculerServerError3(), invoke(MOLECULER_SERVER_ERROR, 3));
		assertEquals(createMoleculerServerError(), invoke(MOLECULER_SERVER_ERROR));

		assertEquals(createMoleculerClientError2(), invoke(MOLECULER_CLIENT_ERROR, 2));
		assertEquals(createMoleculerClientError3(), invoke(MOLECULER_CLIENT_ERROR, 3));
		assertEquals(createMoleculerClientError4(), invoke(MOLECULER_CLIENT_ERROR, 4));
		assertEquals(createMoleculerClientError(), invoke(MOLECULER_CLIENT_ERROR));

		assertEquals(createServiceNotFoundError(), invoke(SERVICE_NOT_FOUND_ERROR));

		assertEquals(createServiceNotAvailableError(), invoke(SERVICE_NOT_AVAILABLE_ERROR));

		assertEquals(createValidationError(), invoke(VALIDATION_ERROR));

		assertEquals(createRequestTimeoutError(), invoke(REQUEST_TIMEOUT_ERROR));

		assertEquals(createRequestSkippedError(), invoke(REQUEST_SKIPPED_ERROR));

		assertEquals(createRequestRejectedError(), invoke(REQUEST_REJECTED_ERROR));

		assertEquals(createQueueIsFullError(), invoke(QUEUE_IS_FULL_ERROR));

		assertEquals(createMaxCallLevelError(), invoke(MAX_CALL_LEVEL_ERROR));

		assertEquals(createServiceSchemaError(), invoke(SERVICE_SCHEMA_ERROR));

		assertEquals(createBrokerOptionsError(), invoke(BROKER_OPTIONS_ERROR));

		assertEquals(createGracefulStopTimeoutError(), invoke(GRACEFUL_STOP_TIMEOUT_ERROR));

		assertEquals(createProtocolVersionMismatchError(), invoke(PROTOCOL_VERSION_MISMATCH_ERROR));

		assertEquals(createInvalidPacketDataError(), invoke(INVALID_PACKET_DATA_ERROR));

		assertEquals(new CustomError("My Error Message", "CurrentNode"), invoke("CustomError"));
	}

	public void checkConvert(MoleculerError e) {
		Tree t = e.toTree();
		String s1 = t.toString();

		MoleculerError e2 = MoleculerErrorUtils.create(t);
		Tree t2 = e2.toTree();
		String s2 = t2.toString();

		assertEquals(s1, s2);
	}

	public void assertEquals(Throwable e1, Throwable e2) {
		String s1;
		if (e1 != null && e1 instanceof MoleculerError) {
			Tree t = ((MoleculerError) e1).toTree().clone();
			t.remove("stack");
			s1 = t.toString();
		} else {
			s1 = String.valueOf(e1);
		}

		String s2;
		if (e2 != null && e2 instanceof MoleculerError) {
			Tree t = ((MoleculerError) e2).toTree().clone();
			t.remove("stack");
			s2 = t.toString();
		} else {
			s2 = String.valueOf(e2);
		}
		assertEquals(s1, s2);
	}

	public Throwable invoke(String type) {
		return invoke(type, 1);
	}

	public Throwable invoke(String type, int variant) {
		try {
			Tree params = new Tree();
			params.put("type", type);
			params.put("variant", variant);
			br2.call("test.test", params).waitFor(1000);
		} catch (Exception e) {
			return e;
		}
		return null;
	}

	// --- TEST SERVICES ---

	protected static final class TestService extends Service {

		public Action test = ctx -> {

			// Eg. MOLECULER_ERROR
			String type = ctx.params.get("type", "");

			// 1,2,3...
			int variant = ctx.params.get("variant", 1);

			switch (type) {
			case MOLECULER_ERROR:
				if (variant == 2) {
					throw createMoleculerError2();
				}
				if (variant == 3) {
					throw createMoleculerError3();
				}
				if (variant == 4) {
					throw createMoleculerError4();
				}
				if (variant == 5) {
					throw createMoleculerError5();
				}
				if (variant == 6) {
					throw createMoleculerError6();
				}
				throw createMoleculerError();
			case MOLECULER_RETRYABLE_ERROR:
				if (variant == 2) {
					throw createMoleculerRetryableError2();
				}
				if (variant == 3) {
					throw createMoleculerRetryableError3();
				}
				if (variant == 4) {
					throw createMoleculerRetryableError4();
				}
				if (variant == 5) {
					throw createMoleculerRetryableError5();
				}
				if (variant == 6) {
					throw createMoleculerRetryableError6();
				}
				throw createMoleculerRetryableError();
			case MOLECULER_SERVER_ERROR:
				if (variant == 2) {
					throw createMoleculerServerError2();
				}
				if (variant == 3) {
					throw createMoleculerServerError3();
				}
				throw createMoleculerServerError();
			case MOLECULER_CLIENT_ERROR:
				if (variant == 2) {
					throw createMoleculerClientError2();
				}
				if (variant == 3) {
					throw createMoleculerClientError3();
				}
				if (variant == 4) {
					throw createMoleculerClientError4();
				}
				throw createMoleculerClientError();
			case SERVICE_NOT_FOUND_ERROR:
				throw createServiceNotFoundError();
			case SERVICE_NOT_AVAILABLE_ERROR:
				throw createServiceNotAvailableError();
			case VALIDATION_ERROR:
				throw createValidationError();
			case REQUEST_TIMEOUT_ERROR:
				throw createRequestTimeoutError();
			case REQUEST_SKIPPED_ERROR:
				throw createRequestSkippedError();
			case REQUEST_REJECTED_ERROR:
				throw createRequestRejectedError();
			case QUEUE_IS_FULL_ERROR:
				throw createQueueIsFullError();
			case MAX_CALL_LEVEL_ERROR:
				throw createMaxCallLevelError();
			case SERVICE_SCHEMA_ERROR:
				throw createServiceSchemaError();
			case BROKER_OPTIONS_ERROR:
				throw createBrokerOptionsError();
			case GRACEFUL_STOP_TIMEOUT_ERROR:
				throw createGracefulStopTimeoutError();
			case PROTOCOL_VERSION_MISMATCH_ERROR:
				throw createProtocolVersionMismatchError();
			case INVALID_PACKET_DATA_ERROR:
				throw createInvalidPacketDataError();
			case "CustomError":
				throw new CustomError("My Error Message", "CurrentNode");
			default:
			}

			return null;
		};

	}

	// --- ERROR "FACTORIES" ---

	protected final static MoleculerError createMoleculerError() {
		return new MoleculerError("message-1", new NullPointerException("test null"), MOLECULER_ERROR, "node1", true,
				123, "TEST_1", "a", 3, "b", true);
	}

	protected final static MoleculerError createMoleculerError2() {
		return new MoleculerError("message-1", new NullPointerException("test null"), "node1", true, 123, "TEST_1",
				new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerError3() {
		return new MoleculerError("message-1", "node1", true, 123, "TEST_1", new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerError4() {
		return new MoleculerError("message-1", "node1", true, 123, new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerError5() {
		return new MoleculerError("message-1", "node1", true, 123);
	}

	protected final static MoleculerError createMoleculerError6() {
		return new MoleculerError("message-1", "node1", true);
	}

	// ---

	protected final static MoleculerError createMoleculerRetryableError() {
		return new MoleculerRetryableError("message-1", new NullPointerException("test null"),
				MOLECULER_RETRYABLE_ERROR, "node1", 123, "TEST_1", "a", 3, "b", true);
	}

	protected final static MoleculerError createMoleculerRetryableError2() {
		return new MoleculerRetryableError("message-1", new NullPointerException("test null"), "node1", 123, "TEST_1",
				new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerRetryableError3() {
		return new MoleculerRetryableError("message-1", "node1", 123, "TEST_1", new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerRetryableError4() {
		return new MoleculerRetryableError("message-1", "node1", 123, new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerRetryableError5() {
		return new MoleculerRetryableError("message-1", "node1", 123);
	}

	protected final static MoleculerError createMoleculerRetryableError6() {
		return new MoleculerRetryableError("message-1", "node1");
	}

	// ---

	protected final static MoleculerError createMoleculerServerError() {
		return new MoleculerServerError("message-1", new NullPointerException("test null"), "node1", "TEST_1", "a", 3,
				"b", true);
	}

	protected final static MoleculerError createMoleculerServerError2() {
		return new MoleculerServerError("message-1", "node1", "TEST_1", new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerServerError3() {
		return new MoleculerServerError("message-1", "node1", "TEST_1");
	}

	// ---

	protected final static MoleculerError createMoleculerClientError() {
		return new MoleculerClientError("message-1", new NullPointerException("test null"), MOLECULER_CLIENT_ERROR,
				"node1", 123, "TEST_1", "a", 3, "b", true);
	}

	protected final static MoleculerError createMoleculerClientError2() {
		return new MoleculerClientError("message-1", new NullPointerException("test null"), "node1", 123, "TEST_1",
				new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerClientError3() {
		return new MoleculerClientError("message-1", new NullPointerException("test null"), "node1",
				new Tree().put("1", 3).put("b", true));
	}

	protected final static MoleculerError createMoleculerClientError4() {
		return new MoleculerClientError("message-1", "node1");
	}

	// ---

	protected final static MoleculerError createServiceNotFoundError() {
		return new ServiceNotFoundError("node1", "action1");
	}

	protected final static MoleculerError createServiceNotAvailableError() {
		return new ServiceNotAvailableError("node1", "action1");
	}

	protected final static MoleculerError createValidationError() {
		return new ValidationError("message-1", "node1", "TEST_1", "a", 3, "b", true);
	}

	protected final static MoleculerError createRequestTimeoutError() {
		return new RequestTimeoutError("node1", "action1");
	}

	protected final static MoleculerError createRequestSkippedError() {
		return new RequestSkippedError("node1", "action1");
	}

	protected final static MoleculerError createRequestRejectedError() {
		return new RequestRejectedError("node1", "action1");
	}

	protected final static MoleculerError createQueueIsFullError() {
		return new QueueIsFullError("node1", "action1");
	}

	protected final static MoleculerError createMaxCallLevelError() {
		return new MaxCallLevelError("node1", 123);
	}

	protected final static MoleculerError createServiceSchemaError() {
		return new ServiceSchemaError("message-1", "node1", "a", 3, "b", true);
	}

	protected final static MoleculerError createBrokerOptionsError() {
		return new BrokerOptionsError("message-1", "node1", "a", 3, "b", true);
	}

	protected final static MoleculerError createGracefulStopTimeoutError() {
		return new GracefulStopTimeoutError("node1", "a", 3, "b", true);
	}

	protected final static MoleculerError createProtocolVersionMismatchError() {
		return new ProtocolVersionMismatchError("node1", "123", "456");
	}

	protected final static MoleculerError createInvalidPacketDataError() {
		return new InvalidPacketDataError("node1", "a", 3, "b", true);
	}

	// --- CUSTOM ERROR CLASS ---

	static {
		MoleculerErrorUtils.registerCustomError("CustomError", CustomError.class);
	}

	public static class CustomError extends MoleculerClientError {

		private static final long serialVersionUID = -8147716261288009037L;

		public CustomError(String message, String nodeID) {
			super(message, null, "CustomError", nodeID, 98765, "CUSTOM_ERROR");
		}

		public CustomError(Tree payload) {
			super(payload);
		}

	}

	// --- UTILITIES ---

	@Override
	protected void setUp() throws Exception {

		// Create transporters
		tr1 = new TcpTransporter();
		tr2 = new TcpTransporter();

		// Enable debug messages
		tr1.setDebug(true);
		tr2.setDebug(true);

		// Create brokers
		br1 = ServiceBroker.builder().transporter(tr1).monitor(new ConstantMonitor()).nodeID("node1").build();
		br2 = ServiceBroker.builder().transporter(tr2).monitor(new ConstantMonitor()).nodeID("node2").build();

		// Create "test" service
		br1.createService("test", new TestService());

		// Start brokers
		br1.start();
		br2.start();

		// Wait for connecting peer
		br2.waitForServices(15000, "test").waitFor(20000);
	}

	@Override
	protected void tearDown() throws Exception {
		if (br1 != null) {
			br1.stop();
		}
		if (br2 != null) {
			br2.stop();
		}
	}

}