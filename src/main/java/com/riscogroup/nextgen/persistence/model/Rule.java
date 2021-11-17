package com.riscogroup.nextgen.home.api.rule.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.events.systementities.RuleActionAddressAddedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleActionAddressRemovedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleExecutedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleLocalActionAddedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleLocalActionRemovedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleRenamedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleScheduleAddedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleScheduleRemovedEvent;
import com.riscogroup.nextgen.home.api.events.systementities.RuleSetManuallyEvent;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.home.api.rule.localfunctions.ExecutableFunctionType;
import com.riscogroup.nextgen.home.api.rule.schedule.ScheduledTask;
import com.riscogroup.nextgen.home.api.rule.schedule.SingleTask;
import com.riscogroup.nextgen.home.api.rule.schedule.TaskScheduler;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.home.api.services.DeviceService;
import com.riscogroup.nextgen.home.api.services.SystemService;

public class Rule implements Executable {
	private static Logger logger = LoggerFactory.getLogger(Rule.class);

	/* These objects should be managed from initCache() method */
	private Map<Integer, LocalAction> localActions;
	private Map<Integer, ActionAddress> actionAddresses;
	private Map<Integer, RuleTrigger> ruleTriggers;
	private Map<Integer, Schedule> schedules;

	private String name;
	private Long duration = 0L; // to avoid NPE;
	private Long executionInterval = 0L; // time between executions of ActionAddresses 
	private Boolean isEnabled; // if the rule is isEnabled or disabled
	private Boolean manuallyExecutable; // if the rule can be run manually
	private Boolean enabledOnVacation;
	private TaskScheduler taskScheduler;
	private Integer uID;
	private DataService dataService;

	public Rule() {
		localActions = new HashMap<>();
		actionAddresses = new HashMap<>();
		ruleTriggers = new HashMap<>();
		schedules = new HashMap<>();
		dataService = Activator.getService(DataService.class);
	}

	public Rule(Integer uID) {
		this();
		this.uID = uID;
	}

	public Integer getUID() {
		return this.uID;
	}

	public void setUID(int uID) {
		this.uID = uID;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}
	/**
	 * Change the name of the #Rule
	 * @param name
	 */
	public void renameRule(String name) {
		setName(name);
		logger.debug("name: {}", name);
		new RuleRenamedEvent(this).postEvent();
	}

	/**
	 * Check if all the actions are currently in the state of the <code>Rule</code> definitions.
	 * @return true, if is active
	 */
	public boolean isActivated() {
		boolean isActivated = false;
		int counterActiveActionAdresses = 0;
		int counterActiveLocalActions = 0;
		final DeviceService deviceService = Activator.getService(DeviceService.class);
		if(deviceService == null) {
			logger.error("DeviceService is null");
			return false;
		}

		for (ActionAddress actionAddress : getActionAddresses()) {
			Integer deviceUID = actionAddress.getDeviceUID();
			String deviceFunction = actionAddress.getDeviceFunction();
			Integer deviceProperty = actionAddress.getPropertyIndex();
			Object aaValue = actionAddress.getValue();
			if (aaValue == null) {
				break;
			}
			Device device = null;
			try {
				device = deviceService.getDevice(deviceUID);
			} catch (Exception e) {
				logger.error("Failed to get device with uID {}", deviceUID);
				e.printStackTrace();
			}
			if (device == null) {
				continue;
			}
			DeviceFunction function = device.getFunction(deviceFunction);
			if (function == null) {
				continue;
			}
			List<DeviceProperty> propertyList = function.getDeviceProperties();
			if (propertyList == null) {
				continue;
			}
			for (DeviceProperty property : propertyList) {
				logger.debug("deviceId {} function {} property {} value {}", deviceUID.intValue(), deviceFunction, deviceProperty, aaValue);
				if (property != null && property.getIndex().equals(deviceProperty)) {
					Object propertyValue = property.getValue();
					//to Avoid NPE
					if ((propertyValue != null) && (aaValue != null) && (propertyValue.equals(aaValue))) {
						counterActiveActionAdresses++;
					}
					logger.debug("Compare values [current property value] {} [presets value] {}", propertyValue , aaValue);
				}
			}
		}

		if(localActions != null) {
			for(LocalAction localAction : getLocalActions()) {
				if(localAction.getFunction() != null && localAction.getFunction().isActive()) {
					counterActiveLocalActions++;
				}
			}
		}

		//if only 1 element appears missing is being ignored with check xxxSize == 0
		boolean activatedActionAddress = (actionAddresses.size() == 0) || (actionAddresses.size() > 0 && actionAddresses.size() == counterActiveActionAdresses) ;
		boolean activatedLocalAction = (localActions.size() == 0) || (localActions.size() > 0 && localActions.size() == counterActiveLocalActions);
		//it must be TRUE both conditions for ActionAddress and LocalAction
		logger.debug("ruleUID: {}, activatedActionAddress: {}, activatedLocalAction: {}", getUID(), activatedActionAddress, activatedLocalAction);
		if (activatedActionAddress && activatedLocalAction) {
			logger.debug("actionAddresses.size(): {}, counterActiveActionAdresses: {}, ", actionAddresses.size(), counterActiveActionAdresses);
			isActivated = true;
		}
			
		logger.trace("returns ruleUID : {}, activated : {}", getUID(), isActivated);
		return isActivated;
	}

