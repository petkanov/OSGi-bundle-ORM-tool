package com.riscogroup.nextgen.home.api.rule.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.events.generic.NotificationEvent;
import com.riscogroup.nextgen.home.api.events.generic.RiscoEvent;
import com.riscogroup.nextgen.home.api.events.generic.RuleTriggeringEvent;
import com.riscogroup.nextgen.home.api.events.generic.RuleTriggeringEventImpl;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.home.api.services.GroupService;
import com.riscogroup.nextgen.home.api.services.SystemService;

public class RuleTrigger implements EventHandler {
	private static final Logger logger = LoggerFactory.getLogger(RuleTrigger.class);

	private Integer uID;
	private Integer eventType;
	private ActionAddress actionAddress;
	private List<Integer> groupUIDs;
	private Integer eventStatus;
	private Rule parent;
	private ServiceRegistration<?> serviceRegistration;
	private Boolean processSystemEvents;
	private DataService dataService;

	public RuleTrigger(Integer uid) {
		this.uID = uid;
	}

	public RuleTrigger() {
		groupUIDs = new ArrayList<Integer>();
		processSystemEvents = true;
		serviceRegistration = registerToEventHandler();
	}

	public Integer getUID() {
		return this.uID;
	}
	
	public void setUID(Integer uID) {
		this.uID = uID;
	}

	public void setParentRule(Rule parent) {
		this.parent = parent;
	}

	public void setActionAddress(ActionAddress actionAddress) {
		this.actionAddress = actionAddress;
	}

	/**
	 * Register RuleManager object to EventHandler
	 */
	private ServiceRegistration<?> registerToEventHandler() {
		final Dictionary<String, String[]> props = new Hashtable<String, String[]>();
		final String[] topics = new String[] {RiscoEvent.getTopic(), RuleTriggeringEventImpl.TOPIC};
		props.put(EventConstants.EVENT_TOPIC, topics);
		final BundleContext context = Activator.getBundleContext();
		return context.registerService(EventHandler.class.getName(), this, props);
	}

	/**
	 * Returns event type.
	 * @return Integer containing event type.
	 */
	public Integer getEventType() {
		return eventType;
	}

	/**
	 * Sets event type.
	 * @param eventType Integer containing event type.
	 */
	public void setEventType(Integer eventType) {
		this.eventType = eventType;
		registerForUpdate();
	}

	/**
	 * Returns ActionAddress object which is executed when this trigger is run.
	 * @see ActionAddress
	 * @return ActionAddress object.
	 */
	public ActionAddress getActionAddress() {
		return actionAddress;
	}

	/**
	 * Adds ActionAddress to current RuleRrigger.
	 * @param deviceUID
	 * @param deviceFunction
	 * @param propertyID
	 * @param value
	 */
	public void addActionAddress(Integer deviceUID, String deviceFunction, Integer propertyID, int value) {
		/* Check whether this RuleTrigger has already ActionAddress and if so - remove it */
		try {
			removeActionAddress();
		} catch (SQLException e) {
			logger.warn("Unable to remove an old action address. Reason: {}", e.getMessage());
		}

		actionAddress = new ActionAddress(this);
		actionAddress.setDeviceUID(deviceUID);
		actionAddress.setDeviceFunction(deviceFunction);
		actionAddress.setPropertyIndex(propertyID);
		actionAddress.setValue(new Integer(value));
	}

	/**
	 * Adds empty {@link ActionAddress} object to the current RuleRrigger.
	 * @return Newly created {@link ActionAddress} object.
	 */
	public ActionAddress addActionAddress() {
		/* Check whether this RuleTrigger has already ActionAddress and if so - remove it */
		try {
			removeActionAddress();
		} catch (SQLException e) {
			logger.warn("Unable to remove an old action address. Reason: {}", e.getMessage());
		}

		actionAddress = new ActionAddress(this);

		return actionAddress;
	}

	/**
	 * Removes ActionAddress from RuleTrigger.
	 * @see ActionAddress
	 * @throws Exception
	 */
	public void removeActionAddress() throws SQLException {
		if(actionAddress != null) {
			actionAddress = null;
		}
	}

	/**
	 * Returns a list of groups IDs related to this trigger.
	 * @return List containing group IDs.
	 */
	public List<Integer> getGroupUIDs() {
		return groupUIDs;
	}

	/**
	 * Sets a list of group IDs related to this trigger
	 * @param groupUIDs List containing group ID.
	 */
	public void setGroupUIDs(List<Integer> groupUIDs) {
		this.groupUIDs = groupUIDs;
		registerForUpdate();
	}

