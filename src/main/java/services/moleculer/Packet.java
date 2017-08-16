package services.moleculer;

public class Packet {

	public static final String PACKET_UNKNOW		= "???";
	public static final String PACKET_EVENT 		= "EVENT";
	public static final String PACKET_REQUEST 		= "REQ";
	public static final String PACKET_RESPONSE		= "RES";
	public static final String PACKET_DISCOVER 		= "DISCOVER";
	public static final String PACKET_INFO 			= "INFO";
	public static final String PACKET_DISCONNECT 	= "DISCONNECT";
	public static final String PACKET_HEARTBEAT 	= "HEARTBEAT";
	
	private Transit transit;
	
	public String type;
	public String target;
	
	public Object payload;
	
	public Packet(Transit transit, String type, String target) {
		this.transit = transit;
		this.type = type;
		this.target = target;
	}

	public Object serialize() {
		return this.transit.serialize(this.payload, this.type);
	}

	/*public static Object deserialize(Transit transit, String type, Object data) {
		Object payload = transit.deserialize(data, type);
		
		Packet packet = new XYPacket(transit);
		packet.transformPayload(payload);
	}*/
	
	private void transformPayload(Object payload) {
		this.payload = payload;
	}
}
