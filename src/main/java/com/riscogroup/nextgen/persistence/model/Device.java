package com.riscogroup.nextgen.persistence.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.device.type.ZWaveDevice;
import com.riscogroup.nextgen.home.api.services.DataService;

/**
 * The Device class abstracts the physical device. It provides common info
 * related to home device as UID, name, status, vendor, version and etc. It
 * contains information about capabilities of the device represents as a <code>DeviceFunction</code> object and
 * information about command classes
 * represents as a <code>DevceProperty</code> object
 */
public abstract class Device {
	private static Logger logger = LoggerFactory.getLogger(Device.class);
	
	public static final int MAX_NUMBER = 10*1000; // max number of devices
	public static final String PROTOCOL_NAME = "protocol.name";
	public static final String HARD_RESET_EVENT = "com/riscogroup/nextgen/CONTROLLER_MODE_HARD_RESET";
	public static final String DEVICE_UID = "uID";
	private static final int LAST_PACKETS_STATUS_NUMBER = 10;	
	public static final int RECALL_BYPASS_STATE = 9; //This state recall the last selected bypassed zones.
	
	private List<DeviceFunction> functions;
	private List<DeviceFunction> functionsEndUser;
	private List<Device> children;
	private String name;
	private String version;
	private String vendor;
	private String type;

	private ProtocolAdapter adapter;
	private Device parentDevice;
	protected DeviceStatus status;
	protected Integer protocolID;
	protected List<MessageInformation> lastPacketsSent;
	protected Integer zoneConfiguration = ZoneConfiguration.ZONE_TYPE_UNDEFINED.getValue();

	protected Boolean commonZone = false;
	protected Boolean exitOpen = false;
	protected Boolean noMotion = false;
	
	private Integer uID;
	
	private int installationStatus;

	/*
	 * In case that the system is armed in Stay/Home/Partial arming, the events from zones which defined as 'internal'
	 * are ignored.
	 */
	protected Boolean internal = false;
	
	/**
	 * Indicates the bypass status of the device. i.e. whether an event from this device will be ignored (bypassed) or not 
	 */
	protected BypassState bypassState = BypassState.DISABLED;
	
	/**
	 *   Keeps the last saved bypass state of the device. It's being used when device bypass state is set to {@code Device#RECALL_BYPASS_STATE}
	 */
	protected BypassState lastBypassState;
	
	/**
	 * the ID of the user set / changed bypass status of the device
	 */
	private Integer userChangedBypassStatus;
	  
	// flag if force arm is enabled or disabled on the device, default: disabled
	private Boolean forceArm = false;
	
	/**
	 * the time stamp of the last panic alarm sent
	 */
	private Long lastPanicAlarmTime = 0L;
	
	/**
	 * the time stamp of the last medical alarm sent
	 */
	private Long lastMedicalAlarmTime = 0L;
	
	private DataService dataService;

	public Device(Integer uID) {
		this();
		this.uID = uID;
	}

	public Device() {
		lastPacketsSent = new ArrayList<>(LAST_PACKETS_STATUS_NUMBER);
		functions = new CopyOnWriteArrayList<>();
		children = new CopyOnWriteArrayList<>();
		functionsEndUser = new ArrayList<>();
		type = DeviceProtocol.GENERIC.getType();
		name = this.getClass().getSimpleName();
	}

	public Integer getUID() {
		return this.uID;
	}
	
	public void setUID(Integer uID) {
		this.uID = uID;
	}
	
	/**
	 * If the device is a sub device (child) of another. it should return the 
	 * ID of it's sub device within the parent device. Parent devices should
	 * return 0.
	 * @return - the ID of the sub-device within the parent device.
	 */
	public Integer getSubDeviceID() {
		return 0;
	}

	/**
	 * Returns "true" if Common Zone is enabled or
	 * "false" if it is disabled.
	 * 
	 * @return Boolean
	 */
	public Boolean isCommonZoneEnabled() {
		return commonZone;
	}

