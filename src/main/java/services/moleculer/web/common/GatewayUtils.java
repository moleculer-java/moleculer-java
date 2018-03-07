package services.moleculer.web.common;

import static services.moleculer.util.CommonUtils.readFully;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.dom.Cache;
import services.moleculer.web.middleware.ServeStatic;

public final class GatewayUtils {

	// --- LOGGER ---

	private static final Logger logger = LoggerFactory.getLogger(GatewayUtils.class);

	// --- CREATE SSL CONTEXT ---

	public static final SSLContext getSslContext(String keyStoreFilePath, String keyStorePassword, String keyStoreType,
			TrustManager[] trustManagers) throws Exception {

		// Load KeyStore
		KeyStore keyStore = KeyStore.getInstance(keyStoreType);
		InputStream keyStoreInputStream = getFileURL(keyStoreFilePath).openStream();
		keyStore.load(keyStoreInputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.getProvider();
		keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());

		// Create TrustManager
		TrustManager[] mgrs;
		if (trustManagers == null) {
			mgrs = new TrustManager[] { new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
						throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

			} };
		} else {
			mgrs = trustManagers;
		}

		// Create SSL context
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), mgrs, null);
		return sslContext;
	}

	// --- FILE HANDLERS ---

	protected static final Cache<String, URL> urlCache = new Cache<>(2048, false);

	protected static final long jarTimestamp = System.currentTimeMillis();

	public static final boolean isReadable(String path) {
		try {
			return getFileURL(path) != null;
		} catch (Exception ignored) {
		}
		return false;
	}

	public static final long getFileSize(String path) {
		try {
			URL url = getFileURL(path);
			if (url != null) {
				if ("file".equals(url.getProtocol())) {
					File file = new File(new URI(url.toString()));
					return file.length();
				}
			}
		} catch (Exception ignored) {
		}
		return -1;
	}

	public static final long getLastModifiedTime(String path) {
		try {
			URL url = getFileURL(path);
			if (url != null) {
				if ("file".equals(url.getProtocol())) {
					File file = new File(new URI(url.toString()));
					return file.lastModified();
				}
			}
		} catch (Exception ignored) {
		}
		return jarTimestamp;
	}

	public static final byte[] readAllBytes(String path) {
		try {
			URL url = getFileURL(path);
			if (url != null) {
				return readFully(url.openStream());
			}
		} catch (Exception ignored) {
		}
		logger.warn("Unable to load file: " + path);
		return new byte[0];
	}

	public static final URL getFileURL(String path) {
		URL url = urlCache.get(path);
		if (url != null) {
			return url;
		}
		url = tryToGetFileURL(path);
		if (url != null) {
			urlCache.put(path, url);
			return url;
		}
		if (path.startsWith("/") && path.length() > 1) {
			url = tryToGetFileURL(path.substring(1));
			if (url != null) {
				urlCache.put(path, url);
			}
		}
		return url;
	}

	private static final URL tryToGetFileURL(String path) {
		try {
			File test = new File(path);
			if (test.isFile()) {
				return test.toURI().toURL();
			}
			URL url = ServeStatic.class.getResource(path);
			if (url != null) {
				return url;
			}
			url = Thread.currentThread().getContextClassLoader().getResource(path);
			if (url != null) {
				return url;
			}
		} catch (Exception cause) {
			logger.warn("Unable to open file: " + path, cause);
		}
		return null;
	}

}