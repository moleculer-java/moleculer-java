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
package services.moleculer.web;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.Adapter;
import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.apache.coyote.ajp.AjpAprProtocol;
import org.apache.coyote.ajp.AjpProtocol;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http11.Http11Protocol;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.net.SocketStatus;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

/**
 * HTTP/1.1 API Gateway based on the Apache Coyote Connector. Required
 * dependency:<br>
 * <br>
 * // https://mvnrepository.com/artifact/org.apache.tomcat/coyote<br>
 * runtime group: 'org.apache.tomcat', name: 'coyote', version: '6.0.53'
 */
public class CoyoteGateway extends ApiGateway {

	// --- SERVER INSTANCE ---

	protected AbstractProtocol protocol;

	// --- PROPERTIES ---

	public int port = 3000;
	public String address;

	// --- START COYOTE SERVER ---

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		super.start(broker, config);

		// Process base properties
		port = config.get("port", port);
		address = config.get("address", address);

		// Use common executor
		ExecutorService executor = broker.components().executor();

		// Create server
		if (protocol == null) {
			String implementation = config.get("implementation", "nio");
			if ("nio".equalsIgnoreCase(implementation)) {

				// Java non-blocking I/O protocol
				Http11NioProtocol p = new Http11NioProtocol();
				p.setExecutor(executor);
				p.setPollerThreadCount(2);
				p.setPort(port);
				if (address != null) {
					p.setAddress(InetAddress.getByName(address));
				}
				protocol = p;

			} else if ("bio".equalsIgnoreCase(implementation)) {

				// Java blocking I/O protocol
				Http11Protocol p = new Http11Protocol();
				p.setExecutor(executor);
				p.setPort(port);
				if (address != null) {
					p.setAddress(InetAddress.getByName(address));
				}
				protocol = p;

			} else if ("apr".equalsIgnoreCase(implementation)) {

				// Native APR protocol
				Http11AprProtocol p = new Http11AprProtocol();
				p.setExecutor(executor);
				p.setPollerThreadCount(2);
				p.setPort(port);
				if (address != null) {
					p.setAddress(InetAddress.getByName(address));
				}
				protocol = p;

			} else if ("ajp".equalsIgnoreCase(implementation)) {

				// AJP protocol
				AjpProtocol p = new AjpProtocol();
				p.setExecutor(executor);
				if (address != null) {
					p.setAddress(InetAddress.getByName(address));
				}
				protocol = p;

			} else if ("ajp-apr".equalsIgnoreCase(implementation)) {

				// Native AJP protocol
				AjpAprProtocol p = new AjpAprProtocol();
				p.setExecutor(executor);
				p.setPort(port);
				if (address != null) {
					p.setAddress(InetAddress.getByName(address));
				}
				protocol = p;

			} else {
				throw new IllegalArgumentException("Invalid implementation type (" + implementation + ")!");
			}
		}

		// Create adapter
		protocol.setAdapter(new Adapter() {

			@Override
			public final void service(Request req, Response rsp) throws Exception {

				// Read body
				byte[] body = null;
				long max = req.getContentLengthLong();
				if (max == -1) {

					// TODO read request
					// req.queryString().toString()

				} else {
					long count = req.getBytesRead();
					if (count >= max) {
						InputBuffer buffer = req.getInputBuffer();

						// TODO read request

					}
				}

				// Get method
				String httpMethod = req.method().getString();

				// Get path
				String path = req.requestURI().toString();

				// Get headers
				LinkedHashMap<String, String> headers = new LinkedHashMap<>();
				MimeHeaders mimeHeaders = req.getMimeHeaders();
				Enumeration<?> names = mimeHeaders.names();
				while (names.hasMoreElements()) {
					String name = String.valueOf(names.nextElement());
					headers.put(name, mimeHeaders.getHeader(name));
				}

				// Invoke Message Broker in async style
				processRequest(httpMethod, path, headers, body).then(in -> {
					try {

						byte[] bytes = in.toBinary(null, false);
						rsp.setContentLength(bytes.length);
						rsp.setContentType("application/json; charset=UTF-8");
						rsp.sendHeaders();

						ByteChunk chunk = new ByteChunk();
						chunk.setBytes(bytes, 0, bytes.length);
						rsp.doWrite(chunk);
						rsp.finish();

					} catch (Exception cause) {
						return cause;
					}
					return null;
				}).Catch(cause -> {
					// TODO Handle error
				});
			}

			@Override
			public final void log(Request req, Response rsp, long arg) {
			}

			@Override
			public final boolean event(Request req, Response rsp, SocketStatus status) throws Exception {
				return false;
			}

		});

		// Start server
		protocol.init();
		protocol.start();
	}

	// --- STOP COYOTE SERVER ---

	@Override
	public void stop() {
		if (protocol != null) {
			try {
				protocol.destroy();
			} catch (Exception cause) {
				logger.info("Unexpected error occured while stopping Coyote Server!", cause);
			}
			protocol = null;
		}
	}

	// --- GETTERS AND SETTERS ---

	public AbstractProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(AbstractProtocol protocol) {
		this.protocol = protocol;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}