	/**
	 * Add new <code>ActionAddress</code> to the current <code>Rule</code>.
	 * 
	 * @param deviceUID
	 * @param deviceFunction
	 * @param propertyID
	 * @param value
	 * @return actionAddressUID, Integer
	 */
	public Integer addActionAddress(Integer deviceUID, String deviceFunction, Integer propertyID, int value) {
		ActionAddress actionAddress = new ActionAddress(this);
		actionAddress.setDeviceUID(deviceUID);
		actionAddress.setDeviceFunction(deviceFunction);
		actionAddress.setPropertyIndex(propertyID);
		actionAddress.setValue(new Integer(value));
		dataService.persistObject(actionAddress);
		
		actionAddresses.put(actionAddress.getUID(), actionAddress);
		registerForUpdate();
		logger.info("Added ActionAddress with properties deviceID {} deviceFunction {} propertyID {} value {}", deviceUID, deviceFunction,
				propertyID, value);
		new RuleActionAddressAddedEvent(actionAddress).postEvent();
		return actionAddress.getUID();
	}

	/**
	 * Adds new empty {@link ActionAddress} object to the current {@link Rule}.
	 * @return Newly created {@link ActionAddress} object.
	 */
	public ActionAddress addActionAddress() {
		ActionAddress actionAddress =  new ActionAddress(this);
		dataService.persistObject(actionAddress);
		
		actionAddresses.put(actionAddress.getUID(), actionAddress);
		registerForUpdate();
		logger.info("Added ActionAddress with UID: {}", actionAddress.getUID());
		new RuleActionAddressAddedEvent(actionAddress).postEvent(); 
		return actionAddress;
	}

	public ActionAddress getActionAddress(Integer deviceID, String deviceFunction, Integer propertyID) {
		ActionAddress actionAddress = null;
		logger.trace("deviceID: {}, deviceFunction: {}, propertyID: {}", deviceID, deviceFunction, propertyID);
		if (deviceID != null && deviceFunction != null && propertyID != null) {
			for (ActionAddress existingActionAddress : getActionAddresses()) {
				if (existingActionAddress.getDeviceUID().equals(deviceID) && existingActionAddress.getDeviceFunction().equals(deviceFunction)
						&& existingActionAddress.getPropertyIndex().equals(propertyID)) {
					actionAddress = existingActionAddress;
					break;
				}
			}
		}
		logger.trace("returns: {}", actionAddress);
		return actionAddress;
	}

