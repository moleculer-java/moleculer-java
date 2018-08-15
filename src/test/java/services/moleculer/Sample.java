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

import java.io.File;

import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.NonBlockingQueue;
import services.moleculer.stream.PacketReceiver;
import services.moleculer.stream.PacketStream;
import services.moleculer.transporter.RedisTransporter;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			// Create Message Broker
			ServiceBroker broker = ServiceBroker.builder().transporter(new RedisTransporter()).build();

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
			broker.call("java.math.add", in).then(rsp -> {

				// Response
				broker.getLogger().info("Response: " + rsp.asInteger());

			});

			// Broadcast event
			broker.broadcast("foo.xyz", "a", 3, "b", 5);

			in = new Tree().put("withActions", true);
			
			System.out.println(broker.getConfig().getServiceRegistry().getDescriptor());
			
			System.out.println("--------------------------");
			
			broker.call("$node.services", in).then(rsp -> {

				// Response
				broker.getLogger().info("services response: " + rsp);

			});
					
			broker.call("$node.actions", in).then(rsp -> {

				// Response
				broker.getLogger().info("actions response: " + rsp);

			});

			broker.call("$node.events", in).then(rsp -> {

				// Response
				broker.getLogger().info("events response: " + rsp);

			});

			// Invoke sender service
			// broker.call("sender.send");
			
			Thread.sleep(100000);
			
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
			
			System.out.println("SENDER - called");
			
			File file1 = new File("/temp/test1.txt");
			PacketStream output = new PacketStream(file1);
			
			output.pipe().then(submitted -> {
				
				long submittedBytes = submitted.asLong();
				System.out.println("SENDER - submitted bytes: " + submittedBytes);
				
			}).catchError(err -> {
				
				System.out.println("SENDER - unable to submit: " + err);
				
			});
			
			ctx.call("receiver.receive", output).then(rsp -> {
				
				PacketStream input = (PacketStream) rsp.asObject();
				File file2 = new File("/temp/test2.txt");
				input.pipe(file2).then(received -> {
				
					long receivedBytes = received.asLong();
					System.out.println("SENDER - received bytes: " + receivedBytes);
					
				});
				
			});
			
			return null;
		};

	};

	@Name("receiver")
	public static class ReceiverService extends Service {

		public Action receive = ctx -> {
			
			System.out.println("RECEIVER - called");
			
			NonBlockingQueue queue = new NonBlockingQueue();
			PacketStream output = new PacketStream(queue);

			ctx.stream.pipe(new PacketReceiver() {
				
				@Override
				public void onData(byte[] bytes) throws Exception {
					System.out.println("RECEIVER - Sending back " + bytes.length + " bytes...");
					queue.sendData(bytes);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					System.out.println("RECEIVER - Sending back: error (" + cause + ")");
					queue.sendError(cause);
				}
								
				@Override
				public void onClose() throws Exception {
					System.out.println("RECEIVER - Sending back: close");
					queue.sendClose();
				}
				
			});
			
			return output;
		};

	};

	@Name("java.math")
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