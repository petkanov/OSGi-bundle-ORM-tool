package com.riscogroup.nextgen.persistence.repository.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.riscogroup.nextgen.home.api.device.type.ZWaveDevice;
import com.riscogroup.nextgen.home.api.function.type.ZWaveDeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class ZWaveDeviceFunctionDAO extends DataAccessObject<ZWaveDeviceFunction> {
	private static final Logger logger = LoggerFactory.getLogger(ZWaveDeviceFunctionDAO.class);

	@Override
	public boolean persist(ZWaveDeviceFunction object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device_function ")
				.append("(name, device_id, device_class_name, end_point_id, command_name, is_processed, ")
				.append("ack_received, embedded_function, command_id, command_class_id, reason) ")
				.append("VALUES (?,?,?,?,?,?,?,?,?,?,?)");
		final String query = sb.toString();

		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setInt(2, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(4, object.getEndPointId() != null ? object.getEndPointId() : -1);
		stmnt.setString(5, object.getCommandName() != null ? object.getCommandName() : "");
		stmnt.setBoolean(6, object.isProcessed() != null ? object.isProcessed() : false);

		stmnt.setBoolean(7, object.getAckReceived());
		stmnt.setBoolean(8, object.isEmbeddedFunction());
		stmnt.setInt(9, object.getCommandId() != null ? object.getCommandId() : -1);
		stmnt.setString(10, object.getCommandClassId() != null ? object.getCommandClassId() : "");
		stmnt.setString(11, object.getReasonToFailed() != null ? object.getReasonToFailed() : "");

		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int deviceFunctionId = rs.getInt(1);
		rs.close();
		stmnt.close();

		object.setUID(deviceFunctionId);
		return true;
	}

	@Override
	public boolean update(ZWaveDeviceFunction object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device_function ").append(
				"SET name=?, device_id=?, device_class_name=?, end_point_id=?, command_name=?, is_processed=?, ")
				.append("ack_received=?, embedded_function=?, command_id=?, command_class_id=?, reason=? ").append("WHERE id=?");

		final PreparedStatement stmnt = conn.prepareStatement(sb.toString());
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setInt(2, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(4, object.getEndPointId() != null ? object.getEndPointId() : -1);
		stmnt.setString(5, object.getCommandName() != null ? object.getCommandName() : "");
		stmnt.setBoolean(6, object.isProcessed() != null ? object.isProcessed() : false);

		stmnt.setBoolean(7, object.getAckReceived());
		stmnt.setBoolean(8, object.isEmbeddedFunction());
		stmnt.setInt(9, object.getCommandId() != null ? object.getCommandId() : -1);
		stmnt.setString(10, object.getCommandClassId() != null ? object.getCommandClassId() : "");
		stmnt.setString(11, object.getReasonToFailed() != null ? object.getReasonToFailed() : "");
		stmnt.setInt(12, object.getUID());

		stmnt.executeUpdate();
		return true;
	}

	@Override
	public Map<Integer, ZWaveDeviceFunction> getAll() throws SQLException {
		return null;
	}

	@Override
	public List<ZWaveDeviceFunction> getAllForID(int parentId) throws SQLException {
		final List<ZWaveDeviceFunction> functions = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.device_function WHERE device_id=?";

		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if (getCache().contains(resultSet.getInt("id"))) {
						functions.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final ZWaveDeviceFunction function = new ZWaveDeviceFunction(resultSet.getInt("id"));
					getCache().addIfAbsent(function.getUID(), function);
					functions.add(function);

					fillWithData(function, resultSet);
				}
			}
		} catch (SQLException e) {
			for (ZWaveDeviceFunction f : functions) {
				getCache().remove(f.getUID());
			}
			throw new SQLException(e);
		}
		return functions;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final String query = "DELETE FROM home_automation.device_function WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		if (getCache().get(objectId) == null) {
			return true;
		}
		for (DeviceProperty property : getCache().get(objectId).getDeviceProperties()) {
			DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName()).delete(property.getUID());
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(ZWaveDeviceFunction obj) throws SQLException {
		for (DeviceProperty property : obj.getDeviceProperties()) {
			DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName()).delete(property);
		}
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}

	public void restoreObjectState(ZWaveDeviceFunction function) throws SQLException {
		if (function == null || function.getUID() == null) {
			return;
		}
		final Connection connection = getTransactionManager().getConnection();
		
		String query = "SELECT * FROM home_automation.device_function WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, function.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(function, rs);
					
				} else {
					getCache().remove(function.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(function.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}

	public ZWaveDeviceFunction get(int objectId) throws SQLException {
		ZWaveDeviceFunction function = getCache().get(objectId);
		if (function != null) {
			return function;
		}

		String query = "SELECT * FROM home_automation.device_function WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					function = new ZWaveDeviceFunction(objectId);
					getCache().addIfAbsent(objectId, function);

					fillWithData(function, resultSet);

				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return function;
	}
	
	private void fillWithData(ZWaveDeviceFunction function, ResultSet resultSet) throws SQLException {
		function.setName(resultSet.getString("name"));
		final ZWaveDevice device = (ZWaveDevice) DaoRegistry.getInstance().getDAO(ZWaveDevice.class.getName())
				.get(resultSet.getInt("device_id"));
		function.setParentDevice(device);
		final DataAccessObject<DeviceProperty> dao = DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName());
		final List<DeviceProperty> props = dao.getAllForID(function.getUID());
		function.setDeviceProperties(props);
		function.setEndPointId(resultSet.getInt("end_point_id"));
		function.setCommandName(resultSet.getString("command_name"));
		function.setIsProcessed(resultSet.getBoolean("is_processed"));

		function.setAckReceived(resultSet.getBoolean("ack_received"));
		function.setEmbeddedFunction(resultSet.getBoolean("embedded_function"));
		function.setCommandId(resultSet.getInt("command_id"));
		function.setCommandClassId(resultSet.getString("command_class_id"));
		function.setReasonToFailed(resultSet.getString("reason"));
	}
}
