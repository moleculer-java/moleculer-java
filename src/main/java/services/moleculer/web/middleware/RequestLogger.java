package services.moleculer.web.middleware;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datatree.Tree;
import services.moleculer.Promise;
import services.moleculer.context.Context;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;
import services.moleculer.web.common.HttpConstants;

@Name("Request Logger")
public class RequestLogger extends Middleware implements HttpConstants {

	// --- LOGGER ---

	protected static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
	
	// --- CREATE NEW ACTION ---

	public Action install(Action action, Tree config) {
		return new Action() {

			@Override
			public Object handler(Context ctx) throws Exception {
				long start = System.currentTimeMillis();
				Object result = action.handler(ctx);				
				return Promise.resolve(result).then(rsp -> {
					long duration = System.currentTimeMillis() - start;
					StringBuilder tmp = new StringBuilder(512);
					tmp.append("Request processed in ");
					tmp.append(duration);
					tmp.append(" milliseconds.\r\nRequest:\r\n");
					tmp.append(ctx.params);
					tmp.append("Response:\r\n");
					if (rsp != null && rsp.isPrimitive()) {
						if (rsp.getType() == byte[].class) {
							tmp.append(new String((byte[]) rsp.asObject()));		
						} else {
							tmp.append(rsp.asObject());
						}
					} else {
						tmp.append(rsp);
					}
					logger.info(tmp.toString());
				}).catchError(cause -> {
					long duration = System.currentTimeMillis() - start;
					StringBuilder tmp = new StringBuilder(512);
					tmp.append("Request processed in ");
					tmp.append(duration);
					tmp.append(" milliseconds.\r\nRequest:\r\n");
					tmp.append(ctx.params);
					tmp.append("Response:\r\n");
					StringWriter stringWriter = new StringWriter(512);
					PrintWriter printWriter = new PrintWriter(stringWriter, true);
					cause.printStackTrace(printWriter);
					tmp.append(stringWriter.toString().trim());
					logger.error(tmp.toString());
					return cause;
				});
			}
			
		};
	}
	
}
