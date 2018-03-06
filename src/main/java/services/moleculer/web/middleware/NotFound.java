package services.moleculer.web.middleware;

import java.nio.charset.StandardCharsets;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.context.Context;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.web.common.HttpConstants;

@Name("Not Found")
public class NotFound extends Middleware implements HttpConstants {

	// --- JSON / HTML RESPONSE ---

	protected boolean htmlResponse;

	// --- CONSTRUCTOR ---
	
	// --- CREATE NEW ACTION ---

	public Action install(Action action, Tree config) {
		return new Action() {

			@Override
			public Object handler(Context ctx) throws Exception {

				// Get path
				String path = ctx.params.getMeta().get(PATH, "/");

				// 404 Not Found
				Tree rsp = new Tree();
				Tree meta = rsp.getMeta();
				meta.put(STATUS, 404);
				Tree headers = meta.putMap(HEADERS, true);
				if (htmlResponse) {
					headers.put(RSP_CONTENT_TYPE, "text/html;charset=utf-8");
					StringBuilder body = new StringBuilder(512);
					body.append("<html><body><h1>Not found: ");
					body.append(path);
					body.append("</h1><hr/>");
					body.append("Moleculer V");
					body.append(ServiceBroker.SOFTWARE_VERSION);
					body.append("</body></html>");
					rsp.setObject(body.toString().getBytes(StandardCharsets.UTF_8));
				} else {
					headers.put(RSP_CONTENT_TYPE, "application/json;charset=utf-8");
					rsp.put("message", "Not Found: " + path);
				}
				return rsp;
			}

		};
	}

	// --- PROPERTY GETTERS AND SETTERS ---

	public boolean isHtmlResponse() {
		return htmlResponse;
	}

	public void setHtmlResponse(boolean htmlResponse) {
		this.htmlResponse = htmlResponse;
	}

}
