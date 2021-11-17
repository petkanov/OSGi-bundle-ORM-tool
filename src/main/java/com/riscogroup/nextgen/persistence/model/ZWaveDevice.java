package com.riscogroup.nextgen.home.api.device.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.db.persistence.configurations.Configurations;
import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.events.systementities.DeviceStatusChangedEvent;
import com.riscogroup.nextgen.home.api.function.type.ZWaveDeviceFunction;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProtocol;
import com.riscogroup.nextgen.home.api.generic.DeviceStatus;

public class ZWaveDevice extends Device {
	// ALL methods that has to be persisted MUST start with "set" prefix. Ex. setBatteryOperated

	public static final String NEWLY_ADDED_NODE = "newly.added.node";
	/** Zwave device property name which indicates the HOME ID of the network. */
	public static final String HOME_ID = "home.id";
	/** Indicates that the device is a portable controller. */
	public static final String DEVICE_TYPE_CONTROLLER = "0x1";
	/** Indicates that the device is a static controller. */
	public static final String DEVICE_TYPE_STATIC_CONTROLLER = "0x2";
	/** Indicates that the device is a slave. */
	public static final String DEVICE_TYPE_SLAVE = "0x3";
	/** Indicates that the device is a routing slave. */
	public static final String DEVICE_TYPE_ROUTING_SLAVE = "0x4";
	/** Indicates that the device is a virtual device. */
	public static final String DEVICE_TYPE_VIRTUAL = "0x5";

	private static Integer controllerID = 1;

	private HashMap<String, Object> mapCmdClassIdToInstance;
	private static Logger logger = LoggerFactory.getLogger(ZWaveDevice.class);
	private List<Integer> lifelineGroups;
	private String[] commandClasses;
	private Boolean commandClassesExist = false;
	private Boolean batteryOperated = false;
	private String roleType;
	private Integer wakeUpInterval;
	private Boolean listening = false;
	private Integer endPoints = 0;
	private Integer manufacturerId;
	private Integer productTypeId;
	private Integer productId;
	private ZWaveControllerType ctrlType; // It must be null in order interrogation process to set the correct
													// value true OR false
	private String description;
	private String basicDeviceClass;
	private String genericDeviceClass;
	private String specificDeviceClass;
	private int dilAttempts;
	private int currentNumberProcessedCommandClasses;
	private int totalNumberCommandClasses; // Number of the device (NIF) command classes supported by the zwave
												// protocol adapter
	private Boolean mcEndPointsDone = false;
	private Boolean simpleAssociation = false;

	public ZWaveDevice(Integer id) {
		super(id);
		setProtocolType(DeviceProtocol.ZWAVE.getType());
		mapCmdClassIdToInstance = new HashMap<String, Object>();		
		addDefaultName();
	}
	
	public ZWaveDevice() {
		setProtocolType(DeviceProtocol.ZWAVE.getType());
		mapCmdClassIdToInstance = new HashMap<String, Object>();		
		addDefaultName();
	}
	
	/**
	 * Returns ZWave USB Controller node id
	 * @param controllerID
	 */
	public static int getControllerID() {
		return controllerID;
	}

	/**
	 * Sets ZWave USB Controller node id
	 * @param controllerID - node id
	 */
	public static void setControllerID(Integer controllerID) {
		ZWaveDevice.controllerID = controllerID;
		logger.debug("This device is the ZWave Controller with protocol ID {}", controllerID);
	}

	/**
	 * Checks if the device is the main controller.
	 * @return true if the device is the controller otherwise false
	 */
	public boolean isMainController() {
		logger.debug("protocolID: {}, controllerId: {}", protocolID, ZWaveDevice.getControllerID());
		return protocolID.equals(ZWaveDevice.getControllerID()); // && !isSecondaryController
	}

	public Boolean isPrimaryController() {
		return ctrlType != null && ctrlType.getValue() == ZWaveControllerType.PRIMARY.getValue();
	}

	public Boolean isSecondaryController() {
		return ctrlType != null && ctrlType.getValue() == ZWaveControllerType.SECONDARY.getValue();
	}