	/**
	 * Remove the specified <code>ActionAddress</code> from the current <code>Rule</code>.
	 * 
	 * @param actionAddress - action address to remove
	 */
	public void removeActionAddress(ActionAddress actionAddress) {
		if (actionAddress == null) {
			return;
		}

		try {
			dataService.registerForDelete(actionAddress);
			actionAddresses.remove(actionAddress.getUID());
			registerForUpdate();
			logger.debug("Removed actionAddress with UID: {}", actionAddress.getUID());
			new RuleActionAddressRemovedEvent(actionAddress).postEvent();
		} catch (Exception e) {
			logger.error("Unable to remove actionAddress with UID: {}", actionAddress.getUID());
		}
	}

	/**
	 * Remove the specified <code>ActionAddress</code> from the current <code>Rule</code>.
	 * @param actionAddressUID - action address UID to remove
	 */
	public void removeActionAddress(Integer actionAddressUID) {
		logger.trace("({})", actionAddressUID);
		ActionAddress actionAddress = getActionAddress(actionAddressUID);
		removeActionAddress(actionAddress);
	}

	/**
	 * Return all <code>ActionAddress</code> for the current <code>Rule</code>.
	 * @return list containing all ActionAddress objects.
	 */
	public List<ActionAddress> getActionAddresses() {
		List<ActionAddress> list = new ArrayList<ActionAddress>();
		actionAddresses.forEach((k, v) -> list.add((ActionAddress) v));
		return list;
	}

	/**
	 * Returns ActionAddress object by given UID.
	 * @param actionAddressUID Integer containing ActionAddress UID
	 * @return ActionAddress object.
	 */
	public ActionAddress getActionAddress(Integer actionAddressUID) {
		logger.trace("({})", actionAddressUID);

		if (actionAddressUID == null) {
			logger.warn("ActionAddress UID is null");
			return null;
		}

		if(!actionAddresses.containsKey(actionAddressUID)) {
			logger.warn("ActionAddress with UID: {} doest not exist", actionAddressUID);
			return null;
		}

		return (ActionAddress) actionAddresses.get(actionAddressUID);
	}

	/**
	 * Adds new empty {@link RuleTrigger} object to the current Rule.
	 * @see RuleTrigger
	 * @return Newly created {@link RuleTrigger} object.
	 */
	public RuleTrigger addRuleTrigger() {
		RuleTrigger ruleTrigger = new RuleTrigger();
		ruleTrigger.setParentRule(this);
		dataService.persistObject(ruleTrigger);
		
		ruleTriggers.put(ruleTrigger.getUID(), ruleTrigger);
		registerForUpdate();
		return ruleTrigger;
	}

	/**
	 * Removes specific RuleTrigger from the current Rule.
	 * @see RuleTrigger
	 * @param ruleTriggerUID ID of the RuleTrigger object that will be removed.
	 */
	public void removeRuleTrigger(Integer ruleTriggerUID) {
		RuleTrigger ruleTrigger = getRuleTrigger(ruleTriggerUID);
		if(ruleTrigger != null) {
			ruleTriggers.remove(ruleTrigger.getUID());
			ruleTrigger.removeObject();
			dataService.registerForDelete(ruleTrigger);
			registerForUpdate();
			logger.debug("Removed RuleTrigger with UID: {}", ruleTrigger.getUID());
		}
	}

	/**
	 * Returns specific RuleTrigger object by its ID.
	 * @param ruleTriggerUID ID of the RuleTrigger object.
	 * @see RuleTrigger
	 * @return RuleTrigger object.
	 */
	public RuleTrigger getRuleTrigger(Integer ruleTriggerUID) {
		if(ruleTriggerUID == null) {
			logger.warn("RuleTrigger UID is null");
			return null;
		}

		if(!ruleTriggers.containsKey(ruleTriggerUID)) {
			logger.warn("RuleTrigger with UID: {} does not exist", ruleTriggerUID);
			return null;
		}

		return ruleTriggers.get(ruleTriggerUID);
	}

	/**
	 * Returns all RuleTrigger objects related to this Rule.
	 * @see RuleTrigger
	 * @return List of all RuleTrigger objects related to this rule.
	 */
	public List<RuleTrigger> getRuleTriggers() {
		List<RuleTrigger> list = new ArrayList<RuleTrigger>();
		ruleTriggers.forEach((k, v) -> list.add((RuleTrigger) v));
		return list;
	}

