package net.porttunnel.pool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;

import net.porttunnel.comment.*;
import net.porttunnel.config.Config;

public class ConnectionPool {
	private static final int MAX_SIZE = 10 * 1024;  //支持通讯包的最大字节数
	private static final int CMD_BIND = 1;
	
	private static Map<String, Socket> idleReverseConnection = new HashMap<String, Socket>();
	private static Map<String, Socket> idleConnection = new HashMap<String, Socket>();
	private static PoolManager manager;
	private static PoolListener listener;
	
	public ConnectionPool() {
		// TODO Auto-generated constructor stub
	}

	public static boolean init(){
		//open listening
		if (!openListening()){
			return false;
		}
		
		//create and check connection
		Thread t = new Thread(new Runnable(){
			public void run(){
				while(true){
					try{
						checkReverseConnection();
						createAndCheckConnect();
						Thread.sleep(1000);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		} );
		t.setName("check connection");
		t.setDaemon(true);
		t.start();
		
		//start manager
		manager = new PoolManager(idleConnection);
		return manager.start();
	}
	
	public static boolean unInit(){
		if (listener != null){
			listener.stop();
		}
		if (manager != null){
			manager.stop();
		}
		
		Iterator<Entry<String, Socket>> itor = idleReverseConnection.entrySet().iterator();
		while (itor.hasNext()){
			Map.Entry<String, Socket> entry = itor.next();
			//String key = entry.getKey();
			Socket socket = entry.getValue();
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	public static boolean openListening(){
		HierarchicalConfiguration configSet = Config.getNode("pool");
		if (configSet == null){
			return false;
		}
		int port = configSet.getInt("port", 0);
		if (port > 0){
			listener = new PoolListener();
			listener.setPort(port);
			listener.start();
		}
		
		return true;
	}
	
	
	public static void createAndCheckConnect(){
		//上级连接
		HierarchicalConfiguration configSet = Config.getNode("pool.tunnel");
		if (configSet == null){
			return;
		}
		List children = configSet.getRoot().getChildren();
		for (int i = 0; i < children.size(); i++){
			String ip = configSet.getString("connect(" + i + ")[@DestIP]", "");
			int port = configSet.getInt("connect(" + i + ")[@DestPort]", 0);
			if (!idleConnection.containsKey(ip)){
				connect(ip, port);
			}
		}
	}
	
	public static void checkReverseConnection(){
		synchronized(idleReverseConnection){
			Iterator<Entry<String, Socket>> itor = idleReverseConnection.entrySet().iterator();
			while (itor.hasNext()){
				Map.Entry<String, Socket> entry = itor.next();
				Socket socket = entry.getValue();
				try {
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeInt(0);
				} catch (IOException e) {
					try {socket.close();} catch (IOException e1) {}
					itor.remove();
					System.out.println("close reverse connection " + socket.getInetAddress().getHostAddress() + socket.getPort());
				}
			}
		}
	}
	
	public static boolean connect(String host, int port){
		//create socket
		Socket socket;
		try {
			//System.out.println(String.format("connect to %s:%d...", host, port));
			socket = new Socket(host, port);
			synchronized(idleConnection){
				idleConnection.put(host, socket);
			}
			System.out.println(String.format("connect to %s:%d success", host, port));
			return true;
		}catch (Exception e) {
			System.out.println(String.format("connect to %s:%d fail,%s", host, port, e.getMessage()));
			return false;
		}
	}
	
	public static Socket getConnection(Peer peer){
		Socket result = null;
		
		//get tunnel connection from pool
		result = getIdleReverseConnection(peer);
		if (result != null){
			return result;
		}
		
		//create connection directly
		try {
			result = new Socket(peer.getMap().getDestIP(), peer.getMap().getDestPort());
			System.out.println(String.format("new Socket(%s,%d)", peer.getMap().getDestIP(), peer.getMap().getDestPort()));
		}catch(ConnectException e){
			System.out.println(String.format("new Socket(%s,%d) fail,%s", peer.getMap().getDestIP(), 
					peer.getMap().getDestPort(), e.getMessage()));
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println(String.format("new Socket(%s,%d) fail,%s", peer.getMap().getDestIP(), 
					peer.getMap().getDestPort(), e.getMessage()));
		}
		return result;
	}
	
	public static void putSocket(String host, Socket socket){
		if (socket != null){
			idleReverseConnection.put(host, socket);
		}
	}
	
	public static Socket getIdleReverseConnection(Peer peer){
		String host = "";
		if (!peer.getMap().getProxyHost().trim().isEmpty()){
			host = peer.getMap().getProxyHost().trim();
		}else{
			host = peer.getMap().getDestIP();
		}
		if (!idleReverseConnection.containsKey(host)){
			return null;
		}
		
		//bind
		Socket socket = idleReverseConnection.get(host);
		boolean ret = bind(peer, socket);
		idleReverseConnection.remove(host, socket);
		if (ret){
			System.out.println(String.format("bind %s:%d", peer.getMap().getDestIP(), peer.getMap().getDestPort()));
			return socket;
		}else{
			try { socket.close(); } catch (Exception e) {	}
			System.out.println(String.format("bind tunnel %s:%d fail", peer.getMap().getDestIP(), peer.getMap().getDestPort()));
			return null;
		}
	}
	
	private static boolean bind(Peer peer, Socket socket){		
		try {
			int oldValue = socket.getSoTimeout();
			socket.setSoTimeout(10000);
			String cmd = String.format("R|%d|%s|%d|", CMD_BIND, peer.getMap().getDestIP(), peer.getMap().getDestPort());
			byte[] recvBuf = new byte[1024];			
			int recvSize = ConnectionPool.SendAndReceive(socket, cmd.getBytes(), cmd.getBytes().length, recvBuf);
			if (recvSize <= 0){
				return false;
			}
			String ansstr = new String(recvBuf, 0, recvSize);
			System.out.println("receive:" + ansstr);
			String[] ans = ansstr.split("\\|");
			if (!ans[0].equals("A")){
				return false;
			}
			int id = Integer.parseInt(ans[1]);
			if (id != CMD_BIND){
				return false;
			}
			String result = ans[2];
			if (!result.equals("OK")){
				return false;
			}
			socket.setSoTimeout(oldValue);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static int SendAndReceive(Socket socket, byte[] sendBuf, int sendSize, byte[] recvBuf) throws IOException{
		
		int oldValue = socket.getSoTimeout();
		socket.setSoTimeout(5000);
		DataInputStream input = new DataInputStream(socket.getInputStream());
		DataOutputStream output = new DataOutputStream(socket.getOutputStream()); 
		
		//write
		output.writeInt(sendSize);
		output.write(sendBuf, 0, sendSize);
		output.flush();
		
		//read
		int recvSize = input.readInt();
		if (recvSize < 0 || recvSize > MAX_SIZE){
        	return -1;
		}
    	int rLen = readn(input, recvBuf, recvSize);
    	if (rLen < recvSize){
    		return -1;
    	}	                    	
    	socket.setSoTimeout(oldValue);
		return rLen;
	}
	
	public static int readn(InputStream input, final byte[] buf, final int bufLen) throws IOException{
    	int rLen = 0, iLen = 0;
    	do{
    		iLen = input.read(buf, rLen, bufLen-rLen);
    		if (iLen == -1){
    			break;
    		}else{
    			rLen += iLen;
    		}
    	}while (rLen < bufLen);
    	
    	return rLen;
    }
	
	
	public static void printPool(){
		System.out.println("|---------pool---------|");
		Iterator<Entry<String, Socket>> itor = idleReverseConnection.entrySet().iterator();
		while (itor.hasNext()){
			Map.Entry<String, Socket> entry = itor.next();
			Socket socket = entry.getValue();
			String line = StringUtils.leftPad(String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort()), 23, ' ');
			System.out.println(line);
		}
	}
	
	public static void printUpper(){
		System.out.println("|---upper connection---|");
		Iterator<Entry<String, Socket>> itor = idleConnection.entrySet().iterator();
		while (itor.hasNext()){
			Map.Entry<String, Socket> entry = itor.next();
			Socket socket = entry.getValue();
			String line = StringUtils.leftPad(String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort()), 23, ' ');
			System.out.println(line);
		}
	}
	
}
