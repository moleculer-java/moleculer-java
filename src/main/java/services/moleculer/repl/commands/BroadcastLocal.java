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
package services.moleculer.repl.commands;

import java.io.PrintStream;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.repl.Command;
import services.moleculer.service.Name;

/**
 * Broadcast an event to local services. Sample of usage:<br>
 * <br>
 * broadcastLocal eventName {"a":3,"b":false} <b>(recommended)</b><br>
 * or<br>
 * broadcastLocal eventName '{"a":3,"b":false}'<br>
 * or<br>
 * broadcastLocal eventName --a 3 --b false<br>
 * or<br>
 * broadcastLocal eventName a 3 b false
 */
@Name("broadcastLocal")
public class BroadcastLocal extends Command {

	@Override
	public String getDescription() {
		return "Broadcast an event locally";
	}

	@Override
	public String getUsage() {
		return "broadcastLocal <eventName>";
	}

	@Override
	public int getNumberOfRequiredParameters() {
		return 1;
	}

	@Override
	public void onCommand(ServiceBroker broker, PrintStream out, String[] parameters) throws Exception {
		String name = parameters[0];
		Tree payload = getPayload(parameters);
		out.println(">> Broadcast '" + name + "' locally with payload: " + payload.toString(false));
		broker.broadcastLocal(name, payload);
	}

}