	/**
	 * Setter for Common Zone.
	 * Set "true" to enable common zone or "false" to disable it.
	 * 
	 * @param commonZone
	 *            Boolean containing state of Common Zone.
	 */
	public void setCommonZone(Boolean commonZone) {
		if(!this.commonZone.equals(commonZone)) {
			this.commonZone = commonZone;
			registerForUpdate();
		}
	}

	/**
	 * Returns the state of Common Zone
	 * "true" if Common Zone is enabled or
	 * "false" if it is disabled.
	 * 
	 * @return Boolean
	 */
	public Boolean getCommonZone() {
		return commonZone;
	}

	/**
	 * Sets exit open flag used for an exit/entry door to prevent system from being not ready when this zone is
	 * violated.
	 * If the zone is faulted when the system is being armed, it does not prevent arming.
	 * 
	 * @param exitOpen
	 *            Boolean showing exit open flag.
	 */
	public void setExitOpen(Boolean exitOpen) {
		if(!this.exitOpen.equals(exitOpen)) {
			this.exitOpen = exitOpen;
			registerForUpdate();
		}
	}

	/**
	 * Returns exit open flag.
	 * @return Returns true if exit open is active, otherwise it returns false.
	 * @see {@link #setExitOpen(Boolean exitOpen)}
	 */
	public Boolean getExitOpen() {
		return exitOpen;
	}

	/**
	 * sets force arm flag to allow automatic bypassing of fault zones when arming the partition
	 * @param forceArm - true to activate force arm flag
	 */
	public void setForceArm(Boolean forceArm) {
		if(!this.forceArm.equals(forceArm)) {
			this.forceArm = forceArm;
			registerForUpdate();
		}
	}
	
	/**
	 * returns force arm flag.
	 * 
	 * @return returns true if force arm is active, otherwise it returns false.
	 * @see {@link #setForceArm(Boolean forceArm)}
	 */
	public Boolean getForceArm() {
		return forceArm;
	}
	
	/**
	 * Gets no motion flag of the zone.
	 * @return true if the zone participates in the No Motion function
	 * @see {@link #setNoMotion(Boolean noMotion)}
	 */
	public Boolean getNoMotion() {
		return noMotion;
	}

	/**
	 * Sets no motion flag to the zone.
	 * Zone flag which determines whether the zone participates in the No Motion function.
	 * The No Motion function is for reception of signals used to monitor the activity of sick, elderly or disabled people.
	 * @param noMotion true for the zone participates in the No Motion function
	 */
	public void setNoMotion(Boolean noMotion) {
		logger.trace("({})", noMotion);
		if(!this.noMotion.equals(noMotion)) {
			this.noMotion = noMotion;
			registerForUpdate();
		}
	}

	/**
	 * Adds the received message information to the list of statuses.
	 * if the list is full (equals LAST_PACKETS_STATUS_NUMBER),
	 * the first status will be removed and the given status will be added (FIFO).
	 * 
	 * @param status
	 *            - the status to add to the last statuses.
	 */
	public void setConnectionToDeviceStatus(MessageInformation status) {
		logger.trace("({})", status);
		while (lastPacketsSent.size() >= LAST_PACKETS_STATUS_NUMBER) {
			lastPacketsSent.remove(0);
		}
		if (!lastPacketsSent.add(status)) {
			logger.warn("Could not add the last connection status to device {}", getUID());
		}
	}

	public MessageInformation getLastMessageInformation() {
		MessageInformation messageInformation = null;
		if (lastPacketsSent != null && lastPacketsSent.size() > 0) {
			messageInformation = lastPacketsSent.get(lastPacketsSent.size() - 1);
		}
		return messageInformation;
	}

	/**
	 * posts event when device configuration has been changed 
	 */
	public void postDeviceConfigurationChangedEvent() {		
		new DeviceConfigurationChangedEvent(this).postEvent();
	}

