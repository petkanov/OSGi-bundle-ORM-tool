package com.riscogroup.nextgen.home.api.device.type.intrusion;

import com.riscogroup.nextgen.db.persistence.configurations.Configurations;
import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.generic.DeviceProtocol;
import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.DeviceType;
import com.riscogroup.nextgen.home.api.generic.TroubleType;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;

public class ControlPanel extends IntrusionDevice {
	
	public ControlPanel(Integer id) {
		super(id);
		setDeviceType(DeviceType.DEVICE_PANEL);
		setProtocolType(DeviceProtocol.SYSTEM.getType());
	}
	public ControlPanel() {
		setDeviceType(DeviceType.DEVICE_PANEL);
		setProtocolType(DeviceProtocol.SYSTEM.getType());
	}
	
	public void initFunctions() {
		super.initFunctions();
		//create system trouble function
		createFunction(IntrusionDevicesFunction.INTRUSION_CONTROLLER_TROUBLE.toString(), TroubleType.STATUS, IntrusionDevicesProperty.OK);
		final DeviceFunction intrusionControllerTrouble = getFunction(IntrusionDevicesFunction.INTRUSION_CONTROLLER_TROUBLE.toString());

		//add separate system troubles as properties with OK status
		createFunctionProperty(intrusionControllerTrouble, TroubleType.AC_LOSS, IntrusionDevicesProperty.OK);
		createFunctionProperty(intrusionControllerTrouble, TroubleType.BELL, IntrusionDevicesProperty.OK);
		createFunctionProperty(intrusionControllerTrouble, TroubleType.AUXILIARY_POWER, IntrusionDevicesProperty.OK);
		createFunctionProperty(intrusionControllerTrouble, TroubleType.POWER_OVERLOAD, IntrusionDevicesProperty.OK);
		createFunctionProperty(intrusionControllerTrouble, TroubleType.JAMMING, IntrusionDevicesProperty.OK);
		
		createFunction(IntrusionDevicesFunction.SYSTEM_TROUBLE.toString(), TroubleType.STATUS, IntrusionDevicesProperty.OK);
		final DeviceFunction systemTrouble = getFunction(IntrusionDevicesFunction.SYSTEM_TROUBLE.toString());

		createFunctionProperty(systemTrouble, TroubleType.IP_NETWORK, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.FALSE_CODE, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.MS1_COMMUNICATION, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.MS2_COMMUNICATION, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.MS3_COMMUNICATION, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.CLOUD_COMMUNICATION, IntrusionDevicesProperty.OK);
		createFunctionProperty(systemTrouble, TroubleType.SYSTEM_CLOCK, IntrusionDevicesProperty.OK);

		createFunction(IntrusionDevicesFunction.PANIC.toString(), TroubleType.STATUS, IntrusionDevicesProperty.OK);
		final DeviceFunction panicFunction = getFunction(IntrusionDevicesFunction.PANIC.toString());		

		createFunctionProperty(panicFunction, TroubleType.STATUS, IntrusionDevicesProperty.OK);
		
		createFunction(IntrusionDevicesFunction.MEDICAL.toString(), TroubleType.STATUS, IntrusionDevicesProperty.OK);
		final DeviceFunction medicalFunction = getFunction(IntrusionDevicesFunction.MEDICAL.toString());
		
		createFunctionProperty(medicalFunction, TroubleType.STATUS, IntrusionDevicesProperty.OK);
	}
	
	public boolean isBatteryStatusAllowsArm() {	
		Boolean lowBatteryArmingConfig = (Boolean) Activator.getConfiguration(Configurations.LOW_BATTERY_ARMING);
		if(lowBatteryArmingConfig != null ? lowBatteryArmingConfig : true)
			return true;			
		
		return !isBatteryTrouble();
	}
	
	/**
	 * returns if AC / main power status allows group to be armed
	 * methods is taking into account AC power status and security standards set to calculate this value
	 * @return true if AC power status allows arm
	 */
	public boolean isACStatusAllowsArm() {
		Boolean ACLossArmingConfig = (Boolean) Activator.getConfiguration(Configurations.AC_LOSS_ARMING);
		if(ACLossArmingConfig != null ? ACLossArmingConfig : true)
			return true;			
		
		Integer status = getFunctionPropertyValue(IntrusionDevicesFunction.INTRUSION_CONTROLLER_TROUBLE.toString(),
				TroubleType.AC_LOSS.toLowerCase().toString());

		return (status == IntrusionDevicesProperty.OK) ? true : false;
		 
	}
}
