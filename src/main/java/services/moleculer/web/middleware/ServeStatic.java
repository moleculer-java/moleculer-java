package services.moleculer.web.middleware;

import static services.moleculer.util.CommonUtils.compress;
import static services.moleculer.util.CommonUtils.formatPath;
import static services.moleculer.web.router.FileUtils.getFileSize;
import static services.moleculer.web.router.FileUtils.getFileURL;
import static services.moleculer.web.router.FileUtils.getLastModifiedTime;
import static services.moleculer.web.router.FileUtils.isReadable;
import static services.moleculer.web.router.FileUtils.readAllBytes;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.Deflater;

import io.datatree.Tree;
import io.datatree.dom.Cache;
import services.moleculer.context.Context;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.web.router.HttpConstants;

/**
 * Service to serve files from within a given root directory. Sample of usage:
 * <br>
 * <br>
 * ServiceBroker broker = new ServiceBroker();<br>
 * ApiGateway gateway = new SunGateway();<br>
 * broker.createService("gateway", gateway);<br>
 * <br>
 * gateway.use(new ServeStatic("/pages", "/path/to/www/root"));<br>
 * <br>
 * broker.start();<br>
 * <br>
 * ...then open browser, and enter the following URL:
 * "http://localhost:3000/pages/index.html"
 */
@Name("Static File Provider")
public class ServeStatic extends Middleware implements HttpConstants {

	// --- URL PATH AND ROOT DIRECTORY ---

	/**
	 * URL prefix (eg. "/files")
	 */
	protected String path;

	/**
	 * Local directory prefix (eg. "C:/content" or "/www")
	 */
	protected String file;

	// --- PROPERTIES ---

	/**
	 * Enables content reloading (in production mode set it to "false" for the
	 * better performance)
	 */
	protected boolean enableReloading = true;

	/**
	 * Maximum number of cached files
	 */
	protected int numberOfCachedFiles = 1024;

	/**
	 * Do not reload cache until... (MILLISECONDS)
	 */
	protected long cacheDelay = 2000L;

	/**
	 * Enable caching for smaller files only (BYTES)
	 */
	protected int maxCachedFileSize = 1024 * 1024;

	/**
	 * Use ETag headers
	 */
	protected boolean useETags = true;

	/**
	 * Compress key and/or value above this size (BYTES), 0 = disable
	 * compression
	 */
	protected int compressAbove = 1024;

	/**
	 * Compression level (best speed = 1, best compression = 9).
	 */
	protected int compressionLevel = Deflater.BEST_SPEED;

	// --- CONTENT TYPES ---

	protected final HashMap<String, String> contentTypes = new HashMap<>();

	// --- CACHES ---

	protected Cache<String, CachedFile> fileCache;

	protected static final class CachedFile {

		protected long lastChecked;
		protected long time;
		protected String etag;

		protected byte[] body;
		protected byte[] compressedBody;

	}

	// --- CONSTRUCTORS ---

	public ServeStatic(String path) {
		this(path, "/www");
	}

	public ServeStatic(String path, String rootDirectory) {
		this.path = formatPath(path);
		this.file = formatPath(rootDirectory);
		if (path.indexOf('*') > -1 || path.indexOf('?') > -1) {
			throw new IllegalArgumentException("Invalid path format (" + path
					+ ")! Use simple path prefix, without wildcard characters, like \"/www\".");
		}
	}

	// --- START HANDLER ---

	public void started(services.moleculer.ServiceBroker broker) throws Exception {
		super.started(broker);
		if (fileCache == null) {
			fileCache = new Cache<>(numberOfCachedFiles, false);
		}
	}

	// --- FILE HANDLER ---

