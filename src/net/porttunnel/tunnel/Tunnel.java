package net.porttunnel.tunnel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.crypto.IllegalBlockSizeException;

import net.porttunnel.comment.Peer;

public class Tunnel implements Runnable {

	private static final int TIME_OUT = 5;
	private static final int BUFFER_SIZE = 1024;
	private Peer peer;
	private Thread thread;
	private boolean run = false;
	
	DataInputStream input1, input2;
	DataOutputStream output1, output2; 
	
	public Tunnel() {
		// TODO Auto-generated constructor stub
	}
	
	public Tunnel(Peer peer){
		this.peer = peer;
		this.peer.setTunnel(this);
	}
	
	public boolean start(){
		try {
			Socket s1 = peer.getSocket1();			
			s1.setSoTimeout(TIME_OUT);
			input1 = new DataInputStream(s1.getInputStream());
			output1 = new DataOutputStream(s1.getOutputStream()); 
			
			Socket s2 = peer.getSocket2();			
			s2.setSoTimeout(TIME_OUT);
			input2 = new DataInputStream(s2.getInputStream());
			output2 = new DataOutputStream(s2.getOutputStream()); 
			
			
            thread = new Thread(this);
	        thread.setDaemon(true);
	        thread.setName(String.format("tunnel %s:%d<-->%s:%d", 
	        		peer.getMap().getSrcIP(), peer.getMap().getSrcPort(), peer.getMap().getDestIP(), peer.getMap().getDestPort()));
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
		long recvCount = 0, sendCount = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		run = true;
		while(run){	
			try {
				//socket 1 --> socket 2
				try{
					int rlen1 = input1.read(buf, 0, BUFFER_SIZE);
					if (rlen1 == -1){
						break;
					}else if (rlen1 > 0){
						output2.write(buf, 0, rlen1);
						output2.flush();
						recvCount += rlen1;
						peer.setReceiveBytes(recvCount);
					}
				}catch(SocketTimeoutException e){
					
				}catch(EOFException | SocketException e){
	        		//System.out.println("connectiong " + peer.getSrcIP() + ":" + peer.getSrcPort() + " close");
	       			break;
	       		}catch (Exception e) {
	       			//System.out.println("connectiong " + peer.getSrcIP() + ":" + peer.getSrcPort() + " exception:" + e.getMessage());
	       			break;
	       		}
				
				//socket 1 <-- socket 2
				try{
					int rlen2 = input2.read(buf, 0, BUFFER_SIZE);
					if (rlen2 == -1){
						break;
					}else if (rlen2 > 0){
						output1.write(buf, 0, rlen2);
						output1.flush();
						sendCount += rlen2;
						peer.setSendBytes(sendCount);
					}
				}catch(SocketTimeoutException e){
					
				}catch(EOFException | SocketException e){
	        		//System.out.println("connectiong " + peer.getSrcIP() + ":" + peer.getSrcPort() + " close");
	       			break;
	       		}catch (Exception e) {
	       			//System.out.println("connectiong " + peer.getSrcIP() + ":" + peer.getSrcPort() + " exception:" + e.getMessage());
	       			break;
	       		}
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		try {
			peer.getSocket1().close();
			peer.getSocket2().close();
			TunnelFactory.removeTunnel(peer);
			run = false;
			System.out.println(String.format("tunnel [%s:%d <--> %s:%d] free", 
					peer.getMap().getSrcIP(), peer.getMap().getSrcPort(), peer.getMap().getDestIP(), peer.getMap().getDestPort()));
		} catch (IOException e) {
		}
	}
}
