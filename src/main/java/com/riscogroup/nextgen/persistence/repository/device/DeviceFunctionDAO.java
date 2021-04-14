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

import com.riscogroup.nextgen.home.api.generic.Device;
import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class DeviceFunctionDAO extends DataAccessObject<DeviceFunction> {
	private static final Logger logger = LoggerFactory.getLogger(DeviceFunctionDAO.class);

	@Override
	public boolean persist(DeviceFunction object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device_function ")
		  .append("(name, device_id, device_class_name, end_point_id, command_name, is_processed) ")
		  .append("VALUES (?,?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setInt(2, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(4, object.getEndPointId() != null ? object.getEndPointId() : -1);
		stmnt.setString(5, object.getCommandName() != null ? object.getCommandName() : "");
		stmnt.setBoolean(6, object.isProcessed() != null ? object.isProcessed() : false);
		
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
	public boolean update(DeviceFunction object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device_function ")
		  .append("SET name=?, device_id=?, device_class_name=?, end_point_id=?, command_name=?, is_processed=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = conn.prepareStatement(sb.toString());
		stmnt.setString(1, object.getName() != null ? object.getName() : "");
		stmnt.setInt(2, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(4, object.getEndPointId() != null ? object.getEndPointId() : -1);
		stmnt.setString(5, object.getCommandName() != null ? object.getCommandName() : "");
		stmnt.setBoolean(6, object.isProcessed() != null ? object.isProcessed() : false);
		stmnt.setInt(7, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<DeviceFunction> getAllForID(int parentId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final List<DeviceFunction> functions = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.device_function WHERE device_id=?";
		
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						functions.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final DeviceFunction function = new DeviceFunction(resultSet.getInt("id"));
					getCache().addIfAbsent(function.getUID(), function);
					functions.add(function);
					
					fillWithData(function, resultSet);
				}
			}
		} catch (SQLException e) {
			for(DeviceFunction f : functions) {
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
		if(getCache().get(objectId) == null) {
			return true;
		}
		for(DeviceProperty property : getCache().get(objectId).getDeviceProperties()) {
			DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName()).delete(property.getUID());
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(DeviceFunction obj) throws SQLException {
		for(DeviceProperty property : obj.getDeviceProperties()) {
			DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName()).delete(property);
		}
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}
	
	private void fillWithData(DeviceFunction function, ResultSet resultSet) throws SQLException {
		function.setName(resultSet.getString("name"));
		final Device device = (Device) DaoRegistry.getInstance().getDAO(resultSet.getString("device_class_name")).get(resultSet.getInt("device_id"));
		function.setParentDevice(device);
		final DataAccessObject<DeviceProperty> dao = DaoRegistry.getInstance().getDAO(DeviceProperty.class.getName());
		final List<DeviceProperty> props = dao.getAllForID(function.getUID());
		function.setDeviceProperties(props);
		function.setEndPointId( resultSet.getInt("end_point_id"));
		function.setCommandName( resultSet.getString("command_name"));
		function.setIsProcessed( resultSet.getBoolean("is_processed"));
	}

	@Override
	public DeviceFunction get(int objectId) throws SQLException {
		DeviceFunction function = getCache().get(objectId);
		if(function != null) {
			return function;
		}

		String query = "SELECT * FROM home_automation.device_function WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					function = new DeviceFunction(objectId);
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

	@Override
	public Map<Integer, DeviceFunction> getAll() throws SQLException {
		return null;
	}

	@Override
	public void restoreObjectState(DeviceFunction function) throws SQLException {
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
}
