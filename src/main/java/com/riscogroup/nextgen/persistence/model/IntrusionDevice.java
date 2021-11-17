package com.riscogroup.nextgen.persistence.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.db.persistence.configurations.Configuration;
import com.riscogroup.nextgen.db.persistence.configurations.Configurations;
import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.generic.BypassState;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.home.api.generic.DeviceProtocol;
import com.riscogroup.nextgen.home.api.generic.DeviceStatus;
import com.riscogroup.nextgen.home.api.generic.DeviceTroublesReportRecord;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.DeviceType;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.NodeAudioChimeOutput;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.NodeAudioOutput;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.ZoneConfiguration;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.ZoneMode;
import com.riscogroup.nextgen.home.api.generic.TroubleType;
import com.riscogroup.nextgen.home.api.services.DataService;

public abstract class IntrusionDevice extends Device {
	private static Logger logger = LoggerFactory.getLogger(IntrusionDevice.class);

	public static final String BASIC_DEVICE_CLASS = "Basic Device Class";

	// Intrusion Device Type: PIR, Contact, and etc.
	private DeviceType deviceType;

	// Node Configuration
	private ZoneConfiguration deviceConfiguration;

	// Node Supervision Time
	private int deviceSupervisionTime;

	// Node Mode
	private ZoneMode deviceMode;

	// Node Audio Description
	private String deviceAudioDescription;

	// Node Audio Output
	private NodeAudioOutput deviceAudioOutput;

	// Node Chime Type
	private NodeAudioChimeOutput deviceChimeType;

	private WirelessIntrusionDeviceProperties wirelessIntrusionDeviceProperties = null;

	// is the zone restorable, a magnetic for example
	protected boolean restorable;

	private Long entryDelay = 0L;
	private Long exitDelay = 0L;
	
	private int deviceOpenStatusCount;

	public IntrusionDevice(Integer uID) {
		super(uID);
		setProtocolType(DeviceProtocol.WIRELESS.getType());
		wirelessIntrusionDeviceProperties = new WirelessIntrusionDeviceProperties();
	}
	public IntrusionDevice() {
		setProtocolType(DeviceProtocol.WIRELESS.getType());
		wirelessIntrusionDeviceProperties = new WirelessIntrusionDeviceProperties();
	}

	/**
	 * creates a function with one property and one property value, an integer
	 * 
	 * @param functionName - function name
	 * @param property - function property
	 * @param propertyValue - property value
	 */
	protected void createFunction(String functionName, TroubleType property, Integer propertyValue) {
		final DeviceFunction deviceFunction = new DeviceFunction(this);
		final DataService dataService = Activator.getService(DataService.class);
		if(dataService != null && dataService.persistObject(deviceFunction)) {
			deviceFunction.setCommandName(functionName);
			createFunctionProperty(deviceFunction, property, propertyValue);
			addFunction(deviceFunction);
			registerForUpdate();
		}else {
			logger.error("Failed to Create DeviceFunction in {}", this);
		}
	}
	
	/**
	 * Creates a device property to current device by given deviceFunction, property name and property value
	 * @param deviceFunction is function where property should be added to
	 * @param property - function property
	 * @param value property value
	 */
	protected void createFunctionProperty(DeviceFunction deviceFunction, TroubleType property, Integer value) {
		logger.trace("deviceFunction: {}, pName: {}, pValue: {}", deviceFunction, property, value);
		if(deviceFunction == null || property == null || value == null) {
			return;
		}

		final DeviceProperty deviceProperty = new DeviceProperty(deviceFunction);
		final DataService dataService = Activator.getService(DataService.class);
		if(dataService != null && dataService.persistObject(deviceProperty)) {
			deviceFunction.addDeviceProperty(deviceProperty);
			
			deviceProperty.setPropertyConfiguration(DeviceProperty.PROPERTY_NAME, property.toLowerCase());
			deviceProperty.setPersistFlag(true);
			deviceProperty.setValue(value);
			deviceProperty.setIndex(property.getValue());
			registerForUpdate();
		}else {
			logger.error("Failed to Create Device Property for {} in {}",deviceFunction, this);
		}
	}

	/**
	 * gets function property
	 * 
	 * @param fName - function name
	 * @param pName - property name
	 * @return DeviceProperty
	 */
	private DeviceProperty getFunctionProperty(String fName, String pName) {
		if(fName == null || pName == null) {
			return null;
		}

		final DeviceFunction function = getFunction(fName);
		return function != null ? function.getDevicePropertyByName(pName) : null;
	}

	/**
	 * gets function property value
	 * 
	 * @param fName - function name
	 * @param pName - property name
	 * @return Integer - property value
	 */
	public Integer getFunctionPropertyValue(String fName, String pName) {

		DeviceProperty devProperty = getFunctionProperty(fName, pName);
		if(devProperty != null)
			return Integer.parseInt( devProperty.getValue().toString());
		else
			return null;
	}