	/**
	 * Sets a vendor for this device
	 * 
	 * @param vendor
	 */
	public void setVendor(String vendor) {
		logger.debug("Device vendor {}", vendor);
		this.vendor = vendor;
		registerForUpdate();
		if (getChildren() != null) {
			for (Device child : getChildren()) {
				child.setVendor(vendor);
			}
		}
	}

	/**
	 * Sets a version for this <code>Device</code>
	 * 
	 * @param vendor
	 */
	public void setVersion(String version) {
		logger.trace("({})", version);
		this.version = version;
		registerForUpdate();
	}
	
	/**
	 * This method returns the user-friendly name of this <code>Device</code> instance.
	 * 
	 * @return the last known value for the device name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets a name for this <code>Device</code>
	 * NOTE: if the name length is longer then the maximum allowed, only part of the name will be set
	 * (the first <max_length> characters will be set).
	 * 
	 * @param newName - the new device name to set.
	 */
	public void setName(String newName) {
		logger.trace("newName:{}", newName);
		if (newName == null) {
			logger.warn("Null value in Set Device Name Method");
			return;
		}
		
		/* The reason that this logic is here and not in the business logic services is
		 * that Device is created by many different entities (adapters) and not by the 
		 * internal or external APIs. To avoid logic duplication, we put it here.
		 */
		final Integer nameLengthConfig = Activator.getConfiguration(Configurations.DEVICE_NAME_LENGTH);
		final int nameLength = (nameLengthConfig != null ? nameLengthConfig : 40);
		
		if (newName.length() > nameLength) {
			logger.warn("Name is too long. Should be no more than {} symbols", nameLength);
			newName = newName.substring(0, nameLength);
		}
		name = newName;
		registerForUpdate();
	}

	/**
	 * Returns a version for this <code>Device</code>
	 * 
	 * @return the value for the device version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns a vendor for this <code>Device</code>
	 * 
	 * @return the value for the device vendor
	 */
	public String getVendor() {
		return vendor;
	}

	/**
	 * This method returns the status of this <code>Device</code>.
	 * @see {@link DeviceStatus}
	 * @return DeviceStatus object representing device state.
	 */
	public DeviceStatus getStatus() {
		return status;
	}

	/**
	 * Sets changed status to the <code>Device</code>
	 * @see {@link DeviceStatus}
	 * @param status - DeviceStatus object representing device state.
	 */
	public void setStatus(DeviceStatus status) {
		logger.debug("Device with protocol id {} has status {} ", protocolID, status);
		if (this.status == null || this.status.equals(status)) {
			// We do not send an event if the status didn't change or if it is the first time we set the status
			this.status = status;
		}
		else {
			this.status = status;
			/* Send Risco event */
			new DeviceStatusChangedEvent(this).postEvent();
		}
	}

	/**
	 * Returns a list contains capabilities (functions) of a <code>Device</code> with some meaning for the End User
	 * @return List<DeviceFunction>
	 */
	public List<DeviceFunction> getFunctionsEndUser() {
		functionsEndUser.clear();
		if (functions != null) {
			for (DeviceFunction function : functions) {
				if (function != null && !function.getSkip()) {
					functionsEndUser.add(function);
				}
			}
		}
		return functionsEndUser;
	}

	/**
	 * Returns a list contains all capabilities (functions) of a <code>Device</code>
	 * @return List<DeviceFunction>
	 */
	public List<DeviceFunction> getFunctions() {
		return Collections.unmodifiableList(functions);
	}
	
	public void removeFunctions() {
		this.functions.clear();
	}

	/**
	 * Removes a DeviceFunction from the list of a <code>Device</code>
	 * @return List<DeviceFunction>
	 */
	public boolean removeFunction(DeviceFunction deviceFunction) {
		return functions.remove(deviceFunction);
	}

