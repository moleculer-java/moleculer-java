package services.moleculer;

import java.util.LinkedList;
import java.util.List;

public class Node {

	public String id;
	
	public long uptime;
	
	public boolean available;
	
	public long lastHeartbeatTime;
	
	public List<Object> services = new LinkedList<Object>();
}
