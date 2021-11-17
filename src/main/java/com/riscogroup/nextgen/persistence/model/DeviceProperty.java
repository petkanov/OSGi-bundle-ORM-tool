package com.riscogroup.nextgen.home.api.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.services.DataService;

public class DeviceProperty {

	private static final Logger logger = LoggerFactory.getLogger(DeviceProperty.class);

	/**
	 * Constant for meta information to describe a property. The value is simple text.
	 */
	public static final String PROPERTY_DESCRIPTION = "description";
	/**
	 * Constant for property name.
	 */
	public static final String PROPERTY_NAME = "name";
	/**
	 * Constant for unit property name. Example of value: V, KW, etc.
	 */
	public static final String PROPERTY_UNIT = "unit";
	/**
	 * Constant to represent type of the property type.
	 */
	public static final String PROPERTY_TYPE = "type";

	public static final String PROPERTY_ACCESS = "access";
	/**
	 * Constant for property name indicates that the described property can be read.
	 */
	public static final String PROPERTY_ACCESS_READ_ONLY = "R";
	/**
	 * Constant for property name indicates that the described property can be read or write.
	 */
	public static final String PROPERTY_ACCESS_READ_WRITE = "W";
	
	/**
	 * Constant for enumeration property name. It specifies valid enumeration. The value
	 * is Map, where the keys and values are all strings. The key specifies
	 * the text representation of the value. The value is simple java value.
	 */
	//public static final String PROPERTY_ENUM = "enum";	
	/**
	 * Constant for property name that indicates if the value of described property is default.
	 */
	public static final String PROPERTY_IS_DEFAULT = "is.default";
	/**
	 * Constant for property name that indicates its default value. The value is
	 * string representation of the default value.
	 */
	public static final String PROPERTY_DEFAULT_VALUE = "default.value";	
	
	private Integer uID;
	private Integer propIndex;
	private DeviceFunction deviceFunction;
	
	private Map<String, String> propertiesMap;
	private List<String> enumsList;
	
	private Object value;
	private List<Integer> valueArray;
	
	private Object targetValue;
	
	private Integer endPointID;
	private boolean persistFlag;

	private DataService dataService;

	public DeviceProperty(Integer uID) {
		this.uID = uID;
	}
	
	public DeviceProperty(DeviceFunction parent) {	
		this.deviceFunction = parent;
		propertiesMap = new HashMap<String, String>(3, 1f);
		enumsList =  new ArrayList<String>();
		logger.debug("DeviceProperty is created with an Index {} Command Class {} and Endpoint {}", propIndex, deviceFunction.getName(), endPointID);
	}

	public Integer getUID() {
		return uID;
	}

	/**
	 * This method returns a <code>Device</code> command class property index.
	 * @return ccPropIndex
	 */
	public Integer getIndex() {
		return propIndex;
	}

	public void setIndex(Integer propIndex){
		this.propIndex = propIndex;	
		registerForUpdate();
	}
	
	/**
	 * This method returns the additional properties of the <code>DeviceProperty</code>.
	 * The type of keys and values is a String.
	 *
	 * @return last known values of the additional properties of this device
	 */
	public Map<String, Object> getProperties() {
		synchronized (propertiesMap) {
			return propertiesMap != null && propertiesMap.size() > 0 ? Collections.unmodifiableMap(propertiesMap) : null;
		}
	}

	/**
	 * This method returns the value of one additional property of the <code>DeviceProperty</code>.
	 * @param property specified the additional property.
	 * @return last known value for an additional property of this <code>Device</code>
	 */
	public Object getProperty(String property) {
		synchronized (propertiesMap) {
			return propertiesMap.get(property);
		}
	}

	/**
	 * Removes specific property of the <code>DeviceProperty</code>.
	 * @param property specified the additional property.
	 */
	public String removeProperty(String property) {
		String rmvProperty =  (String) propertiesMap.remove(property);
		registerForUpdate();
		return rmvProperty;
	}
	
	public void setProperties(Map<String, String> properties){
		this.propertiesMap = properties;
		registerForUpdate();
	}
	
