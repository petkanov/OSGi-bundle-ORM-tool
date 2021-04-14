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

import com.riscogroup.nextgen.home.api.generic.DeviceFunction;
import com.riscogroup.nextgen.home.api.generic.DeviceProperty;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class DevicePropertyDAO extends DataAccessObject<DeviceProperty> {
	private static final Logger logger = LoggerFactory.getLogger(DevicePropertyDAO.class);

	@Override
	public boolean persist(DeviceProperty object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.device_property ")
		  .append("(device_function_id,device_function_class_name, prop_index, properties_map, enums_list, value, value_array, end_point_id, persist_flag) ")
		  .append("VALUES (?,?,?,?,?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setInt(1, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(2, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(3, object.getIndex() != null ? object.getIndex() : -1);
		stmnt.setString(4, object.getProperties() != null ? object.getProperties().toString() : "");
		stmnt.setString(5, object.getEnums() != null ? object.getEnums().toString() : "");
		if(object.getValue() != null) {
			if(object.getValue() instanceof List) {
				stmnt.setString(6, "");
				stmnt.setString(7, object.getValue().toString());
			} else {
				stmnt.setString(6, object.getValue().toString());
				stmnt.setString(7, "");
			}
		} else {
			stmnt.setString(6, "");
			stmnt.setString(7, "");
		}
		stmnt.setInt(8, object.getEndPointID() != null ? object.getEndPointID() : -1);
		stmnt.setBoolean(9, object.isPersistFlag() != null ? object.isPersistFlag() : false);
		
		stmnt.executeUpdate();

		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next();
		final int devicePropertyId = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(devicePropertyId);
		return true;
	}

	@Override
	public boolean update(DeviceProperty object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.device_property ")
		  .append("SET device_function_id=?,device_function_class_name=?, prop_index=?, properties_map=?, enums_list=?, value=?, ")
		  .append("value_array=?, end_point_id=?, persist_flag=? ")
		  .append("WHERE id=?");
		String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query);
		stmnt.setInt(1, object.getParent() != null ? object.getParent().getUID() : -1);
		stmnt.setString(2, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setInt(3, object.getIndex() != null ? object.getIndex() : -1);
		stmnt.setString(4, object.getProperties() != null ? object.getProperties().toString() : "");
		stmnt.setString(5, object.getEnums() != null ? object.getEnums().toString() : "");
		if(object.getValue() != null) {
			if(object.getValue() instanceof List) {
				stmnt.setString(6, "");
				stmnt.setString(7, object.getValue().toString());
			} else {
				stmnt.setString(6, object.getValue().toString());
				stmnt.setString(7, "");
			}
		} else {
			stmnt.setString(6, "");
			stmnt.setString(7, "");
		}
		stmnt.setInt(8, object.getEndPointID() != null ? object.getEndPointID() : -1);
		stmnt.setBoolean(9, object.isPersistFlag() != null ? object.isPersistFlag() : false);
		stmnt.setInt(10, object.getUID());
		stmnt.executeUpdate();
		
		return true;
	}

	@Override
	public Map<Integer, DeviceProperty> getAll() throws SQLException {
		return null;
	}
	
	@Override
	public List<DeviceProperty> getAllForID(int parentId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final List<DeviceProperty> properties = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.device_property WHERE device_function_id=?";
		
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						properties.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final DeviceProperty property = new DeviceProperty(resultSet.getInt("id"));
					getCache().addIfAbsent(property.getUID(), property);
					properties.add(property);

					fillWithData(property, resultSet);
				}
			}
		} catch (SQLException e) {
			for(DeviceProperty p : properties) {
				getCache().remove(p.getUID());
			}
			throw new SQLException(e);
		}
		return properties;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final Connection connection = getTransactionManager().getConnection();
		final String query = "DELETE FROM home_automation.device_property WHERE id=?";
		try (PreparedStatement stmnt = connection.prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(DeviceProperty obj) throws SQLException {
		return delete(obj.getUID());
	}
	
	private void fillWithData(DeviceProperty property, ResultSet resultSet) throws SQLException {
		final DataAccessObject<?> functionDAO = DaoRegistry.getInstance().getDAO(resultSet.getString("device_function_class_name"));
		final DeviceFunction function = (DeviceFunction) functionDAO.get(resultSet.getInt("device_function_id"));
		property.setParentDeviceFunction(function);
		property.setIndex(resultSet.getInt("prop_index"));
		property.setProperties( deserializeMapFromToString( resultSet.getString("properties_map")));
		property.setEnum( deserializeListFromToString( resultSet.getString("enums_list")));
		
		String value = resultSet.getString("value");
		if(value.length() > 0) {
			property.setValue(value);
		}else {
			value = resultSet.getString("value_array");
			property.setValue( deserializeListFromToString( value));
		}
		property.setEndPointID( resultSet.getInt("end_point_id"));
		property.setPersistFlag( resultSet.getBoolean("persist_flag"));
	}

	@Override
	public DeviceProperty get(int objectId) throws SQLException {
		DeviceProperty property = getCache().get(objectId);
		if(property != null) {
			return property;
		}

		String query = "SELECT * FROM home_automation.device_property WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					property = new DeviceProperty(objectId);
					getCache().addIfAbsent(objectId, property);
					
					fillWithData(property, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return property;
	}

	@Override
	public void restoreObjectState(DeviceProperty object) throws SQLException {
		if(object == null || object.getUID() == null) {
			return;
		}
		
		final Connection connection = getTransactionManager().getConnection();
		
		String query = "SELECT * FROM home_automation.device_property WHERE id=?";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setInt(1, object.getUID());
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					
					fillWithData(object, resultSet);
					
				} else {
					getCache().remove(object.getUID());
					return;
				}
			}
		} catch (Exception e) {
			getCache().remove(object.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
	}
}
