package com.riscogroup.nextgen.persistence.repository.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.generic.User;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class UserDAO extends DataAccessObject<User> {
	private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

	@Override
	public boolean persist(User user) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder query = new StringBuilder();
		query.append("INSERT INTO home_automation._user ")
				.append("(name, label, password_hash, authentication_hash, duress_authentication_hash, role,")
				.append("user_expiration_time, user_audio_description, user_language, is_active, is_duress_alaram,")
				.append("duress_password_hash) ").append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

		PreparedStatement stmnt = conn.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, user.getUsername() != null ? user.getUsername() : "");
		stmnt.setString(2, user.getUserLabel() != null ? user.getUserLabel() : "");
		stmnt.setString(3, user.getPasswordHash() != null ? user.getPasswordHash() : "");
		stmnt.setString(4, user.getAuthenticationHash() != null ? user.getAuthenticationHash() : "");
		stmnt.setString(5, user.getDuressAuthenticationHash() != null ? user.getDuressAuthenticationHash() : "");
		stmnt.setInt(6, user.getRole() != null ? user.getRole() : 0);
		stmnt.setLong(7, user.getUserExpirationTime() != null ? user.getUserExpirationTime() : 0L);
		stmnt.setString(8, user.getUserAudioDescription() != null ? user.getUserAudioDescription() : "");
		stmnt.setString(9, user.getUserLanguage() != null ? user.getUserLanguage() : "");
		stmnt.setBoolean(10, user.getActive());
		stmnt.setBoolean(11, user.getDuressAlarm());
		stmnt.setString(12, user.getDuressPasswordHash() != null ? user.getDuressPasswordHash() : "");

		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int lastInsertedUserId = rs.getInt(1);
		rs.close();
		stmnt.close();
		user.setUserId(lastInsertedUserId);

		if (user.getUserGroups() != null && user.getUserGroups().size() > 0) {
			final String insert = getInsertIntoPivotTableStatement("user_to_group", "group_id", user.getUID(),
					user.getUserGroups());
			stmnt = conn.prepareStatement(insert);
			stmnt.executeUpdate();
			stmnt.close();
		}

		persistNextOperationDeactivationMap(user.getUID(), user.getNextOperationDeactivationMap());

		return true;
	}

	@Override
	public boolean update(User user) throws SQLException { // can be optimized to update only changed fields
		final Connection conn = getTransactionManager().getConnection();

		if (user.getUID() == null) {// Object was created and filled with data before being persisted
			persist(user);
			return true;
		}

		final StringBuilder query = new StringBuilder();
		query.append("UPDATE home_automation._user SET ").append(
				"name=?, label=?, password_hash=?, authentication_hash=?, duress_authentication_hash=?, role=?,")
				.append("user_expiration_time=?, user_audio_description=?, user_language=?, is_active=?, is_duress_alaram=?,")
				.append("duress_password_hash=? ").append("WHERE id=?");

		PreparedStatement stmnt = conn.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, user.getUsername() != null ? user.getUsername() : "");
		stmnt.setString(2, user.getUserLabel() != null ? user.getUserLabel() : "");
		stmnt.setString(3, user.getPasswordHash() != null ? user.getPasswordHash() : "");
		stmnt.setString(4, user.getAuthenticationHash() != null ? user.getAuthenticationHash() : "");
		stmnt.setString(5, user.getDuressAuthenticationHash() != null ? user.getDuressAuthenticationHash() : "");
		stmnt.setInt(6, user.getRole() != null ? user.getRole() : 0);
		stmnt.setLong(7, user.getUserExpirationTime() != null ? user.getUserExpirationTime() : 0L);
		stmnt.setString(8, user.getUserAudioDescription() != null ? user.getUserAudioDescription() : "");
		stmnt.setString(9, user.getUserLanguage() != null ? user.getUserLanguage() : "");
		stmnt.setBoolean(10, user.getActive());
		stmnt.setBoolean(11, user.getDuressAlarm());
		stmnt.setString(12, user.getDuressPasswordHash() != null ? user.getDuressPasswordHash() : "");
		stmnt.setInt(13, user.getUID());
		stmnt.executeUpdate();
		stmnt.close();

		deleteUserPivotTableAssosiations(user.getUID());
		deleteNextOperationDeactivationMap(user.getUID());

		if (user.getUserGroups() != null && user.getUserGroups().size() > 0) {
			final String insert = getInsertIntoPivotTableStatement("user_to_group", "group_id", user.getUID(),
					user.getUserGroups());
			stmnt = conn.prepareStatement(insert);
			stmnt.executeUpdate();
			stmnt.close();
		}

		persistNextOperationDeactivationMap(user.getUID(), user.getNextOperationDeactivationMap());

		return true;
	}

	private List<Integer> getUserAggregationsFromPivotTable(String tableName, String columnName, int userId)
			throws SQLException {
		final String query = "SELECT * FROM home_automation." + tableName + " WHERE user_id=?";
		final Connection connection = getTransactionManager().getConnection();
		final List<Integer> items = new ArrayList<>();
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, userId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					items.add(resultSet.getInt(columnName));
				}
			}
		}
		return items;
	}

	protected void initializeObjectWithDatabaseData(User user) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();

		final String query = "SELECT * FROM home_automation._user WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, user.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				if (resultSet.next()) {
					user.setUserId(resultSet.getInt("id"));
					user.setUsername(resultSet.getString("name"));
					user.setUserLabel(resultSet.getString("label"));
					user.setPasswordHash(resultSet.getString("password_hash"));
					user.setAuthenticationHash(resultSet.getString("authentication_hash"));
					user.setDuressAuthenticationHash(resultSet.getString("duress_authentication_hash"));
					user.setRole(resultSet.getInt("role"));
					user.setActive(resultSet.getBoolean("is_active"));
					user.setUserExpirationTime(resultSet.getLong("user_expiration_time"));
					user.setUserAudioDescription(resultSet.getString("user_audio_description"));
					user.setUserLanguage(resultSet.getString("user_language"));
					user.setDuressAlarm(resultSet.getBoolean("is_duress_alaram"));
					user.setDuressPasswordHash(resultSet.getString("duress_password_hash"));
				} else {
					logger.error("User {} does not exist", user.getUID());
					throw new SQLException();
				}
			}
		}
		user.setGroups(getUserAggregationsFromPivotTable("user_to_group", "group_id", user.getUID()));

		final Map<Integer, Boolean> nextOperationDeactivationMap = getNextOperationDeactivationMap(user.getUID());
		for(Integer groupUID : nextOperationDeactivationMap.keySet()) {
			user.setNextOperationDeactivation(groupUID, nextOperationDeactivationMap.get(groupUID));
		}
	}

	@Override
	public boolean delete(Integer userId) throws SQLException {
		getCache().remove(userId);
		final Connection connection = getTransactionManager().getConnection();
		String query = "DELETE FROM home_automation._user WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, userId);
			stmnt.executeUpdate();
		}
		deleteUserPivotTableAssosiations(userId);
		return true;
	}

	@Override
	public boolean delete(User obj) throws SQLException {
		return delete(obj.getUID());
	}

	private void persistNextOperationDeactivationMap(final int userId, final Map<Integer, Boolean> items) {
		if (items == null || items.isEmpty()) {
			return;
		}

		final Connection connection = getTransactionManager().getConnection();
		for (Integer groupUID : items.keySet()) {
			final StringBuilder query = new StringBuilder();
			query.append(
					"INSERT INTO home_automation.user_to_next_operation_deactivation (user_id, group_id, next_operation_deactivation) VALUES ");
			query.append("(");
			query.append(userId);
			query.append(",");
			query.append(groupUID);
			query.append(",");
			query.append(items.get(groupUID).booleanValue());
			query.append(")");

			try (PreparedStatement stmnt = connection.prepareStatement(query.toString())) {
				stmnt.executeUpdate();
			} catch (Exception e) {
				throw new RuntimeException(e.getCause());
			}
		}
	}

	private void deleteNextOperationDeactivationMap(int userId) {
		final Connection connection = getTransactionManager().getConnection();

		final String query = "DELETE FROM home_automation.user_to_next_operation_deactivation WHERE user_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, userId);
			stmnt.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private Map<Integer, Boolean> getNextOperationDeactivationMap(int userId) throws SQLException {
		final String query = "SELECT * FROM home_automation.user_to_next_operation_deactivation WHERE user_id=?";
		final Connection connection = getTransactionManager().getConnection();
		final Map<Integer, Boolean> items = new HashMap<Integer, Boolean>();
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, userId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					final Integer groupUID = resultSet.getInt("group_id");
					final boolean next_operation_deactivation = resultSet.getBoolean("next_operation_deactivation");
					items.put(groupUID, new Boolean(next_operation_deactivation));
				}
			}
		}

		return items;
	}

	private void deleteUserPivotTableAssosiations(int userId) {
		final Connection connection = getTransactionManager().getConnection();

		final String query = "DELETE FROM home_automation.user_to_group WHERE user_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, userId);
			stmnt.executeUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private String getInsertIntoPivotTableStatement(String pivotTableName, String columnName, Integer userId,
			List<Integer> intemIds) {
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation." + pivotTableName + " (user_id, " + columnName + ") VALUES ");
		for (Integer itemId : intemIds) {
			sb.append("(").append(userId).append(",").append(itemId).append("),");
		}
		return sb.replace(sb.length() - 1, sb.length(), "").toString();
	}

	@Override
	public List<User> getAllForID(int parentId) throws SQLException {
		return null;
	}

	@Override
	public User get(int objectId) throws SQLException {
		User user = getCache().get(objectId);
		if (user != null) {
			return user;
		}

		String query = "SELECT * FROM home_automation._user WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					user = new User(objectId);
					getCache().addIfAbsent(objectId, user);

					fillWithData(user, resultSet);

				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return user;
	}

	@Override
	public Map<Integer, User> getAll() throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM home_automation._user");
		if(!getCache().isEmpty()) {
			sb.append(" WHERE id NOT IN(").append(getCache().getExistingIndexesString())
			.append(") ");
		}
		final Connection connection = getTransactionManager().getConnection();

		try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
				ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				final User user = new User( resultSet.getInt("id"));
				getCache().addIfAbsent(user.getUID(), user);
				fillWithData(user, resultSet);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return getCache().getAll();
	}

	@Override
	public void restoreObjectState(User user) throws SQLException {
		if (user == null || user.getUID() == null) {
			return;
		}
		
		final Connection connection = getTransactionManager().getConnection();
		
		String query = "SELECT * FROM home_automation._user WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, user.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(user, rs);
					
				} else {
					getCache().remove(user.getUID());
					return;
				}
			}
		} catch (Exception e) {
			getCache().remove(user.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}

	private void fillWithData(User user, ResultSet resultSet) throws SQLException {
		user.setUserId(resultSet.getInt("id"));
		user.setUsername(resultSet.getString("name"));
		user.setUserLabel(resultSet.getString("label"));
		user.setPasswordHash(resultSet.getString("password_hash"));
		user.setAuthenticationHash(resultSet.getString("authentication_hash"));
		user.setDuressAuthenticationHash(resultSet.getString("duress_authentication_hash"));
		user.setRole(resultSet.getInt("role"));
		user.setActive(resultSet.getBoolean("is_active"));
		user.setUserExpirationTime(resultSet.getLong("user_expiration_time"));
		user.setUserAudioDescription(resultSet.getString("user_audio_description"));
		user.setUserLanguage(resultSet.getString("user_language"));
		user.setDuressAlarm(resultSet.getBoolean("is_duress_alaram"));
		user.setDuressPasswordHash(resultSet.getString("duress_password_hash"));

		user.setGroups(getUserAggregationsFromPivotTable("user_to_group", "group_id", user.getUID()));

		final Map<Integer, Boolean> nextOperationDeactivationMap = getNextOperationDeactivationMap(user.getUID());
		for (Integer groupUID : nextOperationDeactivationMap.keySet()) {
			user.setNextOperationDeactivation(groupUID, nextOperationDeactivationMap.get(groupUID));
		}
	}
}