package com.vormittag.util;

import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.vormittag.util.xml.XmlUtilities;

// TODO - Need to consider synchronization of the cache refresh to avoid race conditions. Not really sure that we care.

public class ConfigCachePunchoutAriba {
	// Setup for Singleton and Cache refresh.
	private static final String cName = ConfigCachePunchoutAriba.class.getCanonicalName();
	private static Logger log = Logger.getLogger(cName);
	private static ConfigCachePunchoutAriba instance = null;
	private static long lastUpdate = 0l;
	private static final long EXPIRY_INTERVAL = 300000l;
	private static final String fileUri = "/config/s2kConfig.xml";
	
	// Constants that we expect to see in s2kconfig.xml
	private static final String CONFIG_ID = "Ariba";
	private static final String ARIBA_USER = "user";
	private static final String ARIBA_PASSWORD = "password";
	private static final String ARIBA_COMPANY = "company";
	private static final String ARIBA_WEBSITE_ID = "website_id";
	private static final String ARIBA_SHARED_SECRET = "shared_secret";
	private static final String ARIBA_JNDI = "jndi_name";
	private static final String ARIBA_PROVIDER_URL = "provider_url";
	private static final String ARIBA_SHOPPING_URL = "shopping_url";
	private static final String ARIBA_DOMAIN = "domain";
	private static final String ARIBA_ALLOW_ORDERS = "allow_orders";
	
	
	// Fields
	private String aribaUser = null;
	private String aribaPassword = null;
	private String aribaCompany = null;
	private String aribaWebsiteId = null;
	private String aribaSharedSecret = null;
	private String aribaJndiName = null;
	private String aribaProviderUrl = null;
	private String aribaShoppingURL = null;
	private String aribaDomain = null;
	private String aribaAllowOrders = null;
	
	// Main entry point for this module. Guarantees lookup won't happen multiple times.
	public static ConfigCachePunchoutAriba getInstance() {
		String cMethod = "getInstance";
		log.entering(cName, cMethod);
		if (instance == null) {
			instance = new ConfigCachePunchoutAriba();
		}
		if (System.currentTimeMillis() > lastUpdate + EXPIRY_INTERVAL) {
			ConfigCachePunchoutAriba newInstance = new ConfigCachePunchoutAriba();
			instance = newInstance;
		}
		log.exiting(cName, cMethod);
		return instance;
	}
	
	private ConfigCachePunchoutAriba() {
		String cMethod = "Private Constructor";
		log.entering(cName, cMethod);
		try	{
			// Get the config from the correct node in s2kConfig.xml
			XmlUtilities xmlUtilities = new XmlUtilities();
			Document myConfig = xmlUtilities.getTheConfig(fileUri);
			NodeList existingConfigurations = myConfig.getElementsByTagName("config");
			NodeList list = xmlUtilities.getTheChildren(existingConfigurations, CONFIG_ID);
			aribaUser = xmlUtilities.getTextNodeValue(list, ARIBA_USER);
			aribaPassword = xmlUtilities.getTextNodeValue(list, ARIBA_PASSWORD);
			aribaCompany = xmlUtilities.getTextNodeValue(list, ARIBA_COMPANY);
			aribaWebsiteId = xmlUtilities.getTextNodeValue(list, ARIBA_WEBSITE_ID);
			aribaSharedSecret = xmlUtilities.getTextNodeValue(list, ARIBA_SHARED_SECRET);
			aribaJndiName = xmlUtilities.getTextNodeValue(list, ARIBA_JNDI);
			aribaProviderUrl = xmlUtilities.getTextNodeValue(list, ARIBA_PROVIDER_URL);
			aribaShoppingURL = xmlUtilities.getTextNodeValue(list, ARIBA_SHOPPING_URL);
			aribaDomain = xmlUtilities.getTextNodeValue(list, ARIBA_DOMAIN);
			aribaAllowOrders = xmlUtilities.getTextNodeValue(list, ARIBA_ALLOW_ORDERS);
			lastUpdate = System.currentTimeMillis();
			log.exiting(cName, cMethod);
		}
		catch (Exception e) {
			new S2KErrorLogger().log(e, "");
		}
	}

	public static long getLastUpdate() {
		return lastUpdate;
	}

	public String getAribaUser() {
		return aribaUser;
	}

	public String getAribaPassword() {
		return aribaPassword;
	}

	public String getAribaCompany() {
		return aribaCompany;
	}

	public String getAribaWebsiteId() {
		return aribaWebsiteId;
	}

	public String getAribaSharedSecret() {
		String cMethod = "getAribaSharedSecret";
		log.entering(cName, cMethod);
		
		log.exiting(cName, cMethod);
		return aribaSharedSecret ;
	}

	public String getJndiName() {
		String cMethod = "getJndiName";
		log.entering(cName, cMethod);
		
		log.exiting(cName, cMethod);
		return aribaJndiName ;
	}

	public String getProviderURL() {
		String cMethod = "getProviderURL";
		log.entering(cName, cMethod);
		
		log.exiting(cName, cMethod);
		return aribaProviderUrl ;
	}

	public String getShoppingURL() {
		String cMethod = "getShoppingURL";
		log.entering(cName, cMethod);
		
		log.exiting(cName, cMethod);
		return aribaShoppingURL ;
	}

	public String getDomain() {
		String cMethod = "getDomain";
		log.entering(cName, cMethod);
		
		log.exiting(cName, cMethod);
		return aribaDomain;
	}

	public boolean allowsOrders() {
		String cMethod = "allowsOrders";
		boolean bRet = false;
		log.entering(cName, cMethod);
		if (aribaAllowOrders.equalsIgnoreCase("yes") || aribaAllowOrders.equalsIgnoreCase("true")) {
			bRet = true;
		}
		log.exiting(cName, cMethod);
		return bRet;
	}
}
