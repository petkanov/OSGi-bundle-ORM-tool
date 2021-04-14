package com.riscogroup.nextgen.persistence.repository.rule;

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

import com.riscogroup.nextgen.home.api.rule.data.LocalAction;
import com.riscogroup.nextgen.home.api.rule.data.Rule;
import com.riscogroup.nextgen.persistence.core.DataAccessObject;

public class LocalActionDAO extends DataAccessObject<LocalAction> {
	private static final Logger logger = LoggerFactory.getLogger(LocalActionDAO.class);

	@Override
	public boolean persist(LocalAction object) throws SQLException {
		final Connection conn = getTransactionManager().getConnection();

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO home_automation.local_action ")
		  .append("(properties_map, parent_id, local_function_id ) ")
		  .append("VALUES (?,?,?)");
		final String query = sb.toString();
		
		PreparedStatement stmnt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		stmnt.setString(1, object.getProperties() != null ? object.getProperties().toString() : "");
		stmnt.setInt(2, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		stmnt.setInt(3, object.getFunctionID() != null ? object.getFunctionID() : -1);
		
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
	public boolean update(LocalAction object) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		sb.append("UPDATE home_automation.local_action ")
		  .append("SET properties_map=?, parent_id=?, local_function_id=? ")
		  .append("WHERE id=?");
		
		final PreparedStatement stmnt = getTransactionManager().getConnection().prepareStatement(sb.toString());
		stmnt.setString(1, object.getProperties() != null ? object.getProperties().toString() : "");
		stmnt.setInt(2, object.getParentRule() != null ? object.getParentRule().getUID() : -1);
		stmnt.setInt(3, object.getFunctionID() != null ? object.getFunctionID() : -1);
		
		stmnt.setInt(4, object.getUID());
		
		stmnt.executeUpdate();
		return true;
	}

	@Override
	public List<LocalAction> getAllForID(int parentId) throws SQLException {
		final List<LocalAction> addresses = new ArrayList<>();

		final String query = "SELECT * FROM home_automation.local_action WHERE parent_id=?";
		
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, parentId);
			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					if(getCache().contains(resultSet.getInt("id"))) {
						addresses.add(getCache().get(resultSet.getInt("id")));
						continue;
					}
					final LocalAction address = new LocalAction(resultSet.getInt("id"));
					getCache().addIfAbsent(address.getUID(), address);
					addresses.add(address);
					
					fillWithData(address, resultSet);
				}
			}
		} catch (SQLException e) {
			for(LocalAction a : addresses) {
				getCache().remove(a.getUID());
			}
			throw new SQLException(e);
		}
		return addresses;
	}

	@Override
	public boolean delete(Integer objectId) throws SQLException {
		final String query = "DELETE FROM home_automation.local_action WHERE id=?";
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
	public boolean delete(LocalAction obj) throws SQLException {
		getCache().remove(obj.getUID());
		return delete(obj.getUID());
	}

	@Override
	public LocalAction get(int objectId) throws SQLException {
		LocalAction action = getCache().get(objectId);
		if(action != null) {
			return action;
		}

		String query = "SELECT * FROM home_automation.local_action WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, objectId);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					action = new LocalAction(objectId);
					getCache().addIfAbsent(objectId, action);
					
					fillWithData(action, resultSet);
					
				} else {
					getCache().remove(objectId);
				}
			}
		} catch (Exception e) {
			getCache().remove(objectId);
			logger.error(e.getMessage());
			throw new SQLException(e);
		}
		return action;
	}

	@Override
	public Map<Integer, LocalAction> getAll() throws SQLException {
		return null;
	}

	@Override
	public void restoreObjectState(LocalAction action) throws SQLException {
		if (action == null || action.getUID() == null) {
			return;
		}
		final String query = "SELECT * FROM home_automation.local_action WHERE id=?";
		try (PreparedStatement preparedStatement = getTransactionManager().getConnection().prepareStatement(query)) {
			preparedStatement.setInt(1, action.getUID());
			try (ResultSet rs = preparedStatement.executeQuery()) {
				if (rs.next()) {
					
					fillWithData(action, rs);
					
				} else {
					getCache().remove(action.getUID());
				}
			}
		} catch (Exception e) {
			getCache().remove(action.getUID());
			logger.error(e.getMessage());
			throw new SQLException(e);
		} 
	}

	private void fillWithData(LocalAction action, ResultSet resultSet) throws SQLException {
		Map<Integer,String> props = deserializeMapFromToString(resultSet.getString("properties_map")).entrySet().stream()
                .collect(Collectors.toMap(e->Integer.parseInt(e.getKey()), Map.Entry::getValue ));
		action.setProperties(props);
		final Rule parent = new Rule(resultSet.getInt("parent_id")); //DaoRegistry.getInstance().getDAO(resultSet.getString("parent_class")).get(resultSet.getInt("parent_id"));
		action.setParentRule(parent);
		action.setFunctionID(resultSet.getInt("local_function_id"));
	}
}