	/**
	 * sets function property with provided value
	 * 
	 * @param fName - function name
	 * @param pName - property name
	 * @param pValue - property value
	 */
	public void setFunctionPropertyValue(String fName, String pName, Integer pValue) {
		logger.trace("fName : {}, pName: {}, pValue: {}", fName, pName, pValue);
		DeviceProperty devProperty = getFunctionProperty(fName, pName);
		if(devProperty != null && pValue != null) {
			devProperty.setValue(pValue);
		}
	}

	/**
	 * init / set functions for all intrusion devices
	 */
	public void initFunctions() {
		createFunction(IntrusionDevicesFunction.FUNCTION_TAMPER_STATUS.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OK);

		createFunction(IntrusionDevicesFunction.FUNCTION_BATTERY_STATUS.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OK);

		createFunction(IntrusionDevicesFunction.FUNCTION_SV_STATUS.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OK);

		createFunction(IntrusionDevicesFunction.FUNCTION_STATUS.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OK);

		createFunction(IntrusionDevicesFunction.FUNCTION_DO_REPORT.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OFF);
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public ZoneConfiguration getDeviceConfiguration() {
		return deviceConfiguration;
	}
	
	public int getDeviceSupervisionTime() {
		return deviceSupervisionTime;
	}

	public void setDeviceSupervisionTime(int deviceSupervisionTime) {
		this.deviceSupervisionTime = deviceSupervisionTime;

	}

	public ZoneMode getDeviceMode() {
		return deviceMode;
	}

	public void setDeviceMode(ZoneMode deviceMode) {
		this.deviceMode = deviceMode;

	}

	public String getDeviceAudioDescription() {
		return deviceAudioDescription;
	}

	public void setDeviceAudioDescription(String deviceAudioDescription) {
		this.deviceAudioDescription = deviceAudioDescription;

	}

	public NodeAudioOutput getDeviceAudioOutput() {
		return deviceAudioOutput;
	}

	public void setDeviceAudioOutput(NodeAudioOutput deviceAudioOutput) {
		this.deviceAudioOutput = deviceAudioOutput;

	}

	public NodeAudioChimeOutput getDeviceChimeType() {
		return deviceChimeType;
	}

	public void setDeviceChimeType(NodeAudioChimeOutput deviceChimeType) {
		this.deviceChimeType = deviceChimeType;

	}

	/**
	 * is restorable a.k.a. magnetic
	 * 
	 * @return
	 */
	public boolean isRestorable() {
		return restorable;
	}

	/**
	 * checks open / close status
	 * 
	 * @return true if it is opened
	 */
	public boolean isOpen() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_STATUS.toString(),
				TroubleType.STATUS.toLowerCase());

