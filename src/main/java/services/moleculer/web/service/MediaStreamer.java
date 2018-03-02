package services.moleculer.web.service;

import static services.moleculer.util.CommonUtils.formatPath;
import static services.moleculer.web.service.ServeStatic.defaultContentTypes;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Name;
import services.moleculer.service.Service;
import services.moleculer.web.router.HttpConstants;

@Name("media-streamer")
public class MediaStreamer extends Service implements HttpConstants {

	// --- ROOT DIRECTORY ---

	protected String rootDirectory;

	// --- CONTENT TYPES ---

	protected final HashMap<String, String> contentTypes = new HashMap<>();

	// --- FLV HEADER ---

	protected byte[] header = new byte[0];

	// --- OTHER PROPERTIES ---

	protected int maxAge = 1000 * 60 * 60 * 8;

	protected int maxPacketLength = 1024 * 64;

	// --- CONSTRUCTORS ---

	public MediaStreamer() {
		this("/www");
	}

	public MediaStreamer(String rootDirectory) {
		this.rootDirectory = formatPath(rootDirectory);
	}

	// --- FILE HANDLER ---

	protected int prefixLength = -1;

	public Action get = ctx -> {
		RandomAccessFile file = null;
		try {
			Tree out = new Tree();

			// Realtive path
			String relativePath = null;

			// Get path and headers block
			Tree reqMeta = ctx.params.getMeta(false);
			Tree reqHeaders = null;
			if (reqMeta != null) {
				Tree path = reqMeta.get(PATH);
				if (path != null) {
					relativePath = path.asString();
				}
				reqHeaders = reqMeta.get(HEADERS);
			}

			// Get response meta and headers
			Tree rspMeta = out.getMeta(true);
			Tree rspHeaders = rspMeta.putMap(HEADERS, true);
			if (relativePath == null || relativePath.isEmpty() || relativePath.contains("..")) {
				rspHeaders.put(STATUS, STATUS_404);
				return out;
			}

			// Remove prefix
			int i;
			if (prefixLength == -1) {
				String pattern = reqMeta.get(PATTERN, "");
				i = pattern.indexOf('*');
				if (i != -1) {
					pattern = pattern.substring(0, i);
					while (pattern.endsWith("/")) {
						pattern = pattern.substring(0, pattern.length() - 1);
					}
					prefixLength = pattern.length();
				}
			}
			relativePath = relativePath.substring(prefixLength);

			// Find in root directory
			File mediaFile = new File(rootDirectory + formatPath(relativePath));
			if (!mediaFile.isFile()) {
				rspHeaders.put(STATUS, STATUS_404);
				return out;
			}

			// Get position
			long start = 0;
			long end = -1;
			String range = reqHeaders.get("range", "");
			if (range == null || range.isEmpty()) {
				try {
					String val = ctx.params.get("start", "");
					if (val != null && !val.isEmpty()) {
						start = Long.parseLong(val);
					}
				} catch (Exception ignored) {

					// Invalid position
					logger.warn("Invalid start position:" + ignored.getMessage());
				}
			} else {
				i = range.indexOf('=');
				if (i > -1) {
					range = range.substring(i + 1);
					i = range.indexOf('-');
					if (i > -1) {
						try {
							start = Long.parseLong(range.substring(0, i));
						} catch (Exception ignored) {
						}
						if (i < range.length() - 1) {
							try {
								end = Long.parseLong(range.substring(i + 1));
							} catch (Exception ignored) {
							}
						}
					}
				}
			}

			// Get extension
			i = relativePath.lastIndexOf('.');
			String extension = "";
			if (i > -1) {
				extension = relativePath.substring(i + 1).toLowerCase();
			}

			// Get content-type
			String contentType = getContentType(extension);

			// Read FLV header
			if ("flv".equalsIgnoreCase(extension)) {
				synchronized (this) {
					if (header.length == 0) {
						header = readHeader(mediaFile);
					}
				}
			}

			// Open file
			file = new RandomAccessFile(mediaFile, "r");
			long length = file.length();

			// Verify start position
			if (start > length - 1) {
				start = length - 1;
			} else {
				if (start < 0) {
					start = 0;
				}
			}

			// Verify end position
			if (end - start > maxPacketLength || end < start || end > length - 1) {
				end = Math.min(start + maxPacketLength, length - 1);
			}

			// Set content type
			rspHeaders.put(RSP_CONTENT_TYPE, contentType);

			// Calculate "content-length"
			long contentLength = end - start + 1;

			// FLV video mode?
			boolean sendFlvHeader = (start > 0 && "flv".equalsIgnoreCase(extension));
			if (sendFlvHeader) {
				contentLength += header.length;
			}

			// HTTP headers
			if (start > 0 || end < length - 1) {

				// 206 = partial content
				rspMeta.put(STATUS, 206);
			}
			rspHeaders.put("Accept-Ranges", "bytes");
			rspHeaders.put("Content-Range", "bytes " + start + '-' + end + '/' + length);

			// Disable caching
			rspHeaders.put("Pragma", "no-cache");
			rspHeaders.put("Expires", "-1");
			rspHeaders.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");

			// Seek to position
			file.seek(start);

			// Read packet
			byte[] bytes = new byte[(int) contentLength];
			if (sendFlvHeader) {
				System.arraycopy(header, 0, bytes, 0, header.length);
				file.readFully(bytes, header.length, bytes.length - header.length);
			} else {
				file.readFully(bytes, 0, bytes.length);
			}
			out.setObject(bytes);

			// Return response
			return out;

		} finally {
			try {
				if (file != null) {
					file.close();
				}
			} catch (Exception ignored) {
			}
		}
	};

	// --- READ FLV HEADER ---

	protected byte[] readHeader(File mediaFile) throws Exception {
		byte[] bytes = new byte[13];
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(mediaFile, "r");
			raf.readFully(bytes);
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (Exception ignored) {
			}
		}
		return bytes;
	}

	// --- STOP SERVICE ---

	@Override
	public void stopped() {
		contentTypes.clear();
	}

	// --- GETTERS AND SETTERS ---

	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = formatPath(rootDirectory);
	}

	public void setContentType(String extension, String contentType) {
		contentTypes.put(extension, contentType);
	}

	public String getContentType(String extension) {
		if (extension != null && !extension.isEmpty()) {
			String contentType = contentTypes.get(extension);
			if (contentType == null) {
				contentType = defaultContentTypes.get(extension);
			}
			if (contentType != null) {
				return contentType;
			}
		}
		return "application/octetstream";
	}

	public int getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public int getMaxPacketLength() {
		return maxPacketLength;
	}

	public void setMaxPacketLength(int maxPacketLength) {
		this.maxPacketLength = maxPacketLength;
	}

}