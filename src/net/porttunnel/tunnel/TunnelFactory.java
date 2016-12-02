package net.porttunnel.tunnel;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import net.porttunnel.comment.MapBean;
import net.porttunnel.comment.Peer;
import net.porttunnel.pool.ConnectionPool;
import net.porttunnel.pormap.Listener;

public class TunnelFactory {
	
	private static SimpleDateFormat timeformat =new SimpleDateFormat("HH:mm:ss");
	private static List<Peer> tunnels = new ArrayList<Peer>();

	public TunnelFactory() {
		// TODO Auto-generated constructor stub
	}

	public static boolean init(){
		return true;
	}
	
	public static boolean unInit(){
		Iterator<Peer> itor = tunnels.iterator();
		while(itor.hasNext()){
			Peer peer = itor.next();
			if (peer != null && peer.getTunnel() != null){
				peer.getTunnel().stop();
			}
		}
		return true;
	}
	
	public static boolean buildTunnel(Peer peer){
		Socket socket = ConnectionPool.getConnection(peer);
		if (socket == null){
			return false;
		}
		
		peer.setSocket2(socket);
		peer.setConnectTime(System.currentTimeMillis());
		//start tunnel
		Tunnel t = new Tunnel(peer);
		boolean result = t.start();
		if (!result){
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			peer.setSocket2(null);
			return false;
		}else{
			peer.setTunnel(t);
			tunnels.add(peer);
		}
		System.out.println(String.format("tunnel [%s:%d <--> %s:%d] build", 
				peer.getMap().getSrcIP(), peer.getMap().getSrcPort(), peer.getMap().getDestIP(), peer.getMap().getDestPort()));
		return true;
	}
	
	public static boolean removeTunnel(Peer p){
		return tunnels.remove(p);
	}
	
	public static void printTunnel(){
		System.out.println("|---------------------tunnel---------------------|---time---|---input--|--output---|");
		Iterator<Peer> itor = tunnels.iterator();
		while (itor.hasNext()){	
			Peer peer = itor.next();
			if (peer == null){ continue; }
			String srcIp = peer.getMap().getSrcIP().trim().isEmpty() ? "*" : peer.getMap().getSrcIP().trim();
			String destIp = peer.getMap().getDestIP().trim().isEmpty() ? "*" : peer.getMap().getDestIP().trim();
			int srcPort = peer.getMap().getSrcPort();
			int destPort = peer.getMap().getDestPort();
			StringBuilder line = new StringBuilder();
			line.append( StringUtils.leftPad(String.format("%s:%d <--> %s:%d", srcIp, srcPort, destIp, destPort), 49, ' ') );
			line.append( StringUtils.leftPad(timeformat.format(new Date(peer.getConnectTime())), 11, ' ') );
			line.append( StringUtils.leftPad(humanized(peer.getReceiveBytes()), 11, ' '));
			line.append( StringUtils.leftPad(humanized(peer.getSendBytes()), 12, ' '));
			System.out.println(line.toString());
		}
	}

	private static String humanized(long size){
		double value = 0.0;
		if (size < 1024){
			return String.format("%dByte", size);
		}
		value = (double)size / 1024;
		if (value < 1024){
			return String.format("%.2fkB", value);
		}
		value = value / 1024;
		if (value < 1024){
			return String.format("%.2fMB", value);
		}
		value = value / 1024;
		return String.format("%.2fGB", value);
	}
	
}
