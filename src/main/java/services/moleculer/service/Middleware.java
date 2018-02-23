package services.moleculer.service;

import io.datatree.Tree;

public abstract class Middleware {

	public abstract Action install(Action action, Tree config);
	
}