	/**
	 * Adds a new collection of functions to the exist collection of functions
	 * @param function
	 */
	public void addFunctions(List<DeviceFunction> functions) {
		this.functions.addAll(functions);
	}

	/**
	 * Adds a new function to the exist collection of functions
	 * @param function
	 */
	public void addFunction(DeviceFunction function) {
		functions.add(function);
		logger.debug("Function size {}", functions.size());
	}

	/**
	 * Returns a single capability (function) of a <code>Device</code>
	 * @param ccName command class name
	 * @return DeviceFunction function id
	 */
	public DeviceFunction getFunction(String cmdName) {
		return getFunction(cmdName, 0);
	}

	/**
	 * Returns a single capability (function) of a <code>Device</code>
	 * @param ccName command class name
	 * @param endPoint
	 * @return DeviceFunction function id
	 */
	public DeviceFunction getFunction(String cmdName, int endPoint) {
		DeviceFunction devFunc = null;
		logger.debug("Looking for function {} with endPoint {}", cmdName, endPoint);
		if (functions != null) {
			for (DeviceFunction function : functions) {
				if (function.getCommandName() != null && function.getCommandName().equals(cmdName) && function.getEndPointId() == endPoint) {
					logger.debug("function found {} endPoint {}", cmdName, endPoint);
					devFunc = function;
					break;
				}
			}
		}
		return devFunc;
	}

	public void setProtocolAdapter(ProtocolAdapter adapter) {
		this.adapter = adapter;
	}

	public ProtocolAdapter getProtocolAdapter() {
		return adapter;
	}

	public void setProtocolType(String type) {
		this.type = type;
		registerForUpdate();
	}

	public String getProtocolType() {
		return type;
	}

	public void setProtocolID(Integer id) {
		this.protocolID = id;
		registerForUpdate();
	}

	public Integer getProtocolID() {
		return protocolID;
	}

	/**
	 * @return zoneConfigurationID from {@link ZoneConfiguration}
	 */
	public Integer getZoneConfiguration() {
		return zoneConfiguration;
	}

	/**
	 * @return {@link ZoneConfiguration} of {@code Device#zoneConfiguration} property}
	 */
	public ZoneConfiguration getZoneConfigurationEnum() {
		if(zoneConfiguration == null)
			return null;
		return ZoneConfiguration.valueOf(zoneConfiguration);
	}

	/**
	 * set zoneID, it must be valid integer from {@link ZoneConfiguration}
	 * @param zoneId
	 */
	public void setZoneConfiguration(Integer zoneConfiguration) {
		this.zoneConfiguration = zoneConfiguration;
		registerForUpdate();
	}

	/**
	 * @return true if device is set as internal
	 */
	public Boolean isInternal() {
		return internal;
	}

	/**
	 * Set internal flag of device
	 * 
	 * @param internal
	 */
	public void setInternal(Boolean internal) {
		if(!this.internal.equals(internal)) {
			this.internal = internal;
			registerForUpdate();
		}
	}
	
	/**
	 * Get bypass state of the device
	 * @see BypassState
	 * @return bypassState
	 */
	public BypassState getBypassState() {
		return bypassState;
	}

	/**
	 * set bypassStateID for current Device for enum {@link BypassState}.  It must be valid integer of BypassState enum. 
	 * BypassEvent is being send to MS when device is bypassed. 
	 * If bypassCommandId = 9 it execute recall last bypass state command. 
	 * @see BypassState
	 * @param bypassCommandId
	 */
	public void setBypassState(Integer bypassCommandId) {
		BypassState bypassStateTmp = BypassState.valueOf(bypassCommandId);	

		if(bypassStateTmp == null)
			return;
		
		bypassState = bypassStateTmp;
		
		if(bypassCommandId == RECALL_BYPASS_STATE) {
			setDeviceBypassRecall();
		} else {
			registerForUpdate();
		}
	}

