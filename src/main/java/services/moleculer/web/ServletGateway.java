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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.config.ServiceBrokerSettings;
import services.moleculer.service.Name;

/**
 * Asynchronous servlet-based API Gateway (draft). Servlet version 3.0+ is
 * required for the asynchronous operations.
 */
@Name("Servlet API Gateway")
@WebServlet(asyncSupported = true)
public class ServletGateway extends HttpServlet {

	// --- SERIAL VERSION UID ---

	private static final long serialVersionUID = -4132693352174303362L;

	// --- PROPERTIES ---

	protected String info = "Moleculer API Gateway";
	protected Exception bootException;

	protected ApiGateway servletGateway;
	protected ServiceBroker broker;

	// --- INIT SERVLET ---

	@Override
	public void init() throws ServletException {
		try {

			// Get config
			ServletConfig servletConfig = getServletConfig();
			String info = servletConfig.getInitParameter("info");
			if (info != null && !info.isEmpty()) {
				this.info = info;
			}
			String springIntegration = servletConfig.getInitParameter("springIntegration");
			ServiceBrokerSettings settings;
			if (springIntegration == null || "true".equals(springIntegration)) {
				settings = new ServiceBrokerSettings();

				// Spring integration enabled

				// TODO Move to spring component registry
				// SpringComponentRegistry componentRegistry = new
				// SpringComponentRegistry();
				// ApplicationContext ctx =
				// getServletContext().getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
				// componentRegistry.setApplicationContext(ctx);
				// settings.setComponents(componentRegistry);

			} else {

				// Spring integration disabled
				String configPath = servletConfig.getInitParameter("configPath");
				if (configPath != null && !configPath.isEmpty()) {

					// Load configuration from file
					settings = new ServiceBrokerSettings(configPath);
					
				} else {

					// Get configuration from web.xml
					settings = new ServiceBrokerSettings();
					Enumeration<String> names = servletConfig.getInitParameterNames();
					Tree config = new Tree();
					String name;
					while (names.hasMoreElements()) {
						name = names.nextElement();
						config.put(name, servletConfig.getInitParameter(name));
					}
					settings.setConfig(config);
					
				}
			}

			// Create internal gateway
			servletGateway = new ApiGateway() {
			};

			// TODO settings.setApiGateway(servletGateway);

			// Create Service Broker
			broker = new ServiceBroker(settings);

			// Start broker (load services, start transporters, etc.)
			broker.start();

		} catch (Exception cause) {
			bootException = cause;
			throw new ServletException(bootException);
		}
	}

	@Override
	public String getServletInfo() {
		return info;
	}

	// --- DESTROY SERVLET ---

	@Override
	public void destroy() {
		if (broker != null) {
			broker.stop();
			broker = null;
		}
		servletGateway = null;
	}

	// --- PROCESS INCOMING HTTP-REQUEST ---

	@Override
	public void service(ServletRequest req, ServletResponse rsp) throws ServletException, IOException {
		try {
			if (bootException != null) {
				throw new ServletException(bootException);
			}

			// Read method and path
			HttpServletRequest httpReq = (HttpServletRequest) req;
			String method = httpReq.getMethod();
			String path = httpReq.getRequestURI();

			// Read headers
			Tree headers = new Tree();
			Enumeration<String> headerNames = httpReq.getHeaderNames();
			String name;
			while (headerNames.hasMoreElements()) {
				name = headerNames.nextElement();
				headers.put(name, httpReq.getHeader(name));
			}

			// Read request
			String query = httpReq.getQueryString();
			byte[] bytes = null;
			if (query == null) {
				int contentLength = httpReq.getContentLength();
				if (contentLength != 0) {
					InputStream in = null;
					try {
						in = httpReq.getInputStream();
						ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(contentLength, 8192));
						byte[] packet = new byte[4096];
						int count;
						while ((count = in.read(packet, 0, packet.length)) != -1) {
							buffer.write(packet, 0, count);
						}
						bytes = buffer.toByteArray();
					} finally {
						if (in != null) {
							in.close();
						}
					}
				}
			} else {
				bytes = query.getBytes(StandardCharsets.UTF_8);
			}

			// Start asynchronous processing
			final AsyncContext async = req.startAsync();

			// Invoke action
			servletGateway.processRequest(method, path, headers, bytes).then(response -> {
				try {

				} finally {
					async.complete();
				}
			}).Catch(cause -> {
				try {

				} finally {
					async.complete();
				}
			});

		} catch (Throwable cause) {

			// TODO Send error in JSON format

		}
	}
	
}