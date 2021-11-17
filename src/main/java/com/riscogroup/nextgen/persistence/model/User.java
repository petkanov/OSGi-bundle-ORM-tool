package com.riscogroup.nextgen.home.api.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.Activator;
import com.riscogroup.nextgen.home.api.services.DataService;
import com.riscogroup.nextgen.home.api.tools.ArrayUtils;

public class User {
	private final static Logger logger = LoggerFactory.getLogger(User.class);
	public static final int MAX_NUMBER = 10*1000; // max number of users 
	
	private static final Set<Integer> superUserRoles = new HashSet<>();
	{
		superUserRoles.add(UserRole.GRANDMASTER.getValue());
		superUserRoles.add(UserRole.INSTALLER.getValue());
		superUserRoles.add(UserRole.SUB_INSTALLER.getValue());
		superUserRoles.add(UserRole.MANUFACTURER.getValue());
	}
	
	private Integer uID;
	private String username;
	private String userLabel;
	//@Transient
	private String password;
	//@Transient
	private String duressPassword;
	private String passwordHash;
	private String duressPasswordHash;
	private String authenticationHash; //Used to verify the user's password according to the authentication method.
	private String duressAuthenticationHash; //Used to verify the user's duress password according to the authentication method.
	private Integer role;
	private boolean active = true;
	private List<Integer> groups;
	private Long userExpirationTime;
	private String userAudioDescription;
	// User Language, string from list of supported
	private String userLanguage;
	private boolean duressAlaram;
	private final UserTimeout userTimeout;
	private final Map<Integer, Boolean> nextOperationDeactivationMap;
	private DataService dataService;
	
	public User() {
		groups = new ArrayList<Integer>();
		userTimeout = new UserTimeout();
		nextOperationDeactivationMap = new ConcurrentHashMap<Integer, Boolean>();
	}

	public User(Integer uID) {
		this();
		this.uID = uID;
	}

	public Integer getUID() {
		return uID;
	}

	public void setUserId(int userId) {
		this.uID = userId;
	}
	
	public void activateTimeout(final long timeout) {
		this.userTimeout.activateTimeout(timeout);
	}
	
	public boolean isTimeoutOver() {
		return this.userTimeout.isOver();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
		registerForUpdate();
	}

	public void setDuressAlarm(boolean duressAlaram) {
		if(this.duressAlaram != duressAlaram) {
			this.duressAlaram = duressAlaram;
			registerForUpdate();
		}
	}

	public Boolean getDuressAlarm() {
		return duressAlaram;
	}

	/**
	 * Decides whether current user belongs to the Super Users set
	 * which contains Manufacture, Sub-Installer, Installer and Grandmaster.
	 * Only these users do not have any groups assigned to them, but have rights 
	 * to rule them all
	 * @return true if he is
	 */
	public boolean isSuperUser() {
		return superUserRoles.contains(role);
	}
	
