package services.moleculer.eventbus;

import io.datatree.Tree;

@FunctionalInterface
public interface Listener {

	void on(Tree payload) throws Exception;

}