	/**
	 * Set last selected bypassed zones state before automatic un-bypass. A copy of bypass state is kept in
	 * {@code Device#lastBypassState} and if bypass state is {@code Device#RECALL_BYPASS_STATE} is loading last bypass state value when system is armed.  
	 */
	private void setDeviceBypassRecall() {
		BypassState lastBypassState = getLastBypassState();
		//if there is no last state set it to default one DISABLED
		if (lastBypassState == null) 
			lastBypassState = BypassState.DISABLED;
		setBypassState(lastBypassState.getValue());
		logger.info("Last bypass state : {} for device id: {} was loaded", lastBypassState, getUID());
	}
	
	/**
	 * Fires an event when device is bypassed. Event is reported when a group of the zone is armed. Bypass EventID depends if 
	 * zone is 24h, is one time bypassing or permanent bypassing. If forced flag is true device bypassStatus is set to {@code BypassState#FORCED}
	 * @param group Group the devices being bypassed belongs to
	 * @param user User that bypasses the device.
	 * @param reportLevel report level, to be logged and reported or just logged.
	 */
	public void sendBypassEvent(Group group, User user, Integer reportLevel) {
		final Integer eventId = getBypassEventIdByDeviceBypassStatus();

		// fire an event when device is bypassed
		if (eventId > 0) {
			new BypassEvent(group, user, this, eventId, IntrusionEvent.EVENT_TRIGGERED, reportLevel).postEvent();	
			logger.debug("Bypassed event with ID: {} is sent", eventId);			
		}
	}
	
	/**
	 * Get appropriate bypass events ID's list according to the device bypass type
	 * Possible values are:
	 *  {@code IntrusionEvent.BYPASS_TEMPORARY} - for one time bypassing
	 *  {@code IntrusionEvent.BYPASS} - for permanent bypassing
	 *  {@code IntrusionEvent.BYPASS_TAMPER} - for 24h zones
	 * @return Bypassed event Id
	 */
	private Integer getBypassEventIdByDeviceBypassStatus() {
		
		Integer eventId = 0;
		
		if (isOneTimeBypassing() || isForceArmBypassing()) {
			eventId = IntrusionEvent.BYPASS_TEMPORARY;
		} else if (bypassState == BypassState.PERMANENT) {
			eventId =  IntrusionEvent.BYPASS;
		} 
		logger.trace("return {}", eventId);
		return eventId;
	}
	
	/**
	 * Get last saved bypass state used when {@code Device#RECALL_BYPASS_STATE} is invoked from client 
	 * @return BypassState
	 */
	public BypassState getLastBypassState() {
		return lastBypassState;
	}

	/**
	 * Set last bypass state. It must be called when arming the system.
	 * @param lastBypassState
	 */
	public void setLastBypassState(BypassState lastBypassState) {
		this.lastBypassState = lastBypassState;
	}

	public Integer getUserChangedBypassStatus() {
		return userChangedBypassStatus;
	}

	public void setUserChangedBypassStatus(Integer userChangedBypassStatus) {
		this.userChangedBypassStatus = userChangedBypassStatus;
		registerForUpdate();
	}

	/**
	 * Returns the child devices of this <code>Device</code> object
	 * or <code>null</code> if the current <code>Device</code> doesn't have
	 * any children.
	 *
	 * @return the child devices or <code>null</code> if this <code>Device</code> doesn't have any children.
	 */
	public List<Device> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	/**
	 * Returns child by given uID
	 * @param uID
	 * @return child object
	 */
	public ZWaveDevice getChildren(Integer uID) {
		ZWaveDevice foundChild = null;
		if (!children.isEmpty()) {
			for (Device child : children) {
				if (child.getUID().equals(uID)) {
					foundChild = (ZWaveDevice) child;
					break;
				}
			}
		}
		return foundChild;
	}
	
	/**
	 * Adds a parent device to this device
	 * 
	 * @param parentDevice
	 */
	public void setParentDevice(Device parentDevice) {
		this.parentDevice = parentDevice;
	}

