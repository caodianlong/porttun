package net.porttunnel.comment;

public class MapBean {
	
	private String srcIP = "";
	private int srcPort = 0;
	private String destIP = "";
	private int destPort = 0;
	private String proxyHost = "";

	public MapBean() {
		// TODO Auto-generated constructor stub
	}
	
	public String toString(){
		return String.format("%s:%d", srcIP, srcPort);
	}

	public String getSrcIP() {
		return srcIP;
	}

	public void setSrcIP(String srcIP) {
		this.srcIP = srcIP;
	}

	public int getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public String getDestIP() {
		return destIP;
	}

	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}

	public int getDestPort() {
		return destPort;
	}

	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

}
