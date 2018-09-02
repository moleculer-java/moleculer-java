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
import java.util.concurrent.ScheduledExecutorService;

import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.stream.IncomingStream;
import services.moleculer.stream.OutgoingStream;

public class Sample {

	public static void main(String[] args) throws Exception {
		System.out.println("START");
		try {

			// Create Message Broker
			// .transporter(new RedisTransporter())
			ServiceBroker broker = ServiceBroker.builder().build();

			// Deploy services
			broker.createService(new SenderService());
			broker.createService(new ReceiverService());

			// Start Message Broker
			broker.start();

			// Invoke sender service
			broker.call("sender.send");

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
			File file2 = new File("/temp/test2.txt");

			OutgoingStream output = new OutgoingStream();

			ctx.call("receiver.receive", output).then(rsp -> {

				IncomingStream in = (IncomingStream) rsp.asObject();
				System.out.println("SENDER - received stream: " + in);

				in.transferTo(file2).then(transfered -> {

					System.out.println("SENDER - file saved.");

				});

			});

			System.out.println("SENDER - start transfer");

			ScheduledExecutorService scheduler = broker.getConfig().getScheduler();
			
			output.transferFrom(file1, scheduler, 100, 100).then(rsp -> {

				System.out.println("SENDER - transfer finished");

			});

			return null;
		};

	};

	@Name("receiver")
	public static class ReceiverService extends Service {

		public Action receive = ctx -> {

			System.out.println("RECEIVER - called");

			OutgoingStream output = new OutgoingStream();

			ctx.stream.onData(bytes -> {
				System.out.println("RECEIVER - Sending back " + bytes.length + " bytes...");
				output.sendData(bytes);
			}).onError(cause -> {
				System.out.println("RECEIVER - Sending back: error (" + cause + ")");
				output.sendError(cause);
			}).onClose(() -> {
				System.out.println("RECEIVER - Sending back: close");
				output.sendClose();
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