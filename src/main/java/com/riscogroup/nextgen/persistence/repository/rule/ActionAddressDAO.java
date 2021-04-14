package com.riscogroup.nextgen.persistence.repository.rule;

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

import com.riscogroup.nextgen.home.api.rule.data.ActionAddress;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.home.api.rule.data.RuleTrigger;
import com.riscogroup.nextgen.persistence.core.DaoRegistry;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class ActionAddressDAO extends DataAccessObject<ActionAddress> {
	private static final Logger logger = LoggerFactory.getLogger(ActionAddressDAO.class);

	@Override
	public boolean persist(ActionAddress object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.action_address ")
		  .append("(device_id, parent_id, parent_class, device_function, property_index, value, end_value ) ")
		  .append("VALUES (?,?,?,?,?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setInt(1, object.getDeviceUID() != null ? object.getDeviceUID() : -1);
		stmnt.setInt(2, object.getParent() != null ? getParentID(object.getParent()) : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setString(4, object.getDeviceFunction() != null ? object.getDeviceFunction() : "");
		stmnt.setInt(5, object.getPropertyIndex() != null ? object.getPropertyIndex() : -1);
		stmnt.setInt(6, object.getValue() != null ? (Integer) object.getValue() : -1);
		stmnt.setInt(7, object.getEndValue() != null ? (Integer) object.getEndValue() : -1);
		
		stmnt.executeUpdate();
		
		final ResultSet rs = stmnt.getGeneratedKeys();
		rs.next(); 
		final int id = rs.getInt(1);
		rs.close();
		stmnt.close();
		
		object.setUID(id);
		return true;
	}
	
	@Override
	public boolean update(ActionAddress object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.action_address ")
		  .append("SET device_id=?, parent_id=?, parent_class=?, device_function=?, property_index=?, value=?, end_value=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(sb.toString());
		stmnt.setInt(1, object.getDeviceUID() != null ? object.getDeviceUID() : -1);
		stmnt.setInt(2, object.getParent() != null ? getParentID(object.getParent()) : -1);
		stmnt.setString(3, object.getParent() != null ? object.getParent().getClass().getName() : "");
		stmnt.setString(4, object.getDeviceFunction() != null ? object.getDeviceFunction() : "");
		stmnt.setInt(5, object.getPropertyIndex() != null ? object.getPropertyIndex() : -1);
		stmnt.setInt(6, object.getValue() != null ? (Integer) object.getValue() : -1);
		stmnt.setInt(7, object.getEndValue() != null ? (Integer) object.getEndValue() : -1);
		
		stmnt.setInt(8, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<ActionAddress> getAllForID(int parentId) throws SQLException {
		final List<ActionAddress> addresses = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.action_address WHERE parent_id=?";
		
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						addresses.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final ActionAddress address = new ActionAddress(resultSet.getInt("id"));
					getCache().addIfAbsent(address.getUID(), address);
					addresses.add(address);
					
					fillWithData(address, resultSet);
				}
			}
		} catch (SQLException e) {
			for(ActionAddress a : addresses) {
				getCache().remove(a.getUID());
			}
			throw new SQLException(e);
		}
		return addresses;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final String query = "DELETE FROM home_automation.action_address WHERE id=?";
		try (PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(query)) {
			stmnt.setInt(1, objectId);
			stmnt.executeUpdate();
		}
		if(getCache().get(objectId) == null) {
			return true;
		}
		getCache().remove(objectId);
		return true;
	}

	@Override
	public boolean delete(ActionAddress obj) throws SQLException {
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}

	@Override
	public ActionAddress get(int objectId) throws SQLException {
		ActionAddress actionAddress = getCache().get(objectId);
		if(actionAddress != null) {
			return actionAddress;
		}

		String query = "SELECT * FROM home_automation.action_address WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					actionAddress = new ActionAddress(objectId);
					getCache().addIfAbsent(objectId, actionAddress);
					
					fillWithData(actionAddress, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return actionAddress;
	}

	private void fillWithData(ActionAddress address, ResultSet resultSet) throws SQLException {
		address.setDeviceUID(resultSet.getInt("device_id"));
		final Object parent = DaoRegistry.getInstance().getDAO(resultSet.getString("parent_class")).get(resultSet.getInt("parent_id"));
		address.setParent(parent);
		address.setDeviceFunction( resultSet.getString("device_function"));
		address.setPropertyIndex(resultSet.getInt("property_index"));
		address.setValue(resultSet.getInt("value"));
		address.setEndValue(resultSet.getInt("end_value"));
	}

	@Override
	public Map<Integer, ActionAddress> getAll() throws SQLException {
		return null;
	}

	@Override
	public void restoreObjectState(ActionAddress address) throws SQLException {
		if (address == null || address.getUID() == null) {
			return;
		}
		final String query = "SELECT * FROM home_automation.action_address WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, address.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(address, rs);
					
				} else {
					getCache().remove(address.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(address.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}
	
	private Integer getParentID(Object parent) {
		Integer id = -1;
		if(parent instanceof RuleTrigger) {
			id = ((RuleTrigger)parent).getUID();
		}
		if(parent instanceof Rule) {
			id = ((Rule)parent).getUID();
		}
		return id;
	}
}
