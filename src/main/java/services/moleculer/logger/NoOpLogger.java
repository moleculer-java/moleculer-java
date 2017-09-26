package services.moleculer.logger;

public final class NoOpLogger implements Logger {

	@Override
	public final void trace(Object msg) {
	}

	@Override
	public final boolean isTraceEnabled() {
		return false;
	}

	@Override
	public final void debug(Object msg) {
	}

	@Override
	public final boolean isDebugEnabled() {
		return false;
	}

	@Override
	public final void info(Object msg) {
	}

	@Override
	public final boolean isInfoEnabled() {
		return false;
	}

	@Override
	public final void warn(Object msg) {
	}

	@Override
	public final void warn(Object msg, Throwable cause) {
	}

	@Override
	public final boolean isWarnEnabled() {
		return false;
	}

	@Override
	public final void error(Object msg) {
	}

	@Override
	public final void error(Object msg, Throwable cause) {
	}

	@Override
	public final boolean isErrorEnabled() {
		return false;
	}

	@Override
	public final void fatal(Object msg) {
	}

	@Override
	public final void fatal(Object msg, Throwable cause) {
	}

	@Override
	public final boolean isFatalEnabled() {
		return false;
	}

}