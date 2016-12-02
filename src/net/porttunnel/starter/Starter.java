package net.porttunnel.starter;

import java.util.Scanner;

import net.porttunnel.config.Config;
import net.porttunnel.pool.ConnectionPool;
import net.porttunnel.pormap.PortMap;
import net.porttunnel.tunnel.TunnelFactory;

public class Starter {
	
	public Starter() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		
		startService(args);
		
		//µÈ´ýÊäÈë
		Scanner sc = new Scanner(System.in);
		String cmd;
		do {
			System.out.print(">");
			cmd = sc.nextLine();
		} while (commond(cmd));		
		sc.close();
		
		stopService();
	}
	
	
	public static void startService(String[] args){
		
		if (!Config.loadConfig()){
			return;
		}
		
		if (!TunnelFactory.init()){
			return;
		}
		
		if (!PortMap.init()){
			return;
		}
		
		if (!ConnectionPool.init()){
			return;
		}
		
		System.out.println("service start");
	}
	
	public static void stopService(){
		ConnectionPool.unInit();
		PortMap.unInit();
		TunnelFactory.unInit();
		System.out.println("service stop");
	}
	
	public static boolean commond(String cmd){
		try{
			cmd = cmd.trim().toLowerCase();
			if (cmd.equals("exit") || cmd.equals("quit")){
				return false;
			}else if (cmd.equals("map") || cmd.equals("m")){
				PortMap.printMap();
			}else if (cmd.equals("tunnel") || cmd.equals("t")){
				TunnelFactory.printTunnel();
			}else if (cmd.equals("pool") || cmd.equals("p")){
				ConnectionPool.printPool();
			}else if (cmd.equals("upper") || cmd.equals("u")){
				ConnectionPool.printUpper();;
			}else if (cmd.equals("help") || cmd.equals("h")){
				showhelp();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public static void showhelp(){
		StringBuilder text = new StringBuilder();
		text.append("The commands are:\n\n");
		text.append("\tmap\tprint port map\n");
		text.append("\ttunnel\tprint tunnels\n");
		text.append("\tpool\tprint pool\n");
		text.append("\tupper\tprint upper connection\n");
		text.append("\thelp\tprint this message\n");
		text.append("\texit\tend the process\n");
		text.append("\tquit\tthe same as \"exit\"\n");
		System.out.println(text.toString());
	}
}
