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
package services.moleculer.config;

import static io.datatree.dom.PackageScanner.scan;
import static services.moleculer.util.CommonUtils.nameOf;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Service;

/**
 * Register Spring Components as Moleculer Services in ServiceBroker.
 * ServiceBroker must be a Spring Component also.
 */
public class SpringRegistrator implements ApplicationContextAware {

	// --- PROPERTIES ---

	/**
	 * Java package(s) where the custom Spring/Moleculer Services are located.
	 * Do not use if you use Spring's "ComponentScan" feature.
	 */
	protected String[] packagesToScan = {};

	// --- LOGGER ---

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	// --- LOAD MOLECULER SERVICES ---

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		try {
			HashSet<Class<?>> registeredTypes = new HashSet<>();

			// Get Service Broker
			ServiceBroker broker = applicationContext.getBean(ServiceBroker.class);

			// Find Services in Spring Application Context
			Map<String, Service> serviceMap = applicationContext.getBeansOfType(Service.class);
			if (serviceMap != null && !serviceMap.isEmpty()) {
				String name;
				for (Map.Entry<String, Service> service : serviceMap.entrySet()) {

					// Register Service in Broker
					name = service.getKey();
					registeredTypes.add(service.getClass());
					if (name != null && name.startsWith("$")) {
						broker.createService(name, service.getValue());
						logger.info("Service \"" + name + "\" registered.");
					} else {
						Service instance = service.getValue();
						broker.createService(instance);
						logger.info("Service \"" + instance.getName() + "\" registered.");
					}
				}
			}

			// Add new Services
			if (packagesToScan != null && packagesToScan.length > 0) {
				DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
						.getAutowireCapableBeanFactory();
				for (String packageName : packagesToScan) {
					if (!packageName.isEmpty()) {
						LinkedList<String> classNames = scan(packageName);
						for (String className : classNames) {
							if (className.indexOf('$') > -1) {
								continue;
							}
							className = packageName + '.' + className;
							Class<?> type = Class.forName(className);
							if (Service.class.isAssignableFrom(type)) {

								// Check type
								if (!registeredTypes.add(type)) {
									continue;
								}

								// Register Service in Spring
								String name = nameOf(type, false);
								BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(type)
										.setScope(BeanDefinition.SCOPE_SINGLETON)
										.setAutowireMode(DefaultListableBeanFactory.AUTOWIRE_BY_TYPE).setLazyInit(false)
										.getBeanDefinition();
								beanFactory.registerBeanDefinition(name, definition);
								Service service = (Service) beanFactory.createBean(type);

								// Register Service in Broker
								broker.createService(service);

								// Log
								logger.info("Service \"" + name + "\" registered.");
							}
						}
					}
				}
			}
		} catch (Exception cause) {
			throw new FatalBeanException("Unable to define Moleculer Service!", cause);
		}
	}

	// --- GETTERS / SETTERS ---

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

}