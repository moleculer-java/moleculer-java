package services.moleculer.services;

import services.moleculer.context.Context;

@FunctionalInterface
public interface Action {

	/**
	 * Action's main method. Allowed return types: Promise, CompletableFuture,
	 * Tree, String, int, double, byte, float, short, long, boolean, byte[],
	 * UUID, Date, InetAddress, BigInteger, BigDecimal, and Java Collections
	 * (Map, List, Set) with these types.
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	public Object handler(Context ctx) throws Exception;

}