	public void setUserLabel(String label) {
		userLabel = label;
		registerForUpdate();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDuressPassword() {
		return duressPassword;
	}

	public void setDuressPassword(String duressPassword) {
		this.duressPassword = duressPassword;
	}

	public String getUserLabel() {
		return userLabel;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		logger.debug("Setting new password for " + username);
		this.passwordHash = passwordHash;
		registerForUpdate();
	}

	public String getDuressPasswordHash() {
		return duressPasswordHash;
	}

	public void setDuressPasswordHash(String duressPasswordHash) {
		logger.debug("Setting new duress password for " + username);
		this.duressPasswordHash = duressPasswordHash;
		registerForUpdate();
	}

	/**
	 * Returns the duress authentication hash.
	 * To validate the user the authenticator must verify the password. The password
	 * is not saved in plain text on the GW to prevent an attacker to read it. every authentication method
	 * has it's own way how to hash the password and validate it later. for example on our current
	 * authentication method (digest) it's username:releam:password in MD5.
	 *   
	 * @return String with the duress authentication hash
	 */
	public String getDuressAuthenticationHash() {
		return duressAuthenticationHash;
	}

	public void setDuressAuthenticationHash(String duressAuthenticationHash) {
		this.duressAuthenticationHash = duressAuthenticationHash;
		registerForUpdate();
	}

	/**
	 * Returns the authentication hash.
	 * To validate the user the authenticator must verify the password. The password
	 * is not saved in plain text on the GW to prevent an attacker to read it. every authentication method
	 * has it's own way how to hash the password and validate it later. for example on our current
	 * authentication method (digest) it's username:releam:password in MD5.
	 *   
	 * @return String with the authentication hash
	 */
	public String getAuthenticationHash() {
		return authenticationHash;
	}

	public void setAuthenticationHash(String authenticationHash) {
		this.authenticationHash = authenticationHash;
		registerForUpdate();
	}
	
	public Integer getRole() {
		return role;
	}

	public void setRole(Integer role) {
		this.role = role;
		registerForUpdate();
	}

	public void setGroups(List<Integer> groups) {
		this.groups = groups;
		registerForUpdate();
	}

	public List<Integer> getUserGroups() {
		return Collections.unmodifiableList(groups);
	}

	public void removeUserGroup(Integer groupUID) {
		groups.remove(groupUID);
		registerForUpdate();
	}
	
	public void addUserGroup(Integer groupUID) {
		if(!groups.contains(groupUID)) {
			groups.add(groupUID);
			registerForUpdate();
		}
	}

	/** Updates the list of the users groups 
	 * @param groupList the updated group list
	 */
	public void updateGroups(final List<Integer> groupList) {
		final List<Integer> groups = getUserGroups();
		final List<Integer> removed = ArrayUtils.removeAll(groups, groupList);
		final List<Integer> added = ArrayUtils.removeAll(groupList, groups);

		for(Integer groupUID : removed) {
			removeUserGroup(groupUID);
		}

		for(Integer groupUID : added) {
			addUserGroup(groupUID);
		}	
	}


	public Long getUserExpirationTime() {
		return userExpirationTime;
	}

	public void setUserExpirationTime(Long userExpirationTime) {
		this.userExpirationTime = userExpirationTime;
		registerForUpdate();
	}

	public String getUserAudioDescription() {
		return userAudioDescription;
	}

	public void setUserAudioDescription(String userAudioDescription) {
		this.userAudioDescription = userAudioDescription;
		registerForUpdate();
	}

	public String getUserLanguage() {
		return userLanguage;
	}

	public void setUserLanguage(String userLanguage) {
		this.userLanguage = userLanguage;
		registerForUpdate();
	}
	
	/**
	 * get active flag status, it's used for Cleaner/Maid feature. Each code is being deactivated if it's used once
	 * @return active flag
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * set active user flag, by default user is active
	 * @param active
	 */
	public void setActive(Boolean active) {
		if(this.active != active) {
			this.active = active;
			registerForUpdate();
		}
	}
	
	/**
	 * If this property is set to true, the user will be deactivated in the next operation. 
	 * Currently arm operation is the only operation that will deactivate the user
	 * @param groupUID
	 * @return nextOperationDeactivation flag
	 */
	public boolean getNextOperationDeactivation(final Integer groupUID) {
		return nextOperationDeactivationMap.containsKey(groupUID) ? nextOperationDeactivationMap.get(groupUID).booleanValue() : false; 
	}

	/**
	 * Sets user's nextOperationDeactivation flag for specific group. 
	 * @param groupUID - ID of the group to which flag will be set
	 * @param nextOperationDeactivation - Boolean value showing whether the user will be marked for deactivation for given group or not. 
	 */
	public void setNextOperationDeactivation(final Integer groupUID, final Boolean nextOperationDeactivation) {
		if(groupUID != null && nextOperationDeactivation != null) {
			nextOperationDeactivationMap.put(groupUID, nextOperationDeactivation);	
			registerForUpdate();
		}
	}
	
	/**
	 * Returns NextOperationDeactivation map.
	 * @return Map<Integer, Boolean> object
	 */
	public Map<Integer, Boolean> getNextOperationDeactivationMap() {
		return nextOperationDeactivationMap;
	}

	/**
	 * Clears Next operation deactivation map.
	 */
	public void clearNextOperationDeactivationMap() {
		nextOperationDeactivationMap.clear();
	}

	/**
	 * Returns whether the current user has permission to arm or disarm the given group.
	 * @param groupID ID of the group.
	 * @param armState ArmState
	 * @return True if the user has rights to arm or disarm the given group, otherwise false
	 */
	public boolean hasPermissionToArmOrDisarmPartition(Integer groupID, ArmState armState) {
		/* MASTER user can disarm any partition */
		if(role == UserRole.GRANDMASTER.getValue()) {
			return true;
		} else if(role == UserRole.ARMER.getValue()) {
			return armState != null && (armState == ArmState.ARMED_STAY || armState == ArmState.ARMED_AWAY) && groups.contains(groupID);
		} else {
			return groups.contains(groupID);
		}
	}
	
	private void registerForUpdate() {
		if(dataService == null) {
			dataService = Activator.getService(DataService.class); 
		}
		dataService.registerForUpdate(this);
	}

	@Override
	public String toString() {
		return "User [uID=" + uID + ", username=" + username + ", userLabel=" + userLabel + ", passwordHash="
				+ passwordHash + ", duressPasswordHash=" + duressPasswordHash + ", authenticationHash="
				+ authenticationHash + ", duressAuthenticationHash=" + duressAuthenticationHash + ", role=" + role
				+ ", active=" + active + ", nextOperationDeactivation=" + nextOperationDeactivationMap + ", groups="
				+ groups + ", userExpirationTime=" + userExpirationTime + ", userAudioDescription="
				+ userAudioDescription + ", userLanguage=" + userLanguage + ", duressAlaram="
				+ duressAlaram + ", userTimeout=" + userTimeout + "]";
	}

}
