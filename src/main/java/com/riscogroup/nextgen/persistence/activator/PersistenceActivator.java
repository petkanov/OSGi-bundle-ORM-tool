package com.riscogroup.nextgen.persistence.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.monitor.ServiceRegistrationManager;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.persistence.service.PersistenceServiceImpl;
 
public class PersistenceActivator implements BundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(PersistenceActivator.class);
	
	private static BundleContext bundleContext;
	private static ServiceRegistrationManager manager;
	
	private DataService dataService;
	
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		manager = new ServiceRegistrationManager(context, 
												"com.riscogroup.nextgen.persistence",
												 new String[] { DataService.class.getName() });
		try {
			dataService = new PersistenceServiceImpl();
			
			logger.info("<com.riscogroup.nextgen.persistence> Bundle started");
		} catch (Exception e) {
			logger.error("Service did not initialize properly " + e.getMessage());
		}
		registerService(DataService.class.getName(), dataService);
	}
	
	public void stop(BundleContext context) throws Exception {
		if (dataService != null) {
			dataService = null;
		}
		manager.unregisterAllServices();
		
		logger.info("<com.riscogroup.nextgen.persistence> Bundle stopped");
	}
 
	public Version getVersion() {
		return bundleContext.getBundle().getVersion();
	}
	
	public static void registerService(String serviceClassName, Object service) {
		if(bundleContext != null) {
			manager.registerService(serviceClassName, service, null);
		}
	}
}