	/**
	 * Sets event status.
	 * Event status can be either EVENT_TRIGGERED or EVENT_RESTORED.
	 * @see {@link com.riscogroup.nextgen.home.api.events.intrusion.IntrusionEvent#EVENT_TRIGGERED}
	 * @see {@link com.riscogroup.nextgen.home.api.events.intrusion.IntrusionEvent#EVENT_RESTORED}
	 * @param status - Integer containing event status
	 */
	public void setEventStatus(Integer status) {
		this.eventStatus = status;
		registerForUpdate();
	}

	/**
	 * Returns event status.
	 * @see {@link #setEventStatus(Integer)}
	 * @return - Integer containing event status
	 */
	public Integer getEventStatus() {
		return eventStatus;
	}

	public void removeObject() {
		// IMPORTANT: unregsitering the service is very important before removing the object.
		// If the object is not unregistered, it stays in memory (eventhough that it is deleted from DB and there are no references to it)
		// and continues to handle and process events. 
		if(serviceRegistration != null) {
			serviceRegistration.unregister();
			serviceRegistration = null;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if(!validateEventProcessing()) {
			return;
		}

		RuleTriggerInfo ruleTriggerInfo = null;
		if(event.getTopic().equals(RiscoEvent.getTopic())) {
			try {
				final RiscoEvent riscoEvent = RiscoEvent.fromOSGiEvent(event);
				if(riscoEvent instanceof RuleTriggeringEvent) {
					ruleTriggerInfo = ((RuleTriggeringEvent) riscoEvent).getRuleTriggerInfo();
				}
			} catch (Exception e) {
				logger.warn("Unable to handle RiscoEvent");
				return;
			}
		} else if(event.getTopic().equals(RuleTriggeringEventImpl.TOPIC)) {
			final RuleTriggeringEventImpl ruleTriggerEvent = NotificationEvent.fromOSGiEvent(event, RuleTriggeringEventImpl.class);
			if(ruleTriggerEvent == null) {
				logger.warn("Unable to handle RuleTriggeringEvent");
				return;
			}
			ruleTriggerInfo = ((RuleTriggeringEvent) ruleTriggerEvent).getRuleTriggerInfo();
		}

		processForExecution(ruleTriggerInfo);
	}

	private static boolean validateEventProcessing() {
		final SystemService systemService = Activator.getService(SystemService.class);
		if(systemService == null || systemService.isSpecialModeActive()) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether related group matches with one in the list.
	 * @param groupUID - Group UID to be cheched.
	 * @return True if related group matches, otherwise false.
	 */
	private boolean doesGroupUIDMatch(final Integer groupUID) {
		final GroupService groupService = Activator.getService(GroupService.class);
		if(groupService == null) {
			logger.warn("Unable to obtain GroupService");
			return false;
		}

		final Integer systemGroupUID = groupService.getSystemGroup().getUID();
		if(systemGroupUID.equals(groupUID)) {
			return processSystemEvents;
		}

		if(groupUIDs == null || groupUIDs.isEmpty()) {
			return false;
		}

		return groupUIDs.contains(groupUID);
	}

	/**
	 * Processes rule triggering information and decides whether to execute the related rule.
	 * @param ruleTriggerInfo - RuleTriggerInfo object
	 */
	private void processForExecution(final RuleTriggerInfo ruleTriggerInfo) {
		if(parent == null || ruleTriggerInfo == null) {
			return;
		}

		final boolean shouldExecute = 
				(eventType == null ? ruleTriggerInfo.getEventType() == null : eventType.equals(ruleTriggerInfo.getEventType())) &&
				(actionAddress == null ? ruleTriggerInfo.getActionAddress() == null : actionAddress.equals(ruleTriggerInfo.getActionAddress())) &&
				(eventStatus == null ? ruleTriggerInfo.getEventStatus() == null : eventStatus.equals(ruleTriggerInfo.getEventStatus())) &&
				(doesGroupUIDMatch(ruleTriggerInfo.getGroupUID()));

		if(shouldExecute) {
			parent.execute();
			logger.info("Executed rule trigger with UID: {}", getUID());
		}
	}

	/**
	 * Returns process system events flag.
	 * @return True if process system events flag is set, false otherwise.
	 */
	public Boolean getProcessSystemEvents() {
		return processSystemEvents;
	}

	/**
	 * Sets process system events flag.
	 * @param processSystemEvents
	 */
	public void setProcessSystemEvents(Boolean processSystemEvents) {
		if(!this.processSystemEvents.equals(processSystemEvents)) {
			this.processSystemEvents = processSystemEvents;
			registerForUpdate();
		}
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

	public Rule getParentRule() {
		return this.parent;
	}
}
