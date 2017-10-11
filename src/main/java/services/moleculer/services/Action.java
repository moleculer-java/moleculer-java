package services.moleculer.services;

import services.moleculer.Promise;
import services.moleculer.context.Context;

@FunctionalInterface
public interface Action {

	public Promise handler(Context ctx) throws Exception;

}