	/**
	 * This method is used only by Persistence service to manage its PersistableMap object
	 * @param ruleTriggers PersistableMap object.
	 * @see RuleTrigger
	 */
	public void setRuleTriggers(Map<Integer, RuleTrigger> ruleTriggers) {
		this.ruleTriggers = ruleTriggers;
		registerForUpdate();
	}

	/**
	 * Create a new schedule to activate the rule
	 * 
	 * @return <code>Schedule</code>
	 */
	public Integer addSchedule(int type, List<Integer> monthsOfYear, List<Integer> daysOfMonth, List<Integer> daysOfWeek, 
			ScheduleTime scheduleTime, ScheduleTime endTime) {
		logger.trace("type: {}, monthsOfYear: {},  daysOfMonth: {}, daysOfWeek: {}, scheduleTime: {}, endTime: {}", 
				type, monthsOfYear, daysOfMonth, daysOfWeek, scheduleTime, endTime);
		Schedule schedule = new Schedule(this);
		schedule.setTaskScheduler(taskScheduler);
		schedule.setDayOfMonth(daysOfMonth);
		schedule.setDayOfWeek(daysOfWeek);
		schedule.setMonthsOfYear(monthsOfYear);
		schedule.setType(type);
		schedule.setStartTime(scheduleTime);
		schedule.setEndTime(endTime);
		dataService.persistObject(schedule);
		schedules.put(schedule.getUID(), schedule);
		registerForUpdate();
		
		schedule.startTimer();
		new RuleScheduleAddedEvent(schedule).postEvent(); 
		logger.info("added newSchedule with ID: {}", schedule.getUID());
		logger.trace("returns: {}", schedule.getUID());
		return schedule.getUID();
	}

	/**
	 * Returns Schedule object by given UID.
	 * @param scheduleUID Integer containing Schedule UID.
	 * @return
	 */
	public Schedule getSchedule(Integer scheduleUID) {
		logger.trace("({})", scheduleUID);

		if(scheduleUID == null) {
			logger.warn("Schedule UID is null");
			return null;
		}

		if(!schedules.containsKey(scheduleUID) ) {
			logger.warn("Schedule with UID: {} does not exist", scheduleUID);
			return null;
		}

		return schedules.get(scheduleUID);
	}

	/**
	 * Remove an existing schedule
	 * 
	 * @param uid
	 */
	public void removeSchedule(Integer uid) {
		logger.trace("({})", uid);
		Schedule schedule = getSchedule(uid);
		if(schedule != null) {
			schedule.stopTimer();
			schedules.remove(uid);
			registerForUpdate();
			dataService.registerForDelete(schedule);
			new RuleScheduleRemovedEvent(schedule).postEvent(); 
			logger.debug("Removed schedule with UID: {}", uid);
		}
	}

	/**
	 * Cancel/stop a schedule
	 * 
	 * @param uid
	 */
	public void cancelSchedule(Integer uid) {
		logger.trace("({})", uid);
		Schedule schedule = getSchedule(uid);
		if (schedule != null) {
			schedule.stopTimer();
			logger.info("Canceled schedule {}", uid);
		} else {
			logger.warn("Schedule UID is null!");
		}
	}

	/**
	 * This method returns all Schedules.
	 * 
	 * @return <code>Schedule</code> list
	 */
	public List<Schedule> getSchedules() {
		List<Schedule> list = new ArrayList<Schedule>();
		schedules.forEach((k, v) -> list.add((Schedule) v));
		return list;
	}

	/**
	 * Adds new {@link LocalAction} object to the current {@link Rule}
	 * @param executableFunctionID Integer containing ExecutableFunction ID
	 * @see {@link ExecutableFunctionType}
	 * @return Integer containing new LocalAction UID
	 */
	public Integer addLocalAction(Integer executableFunctionID) {
		return addLocalAction(ExecutableFunctionType.valueOf(executableFunctionID));
	}

