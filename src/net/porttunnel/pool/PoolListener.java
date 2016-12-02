package net.porttunnel.pool;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class PoolListener implements Runnable {

	private String hostname = "";
	private int port = 0;
	private ServerSocket svrSocket;
	private Thread thread;
	
	public PoolListener() {
		// TODO Auto-generated constructor stub
	}

	public boolean start(){		
        try {
        	svrSocket = new ServerSocket();
        	svrSocket.setReuseAddress(true);        	
			svrSocket.bind(hostname.isEmpty() ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port));
			System.out.println("connection pool listen at " + hostname + ":" + port);

            thread = new Thread(this);
	        thread.setDaemon(true);
	        thread.setName("pool listen thread " + hostname + ":" + port);
	        thread.start();
        }catch(BindException e){
        	System.out.println("Cannot listen at " + hostname + ":" + port + "," + e.getMessage());
        	return false;
		} catch (Exception e) {
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
		do {
            try {
                final Socket socket = svrSocket.accept();
                socket.setTcpNoDelay(true);
                String host = socket.getInetAddress().getHostAddress();
                //socket.setSoTimeout(0);
                System.out.println(String.format("receive reverse connection %s:%d", host, socket.getPort()));
                ConnectionPool.putSocket(host, socket);
            }catch(SocketException e){
            	//e.printStackTrace();
            }catch (Exception e) {
                //e.printStackTrace();
            }
        } while (!svrSocket.isClosed());
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
