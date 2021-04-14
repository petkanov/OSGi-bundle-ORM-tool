package com.riscogroup.nextgen.persistence.repository.group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.generic.DeviceTroublesReportRecord;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class DeviceTroubleReportDAO extends DataAccessObject<DeviceTroublesReportRecord> {
	private static final Logger logger = LoggerFactory.getLogger(DeviceTroubleReportDAO.class);
	
	@Override
	public boolean persist(DeviceTroublesReportRecord object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device_trouble_report_record ")
		  .append("(device_id, restored_during_delay, troubleReportedMap,")
		  .append(" restoreTroubleTypeToBeConfirmedMap, restoreAlarmTypeToBeConfirmedMap) ")
		  .append("VALUES (?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setInt(1, object.getDeviceID() != null ? object.getDeviceID() : -1);
		stmnt.setBoolean(2, object.getRestoredDuringDelay() != null ? object.getRestoredDuringDelay() : true);
		stmnt.setString(3, object.getTroubleReportedMap().toString() );
		stmnt.setString(4, object.getRestoreTroubleTypeToBeConfirmedMap().toString() );
		stmnt.setString(5, object.getRestoreAlarmTypeToBeConfirmedMap().toString() );
		
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int deviceTroubleReportRecordId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(deviceTroubleReportRecordId);

		return true;
	}

	@Override
	public boolean update(DeviceTroublesReportRecord object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device_trouble_report_record SET ")
				.append("device_id=?, restored_during_delay=?, troubleReportedMap=?,")
				.append(" restoreTroubleTypeToBeConfirmedMap=?, restoreAlarmTypeToBeConfirmedMap=? ")
				.append("WHERE id=?");

		PreparedStatement stmnt = conn.prepareStatement(sb.toString());
		stmnt.setInt(1, object.getDeviceID() != null ? object.getDeviceID() : -1);
		stmnt.setBoolean(2, object.getRestoredDuringDelay() != null ? object.getRestoredDuringDelay() : true);
		stmnt.setString(3, object.getTroubleReportedMap().toString() );
		stmnt.setString(4, object.getRestoreTroubleTypeToBeConfirmedMap().toString() );
		stmnt.setString(5, object.getRestoreAlarmTypeToBeConfirmedMap().toString() );
		stmnt.setInt(6, object.getUID());
		stmnt.executeUpdate();
		stmnt.close();

		return true;
	}

	@Override
	public DeviceTroublesReportRecord get(int objectId) throws SQLException {
		final DeviceTroublesReportRecord record = new DeviceTroublesReportRecord(objectId);
		
		initializeObjectWithDatabaseData(record);

		return record;
	}
	
	private void initializeObjectWithDatabaseData(DeviceTroublesReportRecord record) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();

		String query = "SELECT * FROM home_automation.device_trouble_report_record WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, record.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				if (resultSet.next()) {
					setDataFromResultSet(resultSet, record);
				} else {
					logger.error("DeviceTroublesReportRecord {} does not exist", record.getUID());
					throw new SQLException();
				}
			}
		}
	}

	@Override
	public void restoreObjectState(DeviceTroublesReportRecord object) throws SQLException {
		initializeObjectWithDatabaseData(object);
	}

	private void setDataFromResultSet(ResultSet resultSet, DeviceTroublesReportRecord record) throws SQLException {
		record.setDeviceID(resultSet.getInt("device_id"));
		record.setRestoredDuringDelay(resultSet.getBoolean("restored_during_delay"));
		final Map<String, String> trm = deserializeMapFromToString(resultSet.getString("troubleReportedMap"));
		final Map<String, String> trc = deserializeMapFromToString(resultSet.getString("restoreTroubleTypeToBeConfirmedMap"));
		final Map<String, String> rat = deserializeMapFromToString(resultSet.getString("restoreAlarmTypeToBeConfirmedMap"));
		record.setTroubleReportedMap( (Map<String, Boolean>)trm.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v->Boolean.parseBoolean(v.getValue()))));
		record.setRestoreTroubleTypeToBeConfirmedMap( (Map<String, Boolean>)trc.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v->Boolean.parseBoolean(v.getValue()))));
		record.setRestoreAlarmTypeToBeConfirmedMap( (Map<String, Boolean>)rat.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v->Boolean.parseBoolean(v.getValue()))));
	}

	@Override
	public Map<Integer, DeviceTroublesReportRecord> getAll() throws SQLException {
		return null;
	}

	@Override
	public List<DeviceTroublesReportRecord> getAllForID(int parentId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final List<DeviceTroublesReportRecord> records = new ArrayList<>();

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM home_automation.device_trouble_report_record AS dtrr ")
		  .append("INNER JOIN home_automation.group_to_device_trouble_report_record AS gdtrr ")
		  .append("ON dtrr.id=gdtrr.device_trouble_report_record_id ")
		  .append("WHERE gdtrr.group_id=?");
		
		try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					final DeviceTroublesReportRecord record = new DeviceTroublesReportRecord(resultSet.getInt("id"));
					setDataFromResultSet(resultSet, record);
					records.add(record);
				}
			}
		}
		return records;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		String query = "DELETE FROM home_automation.device_trouble_report_record WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		query = "DELETE FROM home_automation.group_to_device_trouble_report_record WHERE device_trouble_report_record_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		return true;
	}

	@Override
	public boolean delete(DeviceTroublesReportRecord obj) throws SQLException {
		return delete(obj.getUID());
	}
}
