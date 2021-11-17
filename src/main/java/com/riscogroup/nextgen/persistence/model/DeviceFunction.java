package com.riscogroup.nextgen.persistence.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.communication.ProtocolAdapter;
import com.riscogroup.nextgen.home.api.services.DataService;

public class DeviceFunction {
	private static Logger logger = LoggerFactory.getLogger(DeviceFunction.class);

	private Integer uID;
	protected String name;	
	private List<DeviceProperty> deviceProperties;
	private Device parent;
	private Integer endPointId = 0;
	private String commandName;
	private boolean isProcessed; 	
	private boolean skip;
	
	private DataService dataService;

	public DeviceFunction(Integer uID) {
		this.uID = uID;
		deviceProperties = new ArrayList<>();
	}
	
	public DeviceFunction(Device parent) {
		this.parent = parent;
		deviceProperties = new ArrayList<>();
	}
	
	/**
	 * This method returns the name of the <code>DeviceFunction</code>.
	 * @return the device function name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set a device function name
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
		registerForUpdate();
	}

	public Integer getEndPointId() {
		return endPointId;
	}

	public void setEndPointId(Integer endPointId) {
		this.endPointId = endPointId;
		logger.debug("EndPointId {}", endPointId);
		registerForUpdate();
	}
	
	/**
	 * Set a command name that this device function represents.
	 * @return command name.
	 */
	public void setCommandName(String commandName) {
		if (this.commandName == null) {
			this.commandName = commandName;
			registerForUpdate();
		}
		logger.debug("Function name {} has command {}", name, commandName);
	}

	/**
	 * Returns command name for this device function.
	 * @return command name.
	 */
	public String getCommandName() {
		return commandName;
	}

	/**
	 * Returns the device <code>Device</code> represents that capability.
	 * @return <code>Device</code> object
	 */
	public Device getParent() {
		return parent;
	}
	
	public void setParentDevice(Device device) {
		this.parent = device;
	}

	/**
	 * Set a <code>Device</code> supported properties
	 * @param properties
	 */
	public void setDeviceProperties(List<DeviceProperty> deviceProperties) {
		logger.debug("deviceProperties: " + deviceProperties);
		synchronized (this.deviceProperties) {
			this.deviceProperties = deviceProperties;
		}
	}

	/**
	 * Adds a new property to the exist collection of properties
	 * @param property
	 */
	public void addDeviceProperty(DeviceProperty property) {
		logger.debug("Property is added {}", property.getIndex());
		synchronized (deviceProperties) {
			deviceProperties.add(property);
		}
	}
	
	/**
	 * Remove DeviceProperty from existing collections of properties and deviceProperties
	 */
	public void removeDeviceProperties() {
			deviceProperties = new ArrayList<>();
			registerForUpdate();
	}

	/**
	 * Returns a <code>Device</code> supported properties
	 * @return List<DeviceCCProperty>
	 */
	public List<DeviceProperty> getDeviceProperties() {
		return Collections.unmodifiableList(deviceProperties);
	}

	/**
	 * Returns a <DeviceProperty> by given name
	 * @param propertyName
	 * @return <DeviceProperty>
	 */
	public DeviceProperty getDevicePropertyByName(String propertyName) {
		DeviceProperty prop = null;
		logger.debug("Looking for Property with name {}", propertyName);
		synchronized (deviceProperties) {
			for (DeviceProperty property : deviceProperties) {
				String name = (String) property.getProperty(DeviceProperty.PROPERTY_NAME);
				logger.debug("Current property name {}", name);
				if (name != null && name.equals(propertyName)) {
					prop = property;
					break;
				}
			}
			return prop;
		}
	}

	/**
	 * Returns a DeviceProperty by given UID
	 * @param propertyIndex - the index of the property
	 * @return DeviceProperty of which the UID is matched.
	 */
	public DeviceProperty getDevicePropertyByUID(Integer uid) {
		DeviceProperty prop = null;
		logger.debug("Looking for Property with UID {}", uid);
		synchronized (deviceProperties) {
			for (DeviceProperty property : deviceProperties) {
				if (property.getUID().equals(uid)) {
					prop = property;
					break;
				}
			}
		}
		return prop;
	}

	/**
	 * Returns a DeviceProperty by given index
	 * @param propertyIndex - the index of the property
	 * @return DeviceProperty of which the index is matched.
	 */
	public DeviceProperty getDevicePropertyByIndex(Integer propertyIndex) {
		DeviceProperty prop = null;
		logger.debug("Looking for Property with index {}", propertyIndex);
		synchronized (deviceProperties) {
			for (DeviceProperty property : deviceProperties) {
				if (propertyIndex.equals(property.getIndex())) {
					prop = property;
					break;
				}
			}
		}
		return prop;
	}

	/**
	 * Sets a <code>Device</code> supported property value. It can be Integer or Double type.
	 * @param propertyName - property name
	 * @param propertyValue - value for that property
	 */
	public void sendDevicePropertyValues() {
		logger.trace("()");
		ProtocolAdapter adapter = getParent().getProtocolAdapter();
		logger.debug("Setting value with adapter {}", adapter);
		if (adapter != null) {
			adapter.setDevicePropertyValues(this);
		}
	}
	
	/**
	 * Sets in a synchronous mode a <code>Device</code> supported property value. It can be Integer or Double type.
	 * @param propertyName - property name
	 * @param propertyValue - value for that property
	 */
	public DeviceFunction sendDevicePropertyValuesSynchronously() {
		logger.trace("()");
		ProtocolAdapter adapter = this.getParent().getProtocolAdapter();
		logger.debug("Setting value with adapter {}", adapter);
		if (adapter != null) {
			return adapter.setDevicePropertyValuesSynchronously(this);
		}
		return null;
	}
	
	/**
	 * This method shows if the device function created all its device properties and filled entire information. 
	 * Set to TRUE if device function is completely process otherwise FALSE.
	 * value is FALSE
	 * This is general property
	 * 
	 * @param isProcessed
	 */
	public void setIsProcessed(Boolean isProcessed) {
		this.isProcessed = isProcessed;
		logger.debug("Device function {} processed flag set to {}", commandName, isProcessed);	
		registerForUpdate();		
	}

	/**
	 * Return the processed status of the device function
	 * @return
	 */
	public Boolean isProcessed() {
		return isProcessed;
	}

	/**
	 * This method is used to define if the function will be return to the end user or not. To skip return set TRUE otherwise FALSE
	 * 
	 * @param skip
	 */
	public void setSkip(Boolean skip) {
		logger.debug("Device function flag skip set to {}", skip);
		this.skip = skip;
	}
	
	/**
	 * Return TRUE if the device function will be return to the client otherwise FAlSE and the function will not be returned.
	 * @return
	 */
	public Boolean getSkip() {
		return skip;
	}

	public Integer getUID() {
		return uID;
	}
	
	public void setUID(Integer uID) {
		this.uID = uID;
	}

	protected void registerForUpdate() {
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}
}