	/**
	 * Adds new {@link LocalAction} object to the current {@link Rule}
	 * @param type Type of the local function.
	 * @see {@link ExecutableFunctionType}
	 * @return Integer containing new LocalAction UID
	 */
	public Integer addLocalAction(ExecutableFunctionType type) {
		LocalAction localAction = new LocalAction(this);
		localAction.setFunction(type);
		dataService.persistObject(localAction);

		localActions.put(localAction.getUID(), localAction);
		registerForUpdate();

		logger.info("Added LocalAction: {}", localAction);
		new RuleLocalActionAddedEvent(localAction).postEvent(); 
		return localAction.getUID();
	}

	/**
	 * Returns all {@link LocalAction} objects added to the current {@link Rule}
	 * @return list List Containing LocalAction objects
	 */
	public List<LocalAction> getLocalActions() {
		List<LocalAction> list = new ArrayList<LocalAction>();
		for(Integer localActionUID : localActions.keySet()) {
			list.add(getLocalAction(localActionUID));
		}
		logger.debug("Returning all LocalAction objects: {}", localActions.keySet());

		return list;
	}
	
	/**
	 * Returns {@link LocalAction} object by given UID.
	 * @param uID Integer containing LocalAction`s unique ID
	 * @return LocalAction object or null (if it does not exist)
	 */
	public LocalAction getLocalAction(Integer uID) {
		if(uID == null) {
			logger.warn("LocalAction UID is null");
			return null;
		}

		if(!localActions.containsKey(uID)) {
			logger.warn("LocalAction with UID: {} does not exist", uID);
			return null;
		}

		return localActions.get(uID);
	}
	
	/**
	 * Returns {@link LocalAction} object by given LocalFunctionType.
	 * @param type {@link ExecutableFunctionType} containing type of the local function.
	 * @return LocalAction object or null (if there is no local function from given type)
	 */
	public LocalAction getLocalAction(ExecutableFunctionType type) {
		LocalAction searchedLocalAction = null;

		for(LocalAction localAction : getLocalActions()) {
			if(localAction.getFunction() != null && localAction.getFunctionID().equals(type.getValue())) {
				searchedLocalAction = localAction;
				break;
			}
		}
		return searchedLocalAction;
	}
	
	/**
	 * Removes {@link LocalAction} object by given UID.
	 * @param localActionUID
	 */
	public void removeLocalAction(Integer localActionUID) {
		final LocalAction localAction = getLocalAction(localActionUID);
		if(localAction == null) {
			return;
		}

		localActions.remove(localAction.getUID());
		registerForUpdate();
		dataService.registerForDelete(localAction);
		new RuleLocalActionRemovedEvent(localAction).postEvent(); 
		logger.debug("Removed LocalAction with UID: {}", localAction.getUID());
	}

	/**
	 * Set the name of the Rule.
	 *
	 * @param name
	 *            the rule name.
	 */
	public void setName(String name) {
		this.name = name;
		registerForUpdate();
	}

	/**
	 * This method returns the user-friendly name of <code>Rule</code>.
	 * 
	 * @return the last known value for the <code>Rule</code> name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * This method checks whether current rule is allowed to be executed.</br>
	 * Checks are: </br>
	 * * Is manual execution and isManually flag is set to 'true'.</br>
	 * * Is system in vacation mode and rule is enabled on vacation.</br>
	 * * Is rule enabled. </br>
	 * @param manualExecution - Flag showing whether the execution is manual or from schedule.
	 * @return True if the rule is allowed to be executed, false otherwise
	 */
	private boolean isAllowedToBeExecuted(final boolean manualExecution) {
		if(manualExecution && !isManually().booleanValue()) {
			logger.info("Rule will not be executed, because manual execution is not allowed");
			return false;
		}

		final SystemService systemService = Activator.getService(SystemService.class);
		if(systemService == null) {
			logger.warn("Rule will not be executed, becacause SystemService cannot be obtained");
			return false;
		}

		// Prevent rule execution on vacation mode
		boolean dontRunOnVacation = systemService.isVacationMode() && !isEnabledOnVacation();
		if(dontRunOnVacation) {
			logger.info("Rule will not be executed, because it is forbidden to run in vacation mode");
			return false;
		}

		if (!isEnabled()) {
			logger.info("Rule will not be executed, because it is not enabled");
			return false;
		}

		return true;
	}

