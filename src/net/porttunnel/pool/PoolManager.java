package net.porttunnel.pool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.porttunnel.comment.Peer;
import net.porttunnel.tunnel.TunnelFactory;

public class PoolManager implements Runnable {

	private static final int MAX_SIZE = 10 * 1024;  //支持通讯包的最大字节数
	private static final int CMD_BIND = 1;
	
	private static Map<String, Socket> upper;
	private Thread thread;
	private static boolean run = false;
	
	public PoolManager() {
		// TODO Auto-generated constructor stub
	}
	
	public PoolManager(Map<String, Socket> upper) {
		PoolManager.upper = upper;
	}
	
	public boolean start(){
		try {			
            thread = new Thread(this);
	        thread.setDaemon(true);
	        thread.setName("SocketManager thread");
	        thread.start();
	        return true;
        } catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
	}
	
	public boolean stop(){
		try{
			run = false;
			thread.join();
			return true;
		} catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
	}

	@Override
	public void run() {		
		run = true;
		while(run){
			bindHandle();
		}
	}
	
	private static void bindHandle(){
		byte[] buf = new byte[1024];
		String key = "";
		Socket socket = null;
		
		synchronized(upper){
			Iterator<Entry<String, Socket>> itor = upper.entrySet().iterator();
			while (itor.hasNext() && run){
				try {
					Map.Entry<String, Socket> entry = (Map.Entry<String, Socket>) itor.next();
					key = entry.getKey();
					socket = entry.getValue();
					socket.setSoTimeout(5);
					DataInputStream input = new DataInputStream(socket.getInputStream());
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					
					//read
					int recvSize = 0;
					try{
						recvSize = input.readInt();
					}catch(SocketTimeoutException e){
					}
					if (recvSize <= 0 || recvSize > MAX_SIZE){
			        	continue;
					}
					if (buf == null || recvSize > buf.length){
						buf = new byte[recvSize];
			    	}
					int rLen = 0;
					try{
						rLen = ConnectionPool.readn(input, buf, recvSize);
					}catch(SocketTimeoutException e){
					}
			    	if (rLen < recvSize){
			    		continue;
			    	}
					
			    	//execute
			    	String ansstr = new String(buf, 0, rLen);
			    	//System.out.println("receive:" + ansstr);
					String[] ans = ansstr.split("\\|");
					if (!(ans[0].equals("R"))){
						continue;
					}
	
					int id = Integer.parseInt(ans[1]);
					if (id == CMD_BIND){
						String ip = ans[2];
						int port = Integer.parseInt(ans[3]);
						Peer peer = new Peer();
						peer.getMap().setSrcIP(socket.getInetAddress().getHostAddress());
						peer.getMap().setSrcPort(socket.getPort());
						peer.setSocket1(socket);
						peer.getMap().setDestIP(ip);
						peer.getMap().setDestPort(port);
						boolean result = buildConnect(peer);
						if (!result){
							continue;
						}
						//answer
						String cmd = String.format("A|%d|OK", CMD_BIND);
						byte[] buff = cmd.getBytes();
						try{
							output.writeInt(buff.length);
							output.write(buff);
							output.flush();
							//System.out.println("send:" + cmd);
							//remove from pool
							upper.remove(key, socket);
							ConnectionPool.connect(peer.getMap().getSrcIP(), peer.getMap().getSrcPort());
						}catch(Exception e){
						}
					}
				}catch(SocketException | EOFException e){
					try {
						socket.close();
					} catch (IOException e1) {
					}
					//remove from pool
					upper.remove(key, socket);
					ConnectionPool.connect(socket.getInetAddress().getHostAddress(), socket.getPort());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static boolean buildConnect(Peer peer){
		System.out.println(String.format("bind request: %s:%d --> %s:%d", 
				peer.getMap().getSrcIP(), peer.getMap().getSrcPort(), peer.getMap().getDestIP(), peer.getMap().getDestPort()));
        return TunnelFactory.buildTunnel(peer);
	}
	
}
