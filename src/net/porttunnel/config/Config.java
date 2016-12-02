package net.porttunnel.config;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

public class Config {
	
	private static final String configFile = "porttunnel-config.xml";
	private static XMLConfiguration config;

	public Config() {
		// TODO Auto-generated constructor stub
	}

	public static boolean loadConfig(){		
		try {
			config =  new XMLConfiguration(configFile);
			return true;
		} catch (ConfigurationException e) {
			//e.printStackTrace();
			System.out.println(e.getMessage());
			return false;
		}		
	}
	
	public static HierarchicalConfiguration getNode(String nodeName){
		HierarchicalConfiguration configSet = (HierarchicalConfiguration)config.subset(nodeName);
		return configSet;
	}
	
}
