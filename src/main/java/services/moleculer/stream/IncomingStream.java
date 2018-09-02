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
package services.moleculer.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;

import io.datatree.Promise;

/**
 * !!! These package are in development phase !!!
 */
public class IncomingStream {

	// --- VARIABLES ---

	protected HashSet<DataListener> dataListeners = new HashSet<>();
	protected HashSet<ErrorListener> errorListeners = new HashSet<>();
	protected HashSet<CloseListener> closeListeners = new HashSet<>();

	// --- RECEIVE BYTES ---

	public IncomingStream onData(DataListener listener) {
		dataListeners.add(listener);
		return this;
	}

	// --- RECEIVE ERROR ---

	public IncomingStream onError(ErrorListener listener) {
		errorListeners.add(listener);
		return this;
	}

	// --- RECEIVE CLOSE MARKER ---

	public IncomingStream onClose(CloseListener listener) {
		closeListeners.add(listener);
		return this;
	}

	// --- TRANSFER METHODS ---

	public Promise transferTo(File destination) {
		try {
			return transferTo(new FileOutputStream(destination));
		} catch (Throwable cause) {
			return Promise.reject(cause);
		}
	}

	public Promise transferTo(OutputStream destination) {
		return new Promise(res -> {
			onData(bytes -> {
				synchronized (destination) {
					destination.write(bytes);
				}
			});
			onError(err -> {
				synchronized (destination) {
					try {
						destination.close();
					} catch (Throwable ignored) {
					}
				}
				res.reject(err);
			});
			onClose(() -> {
				Throwable err = null;
				synchronized (destination) {
					try {
						destination.flush();
						destination.close();
					} catch (Throwable cause) {
						err = cause;
					}
				}
				if (err == null) {
					res.resolve();
				} else {
					res.reject(err);
				}
			});
		});
	}

	public Promise transferTo(WritableByteChannel destination) {
		return new Promise(res -> {
			onData(bytes -> {
				ByteBuffer buffer = ByteBuffer.wrap(bytes);
				synchronized (destination) {
					while (buffer.hasRemaining()) {
						destination.write(buffer);
					}
				}
			});
			onError(err -> {
				synchronized (destination) {
					try {
						destination.close();
					} catch (Throwable ignored) {
					}
				}
				res.reject(err);
			});
			onClose(() -> {
				Throwable err = null;
				synchronized (destination) {
					try {
						destination.close();
					} catch (Throwable cause) {
						err = cause;
					}
				}
				if (err == null) {
					res.resolve();
				} else {
					res.reject(err);
				}
			});
		});
	}

}