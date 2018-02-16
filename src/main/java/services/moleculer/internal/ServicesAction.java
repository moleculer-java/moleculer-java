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
package services.moleculer.internal;

import static services.moleculer.util.CommonUtils.getNodeInfos;

import java.util.Arrays;
import java.util.HashMap;

import io.datatree.Tree;
import services.moleculer.context.Context;

/**
 * Implementation of the "$node.services" action.
 */
public class ServicesAction extends AbstractInternalAction {

	@Override
	public Object handler(Context ctx) throws Exception {
		Tree root = new Tree();
		Tree list = root.putList("list");
		
		// Collect data
		Tree infos = getNodeInfos(broker, transporter);
		
		HashMap<String, HashMap<String, Tree>> serviceMap = new HashMap<>();
		for (Tree info : infos) {
			String nodeID = info.getName();
			Tree services = info.get("services");
			if (services == null || services.isNull()) {
				continue;
			}
			for (Tree service : services) {
				String serviceName = service.get("name", "unknown");
				HashMap<String, Tree> configs = serviceMap.get(serviceName);
				if (configs == null) {
					configs = new HashMap<String, Tree>();
					serviceMap.put(serviceName, configs);
				}
				configs.put(nodeID, service);
			}
		}
		
		// Sort names
		String[] serviceNames = new String[serviceMap.size()];
		serviceMap.keySet().toArray(serviceNames);
		Arrays.sort(serviceNames, String.CASE_INSENSITIVE_ORDER);
		
		for (String serviceName : serviceNames) {
			HashMap<String, Tree> configs = serviceMap.get(serviceName);
			if (configs == null) {
				continue;
			}
			
			
		}
		return list;
	}

}