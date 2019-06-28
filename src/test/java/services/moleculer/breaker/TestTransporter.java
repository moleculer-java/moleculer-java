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
package services.moleculer.breaker;

import java.util.HashMap;

import io.datatree.Promise;
import io.datatree.Tree;
import services.moleculer.transporter.Transporter;

public class TestTransporter extends Transporter {

	protected Tree messages = new Tree();
	protected Tree list;

	protected final HashMap<String, Integer> cpu = new HashMap<>();

	public TestTransporter() {
		list = messages.putList("list");
	}

	@Override
	public void connect() {
	}

	public void received(String channel, Tree message) throws Exception {
		byte[] bytes = serializer.write(message);
		super.received(channel, bytes);
	}

	@Override
	public void publish(String channel, Tree message) {
		message = message.clone();
		message.put("channel", channel);
		list.addObject(message);
	}

	@Override
	public Promise subscribe(String channel) {
		return Promise.resolve();
	}

	public Tree getMessage(String nodeID) {
		for (Tree message : list) {
			if (message.get("channel", "").endsWith('.' + nodeID)) {
				return message;
			}
		}
		return null;
	}

	public Tree getMessages() {
		return list;
	}

	public int getMessageCount() {
		return list.size();
	}

	public boolean hasMessage(String nodeID) {
		for (Tree message : list) {
			if (message.get("channel", "").endsWith('.' + nodeID)) {
				return true;
			}
		}
		return false;
	}

	public void clearMessages() {
		list.clear();
	}

	@Override
	public int getCpuUsage(String nodeID) {
		Integer usage = cpu.get(nodeID);
		if (usage == null) {
			return 0;
		}
		return usage.intValue();
	}

	public void setCpuUsage(String nodeID, int usage) {
		cpu.put(nodeID, usage);
	}

	public void clearCpuUsages() {
		cpu.clear();
	}

	public void broadcastInfoPacket() {

		// Do not send INFO packet
	}

}