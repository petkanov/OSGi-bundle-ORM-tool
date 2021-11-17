package com.riscogroup.nextgen.home.api.device.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.generic.DeviceFunction;

public class ZwaveMultiChannelDevice extends ZWaveDevice {

	private static Logger logger = LoggerFactory.getLogger(ZwaveMultiChannelDevice.class);

	private Integer endPoint;

	public ZwaveMultiChannelDevice(Integer uID) {
		super(uID);
	}

	public ZwaveMultiChannelDevice(ZWaveDevice device) {
		this.setVendor(device.getVendor());
		this.setVersion(device.getVersion());
		this.setBatteryOperated(device.isBatteryOperated());
		this.setLifelineGroup(device.getLifelineGroup());
		this.setCommonZone(device.getCommonZone());
		this.setProtocolAdapter(device.getProtocolAdapter());
		this.setProtocolID(device.getProtocolID());
		this.setProtocolType(device.getProtocolType());
		logger.debug("ZwaveMultiChannelDevice is created with uID {}", this.getUID());
	}

	public Integer getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(Integer endPoint) {
		this.endPoint = endPoint;
		registerForUpdate();
	}

	@Override
	public Integer getSubDeviceID() {
		return endPoint;
	}

	@Override
	public DeviceFunction getFunction(String cmdName) {
		return super.getFunction(cmdName, endPoint);
	}

	@Override
	public String toString() {
		final StringBuffer buffer = new StringBuffer(230);
		buffer.append(super.toString()).append(",EndPoint=").append(endPoint != null ? endPoint : "0").append("}");
		return buffer.toString();
	}
}
