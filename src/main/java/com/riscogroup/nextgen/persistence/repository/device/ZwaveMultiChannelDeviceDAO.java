package com.riscogroup.nextgen.persistence.repository.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.device.type.ZwaveMultiChannelDevice;
import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class ZwaveMultiChannelDeviceDAO extends ZWaveDeviceDAO {
	private static final Logger logger = LoggerFactory.getLogger(ZwaveMultiChannelDevice.class);

	public boolean persist(ZwaveMultiChannelDevice object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device ")
		  .append("(name, device_class_name, is_zwave, version, vendor, type, protocol_id, zone_configuration, common_zone, ")
		  .append("exit_open, no_motion, internal, bypass_state, user_changed_bypass_status, force_arm, ")
		  
		  .append("lifeline_groups, command_classes, command_classes_exist, battery_operated, ")
		  .append("role_type, wake_up_interval, listening, end_points, manufacturer_id, product_type_id, ")
		  .append("product_id, ctrl_type, description, basic_device_class, generic_device_class, ")
		  .append("specific_device_class, mc_end_points_done, simple_association, end_point) ")
		  .append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		  
		PreparedStatement stmnt = conn.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setString(2, object.getClass().getName());
		stmnt.setBoolean(3, true);
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
		
		stmnt.setString(16, object.getLifelineGroup() != null ? object.getLifelineGroup().toString() : "");
		stmnt.setString(17, object.getCommandClasses() != null ? Arrays.asList(object.getCommandClasses()).toString() : "");
		stmnt.setBoolean(18, object.getCommandClassesExist());
		stmnt.setBoolean(19, object.isBatteryOperated());
		stmnt.setString(20, object.getRoleType() != null ? object.getRoleType() : "");
		stmnt.setInt(21, object.getWakeUpInterval() != null ? object.getWakeUpInterval() : -1);
		stmnt.setBoolean(22, object.isListening());
		stmnt.setInt(23, object.getEndPoints());
		stmnt.setInt(24, object.getManufacturerId() != null ? object.getManufacturerId() : -1);
		stmnt.setInt(25, object.getProductTypeId() != null ? object.getProductTypeId() : -1);
		stmnt.setInt(26, object.getProductId() != null ? object.getProductId() : -1);
		stmnt.setInt(27, object.getControllerType() != null ? object.getControllerType().getValue() : -1);
		stmnt.setString(28, object.getDescription() != null ? object.getDescription() : "");
		stmnt.setString(29, object.getBasicDeviceClass() != null ? object.getBasicDeviceClass() : "");
		stmnt.setString(30, object.getGenericDeviceClass() != null ? object.getGenericDeviceClass() : "");
		stmnt.setString(31, object.getSpecificDeviceClass() != null ? object.getSpecificDeviceClass() : "");
		stmnt.setBoolean(32, object.isMultiChannelEndPointsDone());
		stmnt.setBoolean(33, object.isSimpleChannelAssociationDone());
		stmnt.setInt(34, object.getEndPoint() != null ? object.getEndPoint() : -1);
		
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
	
	public boolean update(ZwaveMultiChannelDevice object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device ")
		  .append("SET name=?, device_class_name=?, version=?, vendor=?, type=?, protocol_id=?, ")
		  .append("zone_configuration=?, common_zone=?, exit_open=?, no_motion=?, internal=?, bypass_state=?, ")
		  .append("user_changed_bypass_status=?, force_arm=?, ")
		  
		  .append("lifeline_groups=?, command_classes=?, command_classes_exist=?, battery_operated=?, ")
		  .append("role_type=?, wake_up_interval=?, listening=?, end_points=?, manufacturer_id=?, product_type_id=?, ")
		  .append("product_id=?, ctrl_type=?, description=?, basic_device_class=?, generic_device_class=?, ")
		  .append("specific_device_class=?, mc_end_points_done=?, simple_association=?, end_point=? ")
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
		
		stmnt.setString(15, object.getLifelineGroup() != null ? object.getLifelineGroup().toString() : "");
		stmnt.setString(16, object.getCommandClasses() != null ? Arrays.asList(object.getCommandClasses()).toString() : "");
		stmnt.setBoolean(17, object.getCommandClassesExist());
		stmnt.setBoolean(18, object.isBatteryOperated());
		stmnt.setString(19, object.getRoleType() != null ? object.getRoleType() : "");
		stmnt.setInt(20, object.getWakeUpInterval() != null ? object.getWakeUpInterval() : -1);
		stmnt.setBoolean(21, object.isListening());
		stmnt.setInt(22, object.getEndPoints());
		stmnt.setInt(23, object.getManufacturerId() != null ? object.getManufacturerId() : -1);
		stmnt.setInt(24, object.getProductTypeId() != null ? object.getProductTypeId() : -1);
		stmnt.setInt(25, object.getProductId() != null ? object.getProductId() : -1);
		stmnt.setInt(26, object.getControllerType() != null ? object.getControllerType().getValue() : -1);
		stmnt.setString(27, object.getDescription() != null ? object.getDescription() : "");
		stmnt.setString(28, object.getBasicDeviceClass() != null ? object.getBasicDeviceClass() : "");
		stmnt.setString(29, object.getGenericDeviceClass() != null ? object.getGenericDeviceClass() : "");
		stmnt.setString(30, object.getSpecificDeviceClass() != null ? object.getSpecificDeviceClass() : "");
		stmnt.setBoolean(31, object.isMultiChannelEndPointsDone());
		stmnt.setBoolean(32, object.isSimpleChannelAssociationDone());
		stmnt.setInt(33, object.getEndPoint() != null ? object.getEndPoint() : -1);
		stmnt.setInt(34, object.getUID());
		
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
	
	protected void fillWithData(ZwaveMultiChannelDevice device, ResultSet rs) throws SQLException {
		device.setName(rs.getString("name"));
		device.setVersion(rs.getString("version"));
		device.setVendor(rs.getString("vendor"));
		device.setProtocolType(rs.getString("type"));
		device.setProtocolID(rs.getInt("protocol_id"));
		device.setZoneConfiguration(rs.getInt("zone_configuration"));
		device.setCommonZone(rs.getBoolean("common_zone"));
		device.setExitOpen(rs.getBoolean("exit_open"));
		device.setNoMotion(rs.getBoolean("no_motion"));
		device.setInternal(rs.getBoolean("internal"));
		device.setBypassState(rs.getInt("bypass_state"));
		device.setUserChangedBypassStatus(rs.getInt("user_changed_bypass_status"));
		device.setForceArm(rs.getBoolean("force_arm"));

		final List<Integer> lGroups = deserializeListFromToString(rs.getString("lifeline_groups")).stream()
				.map(v -> Integer.parseInt(v)).collect(Collectors.toList());
		device.setLifelineGroup(lGroups);
		device.setCommandClasses(deserializeArrayFromString(rs.getString("command_classes")));
		device.setCommandClassesExist(rs.getBoolean("command_classes_exist"));
		device.setBatteryOperated(rs.getBoolean("battery_operated"));
		device.setRoleType(rs.getString("role_type"));
		device.setWakeUpInterval(rs.getInt("wake_up_interval"));
		device.setListening(rs.getBoolean("listening"));
		device.setEndPoints(rs.getInt("end_points"));
		device.setManufacturerId(rs.getInt("manufacturer_id"));
		device.setProductTypeId(rs.getInt("product_type_id"));
		device.setProductId(rs.getInt("product_id"));
		device.setControllerType(rs.getInt("ctrl_type"));
		device.setDescription(rs.getString("description"));
		device.setBasicDeviceClass(rs.getString("basic_device_class"));
		device.setGenericDeviceClass(rs.getString("generic_device_class"));
		device.setSpecificDeviceClass(rs.getString("specific_device_class"));
		device.setMultiChannelEndPointsDone(rs.getBoolean("mc_end_points_done"));
		device.setSimpleAssociationDone(rs.getBoolean("simple_association"));
		device.setEndPoint(rs.getInt("end_point"));

		device.removeFunctions();
		device.removeChildren();

		final DataAccessObject<DeviceFunction> dao = DaoRegistry.getInstance().getDAO(DeviceFunction.class.getName());
		final List<DeviceFunction> functions = dao.getAllForID(device.getUID());
		device.addFunctions(functions);

		String query = "SELECT child_device_id FROM home_automation.device_to_child_device WHERE device_id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, device.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				device.removeChildren();
				while (resultSet.next()) {
					device.setChild( get(resultSet.getInt("child_device_id")));
				}
			}
		} catch (Exception e) {
			getCache().remove(device.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
	}
}
