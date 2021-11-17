package com.riscogroup.nextgen.home.api.rule.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.rule.localfunctions.ArmDisarmPartition;
import com.riscogroup.nextgen.home.api.rule.localfunctions.ExecutableFunction;
import com.riscogroup.nextgen.home.api.rule.localfunctions.ExecutableFunctionType;
import com.riscogroup.nextgen.home.api.rule.localfunctions.SetVacationMode;
import com.riscogroup.nextgen.home.api.services.DataService;

public class LocalAction {
	public static final String FUNCTION_ID = "FunctionID";
	private Map<Integer, String> properties = null;
	private ExecutableFunction localFunction = null;
	private Rule parent;
	private Integer functionID = ExecutableFunctionType.UNDEFINED.getValue();
	private DataService dataService;
	private Integer uID;

	public LocalAction(Rule parent) {
		this.parent = parent;
		properties = new HashMap<Integer, String>();
		setFunction(functionID);
	}

	public LocalAction(Integer uid) {
		this.uID = uid;
	}

	public Integer getUID() {
		return this.uID;
	}

	public void setUID(int uID) {
		this.uID = uID;
	}

	/**
	 * Sets new {@link ExecutableFunction} to the LocalAction.
	 * ExecutableFunction ID should be from enum {@link ExecutableFunctionType}
	 * @param type Integer containing ExecutableFunction ID
	 */
	public void setFunction(Integer executableFunctionID) {
		setFunction(ExecutableFunctionType.valueOf(executableFunctionID));
	}

	/**
	 * Sets new {@link ExecutableFunction} to the LocalAction.
	 * ExecutableFunction should be from type {@link ExecutableFunctionType}
	 * @param type ExecutableFunctionType object
	 */
	public void setFunction(ExecutableFunctionType type) {
		if(localFunction != null) {
			removeFunction();
		}

		switch(type) {
			case VACATION_MODE:
				localFunction = new SetVacationMode(this);
				setFunctionID(type.getValue());
				break;
			case ARM_DISARM_PARTITON:
				localFunction = new ArmDisarmPartition(this);
				setFunctionID(type.getValue());
				break;
			case UNDEFINED:
				break;
			default:
				break;
		}
	}

	public void removeFunction() {
		localFunction = null;
		setFunctionID(ExecutableFunctionType.UNDEFINED.getValue());
	}

	/**
	 * This method is used only by Persistence to persist and restore functionID.
	 * @param functionID
	 */
	public void setFunctionID(Integer functionID) {
		this.functionID = functionID;
		registerForUpdate();
	}
	
	/**
	 * Returns ExecutableFunction ID.
	 * @see {@link ExecutableFunctionType}
	 * @return Integer containing ExecutableFunction ID
	 */
	public Integer getFunctionID() {
		return functionID;
	}

	/**
	 * Sets properties map.
	 * @param properties HashMap with new properties to set
	 */
	public void setProperties(Map<Integer, String> properties) {
		this.properties = properties;
		registerForUpdate();
	}

	/**
	 * Adds a property to LocalAction object.
	 * @param key
	 * @param value
	 */
	public void addProperty(Integer key, String value) {
		properties.put(key, value);
		registerForUpdate();
	}

	/**
	 * Returns properties map
	 * @return HashMap with all properties
	 */
	public Map<Integer, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * Returns {@link ExecutableFunction} object of this LocalAction
	 * @return ExecutableFunction object
	 */
	public ExecutableFunction getFunction() {
		return localFunction;
	}
	
	/**
	 * Returns the parent of this object.
	 * @return PersistableObject
	 */
	public Rule getParentRule() {
		return this.parent;
	}
	
	public void setParentRule(Rule parent) {
		this.parent = parent;
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

	@Override
	public String toString() {
		if(localFunction == null) {
			return "UID: " + getUID() + ", function: not set";
		}
		return "UID: " + getUID() + ", function: " + localFunction;
	}
}