	@Override
	public Action install(Action action, Tree config) {
		return new Action() {

			@Override
			public Object handler(Context ctx) throws Exception {
				try {

					// Realtive path
					String relativePath = null;

					// If-None-Match header
					String ifNoneMatch = null;

					// Client supports compressed content
					boolean compressionSupported = false;

					// Get path and headers block
					Tree meta = ctx.params.getMeta(false);
					if (meta != null) {
						Tree path = meta.get(PATH);
						if (path != null) {
							relativePath = path.asString();
						}
						Tree headers = meta.get(HEADERS);
						if (headers != null) {
							if (useETags) {
								ifNoneMatch = headers.get(REQ_IF_NONE_MATCH, "");
							}
							if (compressAbove > 0) {
								compressionSupported = headers.get(REQ_ACCEPT_ENCODING, "").contains(DEFLATE);
							}
						}
					}
					if (relativePath == null || !relativePath.startsWith(path)) {
						return action.handler(ctx);
					}

					// Remove prefix
					relativePath = relativePath.substring(path.length());

					// Get response meta and headers
					Tree out = new Tree();
					meta = out.getMeta(true);
					Tree headers = meta.putMap(HEADERS, true);
					if (relativePath == null || relativePath.isEmpty() || relativePath.contains("..")) {

						// 404 Not Found
						return action.handler(ctx);
					}

					// Absolute path
					String absolutePath = file + formatPath(relativePath);

					// Get file from cache
					CachedFile cached = fileCache.get(relativePath);
					long now = System.currentTimeMillis();
					boolean reload;
					if (cached != null && now - cached.lastChecked < cacheDelay) {
						reload = false;
					} else {
						reload = enableReloading;
					}

					// Valid file?
					if (reload || cached == null) {
						boolean readable = isReadable(absolutePath);
						if (readable) {
							if (cached != null) {
								cached.lastChecked = now;
							}
						} else {

							// 404 Not Found
							fileCache.remove(relativePath);
							return action.handler(ctx);
						}
					}

					// Get extension
					int i = relativePath.lastIndexOf('.');
					String extension = "";
					if (i > -1) {
						extension = relativePath.substring(i + 1).toLowerCase();
					}

					// Get content-type
					String contentType = getContentType(extension);

					// Set "Content-Type" header
					headers.put(RSP_CONTENT_TYPE, contentType);

					// Handling ETag
					String etag = null;
					long time = -1;
					if (reload || cached == null) {
						time = getLastModifiedTime(absolutePath);
						if (time > 0 && useETags) {
							etag = Long.toHexString(time);
						}
					} else if (cached != null) {
						etag = cached.etag;
					}
					if (etag != null) {
						if (ifNoneMatch != null && ifNoneMatch.equals(etag) && (reload || cached != null)) {

							// 304 Not Modified
							headers.put(RSP_CONTENT_TYPE, contentType);
							meta.put(STATUS, 304);
							out.setObject(new byte[0]);
							return out;

						} else {

							// Send ETag header
							headers.put(RSP_ETAG, etag);
						}
					}

					// Set body
					boolean compressed = false;
					if (cached != null && (!reload || (cached.etag != null && cached.etag.equals(etag)))) {

						// Set cached content
						if (!compressionSupported || cached.compressedBody == null) {
							out.setObject(cached.body);
						} else {

							// Client supports compressed content
							out.setObject(cached.compressedBody);
							compressed = true;
						}

					} else {

						// Get file size
						long max = getFileSize(absolutePath);
						if (max > maxCachedFileSize) {

							// Send as file
							URL url = getFileURL(absolutePath);
							out.setObject(new File(new URI(url.toString())));

						} else {

							// Read all bytes of the file
							byte[] body = readAllBytes(absolutePath);

							// Store in cache
							cached = new CachedFile();
							cached.lastChecked = now;
							cached.etag = etag;
							cached.body = body;
							if (compressAbove > 0 && body.length > compressAbove && contentType.startsWith("text/")) {
								cached.compressedBody = compress(body, compressionLevel);
								if (compressionSupported) {

									// Client supports compressed content
									body = cached.compressedBody;
									compressed = true;
								}
							}
							fileCache.put(relativePath, cached);
							out.setObject(body);
						}
					}

					// Add "Content-Encoding" header
					if (compressed) {
						headers.put(RSP_CONTENT_ENCODING, DEFLATE);
					}

					// Return response
					return out;
				} catch (Exception cause) {
					logger.warn("Unable to process request!", cause);
				}
				return action.handler(ctx);
			}
		};
	}

	// --- STOP MIDDLEWARE ---

	@Override
	public void stopped() {
		contentTypes.clear();
		fileCache = null;
	}

	// --- DEFAULT CONTENT TYPES ---

	protected static final HashMap<String, String> defaultContentTypes = new HashMap<>();

