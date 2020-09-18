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
package services.moleculer.transporter;

import io.datatree.Promise;
import io.datatree.Tree;

/**
 * "Null" / empty transporter.
 * 
 * @see TcpTransporter
 * @see NatsTransporter
 * @see NatsStreamingTransporter
 * @see MqttTransporter
 * @see JmsTransporter
 * @see KafkaTransporter
 * @see AmqpTransporter
 */
public class NullTransporter extends Transporter {

	@Override
	public void connect() {

		// Do nothing
	}

	@Override
	public void publish(String channel, Tree message) {

		// Metrics
		if (metrics != null) {
			try {
				byte[] bytes = serializer.write(message);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL, MOLECULER_TRANSPORTER_PACKETS_SENT_TOTAL_DESC);
				metrics.increment(MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES, MOLECULER_TRANSPORTER_PACKETS_SENT_BYTES_DESC, bytes.length);
			} catch (Exception cause) {
				logger.error("Unable to serialize message!", cause);
			}
		}
	}

	@Override
	public Promise subscribe(String channel) {
		return Promise.resolve();
	}

}