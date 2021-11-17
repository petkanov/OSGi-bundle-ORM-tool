package com.riscogroup.nextgen.home.api.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.events.systementities.GroupDeviceAddedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.GroupDeviceRemovedEvent;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.home.api.services.GroupService;
import com.riscogroup.nextgen.home.api.services.UserService;
import com.riscogroup.nextgen.home.api.timers.ArmDelayAction;
import com.riscogroup.nextgen.home.api.timers.ArmDelayType;
import com.riscogroup.nextgen.home.api.timers.DurationTask;
import com.riscogroup.nextgen.home.api.timers.EntryAlarmDelayAction;
import com.riscogroup.nextgen.home.api.timers.EntryDelayAction;

public class Group {
	private static Logger logger = LoggerFactory.getLogger(Group.class);
	
	public static final int MAX_NUMBER = 10*1000; // max number of groups

	private String name;
	private Set<Integer> devices;

	private GroupState groupState;

	private List<DeviceTroublesReportRecord> deviceTroubleReportRecords;

	private DurationTask entryDelayTask;
	private DurationTask exitDelayTask;
	private DurationTask entryAlarmDelayTask;
	private DurationTask armPreWarningTask;
	private DurationTask armDelayTask;	
	private DurationTask deviceNoMotionAlarmTask;

	private Set<Trouble> troubles;
	
	private boolean groupLockedOut;
	private boolean restoredAlarmsConfirmed = true;
	
	private long msReportDelay;
	
	private final DataService dataService;
	
	private Integer groupUID;
	
	public Group(int id) {
		this();
		this.groupUID = id;
	}
	
	public Group() {
		groupState = new GroupState();
		devices = new HashSet<Integer>();
		deviceTroubleReportRecords = new ArrayList<>();
		troubles = new HashSet<Trouble>();
		dataService = Activator.getService(DataService.class);
	}

	public Integer getUID() {
		return this.groupUID;
	}
	
	public void setUID(Integer groupUID) {
		this.groupUID = groupUID;
	}

	/**
	 * Checks whether cancel report should be send.
	 * @return True if cancel report should be send, otherwise false.
	 */
	public boolean shouldSendCancelReport() {
		return groupState.shouldSendCancelReport() && !isMsReportDelay();
	}
	
	/**
	 * This method returns the user-friendly name of <code>Group</code>, e.g. "kitchen" or
	 * "heating system".
	 * 
	 * @return the last known value for the <code>Group</code> name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the <code>Group</code>.
	 * @param newName the <code>Group</code> name.
	 * @return true if setting newName is persisted, otherwise false with logger warn message
	 */
	public void setName(String newName) {
		logger.trace("newName: {}", newName);
		name = newName;
		dataService.registerForUpdate(this);
	}
	
	/**
	 * Adds new <code>Device</code> to the current <code>Group</code>. Adding
	 * the device into new <code>Group</code> will remove it only from the system <code>Group</code> and
	 * the device will not be removed from other <code>Group</code>s where the <code>Device</code> is
	 * already added.
	 *
	 * @param deviceUID - ID of the <code>Device</code> to be added to this <code>Group</code>.
	 */
	public void addDevice(Integer deviceUID) {
		if(deviceUID == null) {
			return;
		}

		devices.add(deviceUID);
		dataService.registerForUpdate(this);
		new GroupDeviceAddedEvent(this, deviceUID).postEvent();
		logger.debug("Added device with UID: {} to group with UID: {} ", deviceUID, getUID());
	}