	/**
	 * Return the parent device of this device
	 * 
	 * @return
	 *  the parent device
	 */
	public Device getParentDevice() {
		return parentDevice;
	}

	/**
	 * Adds a new child device to the exist collection of children
	 * 
	 * @param <Device> as a child
	 */
	public void setChild(Device device) {
		logger.debug("Child with uid {} is added.", device.getUID());
		children.add(device);					
		registerForUpdate();
		device.setParentDevice(this);
	}
	
	public void removeChildren() {
		this.children.clear();
	}
 
	public DeviceFunction getFunctionByUID(int uid) {
		logger.trace("(uid: {})", uid);
		DeviceFunction deviceFunction = null;
		List<DeviceFunction> functions = getFunctions();
		if (functions != null) {
			for (Iterator<DeviceFunction> it = functions.iterator(); it.hasNext();) {
				DeviceFunction function = it.next();
				if (function != null && function.getUID().equals(uid)) {
					deviceFunction = function;
					break;
				}
			}
		}
		return deviceFunction;
	}
	
	public void setInstallationStatus(int installationStatus) {
		this.installationStatus = installationStatus;
		logger.debug("Current device installation progress status is {}%", installationStatus);
	}

	public Integer getInstallationStatus() {
		return installationStatus;
	}
	
	/**
	 * Check if bypass state is FORCED 
	 * @return true if it's one force arm bypassing otherwise false
	 */
	public boolean isForceArmBypassing() {
		BypassState bypassState = getBypassState();
		if (bypassState == null) return false;
		return  bypassState == BypassState.FORCED;
	}

	/**
	 * Check if bypass state is ONE_TIME or ONE_TIME_24H 
	 * @return true if it's one time bypassing otherwise false
	 */
	public boolean isOneTimeBypassing() {
		BypassState bypassState = getBypassState();
		if (bypassState == null) return false;
		return bypassState == BypassState.ONE_TIME;
	}
	
	/**
	 * Check if bypass state is {@code BypassState#PERMANENT}for Intruder zones and {@code BypassState#PERMANENT_24HT} for 24h zones
	 * @return true if it's permanent bypassing otherwise false
	 */
	public boolean isPermanentBypassing() {
		BypassState bypassState = getBypassState();
		if (bypassState == null) return false;
		return bypassState == BypassState.PERMANENT;
	}
	
	/**
	 * Returns whether this device is used or not.<br>
	 * We consider one device as used if its zone configuration is
	 * everything but ZONE_TYPE_NOT_USED.<br>
	 * @see {@link ZoneConfiguration}
	 * @return true if device is used, otherwise false
	 */
	public boolean isUsed() {
		return getZoneConfigurationEnum() != ZoneConfiguration.ZONE_TYPE_NOT_USED;
	}

	public Long getLastPanicAlarmTime() {
		return lastPanicAlarmTime;
	}

	public void setLastPanicAlarmTime(Long lastPanicAlarmTime) {
		this.lastPanicAlarmTime = lastPanicAlarmTime;
	}

	public Long getLastMedicalAlarmTime() {
		return lastMedicalAlarmTime;
	}

	public void setLastMedicalAlarmTime(Long lastMedicalAlarmTime) {
		this.lastMedicalAlarmTime = lastMedicalAlarmTime;
	}
	
	/**
     * A helper method that can be used in {@link #toString()} implementation.
     * 
     * @param buffer the buffer, where to append the Device UID, protocol ID
     * @return the buffer
     */
	protected StringBuffer toStringBuilder(StringBuffer buffer) {
		return buffer
				.append("Device: {UID=")
				.append(getUID())
				.append(",Protocol ID=")
				.append(protocolID);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Device) ||
		   this.uID == null || ((Device)obj).uID == null) {
			return false;
		}
		return this.uID.equals(((Device)obj).uID);
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
