package services.moleculer.logger;

import static services.moleculer.config.ServiceBrokerConfig.AGENT_ENABLED;
import static services.moleculer.config.ServiceBrokerConfig.ASYNC_LOGGING_ENABLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AsyncLoggerFactory {

	public static final Logger getLogger(String name) {
		Logger logger = LoggerFactory.getLogger(name);
		if (AGENT_ENABLED && ASYNC_LOGGING_ENABLED) {
			return new AsyncLogger(logger);
		}
		return logger;
	}

	public static final Logger getLogger(Class<?> clazz) {
		Logger logger = LoggerFactory.getLogger(clazz);
		if (AGENT_ENABLED && ASYNC_LOGGING_ENABLED) {
			return new AsyncLogger(logger);
		}
		return logger;
	}

}
