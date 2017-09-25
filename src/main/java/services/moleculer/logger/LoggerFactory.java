package services.moleculer.logger;

public interface LoggerFactory {

	public Logger getLogger(String name);

	public default Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

}