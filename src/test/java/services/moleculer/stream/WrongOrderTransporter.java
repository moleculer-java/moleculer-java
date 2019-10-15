/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
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
package services.moleculer.stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.transporter.Transporter;

public class WrongOrderTransporter extends Transporter implements Runnable {

	protected ScheduledFuture<?> timer;

	protected static HashMap<String, ConcurrentLinkedDeque<Tree>> channels = new HashMap<>();

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		timer = broker.getConfig().getScheduler().scheduleAtFixedRate(this, 500, 500, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stopped() {
		super.stopped();
		if (timer != null) {
			timer.cancel(false);
			timer = null;
		}
		synchronized (channels) {
			channels.clear();
		}
	}

	@Override
	public void connect() {
		connected();
	}

	@Override
	public void publish(String channel, Tree message) {
		HashSet<ConcurrentLinkedDeque<Tree>> queues = new HashSet<>();
		synchronized (channels) {
			for (Map.Entry<String, ConcurrentLinkedDeque<Tree>> entry : channels.entrySet()) {
				String test = entry.getKey();
				if (test.endsWith(':' + channel)) {
					queues.add(entry.getValue());
				}
			}
		}
		for (ConcurrentLinkedDeque<Tree> queue : queues) {
			queue.addLast(message);
		}
	}

	@Override
	public Promise subscribe(String channel) {
		ConcurrentLinkedDeque<Tree> queue;
		synchronized (channels) {
			queue = channels.get(nodeID + ':' + channel);
			if (queue == null) {
				queue = new ConcurrentLinkedDeque<>();
				channels.put(nodeID + ':' + channel, queue);
			}
		}
		return Promise.resolve();
	}

	public void run() {
		synchronized (channels) {
			for (Map.Entry<String, ConcurrentLinkedDeque<Tree>> entry : channels.entrySet()) {
				String channel = entry.getKey();
				if (!channel.startsWith(nodeID + ':')) {
					continue;
				}
				int i = channel.indexOf(':');
				channel = channel.substring(i + 1);
				ConcurrentLinkedDeque<Tree> queue = entry.getValue();
				while (!queue.isEmpty()) {
					Tree message = queue.removeLast();
					try {
						received(channel, serializer.write(message));
					} catch (Exception cause) {
						logger.error("Unable to serialize message!", cause);
					}
				}
			}
		}
	}

}