	/**
	 * Removes the specified <code>Device</code> from the current <code>Group</code>.
	 * @param deviceUID - ID of the <code>Device</code> to be removed from this <code>Group</code>.
	 */
	public void removeDevice(Integer deviceUID) {
		if(!devices.contains(deviceUID)) {
			return;
		}

		try {
			removeDeviceTroublesReportRecord(deviceUID);
		} catch (Exception e) {
			logger.error("Failed to remove DeviceTroublesReportRecord for device with UID: {}. Reason: {}", deviceUID, e.getMessage());
			throw new RuntimeException("Device ID "+deviceUID+" was not removed");
		}

		final Iterator<Trouble> it = troubles.iterator();
		while(it.hasNext()) {
			final Trouble t = it.next();
			if(t.getDeviceID().equals(deviceUID)) {
				it.remove();
			}
		}
		devices.remove(deviceUID);
		dataService.registerForUpdate(this);

		new GroupDeviceRemovedEvent(this, deviceUID).postEvent();
		logger.debug("Device with UID: {} is removed from group with UID: {} ", deviceUID, getUID());
	}

	/**
	 * This method returns all <code>Device</code> instances, which are added to
	 * the current <code>Group</code>. If a <code>Device</code> is added to a number of <code>Group</code>s, it will be
	 * present only once in the result array.
	 * 
	 * @return devices - Umodifiable set of devices.
	 */
	public Set<Integer> getDevices() {
		return Collections.unmodifiableSet(devices);
	}

	public void setDevices(Set<Integer> deviceList) {
		devices = new HashSet<>(deviceList);
		dataService.registerForUpdate(this);
	}

	/* Intrusion functionality */
	/**
	 * Sets arm state.
	 * @param armState stay or away arm or disarm
	 */
	public void setArmState(ArmState armState) {
		logger.debug("Changing arm state to: " + armState);
		groupState.setArmState(armState);
		dataService.registerForUpdate(this);
	}

	/**
	 * Returns whether the group is ready for arm.
	 * @param armState - New arm state.
	 * @return True if the group is ready for arm, otherwise false.
	 */
	public boolean isReadyForArm(ArmState armState) {
		if(armState == ArmState.ARMED_STAY && groupState.getReadyStateStayArmd() == ReadyState.GROUP_NOT_READY) {
			return false; // not ready instant stay arm
		} else if(armState == ArmState.ARMED_AWAY && groupState.getReadyStateAwayArmd() == ReadyState.GROUP_NOT_READY) {
			return false; // not ready instant away arm
		} else if(armState == ArmState.ARMING_STAY && groupState.getReadyStateStayArming() == ReadyState.GROUP_NOT_READY) {
			return false; // not ready exit delay stay arm
		} else if(armState == ArmState.ARMING_AWAY && groupState.getReadyStateAwayArming() == ReadyState.GROUP_NOT_READY) {
			return false; // not ready exit delay away arm
		}

		return true;
	}

	/**
	 * Returns group state object which combines: armState, alarmState and readyState.
	 * 
	 * @return GroupState object
	 */
	public GroupState getGroupState() {
		return groupState;
	}

	/**
	 * Sets new alarm state of the group.
	 * @param alarmState
	 */
	public void setAlarmState(AlarmState alarmState) {
		logger.debug("Changing alarm state to: " + alarmState);
		groupState.setAlarmState(alarmState);
	}

	/**
	 * gets device trouble report record by device ID
	 * @param deviceID
	 * @return DeviceCurrentTroubles
	 */
	public DeviceTroublesReportRecord getDeviceTroublesReportRecord(Integer deviceID) {
		for(DeviceTroublesReportRecord deviceTrouble : deviceTroubleReportRecords) {
			if(deviceTrouble.getDeviceID().equals(deviceID)) {
				return deviceTrouble;
			}
		}
		return null;
	}

	/**
	 * adds device troubles report record to a device by device ID
	 * @param deviceID
	 * @return DeviceCurrentTroubles
	 */
	public DeviceTroublesReportRecord addDeviceTroublesReportRecord(Integer deviceID) {
		DeviceTroublesReportRecord record = getDeviceTroublesReportRecord(deviceID);
		if(record != null) {
			return record;
		}

		record = new DeviceTroublesReportRecord();
		record.setDeviceID(deviceID);
		dataService.persistObject(record);
		
		deviceTroubleReportRecords.add(record);
		dataService.registerForUpdate(this);
		
		return record;
	}

