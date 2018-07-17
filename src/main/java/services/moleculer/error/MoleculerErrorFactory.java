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

import java.lang.reflect.Constructor;
import java.util.HashMap;

import io.datatree.Tree;

public class MoleculerErrorFactory {

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

	// --- CUSTOM ERROR TYPES ---

	private static final HashMap<String, Class<? extends MoleculerError>> customErrors = new HashMap<>();

	public static final void registerCustomError(String name, Class<? extends MoleculerError> errorClass) {
		customErrors.put(name, errorClass);
	}

	public static final void deregisterCustomError(String name) {
		customErrors.remove(name);
	}

	// --- EXCEPTION FACTORY ---

	public static MoleculerError create(Tree payload) {

		// Get name field (~= NodeJS or Java class)
		String name = payload.get("name", (String) null);

		// Create built-in types
		if (name != null && !name.isEmpty()) {
			switch (name) {
			case MOLECULER_ERROR:
				return new MoleculerError(payload);
			case MOLECULER_RETRYABLE_ERROR:
				return new MoleculerRetryableError(payload);
			case MOLECULER_SERVER_ERROR:
				return new MoleculerServerError(payload);
			case MOLECULER_CLIENT_ERROR:
				return new MoleculerClientError(payload);

			case SERVICE_NOT_FOUND_ERROR:
				return new ServiceNotFoundError(payload);
			case SERVICE_NOT_AVAILABLE_ERROR:
				return new ServiceNotAvailableError(payload);

			case VALIDATION_ERROR:
				return new ValidationError(payload);
			case REQUEST_TIMEOUT_ERROR:
				return new RequestTimeoutError(payload);
			case REQUEST_SKIPPED_ERROR:
				return new RequestSkippedError(payload);
			case REQUEST_REJECTED_ERROR:
				return new RequestRejectedError(payload);
			case QUEUE_IS_FULL_ERROR:
				return new QueueIsFullError(payload);
			case MAX_CALL_LEVEL_ERROR:
				return new MaxCallLevelError(payload);

			case SERVICE_SCHEMA_ERROR:
				return new ServiceSchemaError(payload);
			case BROKER_OPTIONS_ERROR:
				return new BrokerOptionsError(payload);
			case GRACEFUL_STOP_TIMEOUT_ERROR:
				return new GracefulStopTimeoutError(payload);

			case PROTOCOL_VERSION_MISMATCH_ERROR:
				return new ProtocolVersionMismatchError(payload);
			case INVALID_PACKET_DATA_ERROR:
				return new InvalidPacketDataError(payload);

			default:

				// Create custom error class
				Class<? extends MoleculerError> errorClass = customErrors.get(name);
				if (errorClass != null) {
					try {
						Constructor<? extends MoleculerError> c = errorClass.getConstructor(new Class[]{ Tree.class });
						return c.newInstance(payload);
					} catch (Exception cause) {
						throw new MoleculerError("Unable to create error class '" + name + "'!", cause,
								"MoleculerError", "unknown", false, 500, "UNABLE_TO_CREATE_CLASS", "type", name);
					}
				}
			}

		}

		// Create undefined / unknown type
		return new MoleculerError(payload);
	}

}