package net.porttunnel.pormap;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.lang.StringUtils;

import net.porttunnel.comment.*;
import net.porttunnel.config.Config;

public class PortMap {

	private static Map<String, MapBean> mapTable = new HashMap<String, MapBean>();
	private static List<Listener> listeners = new ArrayList<Listener>();
	
	public PortMap() {
		// TODO Auto-generated constructor stub
	}

	public static boolean mapping(Peer peer){
		String key = String.format("%s:%d", peer.getMap().getSrcIP(), peer.getMap().getSrcPort());
		if (mapTable.containsKey(key)){
			MapBean bean = mapTable.get(key);
			peer.getMap().setDestIP(bean.getDestIP());
			peer.getMap().setDestPort(bean.getDestPort());
			peer.getMap().setProxyHost(bean.getProxyHost());
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean add(MapBean bean){
		if (bean == null){
			return false;
		}
		mapTable.put(bean.toString(), bean);
		return true;
	}
	
	public static boolean init(){
		HierarchicalConfiguration configSet = Config.getNode("portmap");
		if (configSet == null){
			System.out.println("Cannot locate \"portmap\" in configuration");
			return false;
		}
		
		List children = configSet.getRoot().getChildren();
		for (int i = 0; i < children.size(); i++){
			MapBean bean = new MapBean();
			bean.setSrcIP(configSet.getString("map(" + i + ")[@SrcIP]", ""));
			bean.setSrcPort(configSet.getInt("map(" + i + ")[@SrcPort]", 0));
			bean.setDestIP(configSet.getString("map(" + i + ")[@DestIP]", ""));
			bean.setDestPort(configSet.getInt("map(" + i + ")[@DestPort]", 0));
			bean.setProxyHost(configSet.getString("map(" + i + ")[@ProxyHost]", ""));
			add(bean);
		}
		
		Iterator<Entry<String, MapBean>> itor = mapTable.entrySet().iterator();
		while (itor.hasNext()){
			Map.Entry<String, MapBean> entry = (Entry<String, MapBean>) itor.next();
			String key = entry.getKey();
			MapBean bean = entry.getValue();
			Listener listen = new Listener(bean.getSrcIP(), bean.getSrcPort());
			boolean ret = listen.start();
			if (ret){
				listeners.add(listen);
			}
		}
		return true;
	}
	
	public static boolean unInit(){
		Iterator<Listener> itor = listeners.iterator();
		while (itor.hasNext()){	
			Listener listen = itor.next();
			listen.stop();
		}
		listeners.clear();
		return true;
	}
	
	public static void printMap(){
		System.out.println("|-----src ip-----|---src port---|-----dest ip-----|---dest port---|");
		Iterator<Entry<String, MapBean>> itor = mapTable.entrySet().iterator();
		while (itor.hasNext()){
			Map.Entry<String, MapBean> entry = (Entry<String, MapBean>) itor.next();
			MapBean bean = entry.getValue();
			StringBuilder line = new StringBuilder();
			line.append( StringUtils.leftPad(bean.getSrcIP().trim().isEmpty() ? "Any IP" : bean.getSrcIP().trim(), 17, ' ') );
			line.append( StringUtils.leftPad(String.valueOf(bean.getSrcPort()), 15, ' ') );
			line.append( StringUtils.leftPad(bean.getDestIP().trim().isEmpty() ? "Any IP" : bean.getDestIP().trim(), 18, ' '));
			line.append( StringUtils.leftPad(String.valueOf(bean.getDestPort()), 16, ' ') );
			System.out.println(line.toString());
		}
	}
}