	/**
	 * removes device trouble report record from a device by device ID
	 * @param deviceID
	 */
	private void removeDeviceTroublesReportRecord(Integer deviceID) throws Exception {
		final Iterator<DeviceTroublesReportRecord> it = deviceTroubleReportRecords.iterator();
		while(it.hasNext()) {
			final DeviceTroublesReportRecord record = it.next();
			if(record.getDeviceID().equals(deviceID)) {
				it.remove();
				dataService.registerForDelete(record);
			}
		}
	}

	/**
	 * gets the list of device troubles report records
	 * 
	 * @return Collection<DeviceCurrentTroubles>
	 */
	public List<DeviceTroublesReportRecord> getDevicesTroublesReportRecordList() {
		return Collections.unmodifiableList(deviceTroubleReportRecords);
	}

	/**
	 * This method should be used only by Persistence service
	 * @param devicesTroubles
	 */
	public void setDevicesTroublesReportRecords(List<DeviceTroublesReportRecord> deviceTroubleReportRecords) {
		this.deviceTroubleReportRecords = new ArrayList<>(deviceTroubleReportRecords);
	}

	@Override
	public String toString() {
		return "Group uID: " + getUID() + ", Name: " + getName() + ", Devices : " + getDevices() + ", GroupState: "
				+ getGroupState();
	}


	/**
	 * Starts entry timer task of a group.
	 * This method is called whenever entry delay timer of a group has to be started.
	 * This happens when entry / exit zone is violated when the group is armed
	 * @param action
	 */
	public void startEntryDelayTask(EntryDelayAction action) {
		if(hasEntryDelayTaskRunning()) {
			logger.warn("There is already entry delay task running");
			return;
		}

		entryDelayTask = new DurationTask(action);
		entryDelayTask.start();
	}

	/**
	 * Returns EntryDelayAction of the entry delay task.
	 * @return EntryDelayAction object.
	 */
	public EntryDelayAction getEntryDelayAction() {
		return entryDelayTask != null ? (EntryDelayAction)entryDelayTask.getAction() : null;
	}

	/**
	 * Checks whether group has entry delay task running.
	 * @return True if the group has entry delay task running, otherwise false
	 */
	public boolean hasEntryDelayTaskRunning() {
		return entryDelayTask != null ? !entryDelayTask.isDone() : false;
	}

	/**
	 * Cancels entry delay task of a group.
	 * This happens if disarm command is sent during entry delay period.
	 */
	public void cancelEntryDelayTask() {
		if(entryDelayTask != null) {
			entryDelayTask.cancel();
		}

		entryDelayTask = null;
	}

	/**
	 * Returns entry delay time remaining or zero if entry timer is not running
	 * @return entry delay time remaining in milliseconds
	 */
	public long getEntryDelayTimeRemaining() {
		return (entryDelayTask != null) ? entryDelayTask.getTimeLeft() : 0;
	}
	
	/**
	 * Returns entry delay task timeout or zero if entry timer is not running
	 * @return entry delay timeout of this task in milliseconds
	 */
	public long getEntryDelayTaskTimeout() {
		return (entryDelayTask != null) ? entryDelayTask.getTaskTime() : 0;
	}

	/**
	 * Starts exit delay timer task of a group.
	 * The method is called whenever exit delay timer of a group should be started.
	 * It happens when group goes to arming state after arm command is sent and group has at least one entry / exit zone
	 * @param action
	 */
	public void startExitDelayTask(ArmDelayAction action) {
		if(hasExitDelayTaskRunning()) {
			logger.warn("There is already exit delay task running");
			return;
		}
		
		action.setArmDelayType(ArmDelayType.EXIT_DELAY);
		exitDelayTask = new DurationTask(action);
		exitDelayTask.attachPostActionMonitor();
		exitDelayTask.start();
	}