	public Boolean isInclusionController() {
		return ctrlType != null && ctrlType.getValue() == ZWaveControllerType.INCLUSION.getValue();
	}

	public ZWaveControllerType getControllerType() {
		return ctrlType;
	}

	public void setControllerType(ZWaveControllerType ctrlType) {
		this.ctrlType = ctrlType;
		setControllerType(ctrlType.getValue());
	}

	public void setControllerType(Integer ctrlType) {
		logger.debug("Device ID {} set controller type {}", this.protocolID, ctrlType);
		this.ctrlType = ZWaveControllerType.valueOf(ctrlType);
		registerForUpdate();
	}

	/**
	 * Checks if the device is a ZWavePlus.
	 *
	 * @return true if the device is ZWavePlus otherwise false
	 */
	public boolean isZWavePlus() {
		return getCommandClass(94) != null;
	}

	/**
	 * Checks if the device supports MultiChannel.
	 *
	 * @return true if the device supports MultiChannel otherwise false
	 */
	public boolean isMultiChannel() {
		return getCommandClass(96) != null;
	}

	@Override
	public void setStatus(DeviceStatus status) {
		logger.debug("ZWave Device with UID {} has status {} ", getUID(), status);
		if (this.status == null || this.status.equals(status)) {
			// We do not send an event if the status didn't change or if it is the first
			// time we set the status
			this.status = status;
		} else {
			this.status = status;
			// We do not send an event for status changed on the main controller
			if (!isMainController()) {
				new DeviceStatusChangedEvent(this).postEvent();
			}
		}
		logger.debug("ZWave device children {}", getChildren());
		List<Device> children = getChildren();
		for (Device child : children) {
			((ZwaveMultiChannelDevice) child).setStatus(this.status);
		}
	}

	/**
	 * Return a DeviceFunction by given command class id
	 * 
	 * @param commandClassID
	 * @return DeviceFunction
	 */
	public ZWaveDeviceFunction getFunctionByCommandClassID(String commandClassID) {
		logger.trace("(commandClassID: {})", commandClassID);
		return getFunctionByCommandClassID(commandClassID, 0);
	}

	/**
	 * Return a DeviceFunction by given command class id and end point
	 * 
	 * @param commandClassID
	 * @return DeviceFunction
	 */
	public ZWaveDeviceFunction getFunctionByCommandClassID(String commandClassID, int endPoint) {
		logger.trace("(commandClassID: {} endPoint: {})", commandClassID, endPoint);
		ZWaveDeviceFunction deviceFunction = null;
		List<DeviceFunction> functions = getFunctions();
		if (functions != null) {
			for (Iterator<DeviceFunction> it = functions.iterator(); it.hasNext();) {
				ZWaveDeviceFunction function = (ZWaveDeviceFunction) it.next();
				if (function != null && function.getCommandClassId().equals(commandClassID)
						&& function.getEndPointId() == endPoint) {
					deviceFunction = function;
					break;
				}
			}
		}
		return deviceFunction;
	}

	/**
	 * Return a DeviceFunction by given UID that is taken from the database
	 */
	public DeviceFunction getFunctionByUID(int uid) {
		logger.trace("(uid: {})", uid);
		return super.getFunctionByUID(uid);
	}

	/**
	 * If the ZWaveDevice supports MultiChannel command class every end point is
	 * represents as a separate child ZwaveMultiChannelDevice. This method is
	 * invoked to get a child by given end point.
	 * 
	 * @param endPoint
	 * @return ZwaveMultiChannelDevice
	 */
	public ZwaveMultiChannelDevice getChildrenByEndPoint(int endPoint) {
		ZwaveMultiChannelDevice child = null;
		logger.debug("Looking for device with endPoint {}", endPoint);
		List<Device> children = getChildren();
		for (Device cd : children) {
			if (((ZwaveMultiChannelDevice) cd).getEndPoint().equals(endPoint)) {
				logger.debug("Child device with UID {} was found!", cd.getUID());
				child = (ZwaveMultiChannelDevice) cd;
				break;
			}
		}
		return child;
	}

