package services.moleculer.config;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Middleware;
import services.moleculer.service.Service;

public class SpringLoader implements ApplicationContextAware {

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		
		// Get Service Broker
		ServiceBroker broker = ctx.getBean(ServiceBroker.class);
		if (broker == null) {
			throw new NoSuchBeanDefinitionException(ServiceBroker.class);
		}

		// Find Middlewares in Spring Application Context
		Map<String, Middleware> middlewareMap = ctx.getBeansOfType(Middleware.class);
		if (!middlewareMap.isEmpty()) {
			Middleware[] middlewares = new Middleware[middlewareMap.size()];
			middlewareMap.values().toArray(middlewares);
			broker.use(middlewares);
		}

		// Find Services in Spring Application Context
		Map<String, Service> serviceMap = ctx.getBeansOfType(Service.class);
		for (Service service : serviceMap.values()) {
			broker.createService(service);
		}
		
	}

}
