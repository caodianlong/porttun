package net.porttunnel.pormap;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.porttunnel.comment.Peer;
import net.porttunnel.tunnel.TunnelFactory;

public class Listener implements Runnable {
	
	private String hostname = "";
	private int port = 0;
	private ServerSocket svrSocket;
	private Thread thread;

	public Listener() {
		// TODO Auto-generated constructor stub
	}
	
	public Listener(String host, int port) {
		this.hostname = host;
		this.port = port;
	}
	
	public boolean start(){		
        try {
        	svrSocket = new ServerSocket();
        	svrSocket.setReuseAddress(true);
			svrSocket.bind(hostname.isEmpty() ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port));
			System.out.println("port map listen at " + hostname + ":" + port);
			
            thread = new Thread(this);
	        thread.setDaemon(true);
	        thread.setName("port map " + hostname + ":" + port);
	        thread.start();
        }catch(BindException e){
        	System.out.println("Cannot listen at " + hostname + ":" + port + "," + e.getMessage());
        	return false;
		}catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
        
        return true;
	}
	
	public boolean stop(){
		try {
            svrSocket.close();
            if (thread != null) {
                this.thread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
		return true;
	}

	@Override
	public void run() {
		Socket socket = null;
		do {
            try {
                socket = svrSocket.accept();
                socket.setTcpNoDelay(true);
                //socket.setSoTimeout(0);
                System.out.println(String.format("new request: %s:%d --> %s:%d", 
                		socket.getInetAddress().getHostAddress(), socket.getPort(),
                		hostname, port));
                boolean result = buildConnect(socket);
                if (!result){                	
                	socket.close();
                }
            }catch(SocketException e){
            	try {socket.close();} catch (Exception e1) {}
            }catch (Exception e) {                	
            	try {socket.close();} catch (Exception e1) {}
            }
        } while (!svrSocket.isClosed());
	}
	
	private boolean buildConnect(Socket srcSocket){
		Peer peer = new Peer();
        peer.getMap().setSrcIP(hostname);
        peer.getMap().setSrcPort(port);
        peer.setSocket1(srcSocket);
        //fill map info
        PortMap.mapping(peer);
        //build a tunnel
        return TunnelFactory.buildTunnel(peer);
	}

	public String getHostName() {
		return hostname;
	}

	public void setHostName(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