	public void setMultiChannelEndPointsDone(Boolean mcEndPointsDone) {
		this.mcEndPointsDone = mcEndPointsDone;
		registerForUpdate();
	}

	public boolean isMultiChannelEndPointsDone() {
		return mcEndPointsDone;
	}

	public ArrayList<Integer> getLifelineGroup() {
		if (isZWavePlus()) {
			lifelineGroups = new ArrayList<Integer>(1);
			lifelineGroups.add(1);
			return (ArrayList<Integer>) lifelineGroups;
		} else {
			return (ArrayList<Integer>) lifelineGroups;
		}
	}

	public void setLifelineGroup(List<Integer> lifelineGroup) {
		this.lifelineGroups = lifelineGroup;
		registerForUpdate();
	}

	public void addCommandClass(String ccId, Object cc) {
		logger.debug("Cached command class id {} for device {}", ccId, getUID());
		mapCmdClassIdToInstance.put(ccId, cc);
	}

	public Object getCommandClass(int ccId) {
		return getCommandClass(ccId + "");
	}

	public Object getCommandClass(String ccId) {
		logger.debug("Obtained from cache {} command class id {} for device {}", mapCmdClassIdToInstance, ccId,
				getUID());
		return (Object) mapCmdClassIdToInstance.get(ccId);
	}

	/** Indicates that the device has the BATTERY command class. */
	public Boolean isBatteryOperated() {
		return batteryOperated;
	}

	public Boolean setBatteryOperated(Boolean batteryOperated) {
		this.batteryOperated = batteryOperated;
		logger.debug("Device {} is battery operated {}", getUID(), batteryOperated);
		registerForUpdate();
		return batteryOperated;
	}

	/**
	 * Sets wake up interval for the device
	 * 
	 * @param wakeUpInterval
	 */
	public void setWakeUpInterval(Integer wakeUpInterval) {
		this.wakeUpInterval = wakeUpInterval;
		registerForUpdate();
	}

	/**
	 * Returns wake up interval.
	 * 
	 * @return
	 */
	public Integer getWakeUpInterval() {
		return wakeUpInterval;
	}

	/** Zwave device property name which indicates the type of this ZWave device. */
	public void setRoleType(String roleType) {
		this.roleType = roleType;
		registerForUpdate();
	}

	public String getRoleType() {
		return roleType;
	}

	/** Indicates that the device is listening. */
	public void setListening(Boolean listening) {
		this.listening = listening;
		registerForUpdate();
	}

	public boolean isListening() {
		return listening;
	}

	/**
	 * Checks if the device is a F.L.I.R.S.
	 *
	 * @return true if the device is F.L.I.R.S otherwise false.
	 */
	public boolean isFLIRS() {
		return isListening() && isBatteryOperated();
	}

	public void setEndPoints(Integer endPoints) {
		this.endPoints = endPoints;
		registerForUpdate();
	}

	public Integer getEndPoints() {
		return endPoints;
	}

	public String[] getCommandClasses() {
		return commandClasses;
	}

	public void setCommandClasses(String[] commandClasses) {
		this.commandClasses = commandClasses;
		setCommandClassesExist(commandClasses != null && commandClasses.length > 0 ? true : false);
		registerForUpdate();
	}

	public void setCommandClassesExist(Boolean commandClassesExist) {
		this.commandClassesExist = commandClassesExist;
		registerForUpdate();
	}

	public Boolean getCommandClassesExist() {
		return commandClassesExist;
	}

	public void setManufacturerId(Integer manufacturerId) {
		this.manufacturerId = manufacturerId;
		registerForUpdate();
	}

	public Integer getManufacturerId() {
		return manufacturerId;
	}

	public void setProductTypeId(Integer productTypeId) {
		this.productTypeId = productTypeId;
		registerForUpdate();
	}

	public Integer getProductTypeId() {
		return productTypeId;
	}

	public void setProductId(Integer productId) {
		this.productId = productId;
		registerForUpdate();
	}

	public Integer getProductId() {
		return productId;
	}

