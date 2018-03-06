package services.moleculer.web.middleware;

import static services.moleculer.web.common.FileUtils.readAllBytes;

import io.datatree.Tree;
import services.moleculer.context.Context;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.util.CheckedTree;
import services.moleculer.web.common.HttpConstants;

@Name("Favicon")
public class Favicon extends Middleware implements HttpConstants {

	// --- PROPERTIES ---

	protected String iconPath;
	protected int maxAge;

	// --- CACHED RESPONSE ---

	protected Tree response;

	// --- CONSTRUCTORS ---

	public Favicon() {
		this("/favicon.ico");
	}

	public Favicon(String pathToIcon) {
		setIconPath(pathToIcon);
	}

	// --- CREATE NEW ACTION ---

	public Action install(Action action, Tree config) {
		return new Action() {

			@Override
			public Object handler(Context ctx) throws Exception {
				if ("/favicon.ico".equals(ctx.params.getMeta().get(PATH, "/"))) {
					return response;
				}
				return action.handler(ctx);
			}

		};
	}

	// --- PROPERTY GETTERS AND SETTERS ---

	public String getIconPath() {
		return iconPath;
	}

	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
		byte[] bytes = readAllBytes(iconPath);
		if (bytes.length == 0) {
			throw new IllegalArgumentException("File or resource not found: " + iconPath);
		}
		response = new CheckedTree(bytes);
		Tree headers = response.getMeta().get(HEADERS);
		if (maxAge > 0) {
			headers.put(RSP_CACHE_CONTROL, "public, max-age=" + maxAge);
		}
		headers.put(RSP_CONTENT_TYPE, "image/x-icon");
	}

	public int getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

}