	static {
		defaultContentTypes.put("3dm", "x-world/x-3dmf");
		defaultContentTypes.put("3dmf", "x-world/x-3dmf");
		defaultContentTypes.put("aab", "application/x-authorware-bin");
		defaultContentTypes.put("aam", "application/x-authorware-map");
		defaultContentTypes.put("aas", "application/x-authorware-seg");
		defaultContentTypes.put("abc", "text/vnd.abc");
		defaultContentTypes.put("acgi", "text/html");
		defaultContentTypes.put("afl", "video/animaflex");
		defaultContentTypes.put("ai", "application/postscript");
		defaultContentTypes.put("aif", "audio/x-aiff");
		defaultContentTypes.put("aifc", "audio/x-aiff");
		defaultContentTypes.put("aiff", "audio/x-aiff");
		defaultContentTypes.put("aim", "application/x-aim");
		defaultContentTypes.put("aip", "text/x-audiosoft-intra");
		defaultContentTypes.put("ani", "application/x-navi-animation");
		defaultContentTypes.put("aps", "application/mime");
		defaultContentTypes.put("art", "image/x-jg");
		defaultContentTypes.put("asf", "video/x-ms-asf");
		defaultContentTypes.put("asm", "text/x-asm");
		defaultContentTypes.put("asp", "text/asp");
		defaultContentTypes.put("asx", "video/x-ms-asf-plugin");
		defaultContentTypes.put("au", "audio/x-au");
		defaultContentTypes.put("avi", "video/x-msvideo");
		defaultContentTypes.put("avs", "video/avs-video");
		defaultContentTypes.put("bcpio", "application/x-bcpio");
		defaultContentTypes.put("bin", "application/x-macbinary");
		defaultContentTypes.put("bm", "image/bmp");
		defaultContentTypes.put("bmp", "image/x-windows-bmp");
		defaultContentTypes.put("boo", "application/book");
		defaultContentTypes.put("book", "application/book");
		defaultContentTypes.put("boz", "application/x-bzip2");
		defaultContentTypes.put("bsh", "application/x-bsh");
		defaultContentTypes.put("bz", "application/x-bzip");
		defaultContentTypes.put("bz2", "application/x-bzip2");
		defaultContentTypes.put("c", "text/x-c");
		defaultContentTypes.put("c++", "text/plain");
		defaultContentTypes.put("cat", "application/vnd.ms-pki.seccat");
		defaultContentTypes.put("cc", "text/x-c");
		defaultContentTypes.put("ccad", "application/clariscad");
		defaultContentTypes.put("cco", "application/x-cocoa");
		defaultContentTypes.put("cdf", "application/x-netcdf");
		defaultContentTypes.put("cer", "application/x-x509-ca-cert");
		defaultContentTypes.put("cha", "application/x-chat");
		defaultContentTypes.put("chat", "application/x-chat");
		defaultContentTypes.put("class", "application/x-java-class");
		defaultContentTypes.put("com", "text/plain");
		defaultContentTypes.put("conf", "text/plain");
		defaultContentTypes.put("cpio", "application/x-cpio");
		defaultContentTypes.put("cpp", "text/x-c");
		defaultContentTypes.put("cpt", "application/x-cpt");
		defaultContentTypes.put("crl", "application/pkix-crl");
		defaultContentTypes.put("crt", "application/x-x509-user-cert");
		defaultContentTypes.put("csh", "text/x-script.csh");
		defaultContentTypes.put("css", "text/css");
		defaultContentTypes.put("cxx", "text/plain");
		defaultContentTypes.put("dcr", "application/x-director");
		defaultContentTypes.put("deepv", "application/x-deepv");
		defaultContentTypes.put("def", "text/plain");
		defaultContentTypes.put("der", "application/x-x509-ca-cert");
		defaultContentTypes.put("dif", "video/x-dv");
		defaultContentTypes.put("dir", "application/x-director");
		defaultContentTypes.put("divx", "video/divx");
		defaultContentTypes.put("dl", "video/x-dl");
		defaultContentTypes.put("doc", "application/msword");
		defaultContentTypes.put("dot", "application/msword");
		defaultContentTypes.put("dp", "application/commonground");
		defaultContentTypes.put("drw", "application/drafting");
		defaultContentTypes.put("dv", "video/x-dv");
		defaultContentTypes.put("dvi", "application/x-dvi");
		defaultContentTypes.put("dwf", "model/vnd.dwf");
		defaultContentTypes.put("dwg", "image/x-dwg");
		defaultContentTypes.put("dxf", "image/x-dwg");
		defaultContentTypes.put("dxr", "application/x-director");
		defaultContentTypes.put("el", "text/x-script.elisp");
		defaultContentTypes.put("elc", "application/x-elc");
		defaultContentTypes.put("env", "application/x-envoy");
		defaultContentTypes.put("eps", "application/postscript");
		defaultContentTypes.put("es", "application/x-esrehber");
		defaultContentTypes.put("etx", "text/x-setext");
		defaultContentTypes.put("evy", "application/x-envoy");
		defaultContentTypes.put("f", "text/x-fortran");
		defaultContentTypes.put("f77", "text/x-fortran");
		defaultContentTypes.put("f90", "text/x-fortran");
		defaultContentTypes.put("fdf", "application/vnd.fdf");
		defaultContentTypes.put("fif", "image/fif");
		defaultContentTypes.put("fli", "video/x-fli");
		defaultContentTypes.put("flo", "image/florian");
		defaultContentTypes.put("flx", "text/vnd.fmi.flexstor");
		defaultContentTypes.put("flv", "video/x-flv");
		defaultContentTypes.put("fmf", "video/x-atomic3d-feature");
		defaultContentTypes.put("for", "text/x-fortran");
		defaultContentTypes.put("fpx", "image/vnd.net-fpx");
		defaultContentTypes.put("frl", "application/freeloader");
		defaultContentTypes.put("funk", "audio/make");
		defaultContentTypes.put("g", "text/plain");
		defaultContentTypes.put("g3", "image/g3fax");
		defaultContentTypes.put("gif", "image/gif");
		defaultContentTypes.put("gl", "video/x-gl");
		defaultContentTypes.put("gsd", "audio/x-gsm");
		defaultContentTypes.put("gsm", "audio/x-gsm");
		defaultContentTypes.put("gsp", "application/x-gsp");
		defaultContentTypes.put("gss", "application/x-gss");
		defaultContentTypes.put("gtar", "application/x-gtar");
		defaultContentTypes.put("gui03", "text/xml");
		defaultContentTypes.put("gz", "application/x-gzip");
		defaultContentTypes.put("gzip", "multipart/x-gzip");
		defaultContentTypes.put("h", "text/x-h");
		defaultContentTypes.put("hdf", "application/x-hdf");
		defaultContentTypes.put("help", "application/x-helpfile");
		defaultContentTypes.put("hgl", "application/vnd.hp-hpgl");
		defaultContentTypes.put("hh", "text/x-h");
		defaultContentTypes.put("hlb", "text/x-script");
		defaultContentTypes.put("hlp", "application/x-winhelp");
		defaultContentTypes.put("hpg", "application/vnd.hp-hpgl");
		defaultContentTypes.put("hpgl", "application/vnd.hp-hpgl");
		defaultContentTypes.put("hqx", "application/x-mac-binhex40");
		defaultContentTypes.put("hta", "application/hta");
		defaultContentTypes.put("htc", "text/x-component");
		defaultContentTypes.put("htm", "text/html");
		defaultContentTypes.put("html", "text/html");
		defaultContentTypes.put("htmls", "text/html");
		defaultContentTypes.put("htt", "text/webviewhtml");
		defaultContentTypes.put("htx ", "text/html");
		defaultContentTypes.put("ice ", "x-conference/x-cooltalk");
		defaultContentTypes.put("ico", "image/x-icon");
		defaultContentTypes.put("idc", "text/plain");
		defaultContentTypes.put("ief", "image/ief");
		defaultContentTypes.put("iefs", "image/ief");
		defaultContentTypes.put("iges", "application/iges");
		defaultContentTypes.put("iges ", "model/iges");
		defaultContentTypes.put("igs", "model/iges");
		defaultContentTypes.put("ima", "application/x-ima");
		defaultContentTypes.put("imap", "application/x-httpd-imap");
		defaultContentTypes.put("inf ", "application/inf");
		defaultContentTypes.put("ins", "application/x-internett-signup");
		defaultContentTypes.put("ip ", "application/x-ip2");
		defaultContentTypes.put("isu", "video/x-isvideo");
		defaultContentTypes.put("it", "audio/it");
		defaultContentTypes.put("iv", "application/x-inventor");
		defaultContentTypes.put("ivr", "i-world/i-vrml");
		defaultContentTypes.put("ivy", "application/x-livescreen");
		defaultContentTypes.put("jad", "text/vnd.sun.j2me.app-descriptor");
		defaultContentTypes.put("jam ", "audio/x-jam");
		defaultContentTypes.put("jar", "application/java-archive");
		defaultContentTypes.put("jav", "text/x-java-source");
		defaultContentTypes.put("java", "text/plain");
		defaultContentTypes.put("java ", "text/x-java-source");
		defaultContentTypes.put("jcm ", "application/x-java-commerce");
		defaultContentTypes.put("jfif", "image/pjpeg");
		defaultContentTypes.put("jfif-tbnl", "image/jpeg");
		defaultContentTypes.put("jform", "text/xml");
		defaultContentTypes.put("jnlp", "application/x-java-jnlp-file");
		defaultContentTypes.put("jpe", "image/pjpeg");
		defaultContentTypes.put("jpeg", "image/pjpeg");
		defaultContentTypes.put("jpg", "image/jpeg");
		defaultContentTypes.put("jpg ", "image/pjpeg");
		defaultContentTypes.put("jps", "image/x-jps");
		defaultContentTypes.put("js", "text/javascript");
		defaultContentTypes.put("js ", "application/x-javascript");
		defaultContentTypes.put("jsf", "text/plain");
		defaultContentTypes.put("json", "application/json");
		defaultContentTypes.put("jsp", "text/html");
		defaultContentTypes.put("jspf", "text/plain");
		defaultContentTypes.put("jut", "image/jutvision");
		defaultContentTypes.put("kar", "music/x-karaoke");
		defaultContentTypes.put("ksh", "text/x-script.ksh");
		defaultContentTypes.put("la ", "audio/x-nspaudio");
		defaultContentTypes.put("lam", "audio/x-liveaudio");
		defaultContentTypes.put("latex", "application/x-latex");
		defaultContentTypes.put("latex ", "application/x-latex");
		defaultContentTypes.put("lha", "application/x-lha");
		defaultContentTypes.put("list", "text/plain");
		defaultContentTypes.put("lma", "audio/x-nspaudio");
		defaultContentTypes.put("log ", "text/plain");
		defaultContentTypes.put("lsp ", "text/x-script.lisp");
		defaultContentTypes.put("lst ", "text/plain");
		defaultContentTypes.put("lsx", "text/x-la-asf");
		defaultContentTypes.put("ltx", "application/x-latex");
		defaultContentTypes.put("lzh", "application/x-lzh");
		defaultContentTypes.put("lzx", "application/x-lzx");
		defaultContentTypes.put("m", "text/x-m");
		defaultContentTypes.put("m1v", "video/mpeg");
		defaultContentTypes.put("m2a", "audio/mpeg");
		defaultContentTypes.put("m2v", "video/mpeg");
		defaultContentTypes.put("m3u", "audio/x-mpegurl");
		defaultContentTypes.put("m3u ", "audio/x-mpequrl");
		defaultContentTypes.put("mac", "image/x-macpaint");
		defaultContentTypes.put("man", "application/x-troff-man");
		defaultContentTypes.put("map", "application/x-navimap");
		defaultContentTypes.put("mar", "text/plain");
		defaultContentTypes.put("mbd", "application/mbedlet");
		defaultContentTypes.put("mc$", "application/x-magic-cap-package-1.0");
		defaultContentTypes.put("mcd", "application/x-mathcad");
		defaultContentTypes.put("mcf", "text/mcf");
		defaultContentTypes.put("mcp", "application/netmc");
		defaultContentTypes.put("me", "application/x-troff-me");
		defaultContentTypes.put("me ", "application/x-troff-me");
		defaultContentTypes.put("mht", "message/rfc822");
		defaultContentTypes.put("mhtml", "message/rfc822");
		defaultContentTypes.put("mid", "x-music/x-midi");
		defaultContentTypes.put("midi", "x-music/x-midi");
		defaultContentTypes.put("mif", "application/x-mif");
		defaultContentTypes.put("mime ", "www/mime");
		defaultContentTypes.put("mjpg ", "video/x-motion-jpeg");
		defaultContentTypes.put("mm", "application/x-meme");
		defaultContentTypes.put("mme", "application/base64");
		defaultContentTypes.put("mod", "audio/x-mod");
		defaultContentTypes.put("moov", "video/quicktime");
		defaultContentTypes.put("mov", "video/quicktime");
		defaultContentTypes.put("movie", "video/x-sgi-movie");
		defaultContentTypes.put("mp1", "audio/x-mpeg");
		defaultContentTypes.put("mp2", "video/x-mpeq2a");
		defaultContentTypes.put("mp3", "audio/mpeg");
		defaultContentTypes.put("mp4", "video/mp4");
		defaultContentTypes.put("mpa", "video/mpeg");
		defaultContentTypes.put("mpc", "application/x-project");
		defaultContentTypes.put("mpe", "video/mpeg");
		defaultContentTypes.put("mpeg", "video/mpeg");
		defaultContentTypes.put("mpega", "audio/x-mpeg");
		defaultContentTypes.put("mpg", "video/mpeg");
		defaultContentTypes.put("mpga", "audio/mpeg");
		defaultContentTypes.put("mpp", "application/vnd.ms-project");
		defaultContentTypes.put("mpt", "application/x-project");
		defaultContentTypes.put("mpv", "application/x-project");
		defaultContentTypes.put("mpv2", "video/mpeg2");
		defaultContentTypes.put("mpx", "application/x-project");
		defaultContentTypes.put("mrc", "application/marc");
		defaultContentTypes.put("ms", "application/x-troff-ms");
		defaultContentTypes.put("mv", "video/x-sgi-movie");
		defaultContentTypes.put("my", "audio/make");
		defaultContentTypes.put("mzz", "application/x-vnd.audioexplosion.mzz");
		defaultContentTypes.put("nap", "image/naplps");
		defaultContentTypes.put("naplps", "image/naplps");
		defaultContentTypes.put("nc", "application/x-netcdf");
		defaultContentTypes.put("ncm", "application/vnd.nokia.configuration-message");
		defaultContentTypes.put("nif", "image/x-niff");
		defaultContentTypes.put("niff", "image/x-niff");
		defaultContentTypes.put("nix", "application/x-mix-transfer");
		defaultContentTypes.put("nsc", "application/x-conference");
		defaultContentTypes.put("nvd", "application/x-navidoc");
		defaultContentTypes.put("oda", "application/oda");
		defaultContentTypes.put("ogg", "video/ogg");
		defaultContentTypes.put("omc", "application/x-omc");
		defaultContentTypes.put("omcd", "application/x-omcdatamaker");
		defaultContentTypes.put("omcr", "application/x-omcregerator");
		defaultContentTypes.put("p", "text/x-pascal");
		defaultContentTypes.put("p10", "application/x-pkcs10");
		defaultContentTypes.put("p12", "application/x-pkcs12");
		defaultContentTypes.put("p7a", "application/x-pkcs7-signature");
		defaultContentTypes.put("p7c", "application/x-pkcs7-mime");
		defaultContentTypes.put("p7m", "application/x-pkcs7-mime");
		defaultContentTypes.put("p7r", "application/x-pkcs7-certreqresp");
		defaultContentTypes.put("p7s", "application/pkcs7-signature");
		defaultContentTypes.put("part ", "application/pro_eng");
		defaultContentTypes.put("pas", "text/pascal");
		defaultContentTypes.put("pbm", "image/x-portable-bitmap");
		defaultContentTypes.put("pbm ", "image/x-portable-bitmap");
		defaultContentTypes.put("pcl", "application/x-pcl");
		defaultContentTypes.put("pct", "image/x-pict");
		defaultContentTypes.put("pcx", "image/x-pcx");
		defaultContentTypes.put("pdb", "chemical/x-pdb");
		defaultContentTypes.put("pdf", "application/pdf");
		defaultContentTypes.put("pfunk", "audio/make.my.funk");
		defaultContentTypes.put("pgm", "image/x-portable-greymap");
		defaultContentTypes.put("pic", "image/pict");
		defaultContentTypes.put("pict", "image/pict");
		defaultContentTypes.put("pkg", "application/x-newton-compatible-pkg");
		defaultContentTypes.put("pko", "application/vnd.ms-pki.pko");
		defaultContentTypes.put("pl", "text/x-script.perl");
		defaultContentTypes.put("pls", "audio/x-scpls");
		defaultContentTypes.put("plx", "application/x-pixclscript");
		defaultContentTypes.put("pm", "text/x-script.perl-module");
		defaultContentTypes.put("pm4 ", "application/x-pagemaker");
		defaultContentTypes.put("pm5", "application/x-pagemaker");
		defaultContentTypes.put("png", "image/png");
		defaultContentTypes.put("pnm", "image/x-portable-anymap");
		defaultContentTypes.put("pnt", "image/x-macpaint");
		defaultContentTypes.put("pot", "application/vnd.ms-powerpoint");
		defaultContentTypes.put("pov", "model/x-pov");
		defaultContentTypes.put("ppa", "application/vnd.ms-powerpoint");
		defaultContentTypes.put("ppm", "image/x-portable-pixmap");
		defaultContentTypes.put("pps", "application/vnd.ms-powerpoint");
		defaultContentTypes.put("ppt", "application/x-mspowerpoint");
		defaultContentTypes.put("ppz", "application/mspowerpoint");
		defaultContentTypes.put("pre", "application/x-freelance");
		defaultContentTypes.put("prt", "application/pro_eng");
		defaultContentTypes.put("ps", "application/postscript");
		defaultContentTypes.put("pvu", "paleovu/x-pv");
		defaultContentTypes.put("pwz ", "application/vnd.ms-powerpoint");
		defaultContentTypes.put("py ", "text/x-script.phyton");
		defaultContentTypes.put("pyc ", "applicaiton/x-bytecode.python");
		defaultContentTypes.put("qcp ", "audio/vnd.qcelp");
		defaultContentTypes.put("qd3 ", "x-world/x-3dmf");
		defaultContentTypes.put("qd3d ", "x-world/x-3dmf");
		defaultContentTypes.put("qif", "image/x-quicktime");
		defaultContentTypes.put("qt", "video/quicktime");
		defaultContentTypes.put("qtc", "video/x-qtc");
		defaultContentTypes.put("qti", "image/x-quicktime");
		defaultContentTypes.put("qtif", "image/x-quicktime");
		defaultContentTypes.put("ra", "audio/x-realaudio");
		defaultContentTypes.put("ram", "audio/x-pn-realaudio");
		defaultContentTypes.put("ras", "image/x-cmu-raster");
		defaultContentTypes.put("rast", "image/cmu-raster");
		defaultContentTypes.put("rexx ", "text/x-script.rexx");
		defaultContentTypes.put("rf", "image/vnd.rn-realflash");
		defaultContentTypes.put("rgb", "image/x-rgb");
		defaultContentTypes.put("rgb ", "image/x-rgb");
		defaultContentTypes.put("rm", "audio/x-pn-realaudio");
		defaultContentTypes.put("rmi", "audio/mid");
		defaultContentTypes.put("rmm ", "audio/x-pn-realaudio");
		defaultContentTypes.put("rmp", "audio/x-pn-realaudio-plugin");
		defaultContentTypes.put("rng", "application/vnd.nokia.ringing-tone");
		defaultContentTypes.put("rnx ", "application/vnd.rn-realplayer");
		defaultContentTypes.put("roff", "application/x-troff");
		defaultContentTypes.put("rp ", "image/vnd.rn-realpix");
		defaultContentTypes.put("rpm", "audio/x-pn-realaudio-plugin");
		defaultContentTypes.put("rt", "text/vnd.rn-realtext");
		defaultContentTypes.put("rtf", "text/richtext");
		defaultContentTypes.put("rtx", "text/richtext");
		defaultContentTypes.put("rv", "video/vnd.rn-realvideo");
		defaultContentTypes.put("s", "text/x-asm");
		defaultContentTypes.put("s3m ", "audio/s3m");
		defaultContentTypes.put("sbk ", "application/x-tbook");
		defaultContentTypes.put("scm", "video/x-scm");
		defaultContentTypes.put("sdml", "text/plain");
		defaultContentTypes.put("sdp ", "application/x-sdp");
		defaultContentTypes.put("sdr", "application/sounder");
		defaultContentTypes.put("sea", "application/x-sea");
		defaultContentTypes.put("set", "application/set");
		defaultContentTypes.put("sgm ", "text/x-sgml");
		defaultContentTypes.put("sgml", "text/x-sgml");
		defaultContentTypes.put("sh", "text/x-script.sh");
		defaultContentTypes.put("shar", "application/x-shar");
		defaultContentTypes.put("shtml", "text/x-server-parsed-html");
		defaultContentTypes.put("shtml ", "text/html");
		defaultContentTypes.put("sid", "audio/x-psid");
		defaultContentTypes.put("sit", "application/x-stuffit");
		defaultContentTypes.put("skd", "application/x-koan");
		defaultContentTypes.put("skm ", "application/x-koan");
		defaultContentTypes.put("skp ", "application/x-koan");
		defaultContentTypes.put("skt ", "application/x-koan");
		defaultContentTypes.put("sl ", "application/x-seelogo");
		defaultContentTypes.put("smf", "audio/x-midi");
		defaultContentTypes.put("smi ", "application/smil");
		defaultContentTypes.put("smil ", "application/smil");
		defaultContentTypes.put("snd", "audio/x-adpcm");
		defaultContentTypes.put("sol", "application/solids");
		defaultContentTypes.put("spc ", "text/x-speech");
		defaultContentTypes.put("spl", "application/futuresplash");
		defaultContentTypes.put("spr", "application/x-sprite");
		defaultContentTypes.put("sprite ", "application/x-sprite");
		defaultContentTypes.put("src", "application/x-wais-source");
		defaultContentTypes.put("ssi", "text/x-server-parsed-html");
		defaultContentTypes.put("ssm ", "application/streamingmedia");
		defaultContentTypes.put("sst", "application/vnd.ms-pki.certstore");
		defaultContentTypes.put("step", "application/step");
		defaultContentTypes.put("stl", "application/x-navistyle");
		defaultContentTypes.put("stp", "application/step");
		defaultContentTypes.put("sv4cpio", "application/x-sv4cpio");
		defaultContentTypes.put("sv4crc", "application/x-sv4crc");
		defaultContentTypes.put("svf", "image/x-dwg");
		defaultContentTypes.put("svg", "image/svg+xml");
		defaultContentTypes.put("svgz", "image/svg+xml");
		defaultContentTypes.put("svr", "x-world/x-svr");
		defaultContentTypes.put("swf", "application/x-shockwave-flash");
		defaultContentTypes.put("t", "application/x-troff");
		defaultContentTypes.put("talk", "text/x-speech");
		defaultContentTypes.put("tar", "application/x-tar");
		defaultContentTypes.put("tbk", "application/x-tbook");
		defaultContentTypes.put("tcl", "text/x-script.tcl");
		defaultContentTypes.put("tcsh", "text/x-script.tcsh");
		defaultContentTypes.put("tex", "application/x-tex");
		defaultContentTypes.put("texi", "application/x-texinfo");
		defaultContentTypes.put("texinfo", "application/x-texinfo");
		defaultContentTypes.put("text", "text/plain");
		defaultContentTypes.put("tgz", "application/x-compressed");
		defaultContentTypes.put("tif", "image/x-tiff");
		defaultContentTypes.put("tiff", "image/x-tiff");
		defaultContentTypes.put("tr", "application/x-troff");
		defaultContentTypes.put("tsi", "audio/tsp-audio");
		defaultContentTypes.put("tsp", "audio/tsplayer");
		defaultContentTypes.put("tsv", "text/tab-separated-values");
		defaultContentTypes.put("turbot", "image/florian");
		defaultContentTypes.put("txt", "text/plain");
		defaultContentTypes.put("uil", "text/x-uil");
		defaultContentTypes.put("ulw", "audio/basic");
		defaultContentTypes.put("uni", "text/uri-list");
		defaultContentTypes.put("unis", "text/uri-list");
		defaultContentTypes.put("unv", "application/i-deas");
		defaultContentTypes.put("uri", "text/uri-list");
		defaultContentTypes.put("uris", "text/uri-list");
		defaultContentTypes.put("ustar", "multipart/x-ustar");
		defaultContentTypes.put("uu", "text/x-uuencode");
		defaultContentTypes.put("uue", "text/x-uuencode");
		defaultContentTypes.put("vcd", "application/x-cdlink");
		defaultContentTypes.put("vcs", "text/x-vcalendar");
		defaultContentTypes.put("vda", "application/vda");
		defaultContentTypes.put("vdo", "video/vdo");
		defaultContentTypes.put("vew ", "application/groupwise");
		defaultContentTypes.put("viv", "video/vnd.vivo");
		defaultContentTypes.put("vivo", "video/vnd.vivo");
		defaultContentTypes.put("vmd ", "application/vocaltec-media-desc");
		defaultContentTypes.put("vmf", "application/vocaltec-media-file");
		defaultContentTypes.put("voc", "audio/x-voc");
		defaultContentTypes.put("vos", "video/vosaic");
		defaultContentTypes.put("vox", "audio/voxware");
		defaultContentTypes.put("vqe", "audio/x-twinvq-plugin");
		defaultContentTypes.put("vqf", "audio/x-twinvq");
		defaultContentTypes.put("vql", "audio/x-twinvq-plugin");
		defaultContentTypes.put("vrml", "x-world/x-vrml");
		defaultContentTypes.put("vrt", "x-world/x-vrt");
		defaultContentTypes.put("vsd", "application/x-visio");
		defaultContentTypes.put("vst", "application/x-visio");
		defaultContentTypes.put("vsw ", "application/x-visio");
		defaultContentTypes.put("w60", "application/wordperfect6.0");
		defaultContentTypes.put("w61", "application/wordperfect6.1");
		defaultContentTypes.put("w6w", "application/msword");
		defaultContentTypes.put("wav", "audio/x-wav");
		defaultContentTypes.put("wb1", "application/x-qpro");
		defaultContentTypes.put("wbmp", "image/vnd.wap.wbmp");
		defaultContentTypes.put("web", "application/vnd.xara");
		defaultContentTypes.put("webm", "video/webm");
		defaultContentTypes.put("wiz", "application/msword");
		defaultContentTypes.put("wk1", "application/x-123");
		defaultContentTypes.put("wmf", "windows/metafile");
		defaultContentTypes.put("wml", "text/vnd.wap.wml");
		defaultContentTypes.put("wmlc", "application/vnd.wap.wmlc");
		defaultContentTypes.put("wmlc ", "application/vnd.wap.wmlc");
		defaultContentTypes.put("wmls", "text/vnd.wap.wmlscript");
		defaultContentTypes.put("wmlsc ", "application/vnd.wap.wmlscriptc");
		defaultContentTypes.put("wmlscriptc", "application/vnd.wap.wmlscriptc");
		defaultContentTypes.put("word ", "application/msword");
		defaultContentTypes.put("wp", "application/wordperfect");
		defaultContentTypes.put("wp5", "application/wordperfect6.0");
		defaultContentTypes.put("wp6 ", "application/wordperfect");
		defaultContentTypes.put("wpd", "application/x-wpwin");
		defaultContentTypes.put("wq1", "application/x-lotus");
		defaultContentTypes.put("wri", "application/x-wri");
		defaultContentTypes.put("wrl", "x-world/x-vrml");
		defaultContentTypes.put("wrz", "x-world/x-vrml");
		defaultContentTypes.put("wsc", "text/scriplet");
		defaultContentTypes.put("wsrc", "application/x-wais-source");
		defaultContentTypes.put("wtk ", "application/x-wintalk");
		defaultContentTypes.put("x-png", "image/png");
		defaultContentTypes.put("xbm", "image/xbm");
		defaultContentTypes.put("xdr", "video/x-amt-demorun");
		defaultContentTypes.put("xgz", "xgl/drawing");
		defaultContentTypes.put("xif", "image/vnd.xiff");
		defaultContentTypes.put("xl", "application/excel");
		defaultContentTypes.put("xla", "application/x-msexcel");
		defaultContentTypes.put("xlb", "application/x-excel");
		defaultContentTypes.put("xlc", "application/x-excel");
		defaultContentTypes.put("xld ", "application/x-excel");
		defaultContentTypes.put("xlk", "application/x-excel");
		defaultContentTypes.put("xll", "application/x-excel");
		defaultContentTypes.put("xlm", "application/x-excel");
		defaultContentTypes.put("xls", "application/x-msexcel");
		defaultContentTypes.put("xlt", "application/x-excel");
		defaultContentTypes.put("xlv", "application/x-excel");
		defaultContentTypes.put("xlw", "application/x-msexcel");
		defaultContentTypes.put("xm", "audio/xm");
		defaultContentTypes.put("xml", "text/xml");
		defaultContentTypes.put("xmz", "xgl/movie");
		defaultContentTypes.put("xpix", "application/x-vnd.ls-xpix");
		defaultContentTypes.put("xpm", "image/xpm");
		defaultContentTypes.put("xsl", "text/xml");
		defaultContentTypes.put("xsr", "video/x-amt-showrun");
		defaultContentTypes.put("xwd", "image/x-xwindowdump");
		defaultContentTypes.put("xyz", "chemical/x-pdb");
		defaultContentTypes.put("wmv", "audio/x-ms-wmv");
		defaultContentTypes.put("z", "application/x-compressed");
		defaultContentTypes.put("zip", "multipart/x-zip");
		defaultContentTypes.put("zsh", "text/x-script.zsh");
	}