	public void setDescription(String description) {
		this.description = description;
		registerForUpdate();
		for (Device child : getChildren()) {
			((ZWaveDevice) child).setDescription(description);
		}
	}

	public String getDescription() {
		return description;
	}

	public void setBasicDeviceClass(String basicDeviceClass) {
		this.basicDeviceClass = basicDeviceClass;
		registerForUpdate();
	}

	public String getBasicDeviceClass() {
		return basicDeviceClass;
	}

	public void setGenericDeviceClass(String genericDeviceClass) {
		this.genericDeviceClass = genericDeviceClass;
		registerForUpdate();
	}

	public String getGenericDeviceClass() {
		return genericDeviceClass;
	}

	public void setSpecificDeviceClass(String specificDeviceClass) {
		this.specificDeviceClass = specificDeviceClass;
		registerForUpdate();
	}

	public String getSpecificDeviceClass() {
		return specificDeviceClass;
	}

	// In case the Root device is return to the UI (at that moment we still don't
	// know if it is a MultiChannel device or not)
	// 1. Setting a name to the Root device and children exist.
	// 2. Setting a name to the Root device but children still missing, it is handle
	// by MutliChannel CC in the zwave adapter.
	public void renameDevice(String name) {
		super.setName(name);
		postDeviceConfigurationChangedEvent();
		List<Device> childs = getChildren();
		if (!childs.isEmpty()) {
			ZWaveDevice deviceEndPoint = getChildrenByEndPoint(1);
			if (deviceEndPoint != null) {
				deviceEndPoint.renameDevice(name);
			}
		}
	}

	public void setDILAttempts(int dilAttempts) {
		logger.info("{} retry attempt {}", this.toString(), dilAttempts);
		this.dilAttempts = dilAttempts;
	}

	public int getDILAttempts() {
		return dilAttempts;
	}

	public void setSimpleAssociationDone(Boolean simpleAssociation) {
		this.simpleAssociation = simpleAssociation;
		registerForUpdate();
	}

	public boolean isSimpleChannelAssociationDone() {
		return simpleAssociation;
	}

	/**
	 * Add default SmartHome device name. Value is taken from
	 * {@code Configurations.SMART_HOME_DEVICE_DEFAULT_NAME}
	 */
	private void addDefaultName() {
		String defaultDeviceName = (String) Activator.getConfiguration(Configurations.SMART_HOME_DEVICE_DEFAULT_NAME);
		setName(String.format(defaultDeviceName, getUID()));
	}

	/**
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer(230);
		toStringBuilder(buffer).append(",ZWavePlus=").append(isZWavePlus()).append(",MainController=")
				.append(isMainController()).append(",PrimaryController=").append(isPrimaryController())
				.append(",SecondaryController=").append(isSecondaryController()).append(",Battery operated=")
				.append(isBatteryOperated()).append(",End points=").append(getEndPoints()).append("}");
		return buffer.toString();
	}

	public void setTotalNumberCommandClasses(int totalNumberCommandClasses) {
		if (this instanceof ZwaveMultiChannelDevice) {
			((ZWaveDevice) getParentDevice()).setTotalNumberCommandClasses(totalNumberCommandClasses);
		} else {
			this.totalNumberCommandClasses = totalNumberCommandClasses;
		}
	}

	public void setInstallationStatus(String ccName) {
		if (this instanceof ZwaveMultiChannelDevice) {
			logger.debug("MC device command class", ccName);
			((ZWaveDevice) getParentDevice()).setInstallationStatus(ccName);
		} else {
			if (totalNumberCommandClasses != 0) { // on restart value should be 0 and calculation will be skipped.
				logger.debug("Command class {} is processed", ccName);
				currentNumberProcessedCommandClasses++;
				int currentProgress = 0;
				if (currentNumberProcessedCommandClasses > totalNumberCommandClasses) {
					currentProgress = 100;
				} else {
					currentProgress = currentNumberProcessedCommandClasses * 100 / totalNumberCommandClasses;
				}
				super.setInstallationStatus(currentProgress);
			}
		}
	}
}
