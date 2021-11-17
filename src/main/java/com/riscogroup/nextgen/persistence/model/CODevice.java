package com.riscogroup.nextgen.home.api.device.type.intrusion;

import com.riscogroup.nextgen.home.api.generic.IntrusionConstants.DeviceType;

public class CODevice extends IntrusionDevice {
	public CODevice(Integer uID) {
		super(uID);
		setDeviceType(DeviceType.DEVICE_CO);
		restorable = true;
	}
	public CODevice() {
		setDeviceType(DeviceType.DEVICE_CO);
		restorable = true;
	}
}