		boolean value = (status == IntrusionDevicesProperty.FAIL) ? true : false;
		return value;
	}

	/**
	 * returns if open status of device allows group to be armed
	 * methods is taking into account open status, bypass settings and zone configuration to calculate this value
	 * @param immediateArm is set to true for checking after exit delay expired
	 * @param shouldAllowArmOnOpen if set to true the device will allow arming even if it is opened, in case it is not opened after alarm, a.k.a it was opened after previous restore.
	 * this flag is set to true in case the device is not common and more than one groups it belongs to is disarmed, otherwise is set to false
	 * @param deviceCurrentTroubles DeviceCurrentTroubles object. Can be null.
	 * @return true if open status allows arm
	 */
	public boolean isOpenStatusAllowsArm(boolean immediateArm, boolean shouldAllowArmOnOpen, DeviceTroublesReportRecord deviceCurrentTroubles) {

		if(zoneConfiguration == null || getZoneConfigurationEnum().isNotUsedZoneType())
			return true; // not configured at all or device not used

		if(!isRestorable()) {
			return true; // non restorable device
		}

		if(bypassState.isIgnoreAlarm(true)) {// checks if allowed 24H zones events skipping 
			return true; // device open status is bypassed
		}

		boolean isRestoreReported = deviceCurrentTroubles != null ? !deviceCurrentTroubles.getReportedStatusTrouble(TroubleType.STATUS) : true;
		boolean isBypassStateAllowsArmForIntruderZone = bypassState == BypassState.ONE_TIME || bypassState == BypassState.PERMANENT;
		boolean isForceArmAllowsArmForIntruderZone = getForceArm() && isRestoreReported;
		if((getZoneConfigurationEnum().isIntruderZoneType()) && (isBypassStateAllowsArmForIntruderZone || isForceArmAllowsArmForIntruderZone)) {
			return true; // device open status is bypassed or force arm flag for intruder, follower and entry / exit types
		}

		if(getZoneConfigurationEnum() == ZoneConfiguration.ZONE_TYPE_ENTRY_EXIT && getExitOpen() && !immediateArm) {
			return true; // open zone flag entry / exit type
		}

		if(!isOpen()){
			return true; // zone is closed
		}
		
		if(shouldAllowArmOnOpen && isRestoreReported) {
			return true; // zone is open, but previous open status was restored, it is not opened after alarm
		}
		
		return false;
	}

	/**
	 * sets open / close status
	 * 
	 * @param isOpened true if it is opened
	 */
	public void setOpen(Boolean isOpened) {

		Integer status = isOpened == true ? IntrusionDevicesProperty.FAIL : IntrusionDevicesProperty.OK;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_STATUS.toString(), TroubleType.STATUS.toLowerCase(),
				status);

	}

	/**
	 * checks SV status
	 * 
	 * @return true if it is SV trouble
	 */
	public boolean isSupervisionTrouble() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_SV_STATUS.toString(),
				TroubleType.STATUS.toLowerCase());

		boolean value = (status == IntrusionDevicesProperty.FAIL) ? true : false;
		return value;
	}
	
	
	/**
	 * returns if supervision status of device allows group to be armed
	 * methods is taking into account supervision status, security standards, bypass settings and zone configuration to calculate this value
	 * @return true if supervision status allows arm
	 */
	public boolean isSupervisionStatusAllowsArm() {
		// TODO to handle security standards requirements
		return true;
	}
	
	/**
	 * sets SV status
	 * 
	 * @param isSVTrouble true for trouble
	 */
	public void setSupervisionTrouble(Boolean isSVTrouble) {

		Integer status = isSVTrouble == true ? IntrusionDevicesProperty.FAIL : IntrusionDevicesProperty.OK;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_SV_STATUS.toString(),
				TroubleType.STATUS.toLowerCase(), status);

		// Set device status according to SV status
		DeviceStatus deviceStatus = isSVTrouble == true ? DeviceStatus.FAILED : DeviceStatus.ALIVE;
		setStatus(deviceStatus);
	}

	/**
	 * checks battery status
	 * 
	 * @return true if it is battery trouble
	 */
	public boolean isBatteryTrouble() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_BATTERY_STATUS.toString(),
				TroubleType.STATUS.toLowerCase());

		boolean value = (status == IntrusionDevicesProperty.FAIL) ? true : false;
		return value;
	}
	
	
	/**
	 * returns if battery status of device allows group to be armed
	 * methods is taking into account battery status, security standards, bypass settings and zone configuration to calculate this value
	 * @return true if battery status allows arm
	 */
	public boolean isBatteryStatusAllowsArm() {
		return true;
	}
	
	/**
	 * sets battery status
	 * 
	 * @param isBatteryTrouble true for trouble
	 */
	public void setBatteryTrouble(Boolean isBatteryTrouble) {

		Integer status = isBatteryTrouble == true ? IntrusionDevicesProperty.FAIL : IntrusionDevicesProperty.OK;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_BATTERY_STATUS.toString(),
				TroubleType.STATUS.toLowerCase(), status);

	}

	/**
	 * checks tamper status
	 * 
	 * @return true if it is tamper trouble
	 */
	public boolean isTamperTrouble() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_TAMPER_STATUS.toString(),
				TroubleType.STATUS.toLowerCase());

		boolean value = (status == IntrusionDevicesProperty.FAIL) ? true : false;
		return value;
	}

	/**
	 * returns if tamper status of device allows group to be armed
	 * methods is taking into account tamper status, bypass settings and zone configuration to calculate this value
	 * @return true if tamper status allows arm
	 */
	public boolean isTamperStatusAllowsArm() {

		if(zoneConfiguration == null || getZoneConfigurationEnum().isNotUsedZoneType())
			return true; // not configured at all or device not used

		if(bypassState.isIgnoreTroubles()) { 
			return true; // device tamper is bypassed
		}

		return (!isTamperTrouble());
	}

	/**
	 * sets tamper status
	 * 
	 * @param isTamperTrouble true for trouble
	 */
	public void setTamperTrouble(Boolean isTamperTrouble) {

		Integer status = isTamperTrouble == true ? IntrusionDevicesProperty.FAIL : IntrusionDevicesProperty.OK;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_TAMPER_STATUS.toString(),
				TroubleType.STATUS.toLowerCase(), status);

	}
	
	
	/**
	 * checks arm state a.k.a. to report or not
	 * 
	 * @return true if armed
	 */
	public boolean isArmed() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_DO_REPORT.toString(),
				TroubleType.STATUS.toLowerCase());

		boolean value = (status == IntrusionDevicesProperty.ON) ? true : false;
		return value;

	}

	/**
	 * sets arm state a.k.a. to report or not
	 * 
	 * @param isArmed true if armed
	 */
	public void setArmed(Boolean isArmed) {

		Integer status = isArmed == true ? IntrusionDevicesProperty.ON : IntrusionDevicesProperty.OFF;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_DO_REPORT.toString(),
				TroubleType.STATUS.toLowerCase(), status);

	}

	
	/**
	 * get wireless device specific properties
	 * 
	 * @return WirelessIntrusionDeviceProperties of this device
	 */
	public WirelessIntrusionDeviceProperties getWirelessIntrusionDeviceProperties() {
		return wirelessIntrusionDeviceProperties;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer(230);
		toStringBuilder(buffer).append(",type=").append(this.getDeviceType()).append("}");
		return buffer.toString();
	}

	/**
	 * returns the intrusion device classes package name used when recreating object
	 * from DB, so all Intrusion Device based classes have to be deployed in this
	 * package
	 * 
	 * @return intrusion device package name
	 */
	public static String getIntrusionDevicePackageName() {
		return IntrusionDevice.class.getPackage().getName();
	}

	/**
	 * Sets entry delay in milliseconds.
	 * If new value does not meet configuration constraints it is not going to be changed.
	 * Zero is a valid value which shows that this configuration will not be taken into
	 * account, a default configuration value will be read from DB.
	 * @param entryDelay Long containing entry delay in milliseconds
	 * @return true if the value meets all constraints, otherwise false.
	 */
	public boolean setEntryDelay(Long entryDelay) {
		boolean result = true;

		// 0 is a valid value, so don`t validate it
		if(entryDelay.compareTo(new Long(0L)) != 0) {
			Configuration configuration = Activator.getConfigurationObject(Configurations.ENTRY_DELAY);
			if(configuration != null && configuration.getConstraints() != null) {
				result = configuration.getConstraints().validate(entryDelay);
			}
		}

		if(result) {
			this.entryDelay = entryDelay;
			registerForUpdate();
		} else {
			logger.warn("Entered value does not meet constraint validation");
		}

		return result;
	}

	/**
	 * Returns entry delay in milliseconds.
	 * Note: If entry delay is not set, default value from DB will be returned.
	 * @return Long containing entry delay in milliseconds.
	 */
	public Long getEntryDelay() {
		return entryDelay.compareTo(new Long(0L)) > 0 ? entryDelay : (Long) Activator.getConfiguration(Configurations.ENTRY_DELAY);
	}

	/**
	 * Sets exit delay in milliseconds.
	 * If new value does not meet configuration constraints it is not going to be changed.
	 * Zero is a valid value which shows that this configuration will not be taken into
	 * account, a default configuration value will be read from DB.
	 * @param exitDelay Long containing exit delay in milliseconds
	 * @return true if the value meets all constraints, otherwise false.
	 */
	public boolean setExitDelay(Long exitDelay) {
		boolean result = true;

		// 0 is a valid value, so don`t validate it
		if(exitDelay.compareTo(new Long(0L)) != 0) {
			Configuration configuration = Activator.getConfigurationObject(Configurations.EXIT_DELAY);
			if(configuration != null && configuration.getConstraints() != null) {
				result = configuration.getConstraints().validate(exitDelay);
			}
		}

		if(result) {
			this.exitDelay = exitDelay;
			registerForUpdate();
		} else {
			logger.warn("Entered value does not meet constraint validation");
		}
		return result;
	}

	/**
	 * Returns exit delay in milliseconds.
	 * Note: If exit delay is not set, default value from DB will be returned.
	 * @return Long containing exit delay in milliseconds.
	 */
	public Long getExitDelay() {
		return exitDelay.compareTo(new Long(0L)) > 0 ? exitDelay : (Long) Activator.getConfiguration(Configurations.EXIT_DELAY);
	}

	/**
	 * Add default Intrusion device name. Value is taken from {@code Configurations.INTRUSION_DEVICE_DEFAULT_NAME}
	 */
	public void addDefaultName() {
		String defaultDeviceName = (String) Activator.getConfiguration(Configurations.INTRUSION_DEVICE_DEFAULT_NAME);
		setName(String.format(defaultDeviceName, getUID()));
	}

	
	public int getDeviceOpenStatusCount() {
		return deviceOpenStatusCount;
	}

	
	/**
	 * Resets the number of open statuses
	 */
	public void resetDeviceOpenStatusCount() {
		deviceOpenStatusCount = 0;		
	}
	
	
	/**
	 * Increases the number of open statuses with one
	 */
	public void increaseDeviceOpenStatusCount() {
		deviceOpenStatusCount++;		
	}
}
