package services.moleculer.services;

import services.moleculer.context.Context;

@FunctionalInterface
public interface Action {
	
	Object handler(Context ctx) throws Exception;
	
}