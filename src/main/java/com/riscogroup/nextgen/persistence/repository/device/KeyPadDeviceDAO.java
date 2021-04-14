package com.riscogroup.nextgen.persistence.repository.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.device.type.intrusion.KeyPadDevice;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class KeyPadDeviceDAO extends IntrusionDeviceDAO {
	private static final Logger logger = LoggerFactory.getLogger(KeyPadDeviceDAO.class);
	
	protected void fillWithData(KeyPadDevice device, ResultSet resultSet) throws SQLException {
		device.setName(resultSet.getString("name"));
		device.setVersion(resultSet.getString("version"));
		device.setVendor(resultSet.getString("vendor"));
		device.setProtocolType(resultSet.getString("type"));
		device.setProtocolID(resultSet.getInt("protocol_id"));
		device.setZoneConfiguration(resultSet.getInt("zone_configuration"));
		device.setCommonZone(resultSet.getBoolean("common_zone"));
		device.setExitOpen(resultSet.getBoolean("exit_open"));
		device.setNoMotion(resultSet.getBoolean("no_motion"));
		device.setInternal(resultSet.getBoolean("internal"));
		device.setBypassState(resultSet.getInt("bypass_state"));
		device.setUserChangedBypassStatus(resultSet.getInt("user_changed_bypass_status"));
		device.setForceArm(resultSet.getBoolean("force_arm"));
		device.setEntryDelay(resultSet.getLong("entry_delay"));
		device.setExitDelay(resultSet.getLong("exit_delay"));
		device.setKeyPadProximityPolicy(resultSet.getBoolean("key_pad_proximity_policy"));
		device.setKeyPadEmergencyKeyPolicy(resultSet.getBoolean("key_pad_emergency_key_policy"));
		device.setKeyPadBuzzerPolicy(resultSet.getBoolean("key_pad_buzzer_policy"));
		
		device.removeFunctions();
		device.removeChildren();
		
		final DataAccessObject<DeviceFunction> dao = DaoRegistry.getInstance().getDAO(DeviceFunction.class.getName());
		final List<DeviceFunction> functions = dao.getAllForID(device.getUID());
		device.addFunctions(functions);
		
		String query = "SELECT child_device_id FROM home_automation.device_to_child_device WHERE device_id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, device.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				while (rs.next()) {
					device.setChild( get(rs.getInt("child_device_id")));
				}
			}
		} catch (Exception e) {
			getCache().remove(device.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
	}

	public boolean persist(KeyPadDevice device) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();
		
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device ")
		  .append("(name, device_class_name, is_zwave, version, vendor, type, protocol_id, zone_configuration, common_zone, ")
		  .append("exit_open, no_motion, internal, bypass_state, user_changed_bypass_status, force_arm, entry_delay, ")
		  .append("exit_delay, key_pad_proximity_policy, key_pad_emergency_key_policy, key_pad_buzzer_policy) ")
		  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, device.getName() != null ? device.getName() : "");
		stmnt.setString(2, device.getClass().getName());
		stmnt.setBoolean(3, false);
		stmnt.setString(4, device.getVersion() != null ? device.getVersion() : "");
		stmnt.setString(5, device.getVendor() != null ? device.getVendor() : "");
		stmnt.setString(6, device.getProtocolType() != null ? device.getProtocolType() : "");
		stmnt.setInt(7, device.getProtocolID() != null ? device.getProtocolID() : -1);
		stmnt.setInt(8, device.getZoneConfiguration() != null ? device.getZoneConfiguration() : -1);
		stmnt.setBoolean(9, device.getCommonZone() != null ? device.getCommonZone() : true);
		stmnt.setBoolean(10, device.getExitOpen() != null ? device.getExitOpen() : false);
		stmnt.setBoolean(11, device.getNoMotion() != null ? device.getNoMotion() : false);
		stmnt.setBoolean(12, device.isInternal() != null ? device.isInternal() : false);
		stmnt.setInt(13, device.getBypassState() != null ? device.getBypassState().getValue() : -1);
		stmnt.setInt(14, device.getUserChangedBypassStatus() != null ? device.getUserChangedBypassStatus() : -1);
		stmnt.setBoolean(15, device.getForceArm() != null ? device.getForceArm() : false);
		stmnt.setLong(16, device.getEntryDelay() != null ? device.getEntryDelay() : -1);
		stmnt.setLong(17, device.getExitDelay() != null ? device.getExitDelay() : -1);
		stmnt.setBoolean(18, device.getKeyPadProximityPolicy() != null ? device.getKeyPadProximityPolicy() : false);
		stmnt.setBoolean(19, device.getKeyPadEmergencyKeyPolicy() != null ? device.getKeyPadEmergencyKeyPolicy() : false);
		stmnt.setBoolean(20, device.getKeyPadBuzzerPolicy() != null ? device.getKeyPadBuzzerPolicy() : false);
		
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int lastInsertedDeviceId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		device.setUID(lastInsertedDeviceId);

		if(device.getChildren() != null && device.getChildren().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.device_to_child_device (device_id, child_device_id) VALUES ");
			for (Device d : device.getChildren()) {
				sb.append("(").append(lastInsertedDeviceId).append(",").append(d.getUID()).append("),");
			}
			final String query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}
		return true;
	}

	public boolean update(KeyPadDevice device) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device ")
		  .append("SET name=?, device_class_name=?, version=?, vendor=?, type=?, protocol_id=?, ")
		  .append("zone_configuration=?, common_zone=?, exit_open=?, no_motion=?, internal=?, bypass_state=?, ")
		  .append("user_changed_bypass_status=?, force_arm=?, entry_delay=?, exit_delay=?, ")
		  .append("key_pad_proximity_policy=?, key_pad_emergency_key_policy=?, key_pad_buzzer_policy=? ")
		  .append("WHERE id=?");
		
		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, device.getName() != null ? device.getName() : "");
		stmnt.setString(2, device.getClass().getName());
		stmnt.setString(3, device.getVersion() != null ? device.getVersion() : "");
		stmnt.setString(4, device.getVendor() != null ? device.getVendor() : "");
		stmnt.setString(5, device.getProtocolType() != null ? device.getProtocolType() : "");
		stmnt.setInt(6, device.getProtocolID() != null ? device.getProtocolID() : -1);
		stmnt.setInt(7, device.getZoneConfiguration() != null ? device.getZoneConfiguration() : -1);
		stmnt.setBoolean(8, device.getCommonZone() != null ? device.getCommonZone() : true);
		stmnt.setBoolean(9, device.getExitOpen() != null ? device.getExitOpen() : false);
		stmnt.setBoolean(10, device.getNoMotion() != null ? device.getNoMotion() : false);
		stmnt.setBoolean(11, device.isInternal() != null ? device.isInternal() : false);
		stmnt.setInt(12, device.getBypassState() != null ? device.getBypassState().getValue() : -1);
		stmnt.setInt(13, device.getUserChangedBypassStatus() != null ? device.getUserChangedBypassStatus() : -1);
		stmnt.setBoolean(14, device.getForceArm() != null ? device.getForceArm() : false);
		stmnt.setLong(15, device.getEntryDelay() != null ? device.getEntryDelay() : -1);
		stmnt.setLong(16, device.getExitDelay() != null ? device.getExitDelay() : -1);
		stmnt.setBoolean(17, device.getKeyPadProximityPolicy() != null ? device.getKeyPadProximityPolicy() : false);
		stmnt.setBoolean(18, device.getKeyPadEmergencyKeyPolicy() != null ? device.getKeyPadEmergencyKeyPolicy() : false);
		stmnt.setBoolean(19, device.getKeyPadBuzzerPolicy() != null ? device.getKeyPadBuzzerPolicy() : false);
		stmnt.setInt(20, device.getUID());
		
		stmnt.executeUpdate();
		stmnt.close();

		stmnt = conn.prepareStatement("DELETE FROM home_automation.device_to_child_device WHERE device_id=?");
		stmnt.setInt(1, device.getUID());
		stmnt.executeUpdate();
		stmnt.close();
		
		if(device.getChildren() != null && device.getChildren().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.device_to_child_device (device_id, child_device_id) VALUES ");
			for (Device d : device.getChildren()) {
				sb.append("(").append(device.getUID()).append(",").append(d.getUID()).append("),");
			}
			final String query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}
		getCache().remove(device.getUID());
		return true;
	}  
}
