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
package services.moleculer;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketListener;
import services.moleculer.stream.PacketStream;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			// Create Message Broker
			ServiceBroker broker = ServiceBroker.builder().build();

			// Deploy services
			broker.createService(new MathService());
			broker.createService(new SenderService());
			broker.createService(new ReceiverService());

			// Start Message Broker
			broker.start();

			// Create input
			Tree in = new Tree();
			in.put("a", 3);
			in.put("b", 5);

			// Local or remote method call
			broker.call("math.add", in).then(rsp -> {

				// Response
				broker.getLogger().info("Response: " + rsp.asInteger());

			});

			// Broadcast event
			broker.broadcast("foo.xyz", "a", 3, "b", 5);

			// Invoke sender service
			broker.call("sender.send");
			
			// Stop Message Broker
			broker.stop();

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("STOP");
	}

	@Name("sender")
	public static class SenderService extends Service {

		public Action send = ctx -> {
			PacketStream output = null;
			Promise promise = null;
			
			// Send request (as stream)
			try {

				// Create stream
				output = broker.createStream();

				// Open stream
				promise = ctx.call("receiver.receive", output);

				// Push bytes into the output stream's queue
				output.write("hello".getBytes());
				
			} finally {

				// Close stream
				if (output != null) {
					output.close();
				}
			}
			
			// Receive response stream
			promise.then(in -> {
				
				PacketStream input = (PacketStream) in.asObject();
				input.addPacketListener(new PacketListener() {
					
					@Override
					public void onData(byte[] bytes) throws Exception {
						System.out.println("RESPONSE DATA: " + new String(bytes));
					}
					
					@Override
					public void onError(Throwable cause) throws Exception {
						System.out.println("RESPONSE ERROR: " + cause);
					}
										
					@Override
					public void onClose() throws Exception {
						System.out.println("RESPONSE CLOSED.");
					}
					
				});
				
			}).catchError(err -> {
				
				// Catch error
				logger.error("Unable to receive response!", err);
				
			});
			
			return null;
		};

	};

	@Name("receiver")
	public static class ReceiverService extends Service {

		public Action receive = ctx -> {
			
			PacketStream input = (PacketStream) ctx.params.asObject();
			PacketStream output = broker.createStream();
			input.addPacketListener(new PacketListener() {
				
				@Override
				public void onError(Throwable cause) throws Exception {
					System.out.println("REQUEST ERROR: " + cause);
					output.error(cause);
				}
				
				@Override
				public void onData(byte[] bytes) throws Exception {
					System.out.println("REQUEST BYTES: " + new String(bytes));
					output.write(bytes);
				}
				
				@Override
				public void onClose() throws Exception {
					System.out.println("REQUEST CLOSED.");
					output.close();
				}
				
			});
			return output;
			
		};

	};

	@Name("math")
	public static class MathService extends Service {

		@Cache(keys = { "a", "b" }, ttl = 5000)
		public Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
		};

		@Subscribe("foo.*")
		public Listener listener = payload -> {
			logger.info("Event received: " + payload);
		};

	};

}