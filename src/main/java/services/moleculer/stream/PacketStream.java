/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2018 Andras Berkes [andras.berkes@programmer.net]<br>
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import io.datatree.Promise;

public class PacketStream {

	// --- CONSTANTS ---

	protected static final int DEFAULT_BUFFER_SIZE = 1024 * 16;

	// --- VARIABLES ---

	protected PacketSource source;
	protected PacketReceiver destination;

	protected final Promise allTransfered = new Promise();

	// --- "TRANSFER FROM" / SOURCE METHODS ---

	public Promise transferFrom(PacketSource source) {
		this.source = source;
		return allTransfered;
	}

	public Promise transferFrom(File in) throws FileNotFoundException {
		return transferFrom(new FileInputStream(in));
	}

	public Promise transferFrom(InputStream in) {
		return transferFrom(in, true, DEFAULT_BUFFER_SIZE);
	}

	public Promise transferFrom(InputStream in, boolean closeStream, int bufferSize) {
		byte[] buffer = new byte[bufferSize];
		return transferFrom(new PacketSource() {

			@Override
			public void sendNextPacket(OutgoingPacket packet) throws Throwable {
				try {
					int len = in.read(buffer);
					if (len < 0) {
						try {
							packet.sendClose();
						} catch (Exception ignored) {
						}
						close();
						return;
					}
					byte[] copy = new byte[len];
					System.arraycopy(buffer, 0, copy, 0, len);
					packet.sendData(copy);
				} catch (Exception cause) {
					// TODO log exception
					try {
						packet.sendError(cause);
					} catch (Exception ignored) {
					}
					close();
				}
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeStream) {
					try {
						in.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	// --- DESTINATIONS ---

	protected Promise transferTo(PacketReceiver destination) {
		this.destination = destination;
		return allTransfered;
	}

	public Promise transferTo(File destination) throws FileNotFoundException {
		return transferTo(destination, false, DEFAULT_BUFFER_SIZE);
	}

	public Promise transferTo(File destination, boolean append, int bufferSize) throws FileNotFoundException {
		return transferTo(new FileOutputStream(destination, append), true, bufferSize);
	}

	public Promise transferTo(OutputStream destination) {
		boolean buffered = destination instanceof BufferedOutputStream;
		return transferTo(destination, true, buffered ? 0 : DEFAULT_BUFFER_SIZE);
	}

	public Promise transferTo(OutputStream destination, boolean closeStream, int bufferSize) {
		final OutputStream out = bufferSize > 1 ? new BufferedOutputStream(destination, bufferSize) : destination;
		return transferTo(new PacketReceiver() {

			@Override
			public void onData(byte[] bytes) throws Exception {
				out.write(bytes);
			}

			@Override
			public void onError(Throwable cause) throws Exception {

				// TODO log exception
				close();
			}

			@Override
			public void onClose() throws Exception {
				close();
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeStream) {
					try {
						out.flush();
						out.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	public Promise transferTo(WritableByteChannel destination) {
		return transferTo(destination, true, DEFAULT_BUFFER_SIZE);
	}

	public Promise transferTo(WritableByteChannel destination, boolean closeChannel, int bufferSize) {
		if (bufferSize > 1) {
			return transferTo(new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					write(new byte[] { (byte) b });
				}

				@Override
				public void write(byte[] b) throws IOException {
					destination.write(ByteBuffer.wrap(b));
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					destination.write(ByteBuffer.wrap(b, off, len));
				}

				@Override
				public void flush() throws IOException {
				}

				@Override
				public void close() throws IOException {
					destination.close();
				}

			}, closeChannel, bufferSize);
		}
		return transferTo(new PacketReceiver() {

			@Override
			public void onData(byte[] bytes) throws Exception {
				destination.write(ByteBuffer.wrap(bytes));
			}

			@Override
			public void onError(Throwable cause) throws Exception {

				// TODO log exception
				close();
			}

			@Override
			public void onClose() throws Exception {
				close();
			}

			@Override
			protected void finalize() throws Throwable {
				close();
			}

			private void close() {
				if (closeChannel) {
					try {
						destination.close();
					} catch (Exception ignored) {
					}
				}
			}

		});
	}

	// --- TRANSFER ---

	public boolean hasMorePackets() {
		return !allTransfered.isDone();
	}
	
	public Promise sendNextPacket() {
		Promise packetTransfered = new Promise();
		try {
			source.sendNextPacket(new OutgoingPacket() {

				private long totalLength;
				
				@Override
				public void sendData(byte[] bytes) {
					boolean transfered = true;
					try {
						if (bytes != null) {
							totalLength += bytes.length;
							destination.onData(bytes);
						}
					} catch (Throwable fatal) {
						transfered = false;
						packetTransfered.complete(fatal);
						allTransfered.complete(fatal);
					} finally {
						if (transfered) {
							packetTransfered.complete(totalLength);
						}
					}
				}

				@Override
				public void sendError(Throwable cause) {
					boolean transfered = true;
					try {
						destination.onError(cause);
					} catch (Throwable fatal) {
						transfered = false;
						packetTransfered.complete(fatal);
						allTransfered.complete(fatal);
					} finally {
						if (transfered) {
							packetTransfered.complete(cause);
							allTransfered.complete(cause);
						}
					}
				}

				@Override
				public void sendClose() {
					boolean transfered = true;
					try {
						destination.onClose();
					} catch (Throwable fatal) {
						transfered = false;
						packetTransfered.complete(fatal);
						allTransfered.complete(fatal);
					} finally {
						if (transfered) {
							packetTransfered.complete(totalLength);
							allTransfered.complete(totalLength);
						}
					}
				}
			});
		} catch (Throwable cause) {
			try {
				destination.onError(cause);
			} catch (Throwable ignored) {
			} finally {
				packetTransfered.complete(cause);
				allTransfered.complete(cause);
			}
		}
		return packetTransfered;
	}
	
}