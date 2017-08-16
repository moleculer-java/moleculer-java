package services.moleculer;

public class Logger {

	public void trace(String msg) {
		System.out.println("[TRACE] " + msg);
	}
	
	public void debug(String msg) {
		System.out.println("[DEBUG] " + msg);
	}
	
	public void info(String msg) {
		System.out.println("[INFO]  " + msg);
	}
	
	public void warn(String msg) {
		System.out.println("[WARN]  " + msg);
	}
	
	public void error(String msg) {
		System.out.println("[ERROR] " + msg);
	}
	
	public void fatal(String msg) {
		System.out.println("[FATAL] " + msg);
	}
	
}
