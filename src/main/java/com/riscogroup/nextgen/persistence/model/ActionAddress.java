package com.riscogroup.nextgen.home.api.rule.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.services.DataService;

/**
 * The ActionAddress class is a helper class that contains information about device, function and property that
 * should be executed when a <code>Rule</code> is activated
 */
public class ActionAddress {
	private static final Logger logger = LoggerFactory.getLogger(ActionAddress.class);
	private Object parent;
	private Integer deviceUID;
	private String deviceFunction;
	private Integer propertyIndex;
	private Object value;
	private Object endValue; //The value to be set after duration
	private Integer uID;
	private DataService dataService;
	
	/**
	 * A constructor for getting a non persisted action address for a single execution use.
	 * Use {@link #ActionAddress(Integer, Rule)} if you want the object to be persisted.  </br>
	 * Do not change this object to avoid persisting fields. Declare the result as <code>final</code>.
	 */
	public ActionAddress(Integer deviceUID, String deviceFunction, Integer propertyIndex,
			Object value, Object endValue) {
		this.deviceUID = deviceUID;
		this.deviceFunction = deviceFunction;
		this.propertyIndex = propertyIndex;
		this.value = value;
		this.endValue = endValue;
	}
	
	public ActionAddress(Object parent) {
		this.parent = parent;
	}

	public ActionAddress(Integer uid) {
		this.uID = uid;
	}

	public void setUID(int uID) {
		this.uID = uID;
	}
	
	public void setDeviceUID(Integer deviceUID) {
		logger.trace("({})", deviceUID);
		if (deviceUID <= 0) {
			logger.error("ActionAddress property 'deviceID' is invalid " + deviceUID);
			return;
		}
		this.deviceUID = deviceUID;
		registerForUpdate();
	}

	public Integer getDeviceUID() {
		return this.deviceUID;
	}
	
	public void setParent(Object parent) {
		this.parent = parent;
	}

	public void setDeviceFunction(String deviceFunction) {
		logger.trace("({})", deviceFunction);
		if (deviceFunction == null || deviceFunction.equals("")) {
			logger.error("ActionAddress property 'deviceFunction' is invalid " + deviceFunction);
			return;
		}
		this.deviceFunction = deviceFunction;
		registerForUpdate();
	}

	public String getDeviceFunction() {
		return this.deviceFunction;
	}

	public void setPropertyIndex(Integer propertyIndex) {
		logger.trace("({})", propertyIndex);
		if (propertyIndex <= 0) {
			logger.error("ActionAddress property 'propertyIndex' is invalid " + propertyIndex);
			return;
		}
		this.propertyIndex = propertyIndex;
		registerForUpdate();
	}

	public Integer getPropertyIndex() {
		return this.propertyIndex;
	}

	// TODO: parameter Object has to be changed to specific type, otherwise persistence complains about it.
	public void setValue(Object value) {
		this.value = value;
		registerForUpdate();
	}

	public Object getValue() {
		return this.value;
	}

	public Object getParent() {
		return parent;
	}

	public Object getEndValue() {
		return endValue;
	}

	public void setEndValue(Object previousValue) {
		this.endValue = previousValue;
		registerForUpdate();
	}
	
	/**
	 * Returns whether current {@link ActionAddress} object is equal to other {@link ActionAddress} object.
	 * If all member variables of the first {@link ActionAddress} are equal to all member variables of the other
	 * {@link ActionAddress} we conclude that two objects are equal.
	 * @param other {@link ActionAddress} to compare with.
	 * @return True if two {@link ActionAddress} objects are equal, otherwise false.
	 */
	public boolean equals(ActionAddress other) {
		if(other == null) {
			return false;
		}

		return 	(deviceUID == null ? other.getDeviceUID() == null : deviceUID.equals(other.getDeviceUID())) &&
				(deviceFunction == null ? other.getDeviceFunction() == null : deviceFunction.equals(other.getDeviceFunction())) &&
				(propertyIndex == null ? other.getPropertyIndex() == null : propertyIndex.equals(other.getPropertyIndex())) &&
				(value == null ? other.getValue() == null : value.equals(other.getValue())) &&
				(endValue == null ? other.getEndValue() == null : endValue.equals(other.getEndValue()));
	}

	@Override
	public String toString() {
		return "ActionAddress Id: " + getUID() + ", deviceFunction: " + getDeviceFunction() + ", deviceUID: "
				+ getDeviceUID() + ", propertyIndex: " + getPropertyIndex() + ", value: " + getValue()
				+ ", previousValue: " + getEndValue() + ", Parent: " + getParent();
	}

	public Integer getUID() {
		return this.uID;
	}

	protected void registerForUpdate() {
		if(this.uID == null) {
			return;
		}
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}
}
