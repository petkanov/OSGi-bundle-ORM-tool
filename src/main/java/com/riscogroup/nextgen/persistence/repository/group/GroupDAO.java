package com.riscogroup.nextgen.persistence.repository.group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.generic.ArmState;
import com.riscogroup.nextgen.home.api.generic.DeviceTroublesReportRecord;
import com.riscogroup.nextgen.home.api.generic.Group;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class GroupDAO extends DataAccessObject<Group>{
	private static final Logger logger = LoggerFactory.getLogger(GroupDAO.class);
	
	@Override
	public boolean persist(Group group) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation._group ")
				.append("(name, arm_state, group_locked_out) ")
				.append("VALUES (?,?,?)");

		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, group.getName() != null ? group.getName() : "");
		stmnt.setInt(2, group.getGroupState().getArmState().getValue());
		stmnt.setBoolean(3, group.getGroupLockedOut() != null ? group.getGroupLockedOut() : false);
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int lastInsertedGroupId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		group.setUID(lastInsertedGroupId);
		
		if(group.getDevices() != null && group.getDevices().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.group_to_device (group_id, device_id) VALUES ");
			for (Integer deviceId : group.getDevices()) {
				sb.append("(").append(lastInsertedGroupId).append(",").append(deviceId).append("),");
			}
			final String query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}

		if (group.getDevicesTroublesReportRecordList() != null && group.getDevicesTroublesReportRecordList().size() > 0) {
			for(DeviceTroublesReportRecord t : group.getDevicesTroublesReportRecordList()) {
				DaoRegistry.getInstance().getDAO(DeviceTroublesReportRecord.class.getName()).persist(t);
			}
			final String query = getInsertIntoPivotTableStatement("group_to_device_trouble_report_record", "device_trouble_report_record_id", group.getUID(),
					group.getDevicesTroublesReportRecordList().stream().map(v->v.getUID()).collect(Collectors.toList()));
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}
		return true;
	}
	
	private String getInsertIntoPivotTableStatement(String pivotTableName, String columnName, Integer userId, List<Integer> intemIds) {
		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation." + pivotTableName + " (group_id, " + columnName + ") VALUES ");
		for (Integer itemId : intemIds) {
			sb.append("(").append(userId).append(",").append(itemId).append("),");
		}
		return sb.replace(sb.length() - 1, sb.length(), "").toString();
	}

	@Override
	public boolean update(Group object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();
		
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation._group SET ")
		  .append("name=?, arm_state=?, group_locked_out=? ")
		  .append("WHERE id=?");

		try (PreparedStatement stmnt = conn.prepareStatement(sb.toString())){
			stmnt.setString(1, object.getName() != null ? object.getName() : "");
			stmnt.setInt(2, object.getGroupState().getArmState().getValue());
			stmnt.setBoolean(3, object.getGroupLockedOut() != null ? object.getGroupLockedOut() : false);
			stmnt.setInt(4, object.getUID());
			stmnt.executeUpdate();
		}

		String query = "DELETE FROM home_automation.group_to_device WHERE group_id=?";
		try (PreparedStatement stmnt = conn.prepareStatement(query)) {
			stmnt.setInt(1, object.getUID());
			stmnt.executeUpdate();
		}
		
		if(object.getDevices() != null && object.getDevices().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.group_to_device (group_id, device_id) VALUES ");
			for (Integer deviceId : object.getDevices()) {
				sb.append("(").append(object.getUID()).append(",").append(deviceId).append("),");
			}
			query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			
			try (PreparedStatement stmnt = conn.prepareStatement(query)) {
				stmnt.executeUpdate();
			}
		} 
		
		query = "DELETE FROM home_automation.group_to_device_trouble_report_record WHERE group_id=?";
		try (PreparedStatement stmnt = conn.prepareStatement(query)) {
			stmnt.setInt(1, object.getUID());
			stmnt.executeUpdate();
		}
		
		if(object.getDevicesTroublesReportRecordList() != null && object.getDevicesTroublesReportRecordList().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.group_to_device_trouble_report_record (group_id, device_trouble_report_record_id) VALUES ");
			for (DeviceTroublesReportRecord record : object.getDevicesTroublesReportRecordList()) {
				sb.append("(").append(object.getUID()).append(",").append(record.getUID()).append("),");
			}
			query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			
			try (PreparedStatement stmnt = conn.prepareStatement(query)) {
				stmnt.executeUpdate();
			}
		} 
		return true;
	}

	@Override
	public Group get(int objectId) throws SQLException {
		Group group = getCache().addIfAbsent(objectId, new Group(objectId));
		if(group != null) {
			return group;
		}
		group = getCache().get(objectId);
		initializeGroupWithDatabaseData(group);
		
		return group;
	}

	@Override
	public void restoreObjectState(Group object) throws SQLException {
		initializeGroupWithDatabaseData(object);
	}
	
	private void initializeGroupWithDatabaseData(Group group) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final String query = "SELECT * FROM home_automation._group WHERE id=?";
		try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
			preparedStatement.setInt(1, group.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				if (resultSet.next()) {
					setGroupDataFromResultSet(resultSet, group);
				} else {
					logger.error("Group {} does not exist", group.getUID());
					throw new SQLException();
				}
			}
		} catch (SQLException e) {
			getCache().remove(group.getUID());
			throw new SQLException(e);
		}
		setGroupDeviceAggregations(group);
		setGroupDeviceTroublesReportRecords(group);
	}

	@Override
	public Map<Integer, Group> getAll() throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM home_automation._group");
		if(!getCache().isEmpty()) {
			sb.append(" WHERE id NOT IN(").append(getCache().getExistingIndexesString()).append(")");
		}
		final List<Group> groups = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
				ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				final Group group = new Group();
				setGroupDataFromResultSet(resultSet, group);
				groups.add(group);
			}
		} 
		for (Group group : groups) {
			setGroupDeviceAggregations(group);
			setGroupDeviceTroublesReportRecords(group);
			
			getCache().addIfAbsent(group.getUID(), group);
		}
		return getCache().getAll();
	}
	
	private void setGroupDeviceTroublesReportRecords(Group group) throws SQLException {
		final List<DeviceTroublesReportRecord> troubles = new ArrayList<>();
		for(Object troubleObject : DaoRegistry.getInstance().getDAO(DeviceTroublesReportRecord.class.getName()).getAllForID(group.getUID())) {
			troubles.add((DeviceTroublesReportRecord) troubleObject);
		}
		group.setDevicesTroublesReportRecords(troubles);
	}
	
	private void setGroupDeviceAggregations(Group group) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final String query = "SELECT * FROM home_automation.group_to_device WHERE group_id=?";
		final Set<Integer> items = new HashSet<>();
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, group.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					items.add(resultSet.getInt("device_id"));
				}
			}
		}
		group.setDevices(items);
	}

	private void setGroupDataFromResultSet(ResultSet resultSet, Group group) throws SQLException {
		group.setUID(resultSet.getInt("id"));
		group.setName(resultSet.getString("name"));
		group.setArmState(ArmState.valueOf(resultSet.getInt("arm_state")));
		group.setGroupLockedOut(resultSet.getBoolean("group_locked_out"));
	}

	@Override
	public boolean delete(Integer groupId) throws SQLException {
		final Group group = get(groupId);
		
		final Connection connection = getTransactionManager().getConnection();
		String query = "DELETE FROM home_automation._group WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, groupId);
			stmnt.executeUpdate();
		}
		query = "DELETE FROM home_automation.group_to_device WHERE group_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, groupId);
			stmnt.executeUpdate();
		}
		query = "DELETE FROM home_automation.user_to_group WHERE group_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, groupId);
			stmnt.executeUpdate();
		}
		if(group.getDevicesTroublesReportRecordList() != null) {
			for(DeviceTroublesReportRecord record : group.getDevicesTroublesReportRecordList()) {
				DaoRegistry.getInstance().getDAO(DeviceTroublesReportRecord.class.getName()).delete(record.getUID());
			}
		}
		getCache().remove(groupId);
		return true;
	}

	@Override
	public List<Group> getAllForID(int parentId) throws SQLException {
		return null;
	}

	@Override
	public boolean delete(Group obj) throws SQLException {
		return delete(obj.getUID());
	}
}
