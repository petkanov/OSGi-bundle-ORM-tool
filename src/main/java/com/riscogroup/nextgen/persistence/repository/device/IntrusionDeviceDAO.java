package com.riscogroup.nextgen.persistence.repository.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.device.type.intrusion.IntrusionDevice;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;
import com.riscogroup.nextgen.persistence.core.IdentityMap;

public class IntrusionDeviceDAO extends DataAccessObject<IntrusionDevice> {
	private static final Logger logger = LoggerFactory.getLogger(IntrusionDeviceDAO.class);
	private static final IdentityMap<IntrusionDevice> intrusionCache = new IdentityMap<>();
	
	@Override
	protected IdentityMap<IntrusionDevice> getCache() {
		return intrusionCache;
	}
	
	protected void fillWithData(IntrusionDevice device, ResultSet resultSet) throws SQLException {
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
	
	public IntrusionDevice get(int objectId) throws SQLException {
		IntrusionDevice device = getCache().get(objectId);
		if(device != null) {
			return device;
		}

		String query = "SELECT * FROM home_automation.device WHERE id=? AND is_zwave=false";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					device = (IntrusionDevice) Class.forName(resultSet.getString("device_class_name")).getConstructor(Integer.class).newInstance(objectId);
					
					getCache().addIfAbsent(objectId, device);
					
					fillWithData(device, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return device;
	}
	
	public Map<Integer, IntrusionDevice> getAll() throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM home_automation.").append("device");
		if(!getCache().isEmpty()) {
			sb.append(" WHERE id NOT IN(").append(getCache().getExistingIndexesString())
			.append(") AND is_zwave=false");
		} else {
			sb.append(" WHERE is_zwave=false");
		}
		final Connection connection = getTransactionManager().getConnection();

		try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
				ResultSet resultSet = preparedStatement.executeQuery()) {
			while (resultSet.next()) {
				IntrusionDevice device = (IntrusionDevice) Class.forName(resultSet.getString("device_class_name")).getConstructor(Integer.class).newInstance(resultSet.getInt("id"));
				getCache().addIfAbsent(device.getUID(), device);
				
				fillWithData(device, resultSet);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return getCache().getAll();
	}
	
	@Override
	public boolean persist(IntrusionDevice object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device ")
		  .append("(name, device_class_name, is_zwave, version, vendor, type, protocol_id, zone_configuration, common_zone, ")
		  .append("exit_open, no_motion, internal, bypass_state, user_changed_bypass_status, force_arm, entry_delay, ")
		  .append("exit_delay) ")
		  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setString(2, object.getClass().getName());
		stmnt.setBoolean(3, false);
		stmnt.setString(4, object.getVersion() != null ? object.getVersion() : "");
		stmnt.setString(5, object.getVendor() != null ? object.getVendor() : "");
		stmnt.setString(6, object.getProtocolType() != null ? object.getProtocolType() : "");
		stmnt.setInt(7, object.getProtocolID() != null ? object.getProtocolID() : -1);
		stmnt.setInt(8, object.getZoneConfiguration() != null ? object.getZoneConfiguration() : -1);
		stmnt.setBoolean(9, object.getCommonZone() != null ? object.getCommonZone() : true);
		stmnt.setBoolean(10, object.getExitOpen() != null ? object.getExitOpen() : false);
		stmnt.setBoolean(11, object.getNoMotion() != null ? object.getNoMotion() : false);
		stmnt.setBoolean(12, object.isInternal() != null ? object.isInternal() : false);
		stmnt.setInt(13, object.getBypassState() != null ? object.getBypassState().getValue() : -1);
		stmnt.setInt(14, object.getUserChangedBypassStatus() != null ? object.getUserChangedBypassStatus() : -1);
		stmnt.setBoolean(15, object.getForceArm() != null ? object.getForceArm() : false);
		stmnt.setLong(16, object.getEntryDelay() != null ? object.getEntryDelay() : -1);
		stmnt.setLong(17, object.getExitDelay() != null ? object.getExitDelay() : -1);
		
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int lastInsertedDeviceId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(lastInsertedDeviceId);

		if(object.getChildren() != null && object.getChildren().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.device_to_child_device (device_id, child_device_id) VALUES ");
			for (Device device : object.getChildren()) {
				sb.append("(").append(lastInsertedDeviceId).append(",").append(device.getUID()).append("),");
			}
			final String query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}
		return true;
	}

	@Override
	public boolean update(IntrusionDevice object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device ")
		  .append("SET name=?, device_class_name=?, version=?, vendor=?, type=?, protocol_id=?, ")
		  .append("zone_configuration=?, common_zone=?, exit_open=?, no_motion=?, internal=?, bypass_state=?, ")
		  .append("user_changed_bypass_status=?, force_arm=?, entry_delay=?, exit_delay=? ")
		  .append("WHERE id=?");
		
		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setString(2, object.getClass().getName());
		stmnt.setString(3, object.getVersion() != null ? object.getVersion() : "");
		stmnt.setString(4, object.getVendor() != null ? object.getVendor() : "");
		stmnt.setString(5, object.getProtocolType() != null ? object.getProtocolType() : "");
		stmnt.setInt(6, object.getProtocolID() != null ? object.getProtocolID() : -1);
		stmnt.setInt(7, object.getZoneConfiguration() != null ? object.getZoneConfiguration() : -1);
		stmnt.setBoolean(8, object.getCommonZone() != null ? object.getCommonZone() : true);
		stmnt.setBoolean(9, object.getExitOpen() != null ? object.getExitOpen() : false);
		stmnt.setBoolean(10, object.getNoMotion() != null ? object.getNoMotion() : false);
		stmnt.setBoolean(11, object.isInternal() != null ? object.isInternal() : false);
		stmnt.setInt(12, object.getBypassState() != null ? object.getBypassState().getValue() : -1);
		stmnt.setInt(13, object.getUserChangedBypassStatus() != null ? object.getUserChangedBypassStatus() : -1);
		stmnt.setBoolean(14, object.getForceArm() != null ? object.getForceArm() : false);
		stmnt.setLong(15, object.getEntryDelay() != null ? object.getEntryDelay() : -1);
		stmnt.setLong(16, object.getExitDelay() != null ? object.getExitDelay() : -1);
		stmnt.setInt(17, object.getUID());
		
		stmnt.executeUpdate();
		stmnt.close();

		stmnt = conn.prepareStatement("DELETE FROM home_automation.device_to_child_device WHERE device_id=?");
		stmnt.setInt(1, object.getUID());
		stmnt.executeUpdate();
		stmnt.close();
		
		if(object.getChildren() != null && object.getChildren().size() > 0) {
			sb = new StringBuilder();
			sb.append("INSERT INTO home_automation.device_to_child_device (device_id, child_device_id) VALUES ");
			for (Device device : object.getChildren()) {
				sb.append("(").append(object.getUID()).append(",").append(device.getUID()).append("),");
			}
			final String query = sb.replace(sb.length() - 1, sb.length(), "").toString();
			stmnt = conn.prepareStatement(query);
			stmnt.executeUpdate();
			stmnt.close();
		}
		getCache().remove(object.getUID());
		return true;
	}

	@Override
	public List<IntrusionDevice> getAllForID(int parentId) throws SQLException {
		return null;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final IntrusionDevice device = get(objectId);
		if(device == null) {
			return true;
		}
		final Connection connection = getTransactionManager().getConnection();
		String query = "DELETE FROM home_automation.device WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		
		query = "SELECT child_device_id FROM home_automation.device_to_child_device WHERE device_id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					final Integer deviceId = resultSet.getInt("child_device_id");
					delete(deviceId);
				}
			}
		} 
		
		query = "DELETE FROM home_automation.device_to_child_device WHERE device_id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		
		if(device.getFunctions() != null) {
			for(DeviceFunction function : device.getFunctions()) {
				DaoRegistry.getInstance().getDAO(DeviceFunction.class.getName()).delete(function.getUID());
			}
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(IntrusionDevice obj) throws SQLException {
		return delete(obj.getUID());
	}
	
	@Override
	public void restoreObjectState(IntrusionDevice device) throws SQLException {
		if(device == null || device.getUID() == null) {
			return;
		}
		
		final Connection connection = getTransactionManager().getConnection();
		
		String query = "SELECT * FROM home_automation.device WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, device.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					
					fillWithData(device, resultSet);
					
				} else {
					getCache().remove(device.getUID());
					return;
				}
			}
		} catch (Exception e) {
			getCache().remove(device.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
	}
}
