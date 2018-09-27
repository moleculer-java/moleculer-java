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

import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.PacketStream;
import services.moleculer.transporter.RedisTransporter;
import services.moleculer.transporter.Transporter;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			// Create Message Brokers
			Transporter t1 = new RedisTransporter();
			Transporter t2 = new RedisTransporter();
			
			boolean debug = false;
			t1.setDebug(debug);
			t2.setDebug(debug);
			
			ServiceBroker broker1 = ServiceBroker.builder().transporter(t1).nodeID("sender").build();
			ServiceBroker broker2 = ServiceBroker.builder().transporter(t2).nodeID("receiver").build();

			// Deploy services
			broker1.createService(new SenderService());
			broker2.createService(new ReceiverService());

			// Start Message Broker
			broker1.start();
			broker2.start();
			broker1.waitForServices("receiver").waitFor(5000);

			// Invoke sender service
			Thread.sleep(1000);
			broker1.call("sender.send");
			Thread.sleep(120000);

			// Stop Message Brokers
			broker1.stop();
			broker2.stop();

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
			File file2 = new File("/temp/test2.txt");

			PacketStream output = broker.createStream();

			ctx.call("receiver.receive", output).then(rsp -> {

				PacketStream in = (PacketStream) rsp.asObject();
				System.out.println("SENDER - received stream: " + in);

				in.transferTo(file2).then(transfered -> {

					System.out.println("SENDER - file saved.");

				});

			});

			System.out.println("SENDER - start transfer");

			output.transferFrom(file1).then(rsp -> {

				System.out.println("SENDER - transfer finished");

			});

			return null;
		};

	};

	@Name("receiver")
	public static class ReceiverService extends Service {

		public Action receive = ctx -> {

			System.out.println("RECEIVER - called");

			PacketStream output = new PacketStream(broker.getConfig().getScheduler());

			ctx.stream.onPacket((bytes, cause, close) -> {
				if (bytes != null) {
					System.out.println("RECEIVER - Sending back " + bytes.length + " bytes...");
					output.sendData(bytes);
				}
				if (cause != null) {
					System.out.println("RECEIVER - Sending back: error (" + cause + ")");
					output.sendError(cause);
				}
				if (close) {
					System.out.println("RECEIVER - Sending back: close");
					output.sendClose();
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