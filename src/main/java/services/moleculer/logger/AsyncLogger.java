package services.moleculer.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;

import co.paralleluniverse.fibers.Fiber;
import services.moleculer.fibers.FiberEngine;

public final class AsyncLogger implements Logger {

	// --- BLOCKING LOGGER ---

	private final Logger logger;

	// --- CONSTRUCTOR ---

	AsyncLogger(Logger logger) {
		this.logger = logger;
	}

	// --- DELEGATED METHODS ---

	public final String getName() {
		return logger.getName();
	}

	public final boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public final void trace(String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, msg);
		} else {
			logger.trace(msg);
		}
	}

	public final void trace(String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, format, arg);
		} else {
			logger.trace(format, arg);
		}
	}

	public final void trace(String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, format, arg1, arg2);
		} else {
			logger.trace(format, format, arg1, arg2);
		}
	}

	public final void trace(String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, format, arguments);
		} else {
			logger.trace(format, format, arguments);
		}
	}

	public final void trace(String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, msg, t);
		} else {
			logger.trace(msg, t);
		}
	}

	public final boolean isTraceEnabled(Marker marker) {
		return logger.isTraceEnabled(marker);
	}

	public final void trace(Marker marker, String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, marker, msg);
		} else {
			logger.trace(marker, msg);
		}
	}

	public final void trace(Marker marker, String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, format, arg);
		} else {
			logger.trace(marker, format, arg);
		}
	}

	public final void trace(Marker marker, String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, marker, format, arg1, arg2);
		} else {
			logger.trace(marker, format, arg1, arg2);
		}
	}

	public final void trace(Marker marker, String format, Object... argArray) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, marker, format, argArray);
		} else {
			logger.trace(marker, format, argArray);
		}
	}

	public final void trace(Marker marker, String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::trace, marker, msg, t);
		} else {
			logger.trace(marker, msg, t);
		}
	}

	public final boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public final void debug(String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, msg);
		} else {
			logger.debug(msg);
		}
	}

	public final void debug(String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, format, arg);
		} else {
			logger.debug(format, arg);
		}
	}

	public final void debug(String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, format, arg1, arg2);
		} else {
			logger.debug(format, arg1, arg2);
		}
	}

	public final void debug(String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, format, arguments);
		} else {
			logger.debug(format, arguments);
		}
	}

	public final void debug(String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, msg, t);
		} else {
			logger.debug(msg, t);
		}
	}

	public final boolean isDebugEnabled(Marker marker) {
		return logger.isDebugEnabled(marker);
	}

	public final void debug(Marker marker, String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, marker, msg);
		} else {
			logger.debug(marker, msg);
		}
	}

	public final void debug(Marker marker, String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, marker, format, arg);
		} else {
			logger.debug(marker, format, arg);
		}
	}

	public final void debug(Marker marker, String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, marker, format, arg1, arg2);
		} else {
			logger.debug(marker, format, arg1, arg2);
		}
	}

	public final void debug(Marker marker, String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, marker, format, arguments);
		} else {
			logger.debug(marker, format, arguments);
		}
	}

	public final void debug(Marker marker, String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::debug, marker, msg, t);
		} else {
			logger.debug(marker, msg, t);
		}
	}

	public final boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public final void info(String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, msg);
		} else {
			logger.info(msg);
		}
	}

	public final void info(String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, format, arg);
		} else {
			logger.info(format, arg);
		}
	}

	public final void info(String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, format, arg1, arg2);
		} else {
			logger.info(format, arg1, arg2);
		}
	}

	public final void info(String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, format, arguments);
		} else {
			logger.info(format, arguments);
		}
	}

	public final void info(String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, msg, t);
		} else {
			logger.info(msg, t);
		}
	}

	public final boolean isInfoEnabled(Marker marker) {
		return logger.isInfoEnabled(marker);
	}

	public final void info(Marker marker, String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, marker, msg);
		} else {
			logger.info(marker, msg);
		}
	}

	public final void info(Marker marker, String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, marker, format, arg);
		} else {
			logger.info(marker, format, arg);
		}
	}

	public final void info(Marker marker, String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, marker, format, arg1, arg2);
		} else {
			logger.info(marker, format, arg1, arg2);
		}
	}

	public final void info(Marker marker, String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, marker, format, arguments);
		} else {
			logger.info(marker, format, arguments);
		}
	}

	public final void info(Marker marker, String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::info, marker, msg, t);
		} else {
			logger.info(marker, msg, t);
		}
	}

	public final boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	public final void warn(String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, msg);
		} else {
			logger.warn(msg);
		}
	}

	public final void warn(String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, format, arg);
		} else {
			logger.warn(format, arg);
		}
	}

	public final void warn(String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, format, arguments);
		} else {
			logger.warn(format, arguments);
		}
	}

	public final void warn(String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, format, arg1, arg2);
		} else {
			logger.warn(format, arg1, arg2);
		}
	}

	public final void warn(String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, msg, t);
		} else {
			logger.warn(msg, t);
		}
	}

	public final boolean isWarnEnabled(Marker marker) {
		return logger.isWarnEnabled(marker);
	}

	public final void warn(Marker marker, String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, marker, msg);
		} else {
			logger.warn(marker, msg);
		}
	}

	public final void warn(Marker marker, String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, marker, format, arg);
		} else {
			logger.warn(marker, format, arg);
		}
	}

	public final void warn(Marker marker, String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, marker, format, arg1, arg2);
		} else {
			logger.warn(marker, format, arg1, arg2);
		}
	}

	public final void warn(Marker marker, String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, marker, format, arguments);
		} else {
			logger.warn(marker, format, arguments);
		}
	}

	public final void warn(Marker marker, String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::warn, marker, msg, t);
		} else {
			logger.warn(marker, msg, t);
		}
	}

	public final boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public final void error(String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, msg);
		} else {
			logger.error(msg);
		}
	}

	public final void error(String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, format, arg);
		} else {
			logger.error(format, arg);
		}
	}

	public final void error(String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, format, arg1, arg2);
		} else {
			logger.error(format, arg1, arg2);
		}
	}

	public final void error(String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, format, arguments);
		} else {
			logger.error(format, arguments);
		}
	}

	public final void error(String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, msg, t);
		} else {
			logger.error(msg, t);
		}
	}

	public final boolean isErrorEnabled(Marker marker) {
		return logger.isErrorEnabled(marker);
	}

	public final void error(Marker marker, String msg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, marker, msg);
		} else {
			logger.error(marker, msg);
		}
	}

	public final void error(Marker marker, String format, Object arg) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, marker, format, arg);
		} else {
			logger.error(marker, format, arg);
		}
	}

	public final void error(Marker marker, String format, Object arg1, Object arg2) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, marker, format, arg1, arg2);
		} else {
			logger.error(marker, format, arg1, arg2);
		}
	}

	public final void error(Marker marker, String format, Object... arguments) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, marker, format, arguments);
		} else {
			logger.error(marker, format, arguments);
		}
	}

	public final void error(Marker marker, String msg, Throwable t) {
		if (Fiber.isCurrentFiber()) {
			FiberEngine.runBlocking(logger::error, marker, msg, t);
		} else {
			logger.error(marker, msg, t);
		}
	}

}