	/**
	 * Starts entry alarm delay task of a group.
	 * @param action
	 */
	public void startEntryAlarmDelayTask(EntryAlarmDelayAction action) {
		if(hasEntryAlarmDelayTask()) {
			logger.warn("There is already entry alarm delay task running");
			return;
		}

		entryAlarmDelayTask = new DurationTask(action);
		entryAlarmDelayTask.start();
	}
	
	/**
	 * Stops entry alarm delay task of a group.
	 */
	public void stopEntryAlarmDelayTask() {
		if(entryAlarmDelayTask != null) {
			entryAlarmDelayTask.stop();
		}
		entryAlarmDelayTask = null;
	}
	
	/**
	 * Returns EntryAlarmDelayAction of the entry delay task.
	 * @return EntryAlarmDelayAction object.
	 */
	public EntryAlarmDelayAction getEntryAlarmDelayAction() {
		return entryAlarmDelayTask != null ? (EntryAlarmDelayAction)entryAlarmDelayTask.getAction() : null;
	}

	/**
	 * Checks whether group has entry alarm task running.
	 * @return True if the group has entry alarm task running, otherwise it returns false
	 */
	public boolean hasEntryAlarmDelayTask() {
		return entryAlarmDelayTask != null ? !entryAlarmDelayTask.isDone() : false;
	}
	
	/**
	 * Checks whether group has exit or guard delay task running.
	 * @return True if the group has exit delay task running, otherwise it returns false
	 */
	public boolean hasExitDelayTaskRunning() {
		return exitDelayTask != null ? !exitDelayTask.isDone() : false;
	}

	/**
	 * Cancels exit delay timer task of a group
	 * the method is called whenever exit delay timer of a group has been stopped / canceled
	 * this happens when exit delay timer expires or if disarm command is sent during arming period
	 */
	public void cancelExitDelayTask() {
		if(exitDelayTask != null) {
			exitDelayTask.cancel();
		}

		exitDelayTask = null;
	}

	/**
	 * returns exit delay time remaining or zero if exit timer is not running
	 * @return exit delay time remaining in milliseconds
	 */
	public long getExitDelayTimeRemaining() {
		return (exitDelayTask != null) ? exitDelayTask.getTimeLeft() : 0;
	}

	/**
	 * Adds new trouble
	 * @param trouble
	 */
	public void addTrouble(Trouble trouble) {
		troubles.add(trouble);
	}

	/**
	 * Removes a trouble
	 */
	public void removeTrouble(Trouble trouble) {
		troubles.remove(trouble);
	}

	/**
	 * Returns all troubles
	 * @return Set of Trouble objects
	 */
	public Set<Trouble> getTroubles() {
		return troubles;
	} 
	
	
	public DurationTask getArmPreWarningTask() {
		return armPreWarningTask;
	}

	/**
	 * Cancels pre-warning timer task of a group
	 * the method is called whenever pre-warning timer of a group has been stopped / canceled
	 */
	public void cancelArmPreWarningTask() {
		if(armPreWarningTask != null) {
			armPreWarningTask.cancel();
		}

		armPreWarningTask = null;
	}
	
	/**
	 * returns pre-warning time remaining or zero if pre-warning timer is not running
	 * @return pre-warning time remaining in milliseconds
	 */
	public long getArmPreWarningTimeRemaining() {
		return (armPreWarningTask != null) ? armPreWarningTask.getTimeLeft() : 0;
	}

	/**
	 * Checks whether group has pre-warning task running.
	 * @return True if the group has pre-warning task running, otherwise it returns false
	 */
	public boolean hasArmPreWarningTaskRunning() {
		return armPreWarningTask != null ? !armPreWarningTask.isDone() : false;
	}
	
	/**
	 * Starts pre-warning timer task of a group.
	 * @param action
	 */
	public void startArmPreWarningTask(ArmDelayAction action) {
		if(hasArmPreWarningTaskRunning()) {
			logger.warn("There is already arm pre-waring task running");
			return;
		}

		action.setArmDelayType(ArmDelayType.PRE_ARM_WARNING_DELAY);
		armPreWarningTask = new DurationTask(action);
		armPreWarningTask.start();
	}
	