	public void setEnum(List<String> enums){
		this.enumsList = enums;
		registerForUpdate();
	}

	/**
	 * This method returns the values of enum property of <code>DeviceProperty</code>.
	 * @return enum values
	 */
	public List<String> getEnums() {
		return enumsList != null && enumsList.size() > 0 ? Collections.unmodifiableList(enumsList) : null;
	}
	
	public void setEnum(String enumName) {
		enumsList.add(enumName);
		registerForUpdate();
	}

	/**
	 * Sets a <code>Device</code> supported command class configuration
	 * @param propertyName - property name
	 * @param propertyValue - value for that property
	 */
	public void setPropertyConfiguration(String propertyName, String propertyValue) {
		logger.trace("Store configuration name {} with value {}", propertyName, propertyValue);
		propertiesMap.put(propertyName, propertyValue);
		registerForUpdate();
	}

	/**
	 * Returns the <code>Device</code> capability represents that property, i.e. <code>DeviceFunction</code>
	 * 
	 * @return <code>DeviceFunction</code> object
	 */
	public DeviceFunction getParent() {
		return deviceFunction;
	}
	
	public void setParentDeviceFunction(DeviceFunction function) {
		this.deviceFunction = function;
	}

	/**
	 * Sets a <code>Device</code> supported property value. It can be Integer or Double type.
	 * @param targetValue - value for that property
	 */
	public void setTargetValue(Object targetValue) {
		logger.trace("({})", targetValue);
		this.targetValue = targetValue;
	}

	public Object getTargetValue() {
		return targetValue;
	}

	public String getTargetString() {
		return targetValue == null ? "" : targetValue.toString();
	}

	public long getTargetLong() {
		if (targetValue instanceof String) {
			return Long.parseLong((String) targetValue);
		} else {
			return ((Number) targetValue).longValue();
		}
	}

	public int getTargetInteger() {
		return (int) getTargetLong();
	}

	public double getTargetDouble() {
		if (targetValue instanceof String) {
			return Double.parseDouble((String) targetValue);
		} else {
			return ((Number) targetValue).doubleValue();
		}
	}

	public boolean getTargetBoolean() {
		return targetValue instanceof String && "TRUE".equalsIgnoreCase((String) targetValue);
	}

	@SuppressWarnings("unchecked")
	public List<Integer> getTargetList() {
		return (List<Integer>) targetValue;
	}

	@SuppressWarnings("unchecked")
	public List<Long> getTargetLongList() {
		return (List<Long>) targetValue;
	}

	/*
	 * Returns an id of the end point device
	 */
	public Integer getEndPointID() {
		return endPointID;
	}

	public void setEndPointID(Integer endPointID){
		this.endPointID = endPointID;
		registerForUpdate();
	}
	
	/**
	 * Sets the command class property value. The type is Integer or Double.
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public void setValue(Object value) {
		if (value == null) {
			return;
		}
		if(value instanceof List) {
			this.valueArray = (List<Integer>) value;
			this.value = null;
		} else {
			this.value = value;
			this.valueArray = null;
		}
		if (persistFlag) {
			registerForUpdate();
		}
	}

	/**
	 * Returns command class property value. The type is Integer or Double.
	 * @param value
	 */
	public Object getValue() {
		return value != null ? value : valueArray;
	}
	
	public void setPersistFlag(Boolean persistFlag){
		this.persistFlag = persistFlag;
		registerForUpdate();
	}
	
	public Boolean isPersistFlag() {
		return persistFlag;
	}
	
	public void setUID(Integer uID) {
		this.uID = uID;
	}

	@Override
	public String toString() {
		return "_DeviceProperty [uID=" + uID + ", propIndex=" + propIndex + ", deviceFunction=" + deviceFunction
				+ ", propertiesMap=" + propertiesMap + ", enumsList=" + enumsList + ", value=" + value + ", valueArray="
				+ valueArray + ", targetValue=" + targetValue + ", endPointID=" + endPointID + ", persistFlag="
				+ persistFlag + ", dataService=" + dataService + "]";
	}

	private void registerForUpdate() {
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}
}
