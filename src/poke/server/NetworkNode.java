package poke.server;

public class NetworkNode {

	String nodeID;
	String host;
	int port;
	int mgmtPort;
	
	public NetworkNode(String nodeId, String host, int port, int mgmtPort) {
		// TODO Auto-generated constructor stub
		this.nodeID = nodeId;
		this.host = host;
		this.port = port;
		this.mgmtPort = mgmtPort;
	}
	
	public String getNodeId()
	{
		return nodeID;
	}
	
	public String getHost()
	{
		return host;
	}
	
	public int getPort()
	{
		return port;
	}

	public int getManagementPort()
	{
		return mgmtPort;
	}
}
