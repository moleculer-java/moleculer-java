package services.moleculer;

import services.moleculer.services.Action;
import services.moleculer.services.Name;
import services.moleculer.services.Service;

@Name("math")
public class TestService extends Service {

	public Action add = (ctx) -> {
		return ctx.params().get("a").asInteger() + ctx.params().get("b").asInteger();
	};

}