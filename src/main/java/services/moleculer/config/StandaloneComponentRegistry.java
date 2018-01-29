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
package services.moleculer.config;

import static services.moleculer.util.CommonUtils.scan;

import java.util.LinkedList;
import java.util.List;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * Standalone Component Registry. It's the simplest way to start a Moleculer
 * Service Broker without any CDI framework (eg. without Spring or Guice).
 * Sample code, to create a new Service Broker:<br>
 * <br>
 * ServiceBroker broker = ServiceBroker.builder()<br>
 * &nbsp;&nbsp;&nbsp;.components(new
 * StandaloneComponentRegistry("my.service.package"))<br>
 * &nbsp;&nbsp;&nbsp;.build();<br>
 * broker.start();
 * 
 * @see SpringComponentRegistry
 * @see GuiceComponentRegistry
 */
@Name("Standalone Component Registry")
public final class StandaloneComponentRegistry extends CommonRegistry {

	// --- PACKAGES TO SCAN ---

	/**
	 * Java package(s) where your Moleculer Services and Components are located.
	 * This is an optional parameter, you can add Services and Components
	 * directly to the {@code ServiceBroker}.
	 */
	private String[] packagesToScan;

	// --- CONSTRUCTORS ---

	/**
	 * Creates a Component Registry without "packagesToScan" parameter. You can
	 * add Services and Components later directly to the {@code ServiceBroker}.
	 */
	public StandaloneComponentRegistry() {
	}

	/**
	 * Creates a new Component Registry.
	 * 
	 * @param packagesToScan
	 *            package(s) where your Moleculer Services and Components are
	 *            located
	 */
	public StandaloneComponentRegistry(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	// --- FIND COMPONENTS AND SERVICES ---

	@Override
	protected final void findServices(ServiceBroker broker, Tree config) throws Exception {

		// Process config
		Tree packagesNode = config.get("packagesToScan");
		if (packagesNode != null) {
			if (packagesNode.isPrimitive()) {

				// List of packages
				String value = packagesNode.asString().trim();
				packagesToScan = value.split(",");
			} else {

				// Array structure of packages
				List<String> packageList = packagesNode.asList(String.class);
				if (!packageList.isEmpty()) {
					packagesToScan = new String[packageList.size()];
					packageList.toArray(packagesToScan);
				}
			}
		}

		// Scan classpath
		if (packagesToScan == null || packagesToScan.length == 0) {
			return;
		}
		for (String packageName : packagesToScan) {
			if (!packageName.isEmpty()) {
				LinkedList<String> classNames = scan(packageName);
				for (String className : classNames) {
					className = packageName + '.' + className;
					try {
						Object component = Class.forName(className).newInstance();
						if (isInternalComponent(component)) {
							continue;
						}

						// Store as custom component or service
						register(broker, component, config);
					} catch (Throwable cause) {
						logger.warn("Unable to load class \"" + className + "\"!", cause);
					}
				}
			}
		}
	}

}