	/**
	 * Cancels delay timer task of a group
	 * the method is called whenever delay timer of a group has been stopped / canceled
	 */
	public void cancelArmDelayTask() {
		if(armDelayTask != null) {
			armDelayTask.cancel();
		}

		armDelayTask = null;
	}
	
	/**
	 * returns delay time remaining or zero if delay timer is not running
	 * @return delay time remaining in milliseconds
	 */
	public long getArmDelayTimeRemaining() {
		return (armDelayTask != null) ? armDelayTask.getTimeLeft() : 0;
	}

	/**
	 * Checks whether group has delay task running.
	 * @return True if the group has delay task running, otherwise it returns false
	 */
	public boolean hasArmDelayTaskRunning() {
		return armDelayTask != null ? !armDelayTask.isDone() : false;
	}
	
	/**Returns arm delay task
	 * @return arm delay task
	 */
	public DurationTask getArmDelayTask() {
		return armDelayTask;
	}
	
	/**
	 * Starts delay timer task of a group.
	 * @param action
	 */
	public void startArmDelayTask(ArmDelayAction action) {
		if(hasArmDelayTaskRunning()) {
			logger.warn("There is already arm delay task running");
			return;
		}
		action.setArmDelayType(ArmDelayType.ARM_DELAY);
		armDelayTask = new DurationTask(action);
		armDelayTask.start();
	}

	public boolean isAllowedToBeRemoved() {
		final GroupService gs = Activator.getService(GroupService.class);
		final UserService us = Activator.getService(UserService.class);
		if(us == null || gs == null) {
			logger.error("UserService Or GroupService is Null");
			return false;
		}
		final Set<Integer> attachedDevices = getDevices();
		final Set<User> systemUsers = Collections.unmodifiableSet(us.getUsers());

		for(Integer deviceId : attachedDevices) {
			final List<Group> devGroups = gs.getDeviceGroups(deviceId);
			if(devGroups == null) {
				logger.warn("Device: "+deviceId+" has no groups attached to it!");
				continue;
			}
			if(devGroups.size() == 1 && devGroups.get(0).getUID().equals(getUID())) {
				logger.error("Can not remove Group: "+getUID()+". It is the only group for the Device: "+deviceId);
				return false;
			}
		}
		for(User user : systemUsers) {
			if(user.isSuperUser()) {
				continue;
			}
			final List<Integer> userGroups = user.getUserGroups();
			if(userGroups != null && userGroups.size() == 1 && userGroups.contains(getUID())) {
				logger.error("Can not remove Group: "+getUID()+". It is the only group for the User: "+user.getUID());
				return false;
			}
		}
		return true;
	}

	
	public Boolean getGroupLockedOut() {
		return groupLockedOut;
	}

	
	public void setGroupLockedOut(Boolean groupLockedOut) {
		this.groupLockedOut = groupLockedOut;
		dataService.registerForUpdate(this);
	}	
	
	
	public DurationTask getDeviceNoMotionAlarmTask() {
		return deviceNoMotionAlarmTask;
	}

	
	public void setDeviceNoMotionAlarmTask(DurationTask deviceNoMotionAlarmTask) {
		this.deviceNoMotionAlarmTask = deviceNoMotionAlarmTask;
	}
	
	
	public boolean isRestoredAlarmsConfirmed() {
		return restoredAlarmsConfirmed;
	}

	
	public void setRestoredAlarmsConfirmed(boolean restoredAlarmsConfirmed) {
		this.restoredAlarmsConfirmed = restoredAlarmsConfirmed;
	}
	
	private boolean isMsReportDelay() {
		return msReportDelay > 0;
	}

	
	public void setMsReportDelay(int msReportDelay) {
		this.msReportDelay = msReportDelay;
	}
	
	public long getMsReportDelay() {
		return this.msReportDelay;
	}
}
