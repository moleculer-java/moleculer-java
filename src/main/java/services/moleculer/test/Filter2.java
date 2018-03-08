package services.moleculer.test;

import org.springframework.stereotype.Component;

import io.datatree.Tree;
import services.moleculer.service.Action;
import services.moleculer.service.Middleware;
import services.moleculer.service.Name;

@Component
@Name("filter1")
public class Filter2 extends Middleware {

	@Override
	public Action install(Action action, Tree config) {
		// TODO Auto-generated method stub
		return null;
	}

}