	/**
	 * Schedules a rule for execution.</br>
	 * Rule is executed immediately in separated thread managed by TaskScheduler sub-system.</br>
	 * <b>NOTE: When executing a rule manually, this method should be used and NOT this one {@link #execute(boolean)}.</b></br>
	 */
	public boolean execute() {
		if(!isAllowedToBeExecuted(true)) {
			return false;
		}

		final String taskID = getUID().toString() + ",duration:" + String.valueOf(duration);
		final ScheduledTask task = new SingleTask(taskID);
		task.setExecutable(this);      
		task.setDuration(duration);
		taskScheduler.add(task);
		return true;
	}

	@Override
	public void execute(boolean reverseExecution) {
		if(!isAllowedToBeExecuted(false)) {
			return;
		}

		executeActionAddresses(reverseExecution);
		executeLocalActions(reverseExecution);

		if(reverseExecution) {
			logger.debug("Reversely executed rule with UID: ", getUID());
		} else {
			/* Send Risco event */
			new RuleExecutedEvent(this).postEvent(); 
			logger.debug("Executed rule with UID: ", getUID());
		}
	}

	private void executeActionAddresses(final boolean reverseExecution) {
		if (actionAddresses == null) {
			return;
		}
		final DeviceService deviceService = Activator.getService(DeviceService.class);
		if(deviceService == null) {
			logger.error("DeviceService is null");
			return;
		}
		for (ActionAddress actionAddress : getActionAddresses()) {
			Integer deviceUID = actionAddress.getDeviceUID();
			String deviceFunction = actionAddress.getDeviceFunction();
			Integer deviceProperty = actionAddress.getPropertyIndex();
			Object value = actionAddress.getValue();
			Device device = deviceService.getDevice(deviceUID);
			if (value == null || device == null) {
				logger.warn("Skipping action {} for rule {}", actionAddress, getUID());
				continue;
			}

			DeviceFunction function = device.getFunction(deviceFunction);
			if (function != null) {
				List<DeviceProperty> propertyList = function.getDeviceProperties();
				if (propertyList != null) {
					for (DeviceProperty property : propertyList) {
						if (property != null && property.getIndex().equals(deviceProperty)) {
							if (reverseExecution) {
								setValues(property, actionAddress.getEndValue());
							} else {
								//If there is no end valued configured, use the previous value as the end value.
								if (actionAddress.getEndValue() == null) {
									actionAddress.setEndValue(Integer.parseInt( property.getValue().toString()));
								}
								setValues(property, value);
							}
							logger.info("Executing the rule for device {} function {} property {} with a value {} ", deviceUID.intValue(),
								deviceFunction, deviceProperty, value);
						}
					}
				}
				function.sendDevicePropertyValues();
				if (executionInterval != null && executionInterval > 0) {
					try {
						Thread.sleep(executionInterval.longValue());
					} catch (InterruptedException e) {
						logger.warn("Sleep between remote executions was interrupted");
					}
				}
			}
		}
	}

	private void executeLocalActions(final boolean reverseExecution) {
		logger.trace("Executing LocalAction Fcuntion");
		for(LocalAction localAction : getLocalActions()) {
			if(localAction.getFunction() != null) {
				localAction.getFunction().execute(reverseExecution);

				if(executionInterval != null && executionInterval > 0) {
					try {
						Thread.sleep(executionInterval.longValue());
					} catch (InterruptedException e) {
						logger.warn("Sleep between local executions was interrupted");
					}
				}	
			}
		}
	}

	/**
	 * Set current and previous value to Device property. If any of 2 parameters is null it will not execute anything
	 * @param Device property
	 * @param Current value
	 */
	private void setValues(DeviceProperty property, Object value) {
		logger.trace("(property: {}, Value: {})", property, value);
		if (property == null || value == null) return;
		property.setTargetValue(value);
	}
	
