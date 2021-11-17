package com.riscogroup.nextgen.home.api.generic;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.services.DataService;

public class DeviceTroublesReportRecord {

	private Integer uID;
	private Integer deviceID;
	
	private static Logger logger = LoggerFactory.getLogger(DeviceTroublesReportRecord.class);
	
	/*
	 * Map key -> troubleName , value -> reportedStatus boolean 
	 * which contains information if trouble status event was reported or not:
	 * 1) to be set when a trouble event to be reported and
	 * 2) to be reset when a restore event to be reported
	 */
	private Map<String, Boolean> troubleReportedMap = new HashMap<>();
 
	private Boolean restoredDuringDelay = false;
	
	/*
	 * Map key -> troubleName representing trouble type (like TroubleType.SUPERVISION_LOSS or TroubleType.AC_LOSS and etc.),
	 * Map value -> confirmedStatus boolean 
	 * which contains information if trouble restore was confirmed or not:
	 * 1) to be set when device trouble is restored
	 * 2) to be reset when trouble restore is confirmed
	 */
	private  Map<String, Boolean> restoreTroubleTypeToBeConfirmedMap = new HashMap<>();
	
	/*
	 * Map key -> alarmName representing alarm type (like TroubleType.STATUS or TroubleType.TAMPER),
	 * Map value -> confirmedStatus boolean 
	 * which contains information if alarm restore was confirmed or not:
	 * 1) to be set when alarm restore is logged and
	 * 2) to be reset when alarm restore is confirmed
	 */
	private Map<String, Boolean> restoreAlarmTypeToBeConfirmedMap = new HashMap<>();
	
	private DataService dataService;
	
	public DeviceTroublesReportRecord() {}
	
	public DeviceTroublesReportRecord(int troubleRecordId) {
		this.uID = troubleRecordId;
	}

	public Integer getDeviceID() {
		return deviceID;
	}

	public void setDeviceID(Integer deviceID) {
		this.deviceID = deviceID;
		registerForUpdate();
	}

	public Boolean getRestoredDuringDelay() {
		return restoredDuringDelay;
	}

	public void setRestoredDuringDelay(Boolean restoredDuringDelay) {
		this.restoredDuringDelay = restoredDuringDelay;
		registerForUpdate();
	}

	/**
	 * Sets status report of specific device trouble
	 * @param troubleType - name of device trouble
	 * @param value - boolean value of status report
	 */
	public void setReportedStatusTrouble(TroubleType troubleType, Boolean value) {
		logger.trace("troubleName: {}, value: {}", troubleType, value);
		if (troubleType == null || value == null) return;
		String troubleName = troubleType.toLowerCase();
		Boolean reportStatus = troubleReportedMap.get(troubleName);
		if (reportStatus == null) {
			troubleReportedMap.put(troubleName, value);
		} else {
			troubleReportedMap.replace(troubleName, value);
		}
		registerForUpdate();
	}
	
	/**
	 * Gets reported status of specific device trouble
	 * @param troubleType name of device trouble
	 * @return Boolean flag of status report - true of trouble was reported, otherwise false
	 */
	public Boolean getReportedStatusTrouble(TroubleType troubleType) {
		if (troubleType == null) return null;
		String troubleName = troubleType.toLowerCase();
		if (troubleReportedMap.containsKey(troubleName)) {
			return troubleReportedMap.get(troubleName);
		}
		return false;
	}
	

	public Map<String, Boolean> getTroubleReportedMap() {
		return troubleReportedMap;
	}
	
	
	public void setTroubleReportedMap (Map<String, Boolean> troubleReportedMap) {
		this.troubleReportedMap = troubleReportedMap;
		registerForUpdate();
	}
	
	
	public Map<String, Boolean> getRestoreTroubleTypeToBeConfirmedMap() {
		return restoreTroubleTypeToBeConfirmedMap;
	}

	
	public void setRestoreTroubleTypeToBeConfirmedMap(Map<String, Boolean> restoreTroubleToBeConfirmedMap) {
		this.restoreTroubleTypeToBeConfirmedMap = restoreTroubleToBeConfirmedMap;
		registerForUpdate();
	}

	/**
	 * Sets status for restore trouble confirmation of specific device trouble
	 * @param troubleType - name of device trouble
	 * @param value - boolean value of confirmation status
	 */
	public void setRestoreTroubleToBeConfirmed(TroubleType troubleType, Boolean value) {
		logger.trace("troubleName: {}, value: {}", troubleType, value);
		if (troubleType == null || value == null) return;
		String troubleName = troubleType.toLowerCase();
		Boolean confirmedStatus  = restoreTroubleTypeToBeConfirmedMap.get(troubleName);
		if (confirmedStatus  == null) {
			restoreTroubleTypeToBeConfirmedMap.put(troubleName, value);
		} else {
			restoreTroubleTypeToBeConfirmedMap.replace(troubleName, value);
		}
		registerForUpdate();
	}
	

	public Map<String, Boolean> getRestoreAlarmTypeToBeConfirmedMap() {
		return restoreAlarmTypeToBeConfirmedMap;
	}	
	
	
	public void setRestoreAlarmTypeToBeConfirmedMap(Map<String, Boolean> restoreAlarmToBeConfirmedMap) {
		this.restoreAlarmTypeToBeConfirmedMap = restoreAlarmToBeConfirmedMap;
		registerForUpdate();
	}
		
	/**
	 * Sets status for restore alarm confirmation of specific device trouble
	 * @param troubleType - name of device trouble
	 * @param value - boolean value of confirmation status
	 */
	public void setRestoreAlarmToBeConfirmed(TroubleType troubleType, Boolean value) {
		logger.trace("troubleName: {}, value: {}", troubleType, value);
		if (troubleType == null || value == null) return;
		String troubleName = troubleType.toLowerCase();
		Boolean confirmedStatus  = restoreAlarmTypeToBeConfirmedMap.get(troubleName);
		if (confirmedStatus  == null) {
			restoreAlarmTypeToBeConfirmedMap.put(troubleName, value);
		} else {
			restoreAlarmTypeToBeConfirmedMap.replace(troubleName, value);
		}
		registerForUpdate();
	}	
	
	/**
	 * Marks all troubles as not reported
	 */
	public void markAllTroublesAsNotReported() {
		for(String currentTrouble : troubleReportedMap.keySet()) {
			troubleReportedMap.put(currentTrouble, false);
		}
		registerForUpdate();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Trouble record ID: " + this.getUID().toString());
		sb.append(", for device with ID: " + (deviceID != null ? deviceID.toString() : "null"));
		sb.append(", troubleReportedMap: " + troubleReportedMap);
		sb.append(", restoredDuringDelay: " + restoredDuringDelay);
		sb.append(", restoreTroubleTypeToBeConfirmedMap: " + restoreTroubleTypeToBeConfirmedMap);
		sb.append(", restoreAlarmTypeToBeConfirmedMap: " + restoreAlarmTypeToBeConfirmedMap);
		return sb.toString();
	}

	public Integer getUID() {
		return this.uID;
	}

	public void setUID(int id) {
		this.uID = id;
	}
	
	private void registerForUpdate() {
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}
}
