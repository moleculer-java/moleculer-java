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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import io.datatree.Tree;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import services.moleculer.ServiceBroker;

/**
 * HTTP/1.1 API Gateway based on Netty framework.
 */
public class NettyGateway extends ApiGateway {

	// --- PROPERTIES ---

	protected int port = 3000;

	protected String address;

	protected int maxContentLength = 10485760;

	protected EventLoopGroup eventLoopGroup;

	protected ChannelHandler handler;

	// --- START NETTY SERVER ---

	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Worker group
		if (eventLoopGroup == null) {
			eventLoopGroup = new NioEventLoopGroup(1,
					new ThreadPerTaskExecutor(new DefaultThreadFactory(NettyGateway.class, Thread.MAX_PRIORITY - 1)));
		}

		// Create request chain
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(eventLoopGroup);
		bootstrap.channel(NioServerSocketChannel.class);

		// Define request chain
		if (handler == null) {
			NettyGateway nettyGateway = this;
			handler = new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast(new HttpRequestDecoder());
					p.addLast(new HttpObjectAggregator(maxContentLength, true));
					p.addLast(new ChunkedWriteHandler());
					p.addLast(new MoleculerHandler(nettyGateway));
				}

			};
		}

		// Set child handler
		bootstrap.childHandler(handler);

		// Start server
		if (address == null) {
			bootstrap.bind(port);
		} else {
			bootstrap.bind(address, port);
		}
	}

	// --- STOP NETTY SERVER ---

	@Override
	public void stop() {
		if (eventLoopGroup != null) {
			eventLoopGroup.shutdownGracefully();
			eventLoopGroup = null;
		}
		handler = null;
	}

	// --- CHANNEL HANDLER ---

	protected static class MoleculerHandler extends SimpleChannelInboundHandler<Object> {

		// --- PARENT GATEWAY ---

		protected NettyGateway nettyGateway;

		// --- WEBSOCKET VARIABLES ---

		protected String path;
		protected WebSocketServerHandshaker handshaker;

		// --- CONSTRUCTOR ---

		protected MoleculerHandler(NettyGateway nettyGateway) {
			this.nettyGateway = nettyGateway;
		}

		// --- PROCESS INCOMING HTTP REQUEST ---

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Object request) throws Exception {
			try {

				// Process HTTP requests
				if (request instanceof FullHttpRequest) {
					FullHttpRequest httpRequest = (FullHttpRequest) request;

					// Get URI + QueryString
					path = httpRequest.uri();

					// Get HTTP headers
					HttpHeaders httpHeaders = httpRequest.headers();

					// Upgrade to WebSocket connection
					if (httpHeaders.contains("Upgrade")) {
						WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(path, null,
								true);
						handshaker = factory.newHandshaker(httpRequest);
						if (handshaker == null) {
							WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
						} else {
							handshaker.handshake(ctx.channel(), httpRequest);
						}
						return;
					}

					// Keep-alive
					CharSequence keepLiveHeader = httpHeaders.get(CONNECTION);
					boolean keepAlive = keepLiveHeader == null ? false
							: KEEP_ALIVE.equalsIgnoreCase(keepLiveHeader.toString());

					// HTTP method
					HttpMethod httpMethod = httpRequest.method();
					String method;
					if (httpMethod == null) {
						method = "UNKNOWN";
					} else {
						method = httpMethod.toString();
					}

					// Copy headers
					Tree headers = new Tree();
					for (Entry<String, String> entry : httpHeaders) {
						headers.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
					}

					// Get body
					int i = path.indexOf('?');
					byte[] bytes = null;
					if (i > -1) {
						path = path.substring(0, i);

						// Get query string
						int len = path.length();
						if (i < len - 1) {
							bytes = new byte[len - i - 1];
							char[] chars = path.toCharArray();
							for (int n = 0; n < bytes.length; n++) {
								bytes[n] = (byte) chars[i + n + 1];
							}
						}
					} else {
						bytes = readFully(httpRequest.content());
					}

					// Invoke Action
					nettyGateway.processRequest(method, path, headers, bytes).then(rsp -> {

						// Send normal HTTP response
						sendHttpResponse(ctx, keepAlive, rsp);

					}).Catch(cause -> {

						// Send HTTP error in JSON format
						sendHttpError(ctx, cause);
					});
					return;
				}

				// Process close/ping/continue WebSocket frames
				if (request instanceof CloseWebSocketFrame) {
					handshaker.close(ctx.channel(), ((CloseWebSocketFrame) request).retain());
					return;
				}
				if (request instanceof PingWebSocketFrame) {
					ctx.channel()
							.writeAndFlush(new PongWebSocketFrame(((PingWebSocketFrame) request).content().retain()));
					return;
				}
				if (request instanceof ContinuationWebSocketFrame) {
					return;
				}

				// Process WebSocket message frame
				if (request instanceof WebSocketFrame) {
					nettyGateway.processRequest("WS", path, null, readFully(((WebSocketFrame) request).content()))
							.then(rsp -> {

								// Send websocket response
								sendWebSocketResponse(ctx, rsp);

							}).Catch(cause -> {

								// Send error in JSON format
								sendWebSocketError(ctx, cause);

							});
					return;
				}

				// Unknown package type
				throw new IllegalStateException("Unknown package type: " + request);

			} catch (Throwable cause) {

				// Send error in JSON format
				if (handshaker == null) {
					sendHttpError(ctx, cause);
				} else {
					sendWebSocketError(ctx, cause);
				}
			}
		}

		// --- READ BUFFER ---

		protected byte[] readFully(ByteBuf byteBuffer) {
			ByteBuffer buffer = byteBuffer.nioBuffer();
			byte[] bytes = new byte[buffer.remaining()];
			if (bytes.length > 0) {
				buffer.get(bytes);
			}
			return bytes;
		}

		// --- SEND METHODS ---

		protected void sendWebSocketResponse(ChannelHandlerContext ctx, Tree rsp) {

			// TODO send websocket response

		}

		protected void sendWebSocketError(ChannelHandlerContext ctx, Throwable cause) {

			// TODO send websocket error

		}

		protected void sendHttpResponse(ChannelHandlerContext ctx, boolean keepAlive, Tree rsp) {

			// Default status
			String status = STATUS_200;
			Tree headers = null;

			// Get status code and response headers
			Tree meta = rsp.getMeta(false);
			if (meta != null) {
				Tree response = rsp.get("response");
				if (response != null) {
					status = response.get("status", status);
					headers = response.get("headers");
				}
			}

			// Convert body
			byte[] body = null;
			if (rsp != null && !rsp.isNull()) {
				Class<?> type = rsp.getType();
				if (type == byte[].class) {
					if (headers == null) {
						headers = rsp.putMap("_meta.response.headers");
					}
					String contentType = headers.get(CONTENT_TYPE, (String) null);
					if (contentType == null || contentType.isEmpty()) {
						contentType = "application/octetstream";
					}
					body = rsp.asBytes();
				} else if (type == String.class) {
					if (headers == null) {
						headers = rsp.putMap("_meta.response.headers");
					}
					String contentType = headers.get(CONTENT_TYPE, (String) null);
					String content = rsp.asString();
					if (contentType == null || contentType.isEmpty()) {
						if (content.toLowerCase().contains("<html")) {
							contentType = "text/html;charset=utf-8";
						} else {
							contentType = "text/plain;charset=utf-8";
						}
					}
					body = content.getBytes(StandardCharsets.UTF_8);
				} else {
					body = rsp.toBinary(null, false);
				}
			}

			// Send body
			sendHttpResponse(ctx, status, headers, keepAlive, body);
		}

		protected void sendHttpError(ChannelHandlerContext ctx, Throwable cause) {

			// Send HTTP error response
			String message = null;
			String trace = null;
			if (cause != null) {
				message = cause.getMessage();
				StringWriter traceWriter = new StringWriter(512);
				cause.printStackTrace(new PrintWriter(traceWriter, true));
				trace = traceWriter.toString().replace('\t', ' ').replace("\r", "\\r").replace("\n", "\\n")
						.replace("\"", "\\\"").trim();
			}
			if (message != null) {
				message = message.replace('\r', ' ').replace('\t', ' ').replace('\n', ' ').replace("\"", "\\\"").trim();
			}
			if (message == null || message.isEmpty()) {
				message = "Unexpected error occured!";
			}

			// Create JSON error message
			StringBuilder json = new StringBuilder(256);
			json.append("{\r\n  \"message\":\"");
			json.append(message);
			if (trace != null) {
				json.append("\",\r\n  \"trace\":\"");
				json.append(trace);
			}
			json.append("\"\r\n}");
			byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
			sendHttpResponse(ctx, STATUS_500, null, true, bytes);
		}

		protected void sendHttpResponse(ChannelHandlerContext ctx, String status, Tree headers, boolean keepAlive,
				byte[] body) {

			// Create HTTP response
			StringBuilder httpHeader = new StringBuilder(512);
			httpHeader.append("HTTP/1.1 ");
			if (status == null) {
				httpHeader.append(STATUS_200);
			} else {
				httpHeader.append(status);
			}
			if (headers == null) {
				httpHeader.append("\r\nContent-Type:application/json;charset=utf-8");
			} else {
				String name, value;
				boolean found = false;
				for (Tree header : headers) {
					name = header.getName();
					if (name.equals(CONNECTION) || name.equals(CONTENT_LENGTH)) {
						continue;
					}
					if (!found && CONTENT_TYPE.equalsIgnoreCase(name)) {
						found = true;
					}
					value = header.asString();
					if (value != null) {
						httpHeader.append("\r\n");
						httpHeader.append(name);
						httpHeader.append(':');
						httpHeader.append(value);
					}
				}
				if (!found) {
					httpHeader.append("\r\nContent-Type:application/json;charset=utf-8");
				}
			}

			int contentLength = body == null ? 0 : body.length;
			httpHeader.append("\r\n");
			httpHeader.append(CONTENT_LENGTH);
			httpHeader.append(':');
			httpHeader.append(contentLength);
			httpHeader.append("\r\n");
			httpHeader.append(CONNECTION);
			httpHeader.append(':');
			if (keepAlive) {
				httpHeader.append(KEEP_ALIVE);
			} else {
				httpHeader.append(CLOSE);
			}
			httpHeader.append("\r\n\r\n");

			// Write HTTP headers
			ChannelFuture last = ctx
					.write(Unpooled.wrappedBuffer(httpHeader.toString().getBytes(StandardCharsets.UTF_8)));

			// Write body
			if (body != null) {
				last = ctx.write(Unpooled.wrappedBuffer(body));
			}
			if (!keepAlive) {
				last.addListener(ChannelFutureListener.CLOSE);
			}

			// Flush response
			ctx.flush();
		}
	}

	// --- GETTERS AND SETTERS ---

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String host) {
		this.address = host;
	}

	public EventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}

	public void setEventLoopGroup(EventLoopGroup parentEventLoopGroup) {
		this.eventLoopGroup = parentEventLoopGroup;
	}

	public ChannelHandler getHandler() {
		return handler;
	}

	public void setHandler(ChannelHandler childHandler) {
		this.handler = childHandler;
	}

	public int getMaxContentLength() {
		return maxContentLength;
	}

	public void setMaxContentLength(int maxContentLength) {
		this.maxContentLength = maxContentLength;
	}

}