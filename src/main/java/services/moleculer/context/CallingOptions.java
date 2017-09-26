package services.moleculer.context;

public class CallingOptions {
	
	public Long timeout;
	
	public Integer retryCount;
	
	// Target nodeID for direct call
	public String nodeID; 
	
	public String requestID;
	
	public Context parentCtx;
	
}
