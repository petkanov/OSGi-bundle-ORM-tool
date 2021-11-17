package com.riscogroup.nextgen.home.api.device.type.intrusion;

import com.riscogroup.nextgen.db.persistence.configurations.Configurations;
import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.DeviceType;
import com.riscogroup.nextgen.home.api.generic.TroubleType;

public class PIRDevice extends IntrusionDevice {
	
	public PIRDevice(Integer id) {
		super(id);
		setDeviceType(DeviceType.DEVICE_PIR_PET);
		restorable = false;
	}
	
	public PIRDevice() {
		setDeviceType(DeviceType.DEVICE_PIR_PET);
		restorable = false;
	}

	public void initFunctions() {
		super.initFunctions();
		createFunction(IntrusionDevicesFunction.FUNCTION_MASKING_STATUS.toString(), TroubleType.STATUS,
				IntrusionDevicesProperty.OK);
	}

	/**
	 * returns if masking trouble status of device allows group to be armed
	 * methods is taking into account masking status, bypass settings and zone configuration to calculate this value
	 * 
	 * @return true if masking status allows arm
	 */
	public boolean isMaskingStatusAllowsArm() {
		
		Boolean maskingAsTamperConfig = (Boolean) Activator.getConfiguration(Configurations.ANTI_MASK_AS_TAMPER);
		if (!(maskingAsTamperConfig != null ? maskingAsTamperConfig : false))
			return true;

		if (zoneConfiguration == null || getZoneConfigurationEnum().isNotUsedZoneType())
			return true; // not configured at all or device not used

		if (bypassState.isIgnoreTroubles()) {
			return true; // device tamper is bypassed
		}

		return (!isMaskingTrouble());
	}
	
	/**
	 * sets masking status
	 * @param isMaskingTrouble true for trouble
	 */
	public void setMaskingTrouble(Boolean isMaskingTrouble) {
		Integer status = isMaskingTrouble == true ? IntrusionDevicesProperty.FAIL : IntrusionDevicesProperty.OK;
		setFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_MASKING_STATUS.toString(),
				TroubleType.STATUS.toLowerCase(), status);
	}
	
	/**
	 * checks masking status
	 * @return true if it is masking trouble
	 */
	public boolean isMaskingTrouble() {
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.FUNCTION_MASKING_STATUS.toString(),
				TroubleType.STATUS.toLowerCase());

		return (status == IntrusionDevicesProperty.FAIL) ? true : false;		 
	}
}