	/**
	 * Check if the <code>Rule</code> is enable.
	 * 
	 * @return true, if it is isEnabled
	 */
	public boolean isEnabled() {
		boolean enabled = (isEnabled == null) ? false : isEnabled.booleanValue();
		logger.trace("returns: {}", enabled);
		return enabled;
	}
	
	/**
	 * Change the enable state of the rule. The rules can be enable or disable.
	 * When the rule is not enable it will not execute the command when the it is triggered.
	 * 
	 * @param isEnabled - is the new enable state
	 */
	public void setEnabled(Boolean isEnabled) {
		if(this.isEnabled() != isEnabled.booleanValue()) {
			this.isEnabled = isEnabled.booleanValue();
			registerForUpdate();
		}
	}
	
	/**
	 * Check if the the rule is in vacation mode.
	 * 
	 * @return true if the rule is on vacation mode
	 */
	public Boolean isEnabledOnVacation() {
		boolean isEnabledOnVacationMode = (enabledOnVacation == null) ? 
				false : enabledOnVacation.booleanValue();
		logger.trace("returns {}", isEnabledOnVacationMode);
		return isEnabledOnVacationMode;
	}

	/**
	 * Set if the rule should be enabled on vacation mode or not.
	 * 
	 * @param isVacationMode - enabled on vacation mode or not
	 */
	public void setEnabledOnVacation(Boolean isVacationMode) {
		if (enabledOnVacation == null || !enabledOnVacation.equals(isVacationMode)) {
			enabledOnVacation = isVacationMode;
			registerForUpdate();
		}
	}
	
	/**
	 * Check if the <code>Rule</code> can be activated manually.
	 * 
	 * @return true, if it can activate manually
	 */
	public Boolean isManually() {
		boolean manually = (manuallyExecutable == null) ? 
				false : manuallyExecutable.booleanValue();
		logger.trace("returns: {}", manually);
		return manually;
	}

	/**
	 * Define if the <code>Rule</code> can be executed manually.
	 * 
	 * @param isManually
	 *            the new manually state
	 */
	public void setManually(Boolean isManually) {
		logger.trace("({})", isManually);
		if (manuallyExecutable == null || !manuallyExecutable.equals(isManually)) {
			manuallyExecutable = isManually;
			registerForUpdate();
			new RuleSetManuallyEvent(this).postEvent();
		}
	}

	public Long getExecutionInterval() {
		return executionInterval;
	}

	public void setExecutionInterval(Long executionInterval) {
		this.executionInterval = executionInterval;
		registerForUpdate();
	}
	
	/**
	 * This method is used only by Persistence service to manage its PersistableMap object
	 * @param actionAddresses PersistableMap object
	 */
	public void setActionAdresses(Map<Integer, ActionAddress> actionAddresses) {
		this.actionAddresses = actionAddresses;
		registerForUpdate();
	}
	
	/**
	 * This method is used only by Persistence service to manage its PersistableMap object
	 * @param localActions PersistableMap object
	 */
	public void setLocalActions(Map<Integer, LocalAction> localActions) {
		this.localActions = localActions;
		registerForUpdate();
	}

	/**
	 * This method is used only by Persistence service to manage its PersistableMap object
	 * @param schedules PersistableMap object
	 */
	public void setSchedules(Map<Integer, Schedule> schedules) {
		this.schedules = schedules;
		registerForUpdate();
	}
	
	public Long getDuration() {
		logger.trace("returns {}" , duration);
		return duration;
	}

	public void setDuration(Long duration) {
		if (duration > 0) {
			this.duration = duration;
			registerForUpdate();
		}				
	}

	@Override
	public String toString() {
		return "Rule Id: " + getUID() + ", Name: " + getName()
		+ ", isManually: " + isManually() + ", isActivated: " + isActivated() + ", isEnabled: " + isEnabled() + ", isEnabledOnVacation: " + isEnabledOnVacation()
		+ ", Duration: " + getDuration() + ", schedules: {}" + schedules.keySet() 
		+ ", actionAddresses: " + actionAddresses.keySet() + ", localActions: " + localActions.keySet();
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
}
