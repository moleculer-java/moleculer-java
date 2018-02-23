package services.moleculer.config;

import static services.moleculer.util.CommonUtils.nameOf;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import services.moleculer.ServiceBroker;
import services.moleculer.service.Service;

public class SpringLoader implements ApplicationContextAware {

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		
		// Find Services in Spring Application Context
		ServiceBroker broker = ctx.getBean(ServiceBroker.class);
		Map<String, Service> serviceMap = ctx.getBeansOfType(Service.class);
		for (Service service : serviceMap.values()) {
			broker.createService(nameOf(service, false), service);
		}
		
	}

}
