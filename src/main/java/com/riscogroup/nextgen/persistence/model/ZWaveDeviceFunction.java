package com.riscogroup.nextgen.home.api.function.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;

public class ZWaveDeviceFunction extends DeviceFunction {
private static Logger logger = LoggerFactory.getLogger(ZWaveDeviceFunction.class);
	
	private boolean ackReceived;
	private boolean embeddedFunction;
	private Integer commandId;
	private String commandClassId;
	private byte notificationType;
	private String reason;

	public ZWaveDeviceFunction(Integer uID) {
		super(uID);
	}
	
	public ZWaveDeviceFunction(Device parent) {
		super(parent);
	}

	public ZWaveDeviceFunction(ZWaveDeviceFunction devFunc) {
		this(devFunc.getParent());
		logger.debug("Copy constructor is invoked for ZWaveDeviceFunction");
		this.setName(devFunc.getName());
		this.setCommandClassId(devFunc.getCommandClassId());
		this.setEndPointId(devFunc.getEndPointId());		
		//this.setIsProcessed(devFunc.isProcessed());
		this.setCommandId(devFunc.getCommandId());
		this.setAckReceived(devFunc.getAckReceived());
		this.setSkip(devFunc.getSkip());
		this.setNotificationType(devFunc.getNotificationType());
		this.setEmbeddedFunction(true);
		this.setReasonToFailed(devFunc.getReasonToFailed());
		devFunc.getParent().addFunction(this);
	}

	/**
	 * This method is invoked to set the command class command id for which this device function is responsible for.
	 * 
	 * @param commandId
	 */
	public void setCommandId(Integer commandId) {
		this.commandId = commandId;
		registerForUpdate();
		logger.debug("Zwave device function {} has command id {}", getCommandName(), commandId);
	}

	/**
	 * Return command class command id.
	 * 
	 * @return
	 */
	public Integer getCommandId() {
		return commandId;
	}

	/**
	 * Invoke this method to set TRUE when a data is sent through this device function and was received from the end
	 * device otherwise set FALSE.
	 * 
	 * @param ackReceived
	 */
	public void setAckReceived(Boolean ackReceived) {
		if(!ackReceived.equals(this.ackReceived)) {
			this.ackReceived = ackReceived;
			registerForUpdate();	
			logger.debug("ZWave device function {} ackReceived flag is set to {}", getCommandName(), ackReceived);
		}
	}

	/**
	 * Return TRUE if the sent data was acknowledge by the device otherwise FALSE .
	 * 
	 * @return
	 */
	public Boolean getAckReceived() {
		return ackReceived;
	}

	/**
	 * Return TRUE if this function belongs to a multichannel device otherwise FALSE
	 * 
	 * @return
	 */
	public Boolean isMultiChannel() {
		return super.getEndPointId() > 0 ? Boolean.TRUE : Boolean.FALSE;
	}
	

	/**
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer(230);
		buffer.append(",commandId=").append(getCommandId())
		.append(",ackReceived=").append(getAckReceived())
		.append(",isMultiChannel=").append(isMultiChannel())
		.append("}");
		return super.toString()+", "+buffer.toString();
	}
	
	/**
	 * Sets notification type which is using in the Notification command class
	 * 
	 * @param notificationType
	 */
	public void setNotificationType(byte notificationType) {
		this.notificationType = notificationType;
	}

	/**
	 * Returns notification type
	 * 
	 * @return
	 */
	public byte getNotificationType() {
		return notificationType;
	}
	
	/**
	 * This method is invoked with TRUE when this device function will be created by specific command class in its handle method. 
	 * It is used a copy constructor internally by the specific CommandClass implementation.
	 * 
	 * @param embeddedFunction
	 */
	public void setEmbeddedFunction(Boolean embeddedFunction) {
		if(!embeddedFunction.equals(this.embeddedFunction)) {
			this.embeddedFunction = embeddedFunction;
			registerForUpdate();	
		}
	}
	
	/**
	 * This method returns a property ID for the <code>DeviceFunction</code>.
	 * 
	 * @param cmdClasId
	 */
	public void setCommandClassId(String cmdClasId) {
		this.commandClassId = cmdClasId;
		registerForUpdate();
		logger.debug("cmdClscId {}", cmdClasId);
	}

	/**
	 * Return a Command class id value
	 * 
	 * @param commandClassId
	 */
	public String getCommandClassId() {
		return commandClassId;
	}

	/**
	 * Return TRUE if this device function is created by the handle method of one of the command classes implementation. 
	 * 
	 * @return
	 */
	public Boolean isEmbeddedFunction() {
		logger.debug("Device function {}, cmdClsId {}, name {}, is embedded: {}", getCommandName(), commandClassId, name, embeddedFunction);
		return embeddedFunction;
	}
	/**
	 * When device function is not processed because of a failure set the reason of this failure.
	 * 
	 */
	public void setReasonToFailed(String reason) {
		this.reason = reason;
		registerForUpdate();
	}
	
	/**
	 *  Return reason of the failure 
	 */
	public String getReasonToFailed() {
		return reason;
	}
}