	// --- GETTERS AND SETTERS ---

	public String getFile() {
		return file;
	}

	public void setFile(String rootDirectory) {
		this.file = formatPath(rootDirectory);
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

	public int getNumberOfCachedFiles() {
		return numberOfCachedFiles;
	}

	public void setNumberOfCachedFiles(int numberOfCachedFiles) {
		if (this.numberOfCachedFiles != numberOfCachedFiles) {
			fileCache = new Cache<>(numberOfCachedFiles, false);
		}
		this.numberOfCachedFiles = numberOfCachedFiles;
	}

	public boolean isEnableReloading() {
		return enableReloading;
	}

	public void setEnableReloading(boolean enableReloading) {
		this.enableReloading = enableReloading;
	}

	public long getCacheDelay() {
		return cacheDelay;
	}

	public void setCacheDelay(long cacheDelay) {
		this.cacheDelay = cacheDelay;
	}

	public int getMaxCachedFileSize() {
		return maxCachedFileSize;
	}

	public void setMaxCachedFileSize(int maxCachedFileSize) {
		this.maxCachedFileSize = maxCachedFileSize;
	}

	public int getCompressAbove() {
		return compressAbove;
	}

	public void setCompressAbove(int compressAbove) {
		this.compressAbove = compressAbove;
	}

	public boolean isUseETags() {
		return useETags;
	}

	public void setUseETags(boolean useETags) {
		this.useETags = useETags;
	}

	public int getCompressionLevel() {
		return compressionLevel;
	}

	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

}