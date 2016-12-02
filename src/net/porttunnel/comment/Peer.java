package net.porttunnel.comment;

import java.net.Socket;

import net.porttunnel.tunnel.Tunnel;

public class Peer {
	private MapBean map = new MapBean();
	private long connectTime = 0;
	private long receiveBytes = 0;
	private long sendBytes = 0;
	private Socket socket1;
	private Socket socket2;
	private Tunnel tunnel;
	
	public Peer() {
		// TODO Auto-generated constructor stub
	}

	public long getConnectTime() {
		return connectTime;
	}

	public void setConnectTime(long connectTime) {
		this.connectTime = connectTime;
	}

	public long getReceiveBytes() {
		return receiveBytes;
	}

	public void setReceiveBytes(long receiveBytes) {
		this.receiveBytes = receiveBytes;
	}

	public long getSendBytes() {
		return sendBytes;
	}

	public void setSendBytes(long sendBytes) {
		this.sendBytes = sendBytes;
	}

	public Socket getSocket1() {
		return socket1;
	}

	public void setSocket1(Socket socket1) {
		this.socket1 = socket1;
	}

	public Socket getSocket2() {
		return socket2;
	}

	public void setSocket2(Socket socket2) {
		this.socket2 = socket2;
	}

	public Tunnel getTunnel() {
		return tunnel;
	}

	public void setTunnel(Tunnel tunnel) {
		this.tunnel = tunnel;
	}

	public MapBean getMap() {
		return map;
	}

	public void setMap(MapBean map) {
		this.map = map;
	}

}
