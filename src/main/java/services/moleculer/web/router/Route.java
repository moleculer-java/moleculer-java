/**
 * MOLECULER MICROSERVICES FRAMEWORK<br>
 * <br>
 * This project is based on the idea of Moleculer Microservices
 * Framework for NodeJS (https://moleculer.services). Special thanks to
 * the Moleculer's project owner (https://github.com/icebob) for the
 * consultations.<br>
 * <br>
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
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
package services.moleculer.web.router;

import static services.moleculer.util.CommonUtils.formatPath;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

import services.moleculer.context.CallingOptions;
import services.moleculer.eventbus.Matcher;
import services.moleculer.service.Middleware;
import services.moleculer.web.ApiGateway;

public class Route {

	// --- PARENT GATEWAY ---

	protected final ApiGateway gateway;

	// --- PROPERTIES ---

	protected final String path;
	protected final MappingPolicy mappingPolicy;
	protected final CallingOptions.Options opts;
	protected final String[] whitelist;
	protected final Alias[] aliases;

	// --- CONSTRUCTOR ---

	public Route(ApiGateway gateway, String path, MappingPolicy mappingPolicy, CallingOptions.Options opts,
			String[] whitelist, Alias[] aliases) {
		this.gateway = Objects.requireNonNull(gateway);
		this.path = formatPath(path);
		this.mappingPolicy = mappingPolicy;
		this.opts = opts;
		if (whitelist != null && whitelist.length > 0) {
			for (int i = 0; i < whitelist.length; i++) {
				whitelist[i] = formatPath(whitelist[i]);
			}
		}
		this.whitelist = whitelist;
		if (aliases != null && aliases.length > 0) {
			LinkedList<Alias> list = new LinkedList<>();
			for (Alias alias : aliases) {
				if (Alias.REST.endsWith(alias.httpMethod)) {
					list.addLast(new Alias(Alias.GET, alias.pathPattern, alias.actionName + ".find"));
					list.addLast(new Alias(Alias.GET, alias.pathPattern + "/:id", alias.actionName + ".get"));
					list.addLast(new Alias(Alias.POST, alias.pathPattern, alias.actionName + ".create"));
					list.addLast(new Alias(Alias.PUT, alias.pathPattern + "/:id", alias.actionName + ".update"));
					list.addLast(new Alias(Alias.DELETE, alias.pathPattern + "/:id", alias.actionName + ".remove"));
				} else {
					list.addLast(alias);
				}
			}
			this.aliases = new Alias[list.size()];
			list.toArray(this.aliases);
		} else {
			this.aliases = null;
		}
	}

	// --- REQUEST PROCESSOR ---

	public Mapping findMapping(String httpMethod, String path) {
		if (this.path != null && !this.path.isEmpty()) {
			if (!path.startsWith(this.path)) {
				return null;
			}
		}
		String shortPath = path.substring(this.path.length());
		if (aliases != null && aliases.length > 0) {
			for (Alias alias : aliases) {
				if ("ALL".equals(alias.httpMethod) || httpMethod.equals(alias.httpMethod)) {
					Mapping mapping = new Mapping(gateway.getBroker(), this.path + alias.pathPattern, alias.actionName,
							opts);
					if (mapping.matches(path)) {
						if (!checkedMiddlewares.isEmpty()) {
							mapping.use(checkedMiddlewares);
						}
						return mapping;
					}
				}
			}
		}
		String actionName = shortPath.replace('/', '.').replace('~', '$');
		while (actionName.startsWith(".")) {
			actionName = actionName.substring(1);
		}
		if (whitelist != null && whitelist.length > 0) {
			for (String pattern : whitelist) {
				if (Matcher.matches(shortPath, pattern)) {
					Mapping mapping = new Mapping(gateway.getBroker(), this.path + pattern, actionName, opts);
					if (!checkedMiddlewares.isEmpty()) {
						mapping.use(checkedMiddlewares);
					}
					return mapping;
				}
			}
		}
		if (mappingPolicy == MappingPolicy.ALL) {
			String pattern;
			if (this.path == null || this.path.isEmpty()) {
				pattern = path;
			} else {
				pattern = this.path + '*';
			}
			Mapping mapping = new Mapping(gateway.getBroker(), pattern, actionName, opts);
			if (!checkedMiddlewares.isEmpty()) {
				mapping.use(checkedMiddlewares);
			}
			return mapping;
		}
		return null;
	}

	// --- GLOBAL MIDDLEWARES ---

	protected HashSet<Middleware> checkedMiddlewares = new HashSet<>(32);

	public void use(Middleware... middlewares) {
		use(Arrays.asList(middlewares));
	}

	public void use(Collection<Middleware> middlewares) {
		checkedMiddlewares.addAll(middlewares);
	}

	// --- PROPERTY GETTERS ---

	public ApiGateway getApiGateway() {
		return gateway;
	}

	public String getPath() {
		return path;
	}

	public MappingPolicy getMappingPolicy() {
		return mappingPolicy;
	}

	public CallingOptions.Options getOpts() {
		return opts;
	}

	public String[] getWhitelist() {
		return whitelist;
	}

	public Alias[] getAliases() {
		return aliases;
	}

}