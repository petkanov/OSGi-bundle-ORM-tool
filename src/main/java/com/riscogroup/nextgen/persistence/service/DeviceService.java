package com.riscogroup.nextgen.home.api.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.riscogroup.nextgen.home.api.device.type.intrusion.SirenDevice;
import com.riscogroup.nextgen.home.api.generic.ActionInfo;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFilter;
import com.riscogroup.nextgen.home.api.generic.DeviceProtocol;
import com.riscogroup.nextgen.home.api.generic.ProtocolType;
import com.riscogroup.nextgen.home.api.generic.StatusResult;
import com.riscogroup.nextgen.home.api.generic.User;

public interface DeviceService {
	/**
	 * Adds a new <code>Device</code> to the system
	 * 
	 * @return new <code>Device</code>
	 * 
	 */
	Device addDevice(ProtocolType type) throws Exception;

	/**
	 * Removes a specified <code>Device</code>.
	 * 
	 * @param uID
	 *            the uID of the <code>Device</code> to be removed.
	 * @param action type
	 */
	void removeDevice(Integer uID, byte action) throws Exception;

	/**
	 * This method returns all <code>Device</code>s.
	 * 
	 * @return all available <code>Device</code>s on the system
	 */
	List<Device> getDevices(DeviceFilter filter) throws Exception;
	
	/**
	 * This method returns List of all <code>Device</code>s for a particular user, based 
	 * on the applied filter.
	 * @param user User that devices belong to
	 * @return all <code>Device</code>s on the system that belong to a certain user
	 */
	List<Device> getDevices(User user, DeviceFilter filter);

	/**
	 * This method returns all <code>Device</code>s from a specific adapter.
	 * 
	 * @param filter Specifies subset of devices
	 * @param adapterType ProtocolType Object that Specifies the adapter those devices belong to. (Intrusion, ZWave ..)
	 * @return List with the selected devices
	 */
	List<Device> getDevices(DeviceFilter filter, ProtocolType adapterType);

	/**
	 * Returns a <code>Device</code> by given uID.
	 * 
	 * @return <code>Device</code>
	 */
	Device getDevice(Integer uID);

	/**
	 * Executes an action over a specific protocol
	 *  
	 * @param action
	 */
	boolean doAction(ActionInfo actionInfo);

	/**
	 * Returns status for specific operation for predefined protocol
	 * 
	 * @param protocol name
	 * @param action type
	 */
	StatusResult getStatus(String protocol, byte action) throws Exception;

	/**
	 * handles bypass state of the devices when they are being changed by users.
	 * method first checks if bypass is allowed, if no - it returns, no further activity
	 * if is allowed: method goes thought the list of devices with changed bypass states and
	 * if they are set to anything different than bypass disabled: triggers a bypass event for each device
	 * additionally it sets the bypass state in the device configuration and triggers event - devices configuration changed 
	 * @param newBypassStateMap HashMap<Integer, Integer> key is deviceUID, value is newDeviceBypassStateID
	 * @param user changed bypass states
	 * @return true if change device bypass state is allowed, otherwise false
	 */
	boolean setBypassState(HashMap<Integer, Integer> newBypassStateMap, User user);

	/**
	 * Gets Control Panel Bell single device, created upon system startup 
	 * and assigned to all system groups
	 * @return Control Panel Bell device
	 */
	SirenDevice getControlPanelBell();

	/**
	 * Gets Device Panel's ID.
	 * @return Integer containing Device Panel's ID
	 */
	Integer getDevicePanelID();

	/**
	 * This method marks all irrelevant troubles as not reported.<br>
	 * For instance: If a device is set to not-used, all device current troubles are set to not reported.<br>
	 * @param deviceList - List of devices to be checked
	 */
	void markIrrelevantTroublesAsNotReported(Collection<Device> deviceList);

	/**
	 * Returns all devices from specific device protocol.
	 * @param deviceProtocol - {@link DeviceProtocol} object.
	 * @see {@link DeviceProtocol} for possible options.
	 * @return List of {@link Device} objects.
	 */
	List<Device> getDevices(final DeviceProtocol deviceProtocol);
	
	/**

	 * Returns the list of the disarmed groups of a device.</br>
	 * If the device is with internal flag set, groups in stay arm are considered disarmed as well
	 * @param device to be checked
	 * @return the list of device disarmed groups
	 */
	List<Integer> getDisarmedGroupsListofDevice(final Device device);
	
     /**
	 * Checks if all device's groups are disarmed.</br>
	 * It goes through the list of all groups the device belongs to and returned true if all groups are disarmed
	 * @param device to be checked
	 * @return true if all groups are disarmed, otherwise false
	 */
	boolean isAllDeviceGroupsDisarmed(Device device);

}
